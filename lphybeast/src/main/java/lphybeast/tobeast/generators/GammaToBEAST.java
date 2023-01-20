package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.inference.distribution.Prior;
import beast.base.inference.parameter.RealParameter;
import lphy.core.distributions.Gamma;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class GammaToBEAST implements GeneratorToBEAST<Gamma, Prior> {
    @Override
    public Prior generatorToBEAST(Gamma generator, BEASTInterface value, BEASTContext context) {
        beast.base.inference.distribution.Gamma gammaDistribution = new beast.base.inference.distribution.Gamma();
        gammaDistribution.setInputValue("alpha", context.getBEASTObject(generator.getShape()));
        gammaDistribution.setInputValue("beta", context.getBEASTObject(generator.getScale()));
        gammaDistribution.initAndValidate();
        return BEASTContext.createPrior(gammaDistribution, (RealParameter) value);
    }

    @Override
    public Class<Gamma> getGeneratorClass() {
        return Gamma.class;
    }

    @Override
    public Class<Prior> getBEASTClass() {
        return Prior.class;
    }
}
