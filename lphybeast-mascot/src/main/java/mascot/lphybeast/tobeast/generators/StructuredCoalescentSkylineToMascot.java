package mascot.lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.evolution.alignment.Taxon;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.tree.TraitSet;
import beast.base.inference.parameter.BooleanParameter;
import beast.base.inference.parameter.CompoundRealParameter;
import beast.base.inference.parameter.RealParameter;
import beastlabs.util.BEASTVector;
import lphy.base.evolution.coalescent.StructuredCoalescentSkyline;
import lphy.base.evolution.tree.TimeTree;
import lphy.base.evolution.tree.TimeTreeNode;
import lphy.core.model.Value;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;
import lphybeast.tobeast.values.TimeTreeToBEAST;
import mascot.distribution.StructuredTreeIntervals;
import mascot.dynamics.RateShifts;
import mascot.dynamics.StructuredSkyline;
import mascot.lphybeast.tobeast.loggers.MascotExtraTreeLogger;
import mascot.parameterdynamics.NeDynamics;
import mascot.parameterdynamics.NeDynamicsList;
import mascot.parameterdynamics.Skygrowth;
import mascot.parameterdynamics.StructuredSkygrid;
import mascot.util.InitializedNeDynamicsList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Converts LPhy {@link StructuredCoalescentSkyline} to BEAST2 Mascot
 * {@link mascot.distribution.Mascot} with a {@link StructuredSkyline} dynamics
 * built from K per-deme {@link Skygrowth} Ne trajectories and time-constant
 * migration.
 *
 * <h2>LPhy-side inputs</h2>
 * <ul>
 *   <li>{@code logNe}: {@code Double[K][n]} — K demes in alphabetical order, n epochs,
 *       epoch 0 = most recent. Expected BEAST-side representation is a
 *       {@link BEASTVector} of K {@link RealParameter} (each of dimension n),
 *       produced by auto-vectorisation of e.g. {@code GaussianRandomWalk} over a
 *       {@code Normal(..., replicates=K)} firstValue.</li>
 *   <li>{@code M}: flat {@code Double[K*(K-1)]} — source-major migration layout.
 *       Either a single {@link RealParameter} of dim K*(K-1) or a {@link BEASTVector}
 *       of K*(K-1) scalar RealParameters; in the latter case we wrap the components
 *       in a {@link CompoundRealParameter} so Mascot sees a flat RealParameter while
 *       operators continue to act on the underlying scalars.</li>
 *   <li>{@code rateShifts}: {@code Double[n]} — start times of the n Ne epochs,
 *       strictly increasing from the present. A leading 0.0 (if present) is stripped
 *       before being passed to Mascot's {@link RateShifts} (which expects only the
 *       internal epoch boundaries, giving Ne.dim = rateShifts.dim + 1).</li>
 * </ul>
 */
public class StructuredCoalescentSkylineToMascot implements
        GeneratorToBEAST<StructuredCoalescentSkyline, mascot.distribution.Mascot> {

    @Override
    public mascot.distribution.Mascot generatorToBEAST(StructuredCoalescentSkyline coalescent,
                                                        BEASTInterface value,
                                                        BEASTContext context) {

        int nDemes = coalescent.getNDemes();
        int nEpochs = coalescent.getNEpochs();
        int nMigRates = nDemes * (nDemes - 1);
        List<String> uniqueDemes = coalescent.getUniqueDemes();
        boolean isLinear = coalescent.isLinearInterpolation();

        // === Per-deme logNe RealParameters (K of them, each dim n) ===
        List<RealParameter> logNePerDeme = extractPerDemeLogNe(coalescent, context);

        // === Flat migration RealParameter (dim K*(K-1)) ===
        RealParameter migrationParam = extractFlatM(coalescent, context, nMigRates);

        // === Rate-shift times: strip a leading 0.0 if present ===
        Double[] fullShifts = coalescent.getRateShifts().value();
        Double[] innerShifts = stripLeadingZero(fullShifts);
        if (innerShifts.length != nEpochs - 1) {
            throw new IllegalArgumentException(
                    "After stripping a leading 0.0, rateShifts must have length nEpochs - 1 = "
                            + (nEpochs - 1) + "; got " + innerShifts.length);
        }

        // === Build the NeDynamics list and the StructuredSkyline rateShifts ===
        //
        // Linear mode (Skygrowth): continuous piecewise-exponential Ne. Each
        // per-deme Skygrowth owns its own rateShifts input of length n-1 (the
        // internal knot times); Skygrowth sets Ne.dim = rateShifts.dim + 1 = n.
        // The OUTER StructuredSkyline.rateShifts (the integration grid) can be
        // the same n-1 interior times.
        //
        // Constant mode (StructuredSkygrid): piecewise-constant Ne per Mascot
        // integration interval. StructuredSkygrid has no rateShifts input of its
        // own; instead, StructuredSkyline.setNrIntervals drives NeLog.dim via the
        // outer rateShifts. For n epochs we need outer intTimes.length == n, which
        // requires outer rateShifts of length n with all values > 0. We supply
        // [t_1, ..., t_{n-1}, LARGE_TAIL] so the last interval covers everything
        // after the final knot (the tail region where log-Ne stays at logNe[n-1]).
        List<NeDynamics> neDynamicsPerDeme = new ArrayList<>();
        RateShifts outerRateShifts;

        if (isLinear) {
            // Per-deme Skygrowth rateShifts = [t_1, ..., t_{n-1}] (length n-1).
            // Skygrowth's initAndValidate sets Ne.dim = rateShifts.dim + 1 = n.
            RateShifts perDemeRateShifts = new RateShifts();
            perDemeRateShifts.setInputValue("value", new ArrayList<>(Arrays.asList(innerShifts)));
            perDemeRateShifts.setID("SkygrowthRateShifts");
            perDemeRateShifts.initAndValidate();

            for (int d = 0; d < nDemes; d++) {
                RealParameter demeLogNe = logNePerDeme.get(d);
                demeLogNe.setID("SkylineNe." + uniqueDemes.get(d));

                Skygrowth sg = new Skygrowth();
                sg.setInputValue("logNe", demeLogNe);
                sg.setInputValue("rateShifts", perDemeRateShifts);
                sg.setID("Skygrowth." + uniqueDemes.get(d));
                sg.initAndValidate();
                neDynamicsPerDeme.add(sg);
            }

            // Outer StructuredSkyline rateShifts must have ≥ 2 values: Mascot's
            // getCoalescentRate references rateShifts.dim - 2 as an "end-of-range"
            // sentinel, which underflows for dim == 1 (see mascot.dynamics
            // .StructuredSkyline#getCoalescentRate). Pad with a large tail like
            // we do for the constant mode so a single-knot skyline still works.
            List<Double> outerValues = new ArrayList<>(Arrays.asList(innerShifts));
            outerValues.add(computeTailEnd(innerShifts));
            outerRateShifts = new RateShifts();
            outerRateShifts.setInputValue("value", outerValues);
            outerRateShifts.setID("SkylineRateShifts");
            outerRateShifts.initAndValidate();
        } else {
            // Constant mode: StructuredSkygrid with n intervals. Outer rateShifts
            // has length n, all positive, last entry a large upper bound.
            List<Double> outerValues = new ArrayList<>(Arrays.asList(innerShifts));
            double tailEnd = computeTailEnd(innerShifts);
            outerValues.add(tailEnd);

            outerRateShifts = new RateShifts();
            outerRateShifts.setInputValue("value", outerValues);
            outerRateShifts.setID("SkylineRateShifts");
            outerRateShifts.initAndValidate();

            for (int d = 0; d < nDemes; d++) {
                RealParameter demeLogNe = logNePerDeme.get(d);
                demeLogNe.setID("SkylineNe." + uniqueDemes.get(d));

                StructuredSkygrid sg = new StructuredSkygrid();
                sg.setInputValue("NeLog", demeLogNe);
                sg.setID("Skygrid." + uniqueDemes.get(d));
                sg.initAndValidate();
                neDynamicsPerDeme.add(sg);
            }
        }

        NeDynamicsList neDynamicsList = new InitializedNeDynamicsList();
        neDynamicsList.setInputValue("neDynamics", neDynamicsPerDeme);
        neDynamicsList.setID("NeDynamicsList");
        neDynamicsList.initAndValidate();

        // === Indicators: all true, not estimated (consistent with ZIKV reference XML) ===
        BooleanParameter indicators = new BooleanParameter();
        Boolean[] allTrue = new Boolean[nMigRates];
        Arrays.fill(allTrue, Boolean.TRUE);
        indicators.setInputValue("value", Arrays.asList(allTrue));
        indicators.setInputValue("dimension", nMigRates);
        indicators.setInputValue("estimate", false);
        indicators.setID("MigrationIndicators");
        indicators.initAndValidate();

        // === Trait set for tip → deme mapping ===
        String popLabel = StructuredCoalescentSkyline.populationLabel;
        TimeTree timeTree = ((Value<TimeTree>) context.getGraphicalModelNode(value)).value();
        String traitStr = createTraitString(timeTree, popLabel);
        List<Taxon> taxonList = context.createTaxonList(TimeTreeToBEAST.getTaxaNames(timeTree));

        TaxonSet taxonSet = new TaxonSet();
        taxonSet.setInputValue("taxon", taxonList);
        taxonSet.initAndValidate();

        TraitSet traitSet = new TraitSet();
        traitSet.setInputValue("traitname", popLabel);
        traitSet.setInputValue("value", traitStr);
        traitSet.setInputValue("taxa", taxonSet);
        traitSet.initAndValidate();

        // === StructuredSkyline dynamics ===
        StructuredSkyline dynamics = new StructuredSkyline();
        dynamics.setInputValue("NeDynamics", neDynamicsList);
        dynamics.setInputValue("forwardsMigration", migrationParam);
        dynamics.setInputValue("rateShifts", outerRateShifts);
        dynamics.setInputValue("indicators", indicators);
        dynamics.setInputValue("typeTrait", traitSet);
        dynamics.setInputValue("dimension", nDemes);
        dynamics.setInputValue("fromBeauti", false);
        dynamics.setID("StructuredSkyline");
        dynamics.initAndValidate();

        // === Mascot distribution ===
        mascot.distribution.Mascot mascot = new mascot.distribution.Mascot();
        mascot.setInputValue("dynamics", dynamics);

        StructuredTreeIntervals structuredTreeIntervals = new StructuredTreeIntervals();
        structuredTreeIntervals.setInputValue("tree", value);
        structuredTreeIntervals.initAndValidate();

        mascot.setInputValue("structuredTreeIntervals", structuredTreeIntervals);
        mascot.setInputValue("tree", value);
        mascot.initAndValidate();

        context.addExtraLoggable(mascot);
        MascotExtraTreeLogger extraTreeLogger = new MascotExtraTreeLogger(mascot);
        context.addExtraLogger(extraTreeLogger);

        return mascot;
    }

    /**
     * Resolve the LPhy logNe to K per-deme RealParameters of dim nEpochs each.
     * Expects a BEASTVector of K RealParameters (the auto-vectorisation result).
     */
    private List<RealParameter> extractPerDemeLogNe(StructuredCoalescentSkyline coalescent,
                                                     BEASTContext context) {
        int nDemes = coalescent.getNDemes();
        int nEpochs = coalescent.getNEpochs();
        Value<Double[][]> logNeValue = coalescent.getLogNe();
        BEASTInterface beastObj = context.getBEASTObject(logNeValue);

        if (beastObj instanceof BEASTVector vec) {
            List<BEASTInterface> components = vec.getObjectList();
            if (components.size() != nDemes) {
                throw new IllegalArgumentException(
                        "logNe BEASTVector has " + components.size() + " components, expected " + nDemes);
            }
            List<RealParameter> result = new ArrayList<>();
            for (int d = 0; d < nDemes; d++) {
                BEASTInterface c = components.get(d);
                if (!(c instanceof RealParameter rp)) {
                    throw new IllegalArgumentException(
                            "logNe component " + d + " is " + c.getClass() + ", expected RealParameter");
                }
                if (rp.getDimension() != nEpochs) {
                    throw new IllegalArgumentException(
                            "logNe component " + d + " has dim " + rp.getDimension() + ", expected " + nEpochs);
                }
                result.add(rp);
            }
            return result;
        }

        throw new IllegalArgumentException(
                "StructuredCoalescentSkyline requires logNe as per-deme vectorised chains " +
                        "(BEASTVector of K RealParameters). Got: " + beastObj.getClass().getSimpleName() +
                        ". Use auto-vectorisation, e.g. " +
                        "logNe ~ GaussianRandomWalk(firstValue=Normal(..., replicates=K), sd=..., n=n).");
    }

    /**
     * Resolve the LPhy M to a single flat RealParameter of dim K*(K-1).
     * Accepts either a single RealParameter (e.g. from LogNormalMulti / Dirichlet /
     * raw data) or a BEASTVector of scalars (e.g. from LogNormal with replicates),
     * wrapping the latter in a {@link CompoundRealParameter}.
     */
    private RealParameter extractFlatM(StructuredCoalescentSkyline coalescent,
                                       BEASTContext context,
                                       int expectedDim) {
        Value<Double[]> mValue = coalescent.getM();
        BEASTInterface beastObj = context.getBEASTObject(mValue);

        if (beastObj instanceof RealParameter rp) {
            if (rp.getDimension() != expectedDim) {
                throw new IllegalArgumentException(
                        "M RealParameter has dim " + rp.getDimension() + ", expected " + expectedDim);
            }
            return rp;
        }

        if (beastObj instanceof BEASTVector vec) {
            List<BEASTInterface> components = vec.getObjectList();
            List<RealParameter> realParams = new ArrayList<>();
            int totalDim = 0;
            for (BEASTInterface c : components) {
                if (!(c instanceof RealParameter rp)) {
                    throw new IllegalArgumentException(
                            "M BEASTVector contains " + c.getClass() + ", expected RealParameter");
                }
                realParams.add(rp);
                totalDim += rp.getDimension();
            }
            if (totalDim != expectedDim) {
                throw new IllegalArgumentException(
                        "M BEASTVector total dim " + totalDim + " != expected " + expectedDim);
            }

            CompoundRealParameter compound = new CompoundRealParameter();
            compound.setInputValue("parameter", realParams);
            compound.setID("M.compound");
            compound.initAndValidate();
            return compound;
        }

        throw new IllegalArgumentException(
                "Unexpected BEAST representation of M: " + beastObj.getClass().getSimpleName());
    }

    private Double[] stripLeadingZero(Double[] times) {
        if (times.length > 0 && times[0] == 0.0) {
            Double[] stripped = new Double[times.length - 1];
            System.arraycopy(times, 1, stripped, 0, stripped.length);
            return stripped;
        }
        return times;
    }

    /**
     * Pick an end-of-tail time for the outer StructuredSkyline.rateShifts in
     * constant mode. Must be strictly greater than the last internal rateShift
     * and large enough to cover any tree root age Mascot will encounter. Scales
     * off the largest internal shift so units follow the user's time axis.
     */
    private double computeTailEnd(Double[] innerShifts) {
        if (innerShifts.length == 0) return 1.0;
        double last = innerShifts[innerShifts.length - 1];
        return Math.max(last * 1000.0, last + 1000.0);
    }

    private String createTraitString(TimeTree tree, String traitName) {
        StringBuilder builder = new StringBuilder();
        int leafCount = 0;
        for (TimeTreeNode node : tree.getNodes()) {
            if (node.isLeaf()) {
                if (leafCount > 0) builder.append(", ");
                builder.append(node.getId());
                builder.append("=");
                builder.append(node.getMetaData(traitName));
                leafCount += 1;
            }
        }
        return builder.toString();
    }

    @Override
    public Class<StructuredCoalescentSkyline> getGeneratorClass() {
        return StructuredCoalescentSkyline.class;
    }

    @Override
    public Class<mascot.distribution.Mascot> getBEASTClass() {
        return mascot.distribution.Mascot.class;
    }
}
