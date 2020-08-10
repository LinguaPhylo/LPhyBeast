package lphybeast.tobeast.generators;

import beast.core.BEASTInterface;
import beast.core.parameter.RealParameter;
import lphy.core.distributions.NormalMulti;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class NormalMultiToBEAST implements GeneratorToBEAST<NormalMulti> {
    @Override
    public BEASTInterface generatorToBEAST(NormalMulti generator, BEASTInterface value, BEASTContext context) {
        beast.math.distributions.Normal normal = new beast.math.distributions.Normal();
        normal.setInputValue("mean", context.getBEASTObject(generator.getMean()));
        normal.setInputValue("sigma", context.getBEASTObject(generator.getSd()));
        normal.initAndValidate();

        return BEASTContext.createPrior(normal, (RealParameter)value);
    }

    @Override
    public Class<NormalMulti> getGeneratorClass() {
        return NormalMulti.class;
    }

    @Override
    public Class<beast.math.distributions.Normal> getBEASTClass() {
        return beast.math.distributions.Normal.class;
    }
}
