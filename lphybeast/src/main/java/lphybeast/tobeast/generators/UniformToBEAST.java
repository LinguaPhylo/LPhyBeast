package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.inference.Distribution;
import beast.base.spec.domain.Real;
import beast.base.spec.type.RealScalar;
import lphy.base.distribution.Uniform;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class UniformToBEAST implements GeneratorToBEAST<Uniform, Distribution> {
    @Override
    public Distribution generatorToBEAST(Uniform generator, BEASTInterface value, BEASTContext context) {

        RealScalar<Real> lower = BEASTContext.toRealScalar(
                context.getAsRealParameter(generator.getLower()), Real.INSTANCE);
        RealScalar<Real> upper = BEASTContext.toRealScalar(
                context.getAsRealParameter(generator.getUpper()), Real.INSTANCE);

        beast.base.spec.inference.distribution.Uniform dist =
                new beast.base.spec.inference.distribution.Uniform(
                        (RealScalar<Real>) value, lower, upper);

        dist.setID(((BEASTInterface) value).getID() + ".prior");
        return dist;
    }

    @Override
    public Class<Uniform> getGeneratorClass() {
        return Uniform.class;
    }

    @Override
    public Class<Distribution> getBEASTClass() {
        return Distribution.class;
    }
}
