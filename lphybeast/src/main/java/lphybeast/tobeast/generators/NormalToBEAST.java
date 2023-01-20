package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.inference.distribution.Prior;
import beast.base.inference.parameter.RealParameter;
import lphy.core.distributions.Normal;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class NormalToBEAST implements GeneratorToBEAST<Normal, Prior> {
    @Override
    public Prior generatorToBEAST(Normal generator, BEASTInterface value, BEASTContext context) {
        beast.base.inference.distribution.Normal normal = new beast.base.inference.distribution.Normal();
        normal.setInputValue("mean", context.getBEASTObject(generator.getMean()));
        normal.setInputValue("sigma", context.getBEASTObject(generator.getSd()));
        normal.initAndValidate();

        return BEASTContext.createPrior(normal, (RealParameter)value);
    }

    @Override
    public Class<Normal> getGeneratorClass() {
        return Normal.class;
    }

    @Override
    public Class<Prior> getBEASTClass() {
        return Prior.class;
    }
}
