package mascot.lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.evolution.alignment.Taxon;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.tree.TraitSet;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.BoolVectorParam;
import beast.base.spec.inference.parameter.CompoundRealScalarParam;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.inference.parameter.RealVectorParam;
import lphybeast.util.BEASTVector;
import lphy.base.evolution.coalescent.StructuredCoalescentSkyline;
import lphy.base.evolution.tree.TimeTree;
import lphy.base.evolution.tree.TimeTreeNode;
import lphy.core.model.Value;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;
import lphybeast.tobeast.values.TimeTreeToBEAST;
import mascot.distribution.StructuredTreeIntervals;
import mascot.dynamics.Dynamics;
import mascot.dynamics.RateShifts;
import mascot.dynamics.StructuredMigrationSkyline;
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
 * {@link mascot.distribution.Mascot}. Two paths depending on whether migration
 * is time-constant or time-varying:
 *
 * <ul>
 *   <li>Constant migration ({@code M}) — {@link StructuredSkyline} dynamics with
 *       K per-deme Ne trajectories and a flat migration {@link RealVectorParam}.</li>
 *   <li>Time-varying migration ({@code logM} + {@code migrationRateShifts}) —
 *       {@link StructuredMigrationSkyline} dynamics with K per-deme Ne
 *       trajectories and K·(K-1) per-pair {@link Skygrowth} migration
 *       trajectories. In this path, migration is always piecewise-linear in
 *       log space: {@link StructuredSkygrid} cannot serve as a migration
 *       dynamic because {@link StructuredMigrationSkyline#getBackwardsMigration}
 *       calls {@code getNeTime(t)} on migration objects, which
 *       {@code StructuredSkygrid} does not implement. Current cut also
 *       requires {@code migrationRateShifts} and {@code rateShifts} to share
 *       the same knot times so per-deme Ne dimensions line up with the outer
 *       integration grid without expansion.</li>
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
        boolean timeVaryingMigration = coalescent.isTimeVaryingMigration();

        // === Per-deme logNe RealVectorParams (K of them, each dim n) ===
        List<RealVectorParam<? extends Real>> logNePerDeme = extractPerDemeLogNe(coalescent, context);

        // === Rate-shift times: strip a leading 0.0 if present ===
        Double[] fullShifts = coalescent.getRateShifts().value();
        Double[] innerShifts = stripLeadingZero(fullShifts);
        if (innerShifts.length != nEpochs - 1) {
            throw new IllegalArgumentException(
                    "After stripping a leading 0.0, rateShifts must have length nEpochs - 1 = "
                            + (nEpochs - 1) + "; got " + innerShifts.length);
        }

        Dynamics dynamics;
        if (timeVaryingMigration) {
            dynamics = buildTimeVaryingMigrationDynamics(
                    coalescent, context, logNePerDeme, innerShifts, isLinear, nDemes, nEpochs, nMigRates, uniqueDemes);
        } else {
            dynamics = buildConstantMigrationDynamics(
                    coalescent, context, logNePerDeme, innerShifts, isLinear, nDemes, nMigRates, uniqueDemes);
        }

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

        dynamics.setInputValue("typeTrait", traitSet);
        dynamics.setInputValue("dimension", nDemes);
        dynamics.setInputValue("fromBeauti", false);
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

    private StructuredSkyline buildConstantMigrationDynamics(
            StructuredCoalescentSkyline coalescent, BEASTContext context,
            List<RealVectorParam<? extends Real>> logNePerDeme, Double[] innerShifts,
            boolean isLinear, int nDemes, int nMigRates, List<String> uniqueDemes) {

        RealVectorParam<? extends Real> migrationParam = extractFlatM(coalescent, context, nMigRates);

        List<NeDynamics> neDynamicsPerDeme = buildNeDynamicsPerDeme(
                logNePerDeme, innerShifts, isLinear, nDemes, uniqueDemes);

        NeDynamicsList neDynamicsList = new InitializedNeDynamicsList();
        neDynamicsList.setInputValue("neDynamics", neDynamicsPerDeme);
        neDynamicsList.setID("NeDynamicsList");
        neDynamicsList.initAndValidate();

        // Outer integration grid. StructuredSkygrid Ne (interpolation="constant")
        // is piecewise-constant and has its NeLog dimension driven by the outer
        // grid via setNrIntervals, so we MUST keep the outer grid knot-aligned
        // there. Skygrowth Ne (interpolation="linear") is time-based and
        // evaluated at interval midpoints — needs a fine grid for accuracy.
        RateShifts outerRateShifts = isLinear
                ? buildFineIntegrationGrid(innerShifts, "SkylineRateShifts")
                : buildOuterRateShifts(innerShifts, "SkylineRateShifts");

        BoolVectorParam indicators = buildAllTrueIndicators(nMigRates);

        StructuredSkyline dynamics = new StructuredSkyline();
        dynamics.setInputValue("NeDynamics", neDynamicsList);
        dynamics.setInputValue("forwardsMigration", migrationParam);
        dynamics.setInputValue("rateShifts", outerRateShifts);
        dynamics.setInputValue("indicators", indicators);
        dynamics.setID("StructuredSkyline");
        return dynamics;
    }

    /**
     * Time-varying migration path. Builds K per-deme {@link Skygrowth} Ne
     * trajectories (with their own rateShifts from {@code rateShifts}), K·(K-1)
     * per-pair {@link Skygrowth} migration trajectories (with their own
     * rateShifts from {@code migrationRateShifts}), and an outer integration
     * grid that is the union of both. Wires them into a
     * {@link StructuredMigrationSkyline}.
     *
     * <p>In this path we always use {@code Skygrowth} for Ne regardless of
     * {@code interpolation} — {@code StructuredSkygrid} cannot support
     * independent per-trajectory grids (its Ne dimension is driven by the
     * outer grid via {@code setNrIntervals}), and it also cannot serve as
     * migration dynamics because it doesn't implement {@code getNeTime}.
     * Using {@code Skygrowth} uniformly lets Ne and migration have arbitrary,
     * independent knot counts and positions — matching what BEAUti's MASCOT
     * Skyline template offers.</p>
     *
     * <p>Trade-off: when {@code interpolation="constant"} is set on the LPhy
     * side, the LPhy simulator produces piecewise-constant Ne and migration,
     * while Mascot here interpolates piecewise-linearly in log space between
     * knots. Values at the knot times agree; between-knot values differ. For
     * a 2-epoch expansion/endemic split with knots at the transition, the
     * between-knot region is the only disagreement.</p>
     */
    private StructuredMigrationSkyline buildTimeVaryingMigrationDynamics(
            StructuredCoalescentSkyline coalescent, BEASTContext context,
            List<RealVectorParam<? extends Real>> logNePerDeme, Double[] neInnerShifts,
            boolean isLinear, int nDemes, int nEpochs, int nMigRates, List<String> uniqueDemes) {

        int nMigEpochs = coalescent.getNMigEpochs();
        Double[] migShifts = coalescent.getMigrationRateShifts().value();
        Double[] migInnerShifts = stripLeadingZero(migShifts);
        if (migInnerShifts.length != nMigEpochs - 1) {
            throw new IllegalArgumentException(
                    "After stripping a leading 0.0, migrationRateShifts must have length nMigEpochs - 1 = "
                            + (nMigEpochs - 1) + "; got " + migInnerShifts.length);
        }

        // Per-deme Ne: Skygrowth with its own rateShifts (n_Ne - 1 inner knots).
        RateShifts perDemeRateShifts = new RateShifts();
        perDemeRateShifts.setInputValue("value", new ArrayList<>(Arrays.asList(neInnerShifts)));
        perDemeRateShifts.setID("SkygrowthRateShifts");
        perDemeRateShifts.initAndValidate();

        List<NeDynamics> neDynamicsPerDeme = new ArrayList<>();
        for (int d = 0; d < nDemes; d++) {
            RealVectorParam<? extends Real> demeLogNe = logNePerDeme.get(d);
            demeLogNe.setID("SkylineNe." + uniqueDemes.get(d));

            Skygrowth sg = new Skygrowth();
            sg.setInputValue("logNe", demeLogNe);
            sg.setInputValue("rateShifts", perDemeRateShifts);
            sg.setID("Skygrowth." + uniqueDemes.get(d));
            sg.initAndValidate();
            neDynamicsPerDeme.add(sg);
        }
        NeDynamicsList neDynamicsList = new InitializedNeDynamicsList();
        neDynamicsList.setInputValue("neDynamics", neDynamicsPerDeme);
        neDynamicsList.setID("NeDynamicsList");
        neDynamicsList.initAndValidate();

        // Per-pair migration: Skygrowth with its own rateShifts (n_M - 1 inner knots).
        List<RealVectorParam<? extends Real>> logMPerPair = extractPerPairLogM(coalescent, context, nMigRates, nMigEpochs);
        RateShifts perPairMigRateShifts = new RateShifts();
        perPairMigRateShifts.setInputValue("value", new ArrayList<>(Arrays.asList(migInnerShifts)));
        perPairMigRateShifts.setID("MigrationRateShifts");
        perPairMigRateShifts.initAndValidate();

        List<NeDynamics> migDynamicsPerPair = new ArrayList<>();
        int pairIdx = 0;
        for (int i = 0; i < nDemes; i++) {
            for (int j = 0; j < nDemes; j++) {
                if (i == j) continue;
                RealVectorParam<? extends Real> pairLogM = logMPerPair.get(pairIdx);
                String label = uniqueDemes.get(i) + "_to_" + uniqueDemes.get(j);
                pairLogM.setID("SkylineM." + label);

                Skygrowth sg = new Skygrowth();
                sg.setInputValue("logNe", pairLogM);
                sg.setInputValue("rateShifts", perPairMigRateShifts);
                sg.setID("SkygrowthM." + label);
                sg.initAndValidate();
                migDynamicsPerPair.add(sg);
                pairIdx++;
            }
        }

        NeDynamicsList migDynamicsList = new InitializedNeDynamicsList();
        migDynamicsList.setInputValue("neDynamics", migDynamicsPerPair);
        migDynamicsList.setID("MigrationDynamicsList");
        migDynamicsList.initAndValidate();

        // Outer integration grid: a fine uniform grid across the skyline
        // region, with all user knots (Ne + migration) inserted so Mascot
        // integrates across every transition. Mascot evaluates Skygrowth Ne
        // and migration at each interval's midpoint (getIntervalMidpoint in
        // StructuredMigrationSkyline.getBackwardsMigration / getCoalescentRate),
        // so a knot-aligned grid would approximate each time-varying trajectory
        // by its midpoint value over a full skyline segment — far too coarse.
        Double[] unionInner = unionSorted(neInnerShifts, migInnerShifts);
        RateShifts outerRateShifts = buildFineIntegrationGrid(unionInner, "StructuredMigrationSkylineRateShifts");
        BoolVectorParam indicators = buildAllTrueIndicators(nMigRates);

        StructuredMigrationSkyline dynamics = new StructuredMigrationSkyline();
        dynamics.setInputValue("NeDynamics", neDynamicsList);
        dynamics.setInputValue("migrationDynamics", migDynamicsList);
        dynamics.setInputValue("rateShifts", outerRateShifts);
        dynamics.setInputValue("indicators", indicators);
        dynamics.setID("StructuredMigrationSkyline");
        return dynamics;
    }

    /**
     * Sorted union of two shift-time arrays (deduplicated). Shift times come
     * straight from LPhy user input; intentional matches across the two
     * arrays are expected to be bit-identical, so exact equality is fine.
     */
    private Double[] unionSorted(Double[] a, Double[] b) {
        java.util.TreeSet<Double> set = new java.util.TreeSet<>();
        for (Double x : a) set.add(x);
        for (Double x : b) set.add(x);
        return set.toArray(new Double[0]);
    }

    /**
     * Per-deme Ne dynamics list (Skygrowth for linear mode, StructuredSkygrid
     * for constant mode). Shared between the constant-migration and
     * time-varying-migration paths.
     */
    private List<NeDynamics> buildNeDynamicsPerDeme(
            List<RealVectorParam<? extends Real>> logNePerDeme, Double[] innerShifts,
            boolean isLinear, int nDemes, List<String> uniqueDemes) {

        List<NeDynamics> neDynamicsPerDeme = new ArrayList<>();
        if (isLinear) {
            RateShifts perDemeRateShifts = new RateShifts();
            perDemeRateShifts.setInputValue("value", new ArrayList<>(Arrays.asList(innerShifts)));
            perDemeRateShifts.setID("SkygrowthRateShifts");
            perDemeRateShifts.initAndValidate();

            for (int d = 0; d < nDemes; d++) {
                RealVectorParam<? extends Real> demeLogNe = logNePerDeme.get(d);
                demeLogNe.setID("SkylineNe." + uniqueDemes.get(d));

                Skygrowth sg = new Skygrowth();
                sg.setInputValue("logNe", demeLogNe);
                sg.setInputValue("rateShifts", perDemeRateShifts);
                sg.setID("Skygrowth." + uniqueDemes.get(d));
                sg.initAndValidate();
                neDynamicsPerDeme.add(sg);
            }
        } else {
            for (int d = 0; d < nDemes; d++) {
                RealVectorParam<? extends Real> demeLogNe = logNePerDeme.get(d);
                demeLogNe.setID("SkylineNe." + uniqueDemes.get(d));

                StructuredSkygrid sg = new StructuredSkygrid();
                sg.setInputValue("NeLog", demeLogNe);
                sg.setID("Skygrid." + uniqueDemes.get(d));
                sg.initAndValidate();
                neDynamicsPerDeme.add(sg);
            }
        }
        return neDynamicsPerDeme;
    }

    /**
     * Build outer integration {@link RateShifts} of length {@code innerShifts.length + 1}:
     * the inner knot times plus a large-valued tail so Mascot's
     * {@code getCoalescentRate} (which references {@code rateShifts.dim - 2}) does
     * not underflow for single-knot skylines. Used for the constant-mode
     * {@link StructuredSkygrid} path, where Ne is piecewise-constant per
     * interval (setNrIntervals drives NeLog.dim from the outer grid, so the
     * outer grid MUST stay knot-aligned).
     */
    private RateShifts buildOuterRateShifts(Double[] innerShifts, String id) {
        List<Double> outerValues = new ArrayList<>(Arrays.asList(innerShifts));
        outerValues.add(computeTailEnd(innerShifts));
        RateShifts outerRateShifts = new RateShifts();
        outerRateShifts.setInputValue("value", outerValues);
        outerRateShifts.setID(id);
        outerRateShifts.initAndValidate();
        return outerRateShifts;
    }

    /**
     * Build a fine integration grid for the {@link Skygrowth} paths. Mascot
     * evaluates time-varying trajectories (Ne and migration) at the midpoint
     * of each integration interval, so to approximate a piecewise-linear
     * trajectory accurately the integration intervals must be fine compared
     * to the skyline segments. Matches BEAUti's MASCOT Skyline template,
     * which uses ~100 uniform subdivisions of the modelled time range.
     *
     * <p>Grid = uniform N_STEPS subdivisions of {@code [0, max_inner_knot]},
     * plus every user knot inserted so transitions align exactly, plus a
     * tail from {@link #computeTailEnd} to cover tree-root age past the last
     * knot (rates in that trailing interval are held at their last-knot
     * value by both Skygrowth and StructuredSkygrid).</p>
     */
    private RateShifts buildFineIntegrationGrid(Double[] innerShifts, String id) {
        final int N_STEPS = 100;
        java.util.TreeSet<Double> pts = new java.util.TreeSet<>();
        double maxInner = innerShifts.length > 0 ? innerShifts[innerShifts.length - 1] : 1.0;
        double step = maxInner / N_STEPS;
        for (int i = 1; i <= N_STEPS; i++) {
            pts.add(step * i);
        }
        for (Double s : innerShifts) pts.add(s);
        pts.add(computeTailEnd(innerShifts));

        List<Double> values = new ArrayList<>(pts);
        RateShifts rs = new RateShifts();
        rs.setInputValue("value", values);
        rs.setID(id);
        rs.initAndValidate();
        return rs;
    }

    private BoolVectorParam buildAllTrueIndicators(int nMigRates) {
        Boolean[] allTrue = new Boolean[nMigRates];
        Arrays.fill(allTrue, Boolean.TRUE);
        BoolVectorParam indicators = new BoolVectorParam();
        indicators.setInputValue("value", Arrays.asList(allTrue));
        indicators.setInputValue("dimension", nMigRates);
        indicators.setInputValue("estimate", false);
        indicators.setID("MigrationIndicators");
        indicators.initAndValidate();
        return indicators;
    }

    /**
     * Resolve the LPhy logNe to K per-deme RealVectorParams of dim nEpochs each.
     * Expects a BEASTVector of K RealVectorParams (the auto-vectorisation result).
     */
    private List<RealVectorParam<? extends Real>> extractPerDemeLogNe(
            StructuredCoalescentSkyline coalescent, BEASTContext context) {
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
            List<RealVectorParam<? extends Real>> result = new ArrayList<>();
            for (int d = 0; d < nDemes; d++) {
                BEASTInterface c = components.get(d);
                if (!(c instanceof RealVectorParam<?> rp)) {
                    throw new IllegalArgumentException(
                            "logNe component " + d + " is " + c.getClass() + ", expected RealVectorParam");
                }
                if (rp.size() != nEpochs) {
                    throw new IllegalArgumentException(
                            "logNe component " + d + " has dim " + rp.size() + ", expected " + nEpochs);
                }
                result.add(rp);
            }
            return result;
        }

        throw new IllegalArgumentException(
                "StructuredCoalescentSkyline requires logNe as per-deme vectorised chains " +
                        "(BEASTVector of K RealVectorParams). Got: " + beastObj.getClass().getSimpleName() +
                        ". Use auto-vectorisation, e.g. " +
                        "logNe ~ GaussianRandomWalk(firstValue=Normal(..., replicates=K), sd=..., n=n).");
    }

    /**
     * Resolve the LPhy logM to K·(K-1) per-pair RealVectorParams of dim nMigEpochs
     * each. Expects a BEASTVector of K·(K-1) RealVectorParams (the auto-vectorisation
     * result), analogous to logNe.
     */
    private List<RealVectorParam<? extends Real>> extractPerPairLogM(
            StructuredCoalescentSkyline coalescent, BEASTContext context,
            int nPairs, int nMigEpochs) {
        Value<Double[][]> logMValue = coalescent.getLogM();
        BEASTInterface beastObj = context.getBEASTObject(logMValue);

        if (beastObj instanceof BEASTVector vec) {
            List<BEASTInterface> components = vec.getObjectList();
            if (components.size() != nPairs) {
                throw new IllegalArgumentException(
                        "logM BEASTVector has " + components.size() + " components, expected " + nPairs);
            }
            List<RealVectorParam<? extends Real>> result = new ArrayList<>();
            for (int p = 0; p < nPairs; p++) {
                BEASTInterface c = components.get(p);
                if (!(c instanceof RealVectorParam<?> rp)) {
                    throw new IllegalArgumentException(
                            "logM component " + p + " is " + c.getClass() + ", expected RealVectorParam");
                }
                if (rp.size() != nMigEpochs) {
                    throw new IllegalArgumentException(
                            "logM component " + p + " has dim " + rp.size() + ", expected " + nMigEpochs);
                }
                result.add(rp);
            }
            return result;
        }

        throw new IllegalArgumentException(
                "StructuredCoalescentSkyline requires logM as per-pair vectorised chains " +
                        "(BEASTVector of K*(K-1) RealVectorParams). Got: " + beastObj.getClass().getSimpleName() +
                        ". Use auto-vectorisation, e.g. " +
                        "logM ~ GaussianRandomWalk(firstValue=Normal(..., replicates=K*(K-1)), sd=..., n=n_M).");
    }

    /**
     * Resolve the LPhy M to a single flat RealVectorParam of dim K*(K-1).
     * Accepts either a single RealVectorParam (e.g. from LogNormalMulti / Dirichlet /
     * raw data) or a BEASTVector of RealScalarParams (e.g. from LogNormal with
     * replicates), wrapping the latter in a {@link CompoundRealScalarParam}.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private RealVectorParam<? extends Real> extractFlatM(StructuredCoalescentSkyline coalescent,
                                                          BEASTContext context,
                                                          int expectedDim) {
        Value<Double[]> mValue = coalescent.getM();
        BEASTInterface beastObj = context.getBEASTObject(mValue);

        if (beastObj instanceof RealVectorParam<?> rvp) {
            if (rvp.size() != expectedDim) {
                throw new IllegalArgumentException(
                        "M RealVectorParam has dim " + rvp.size() + ", expected " + expectedDim);
            }
            return rvp;
        }

        if (beastObj instanceof BEASTVector vec) {
            List<BEASTInterface> components = vec.getObjectList();
            List<RealScalarParam> realScalars = new ArrayList<>();
            int totalDim = 0;
            for (BEASTInterface c : components) {
                if (!(c instanceof RealScalarParam rsp)) {
                    throw new IllegalArgumentException(
                            "M BEASTVector contains " + c.getClass() + ", expected RealScalarParam");
                }
                realScalars.add(rsp);
                totalDim += 1; // RealScalarParam is always dim 1
            }
            if (totalDim != expectedDim) {
                throw new IllegalArgumentException(
                        "M BEASTVector total dim " + totalDim + " != expected " + expectedDim);
            }

            CompoundRealScalarParam<Real> compound = new CompoundRealScalarParam<>();
            compound.setInputValue("parameter", realScalars);
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
