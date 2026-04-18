package mascot.lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.inference.CompoundDistribution;
import beast.base.inference.distribution.Normal;
import beast.base.inference.distribution.Prior;
import beastlabs.core.util.Slice;
import lphy.base.distribution.GaussianRandomWalk;
import lphy.core.model.Generator;
import lphy.core.model.Value;
import lphy.core.vectorization.VectorUtils;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;
import lphybeast.SliceFactory;
import mascot.util.Difference;
import mascot.util.First;

/**
 * Converts LPhy {@link GaussianRandomWalk} to a BEAST2 prior on the chain
 * parameter using the Mascot-idiomatic pattern:
 * <ul>
 *   <li>Rewire the {@code firstValue} upstream prior to target {@code Slice(chain, 0)}
 *       so the first element of the chain is constrained as the user intended, while
 *       the original standalone scalar is dropped from the BEAST object graph
 *       (mirrors {@link ExpMarkovChainToBEAST}).</li>
 *   <li>Emit a {@link Prior} on {@link Difference}(chain) with {@link Normal}(0, sd)
 *       so the Gaussian random walk on consecutive elements is scored.</li>
 * </ul>
 *
 * This converter lives in the mascot module because it uses Mascot's
 * {@link Difference} helper. It is independent of the Skyline converter so any
 * LPhy script using {@code GaussianRandomWalk} within a Mascot analysis picks it
 * up automatically.
 */
public class GaussianRandomWalkToBEAST implements GeneratorToBEAST<GaussianRandomWalk, BEASTInterface> {

    @Override
    public BEASTInterface generatorToBEAST(GaussianRandomWalk generator, BEASTInterface value, BEASTContext context) {

        // Collected priors to return (single Prior if non-vectorised, BEASTVector of
        // Difference+First priors if vectorised).
        java.util.List<BEASTInterface> produced = new java.util.ArrayList<>();

        Value firstValue = generator.getParams().get(GaussianRandomWalk.firstValueParamName);
        if (firstValue != null) {
            Generator dist = firstValue.getGenerator();
            BEASTInterface upstreamPrior = (dist != null) ? context.getBEASTObject(dist) : null;
            if (upstreamPrior != null) {
                // Non-vectorised path: rewire the firstValue's own prior onto
                // Slice(chain, 0) and drop the standalone firstValue — same as
                // ExpMarkovChainToBEAST.
                BEASTInterface firstV = context.getBEASTObject(firstValue);
                if (firstV != null) context.removeBEASTObject(firstV);

                Slice feastSlice = SliceFactory.createSlice(value, 0, firstValue.getCanonicalId());
                upstreamPrior.setInputValue("x", feastSlice);
                context.putBEASTObject(dist, upstreamPrior);
            } else {
                // Vectorised path: the firstValue is a component of an auto-vectorised
                // parent (e.g. init ~ Normal(replicates=K)), which is registered as a
                // single dim-K RealParameter with one upstream prior. We emit a fresh
                // First(chain) ~ firstValue_distribution prior per chain component and
                // best-effort drop the redundant parent parameter + its prior.
                Prior firstPrior = emitFirstPrior(value, dist);
                if (firstPrior != null) produced.add(firstPrior);
                dropVectorisedFirstValueParent(firstValue, context);
            }
        }
        // initialMean mode is deferred — current consumers (Skyline) use firstValue.

        // Prior on consecutive differences: x[i] - x[i-1] ~ Normal(0, sd)
        Difference diff = new Difference();
        diff.setInputValue("arg", value);
        diff.initAndValidate();

        Normal normalDist = new Normal();
        normalDist.setInputValue("mean", "0.0");
        Value<Double> sdValue = (Value<Double>) generator.getParams().get("sd");
        BEASTInterface sdBEAST = context.getBEASTObject(sdValue);
        normalDist.setInputValue("sigma", sdBEAST);
        normalDist.initAndValidate();

        Prior diffPrior = new Prior();
        diffPrior.setInputValue("x", diff);
        diffPrior.setInputValue("distr", normalDist);
        diffPrior.initAndValidate();

        produced.add(diffPrior);

        // Return a single Prior if that's all we produced; otherwise wrap in a
        // CompoundDistribution so both the Difference and First priors are picked
        // up by the framework's prior-gathering pass (which only unpacks single
        // Distribution registrations per generator key).
        if (produced.size() == 1) {
            return produced.get(0);
        }
        CompoundDistribution compound = new CompoundDistribution();
        compound.setInputValue("distribution", produced);
        compound.initAndValidate();
        return compound;
    }

    /**
     * Extract mean/sigma from the firstValue's upstream LPhy Normal distribution
     * (e.g. from {@code init ~ Normal(mean=0, sd=1, replicates=K)}) and emit a
     * {@code Prior} on {@code First(chain)} with those parameters. Returns null
     * if the upstream distribution isn't a supported type.
     */
    private Prior emitFirstPrior(BEASTInterface chain, Generator firstValueDist) {
        if (!(firstValueDist instanceof lphy.base.distribution.Normal lphyNormal)) {
            // Only Normal firstValues supported for now; other distributions would
            // need their own extraction logic.
            return null;
        }

        Value<Number> meanV = lphyNormal.getMean();
        Value<Number> sdV = lphyNormal.getSd();
        double mean = meanV.value().doubleValue();
        double sd = sdV.value().doubleValue();

        First first = new First();
        first.setInputValue("arg", chain);
        first.initAndValidate();

        Normal normal = new Normal();
        normal.setInputValue("mean", Double.toString(mean));
        normal.setInputValue("sigma", Double.toString(sd));
        normal.initAndValidate();

        Prior prior = new Prior();
        prior.setInputValue("x", first);
        prior.setInputValue("distr", normal);
        prior.initAndValidate();
        return prior;
    }

    /**
     * Best-effort removal of the parent vectorised state node (e.g. {@code init}
     * for {@code init ~ Normal(replicates=K)}) and its upstream prior. The GRW
     * converter is called once per component and each call re-attempts the same
     * lookup; subsequent calls are no-ops once the parent is removed.
     */
    private void dropVectorisedFirstValueParent(Value firstValue, BEASTContext context) {
        String canonicalId = firstValue.getCanonicalId();
        if (canonicalId == null) return;

        // Component ids are "{parent}{SEP}{index}" (e.g. "init.0"). Strip the last
        // SEP-suffix to get the parent id.
        String sep = VectorUtils.INDEX_SEPARATOR;
        int idx = canonicalId.lastIndexOf(sep);
        if (idx < 0) return;
        String parentId = canonicalId.substring(0, idx);

        BEASTInterface parent = context.getBEASTObject(parentId);
        if (parent == null) return;

        // Find any Prior in the BEAST context whose x input points at the parent
        // RealParameter and remove it. (Only one upstream prior is expected.)
        for (BEASTInterface bi : new java.util.ArrayList<>(context.getElements().keySet())) {
            if (bi instanceof Prior p) {
                Object x = p.getInputValue("x");
                if (x == parent) {
                    context.removeBEASTObject(p);
                }
            }
        }
        context.removeBEASTObject(parent);
    }

    @Override
    public Class<GaussianRandomWalk> getGeneratorClass() {
        return GaussianRandomWalk.class;
    }

    @Override
    public Class<BEASTInterface> getBEASTClass() {
        return BEASTInterface.class;
    }
}
