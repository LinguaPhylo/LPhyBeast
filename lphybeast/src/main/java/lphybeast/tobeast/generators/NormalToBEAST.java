package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.inference.Distribution;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.type.RealScalar;
import lphy.base.distribution.Normal;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class NormalToBEAST implements GeneratorToBEAST<Normal, Distribution> {
    @Override
    public Distribution generatorToBEAST(Normal generator, BEASTInterface value, BEASTContext context) {

        RealScalar<Real> mean = BEASTContext.toRealScalar(
                context.getAsRealParameter(generator.getMean()), Real.INSTANCE);
        RealScalar<PositiveReal> sigma = BEASTContext.toRealScalar(
                context.getAsRealParameter(generator.getSd()), PositiveReal.INSTANCE);

        beast.base.spec.inference.distribution.Normal dist =
                new beast.base.spec.inference.distribution.Normal(
                        (RealScalar<Real>) value, mean, sigma);

        dist.setID(((BEASTInterface) value).getID() + ".prior");
        return dist;
    }

    @Override
    public Class<Normal> getGeneratorClass() {
        return Normal.class;
    }

    @Override
    public Class<Distribution> getBEASTClass() {
        return Distribution.class;
    }
}
