package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.inference.distribution.Prior;
import beast.base.inference.parameter.RealParameter;
import lphy.base.distribution.Beta;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class BetaToBEAST implements GeneratorToBEAST<Beta, Prior> {
    @Override
    public Prior generatorToBEAST(Beta generator, BEASTInterface value, BEASTContext context) {
        beast.base.inference.distribution.Beta betaDistribution = new beast.base.inference.distribution.Beta();
        betaDistribution.setInputValue("alpha", context.getAsRealParameter(generator.getParams().get("alpha")));
        betaDistribution.setInputValue("beta", context.getAsRealParameter(generator.getParams().get("beta")));
        betaDistribution.initAndValidate();
        return BEASTContext.createPrior(betaDistribution, (RealParameter) value);
    }

    @Override
    public Class<Beta> getGeneratorClass() {
        return Beta.class;
    }

    @Override
    public Class<Prior> getBEASTClass() {
        return Prior.class;
    }
}
