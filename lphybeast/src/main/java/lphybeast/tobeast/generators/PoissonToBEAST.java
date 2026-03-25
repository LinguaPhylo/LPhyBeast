package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.inference.Distribution;
import beast.base.spec.domain.NonNegativeReal;
import beast.base.spec.inference.distribution.OffsetInt;
import beast.base.spec.inference.distribution.Poisson;
import beast.base.spec.type.RealScalar;
import lphy.core.logger.LoggerUtils;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class PoissonToBEAST implements GeneratorToBEAST<lphy.base.distribution.Poisson, Distribution> {
    @Override
    public Distribution generatorToBEAST(lphy.base.distribution.Poisson generator, BEASTInterface value, BEASTContext context) {

        if (generator.getOffset() == null)
            throw new UnsupportedOperationException("Only offset Poisson prior is available !");
        double offset = generator.getOffset().value();

        RealScalar<NonNegativeReal> lambda =
                (RealScalar<NonNegativeReal>) context.getAsRealScalar(generator.getLambda());

        Poisson poisson = new Poisson();
        poisson.setInputValue("lambda", lambda);
        poisson.initAndValidate();

        if (offset != 0) {
            LoggerUtils.log.info("Set Poisson (" + generator.getName() + ") offset = " + (int) offset + " in BEAST XML.");
            OffsetInt offsetDist = new OffsetInt();
            offsetDist.setInputValue("distribution", poisson);
            offsetDist.setInputValue("offset", (int) offset);
            offsetDist.setInputValue("param", value);
            offsetDist.setID(((BEASTInterface) value).getID() + ".prior");
            offsetDist.initAndValidate();
            return offsetDist;
        } else {
            poisson.setInputValue("param", value);
            poisson.setID(((BEASTInterface) value).getID() + ".prior");
            poisson.initAndValidate();
            return poisson;
        }
    }

    @Override
    public Class<lphy.base.distribution.Poisson> getGeneratorClass() {
        return lphy.base.distribution.Poisson.class;
    }

    @Override
    public Class<Distribution> getBEASTClass() {
        return Distribution.class;
    }
}
