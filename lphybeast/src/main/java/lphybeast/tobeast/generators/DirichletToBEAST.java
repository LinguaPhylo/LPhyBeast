package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.inference.distribution.Prior;
import beast.base.inference.parameter.RealParameter;
import lphy.base.distribution.Dirichlet;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class DirichletToBEAST implements GeneratorToBEAST<Dirichlet, Prior> {
    @Override
    public Prior generatorToBEAST(Dirichlet generator, BEASTInterface value, BEASTContext context) {
        beast.base.inference.distribution.Dirichlet beastDirichlet = new beast.base.inference.distribution.Dirichlet();
        beastDirichlet.setInputValue("alpha", context.getAsRealParameter(generator.getConcentration()));
        beastDirichlet.setInputValue("sum", generator.getSum().value().doubleValue());
        beastDirichlet.initAndValidate();

        return BEASTContext.createPrior(beastDirichlet, (RealParameter) value);
    }

    @Override
    public Class<Dirichlet> getGeneratorClass() {
        return Dirichlet.class;
    }

    @Override
    public Class<Prior> getBEASTClass() {
        return Prior.class;
    }
}
