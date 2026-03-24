package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.inference.Distribution;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.type.RealScalar;
import lphy.base.distribution.Gamma;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class GammaToBEAST implements GeneratorToBEAST<Gamma, Distribution> {
    @Override
    public Distribution generatorToBEAST(Gamma generator, BEASTInterface value, BEASTContext context) {

        RealScalar<PositiveReal> alpha = BEASTContext.toRealScalar(
                context.getAsRealParameter(generator.getShape()), PositiveReal.INSTANCE);
        RealScalar<PositiveReal> theta = BEASTContext.toRealScalar(
                context.getAsRealParameter(generator.getScale()), PositiveReal.INSTANCE);

        beast.base.spec.inference.distribution.Gamma dist =
                new beast.base.spec.inference.distribution.Gamma(
                        (RealScalar<PositiveReal>) value, alpha, theta);

        dist.setID(((BEASTInterface) value).getID() + ".prior");
        return dist;
    }

    @Override
    public Class<Gamma> getGeneratorClass() {
        return Gamma.class;
    }

    @Override
    public Class<Distribution> getBEASTClass() {
        return Distribution.class;
    }
}
