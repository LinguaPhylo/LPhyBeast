package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.inference.distribution.Exponential;
import beast.base.inference.distribution.Prior;
import beast.base.inference.parameter.RealParameter;
import lphy.base.distribution.Exp;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class ExpToBEAST implements GeneratorToBEAST<Exp, Prior> {
    @Override
    public Prior generatorToBEAST(Exp generator, BEASTInterface value, BEASTContext context) {
        Exponential exponential = new Exponential();
        exponential.setInputValue("mean", context.getBEASTObject(generator.getParams().get("mean")));
        exponential.initAndValidate();
        return BEASTContext.createPrior(exponential, (RealParameter) value);
    }

    @Override
    public Class<Exp> getGeneratorClass() {
        return Exp.class;
    }

    @Override
    public Class<Prior> getBEASTClass() {
        return Prior.class;
    }
}
