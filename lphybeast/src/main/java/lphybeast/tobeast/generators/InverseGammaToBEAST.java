package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.inference.Distribution;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.UnitInterval;
import beast.base.spec.type.RealScalar;
import lphy.base.distribution.InverseGamma;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class InverseGammaToBEAST implements GeneratorToBEAST<InverseGamma, Distribution> {
    @Override
    public Distribution generatorToBEAST(InverseGamma generator, BEASTInterface value, BEASTContext context) {

        RealScalar<PositiveReal> alpha =
                (RealScalar<PositiveReal>) context.getAsRealScalar(generator.getAlpha());
        RealScalar<PositiveReal> beta =
                (RealScalar<PositiveReal>) context.getAsRealScalar(generator.getBeta());

        beast.base.spec.inference.distribution.InverseGamma dist =
                new beast.base.spec.inference.distribution.InverseGamma(
                        (RealScalar<UnitInterval>) BEASTContext.ensureRealScalar(value), alpha, beta);

        dist.setID(((BEASTInterface) value).getID() + ".prior");
        return dist;
    }

    @Override
    public Class<InverseGamma> getGeneratorClass() {
        return InverseGamma.class;
    }

    @Override
    public Class<Distribution> getBEASTClass() {
        return Distribution.class;
    }
}
