package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.inference.distribution.Prior;
import beast.base.inference.parameter.RealParameter;
import lphy.base.distribution.InverseGamma;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class InverseGammaToBEAST implements GeneratorToBEAST<InverseGamma, Prior> {
    @Override
    public Prior generatorToBEAST(InverseGamma generator, BEASTInterface value, BEASTContext context) {
        beast.base.inference.distribution.InverseGamma inverseGamma = new beast.base.inference.distribution.InverseGamma();
        inverseGamma.setInputValue("alpha", context.getBEASTObject(generator.getAlpha()));
        inverseGamma.setInputValue("beta", context.getBEASTObject(generator.getBeta()));
        inverseGamma.initAndValidate();
        return BEASTContext.createPrior(inverseGamma, (RealParameter) value);
    }

    @Override
    public Class<InverseGamma> getGeneratorClass() {
        return InverseGamma.class;
    }

    @Override
    public Class<Prior> getBEASTClass() {
        return Prior.class;
    }
}
