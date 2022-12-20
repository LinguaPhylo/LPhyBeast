package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.inference.distribution.Exponential;
import beast.base.inference.distribution.Prior;
import beast.base.inference.parameter.RealParameter;
import lphy.core.distributions.ExpMulti;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class ExpMultiToBEAST implements GeneratorToBEAST<ExpMulti, Prior> {
    @Override
    public Prior generatorToBEAST(ExpMulti generator, BEASTInterface value, BEASTContext context) {
        Exponential exponential = new Exponential();
        exponential.setInputValue("mean", context.getBEASTObject(generator.getParams().get("mean")));
        exponential.initAndValidate();
        return BEASTContext.createPrior(exponential, (RealParameter) value);
    }

    @Override
    public Class<ExpMulti> getGeneratorClass() {
        return ExpMulti.class;
    }

    @Override
    public Class<Prior> getBEASTClass() {
        return Prior.class;
    }
}
