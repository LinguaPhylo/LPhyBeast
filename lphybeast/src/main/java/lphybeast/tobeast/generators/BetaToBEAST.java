package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.inference.Distribution;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.UnitInterval;
import beast.base.spec.type.RealScalar;
import lphy.base.distribution.Beta;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class BetaToBEAST implements GeneratorToBEAST<Beta, Distribution> {
    @Override
    public Distribution generatorToBEAST(Beta generator, BEASTInterface value, BEASTContext context) {

        RealScalar<PositiveReal> alpha =
                (RealScalar<PositiveReal>) context.getAsRealScalar(generator.getParams().get("alpha"));
        RealScalar<PositiveReal> beta =
                (RealScalar<PositiveReal>) context.getAsRealScalar(generator.getParams().get("beta"));

        beast.base.spec.inference.distribution.Beta dist =
                new beast.base.spec.inference.distribution.Beta(
                        (RealScalar<UnitInterval>) value, alpha, beta);

        dist.setID(((BEASTInterface) value).getID() + ".prior");
        return dist;
    }

    @Override
    public Class<Beta> getGeneratorClass() {
        return Beta.class;
    }

    @Override
    public Class<Distribution> getBEASTClass() {
        return Distribution.class;
    }
}
