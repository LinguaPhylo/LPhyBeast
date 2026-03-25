package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.spec.domain.UnitInterval;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.spec.type.RealScalar;
import beastlabs.math.distributions.BernoulliDistribution;
import lphy.base.distribution.BernoulliMulti;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

// Note: because there is a optional condition "minSuccesses",
// it cannot be replaced by {@link IID}.
public class BernoulliMultiToBEAST implements GeneratorToBEAST<BernoulliMulti, BernoulliDistribution> {
    @Override
    public BernoulliDistribution generatorToBEAST(BernoulliMulti generator, BEASTInterface value, BEASTContext context) {

        // BernoulliDistribution.p expects RealVectorParam; wrap scalar p as 1-element vector
        BEASTInterface pObj = context.getBEASTObject(generator.getP());
        if (pObj instanceof RealScalar<?> scalar) {
            RealVectorParam<UnitInterval> pVec = new RealVectorParam<>(new double[]{scalar.get()}, UnitInterval.INSTANCE);
            pVec.setID(((BEASTInterface) scalar).getID());
            pObj = pVec;
        }

        BernoulliDistribution bernoulliDistribution = new BernoulliDistribution();
        bernoulliDistribution.setInputValue("p", pObj);
        bernoulliDistribution.setInputValue("parameter", value);
        bernoulliDistribution.setInputValue(generator.minSuccessesParamName, context.getBEASTObject(generator.getMinSuccesses()));
        bernoulliDistribution.initAndValidate();
        return bernoulliDistribution;
    }

    @Override
    public Class<BernoulliMulti> getGeneratorClass() {
        return BernoulliMulti.class;
    }

    @Override
    public Class<BernoulliDistribution> getBEASTClass() {
        return BernoulliDistribution.class;
    }
}
