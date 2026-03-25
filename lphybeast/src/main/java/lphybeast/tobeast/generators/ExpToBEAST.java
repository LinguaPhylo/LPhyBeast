package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.inference.Distribution;
import beast.base.spec.domain.NonNegativeReal;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.type.RealScalar;
import lphy.base.distribution.Exp;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class ExpToBEAST implements GeneratorToBEAST<Exp, Distribution> {
    @Override
    public Distribution generatorToBEAST(Exp generator, BEASTInterface value, BEASTContext context) {

        RealScalar<PositiveReal> mean =
                (RealScalar<PositiveReal>) context.getAsRealScalar(generator.getParams().get("mean"));

        beast.base.spec.inference.distribution.Exponential dist =
                new beast.base.spec.inference.distribution.Exponential(
                        (RealScalar<NonNegativeReal>) BEASTContext.ensureRealScalar(value), mean);

        dist.setID(((BEASTInterface) value).getID() + ".prior");
        return dist;
    }

    @Override
    public Class<Exp> getGeneratorClass() {
        return Exp.class;
    }

    @Override
    public Class<Distribution> getBEASTClass() {
        return Distribution.class;
    }
}
