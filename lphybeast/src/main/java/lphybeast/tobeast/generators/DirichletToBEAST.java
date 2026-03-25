package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.inference.Distribution;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.type.RealVector;
import beast.base.spec.type.Simplex;
import lphy.base.distribution.Dirichlet;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class DirichletToBEAST implements GeneratorToBEAST<Dirichlet, Distribution> {
    @Override
    public Distribution generatorToBEAST(Dirichlet generator, BEASTInterface value, BEASTContext context) {

        RealVector<PositiveReal> alpha = (RealVector<PositiveReal>) context.getAsRealVector(generator.getConcentration());

        beast.base.spec.inference.distribution.Dirichlet dirichlet =
                new beast.base.spec.inference.distribution.Dirichlet((Simplex) value, alpha);

        dirichlet.setID(((BEASTInterface) value).getID() + ".prior");
        return dirichlet;
    }

    @Override
    public Class<Dirichlet> getGeneratorClass() {
        return Dirichlet.class;
    }

    @Override
    public Class<Distribution> getBEASTClass() {
        return Distribution.class;
    }
}
