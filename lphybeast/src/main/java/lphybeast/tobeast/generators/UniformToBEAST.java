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

        RealScalar<Real> lower = (RealScalar<Real>) context.getAsRealScalar(generator.getLower());
        RealScalar<Real> upper = (RealScalar<Real>) context.getAsRealScalar(generator.getUpper());

        beast.base.spec.inference.distribution.Uniform dist =
                new beast.base.spec.inference.distribution.Uniform(
                        (RealScalar<Real>) BEASTContext.ensureRealScalar(value), lower, upper);

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
