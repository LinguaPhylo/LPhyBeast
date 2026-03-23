package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.inference.Distribution;
import beast.base.inference.parameter.RealParameter;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.type.RealScalar;
import lphy.base.distribution.LogNormal;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class LogNormalToBEAST implements GeneratorToBEAST<LogNormal, Distribution> {
    @Override
    public Distribution generatorToBEAST(LogNormal generator, BEASTInterface value, BEASTContext context) {

        RealScalar<Real> M = BEASTContext.toRealScalar(
                context.getAsRealParameter(generator.getMeanLog()), Real.INSTANCE);
        RealScalar<PositiveReal> S = BEASTContext.toRealScalar(
                context.getAsRealParameter(generator.getSDLog()), PositiveReal.INSTANCE);

        beast.base.spec.inference.distribution.LogNormal dist =
                new beast.base.spec.inference.distribution.LogNormal(
                        (RealScalar<PositiveReal>) value, M, S);

        dist.setID(((BEASTInterface) value).getID() + ".prior");
        return dist;
    }

    @Override
    public Class<LogNormal> getGeneratorClass() {
        return LogNormal.class;
    }

    @Override
    public Class<Distribution> getBEASTClass() {
        return Distribution.class;
    }
}
