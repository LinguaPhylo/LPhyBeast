package mascot.lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.inference.CompoundDistribution;
import beast.base.inference.Distribution;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.distribution.IID;
import beast.base.spec.inference.distribution.Normal;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.inference.parameter.VectorElement;
import beast.base.spec.type.RealVector;
import lphy.base.distribution.GaussianRandomWalk;
import lphy.core.model.Generator;
import lphy.core.model.Value;
import lphy.core.vectorization.VectorUtils;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;
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
    @SuppressWarnings({"rawtypes", "unchecked"})
    public BEASTInterface generatorToBEAST(GaussianRandomWalk generator, BEASTInterface value, BEASTContext context) {

        // Collected distributions to return (single Distribution if only a Difference
        // prior, or CompoundDistribution if we also emit a First prior in the
        // vectorised-firstValue case).
        java.util.List<BEASTInterface> produced = new java.util.ArrayList<>();

        Value firstValue = generator.getParams().get(GaussianRandomWalk.firstValueParamName);
        if (firstValue != null) {
            Generator dist = firstValue.getGenerator();
            BEASTInterface upstreamDist = (dist != null) ? context.getBEASTObject(dist) : null;
            if (upstreamDist != null) {
                // Non-vectorised path: rewire the firstValue's upstream distribution
                // (e.g. spec Normal) onto a VectorElement view of chain[0] and drop
                // the standalone firstValue state node, as in ExpMarkovChainToBEAST.
                BEASTInterface firstV = context.getBEASTObject(firstValue);
                if (firstV != null) context.removeBEASTObject(firstV);

                VectorElement<Real> element = new VectorElement<>(
                        (RealVector<Real>) value, 0);
                element.setID(firstValue.getCanonicalId());
                upstreamDist.setInputValue("param", element);
                context.putBEASTObject(dist, upstreamDist);
            } else {
                // Vectorised path: the firstValue is a component of an auto-vectorised
                // parent (e.g. init ~ Normal(replicates=K)), which is registered as a
                // single dim-K parameter with one upstream Normal. The component
                // distributions aren't individually registered, so emit a fresh
                // Normal(mean, sd) on First(chain) and best-effort drop the
                // redundant parent parameter + its upstream distribution.
                Distribution firstPrior = emitFirstPrior(value, dist);
                if (firstPrior != null) produced.add(firstPrior);
                dropVectorisedFirstValueParent(firstValue, context);
            }
        }
        // initialMean mode is deferred — current consumers (Skyline) use firstValue.

        // Prior on consecutive differences: x[i] - x[i-1] ~ Normal(0, sd) i.i.d.
        // Difference is a RealVector<Real>, so we wrap a scalar Normal in an IID.
        Difference diff = new Difference();
        diff.setInputValue("arg", value);
        diff.initAndValidate();

        Value<Double> sdValue = (Value<Double>) generator.getParams().get("sd");
        Normal perElementNormal = new Normal();
        perElementNormal.setInputValue("mean", new RealScalarParam<>(0.0, Real.INSTANCE));
        perElementNormal.setInputValue("sigma",
                (RealScalarParam<PositiveReal>) context.getAsRealScalar(sdValue));
        perElementNormal.initAndValidate();

        IID diffIid = new IID();
        diffIid.setInputValue("param", diff);
        diffIid.setInputValue("distr", perElementNormal);
        diffIid.initAndValidate();

        produced.add(diffIid);

        // Return the single distribution if that's all we produced; otherwise wrap
        // the pair (First-prior + Difference-IID) in a CompoundDistribution so the
        // framework's prior-gathering pass picks up both.
        if (produced.size() == 1) {
            return produced.get(0);
        }
        CompoundDistribution compound = new CompoundDistribution();
        compound.setInputValue("distribution", produced);
        compound.initAndValidate();
        return compound;
    }

    /**
     * Emit a spec {@link Normal} prior on {@link First}(chain) using the mean
     * and sd of the firstValue's upstream LPhy Normal distribution (e.g. from
     * {@code init ~ Normal(mean=0, sd=1, replicates=K)}). Returns null if the
     * upstream distribution isn't a supported type. First is a RealScalar so
     * the scalar Normal can take it directly as {@code param}.
     */
    private Distribution emitFirstPrior(BEASTInterface chain, Generator firstValueDist) {
        if (!(firstValueDist instanceof lphy.base.distribution.Normal lphyNormal)) {
            // Only Normal firstValues supported for now; other distributions would
            // need their own extraction logic.
            return null;
        }

        double mean = lphyNormal.getMean().value().doubleValue();
        double sd = lphyNormal.getSd().value().doubleValue();

        First first = new First();
        first.setInputValue("arg", chain);
        first.initAndValidate();

        Normal normal = new Normal();
        normal.setInputValue("mean", new RealScalarParam<>(mean, Real.INSTANCE));
        normal.setInputValue("sigma", new RealScalarParam<>(sd, PositiveReal.INSTANCE));
        normal.setInputValue("param", first);
        normal.initAndValidate();
        return normal;
    }

    /**
     * Best-effort removal of the parent vectorised state node (e.g. {@code init}
     * for {@code init ~ Normal(replicates=K)}) and any spec Normal/Distribution
     * whose {@code param} points at it. The GRW converter is called once per
     * component; subsequent calls are no-ops once the parent is removed.
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

        // Find any TensorDistribution in the BEAST context whose `param` input
        // points at the parent and remove it. Then drop the parent itself.
        for (BEASTInterface bi : new java.util.ArrayList<>(context.getElements().keySet())) {
            try {
                Object p = bi.getInputValue("param");
                if (p == parent) {
                    context.removeBEASTObject(bi);
                }
            } catch (Exception ignore) {
                // Not every BEASTInterface has a "param" input; skip those.
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
