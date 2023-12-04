package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.inference.distribution.Prior;
import beast.base.inference.parameter.Parameter;
import lphy.base.distribution.Poisson;
import lphy.core.logger.LoggerUtils;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class PoissonToBEAST implements GeneratorToBEAST<Poisson, Prior> {
    @Override
    public Prior generatorToBEAST(Poisson generator, BEASTInterface value, BEASTContext context) {

        // BEAST Poisson uses offset to shift value to be sampled between 0 and 1, where offset = len(states) - 1
        // for example, symmetric discrete phylogeography has locations S = len(states) and then min=S, max=S*(S-1)
        if (generator.getOffset() == null)
            throw new UnsupportedOperationException("Only offset Poisson prior is available !");
        double offset = generator.getOffset().value();

//        if (generator.getMax() != null) {
//            int max = generator.getMax().value();
//            int min = 0;
//            if (generator.getMin() != null)
//                min = generator.getMin().value();
//            // len(states) = max - min
//            offset = max - min - 1;
//        } else if (generator.getMin() != null) {
//            LoggerUtils.log.warning("LPhy Poisson maximum boundary is default to Integer.MAX_VALUE ! " +
//                    "Please define it when you defined minimum boundary " + generator.getMin().value());
//        }

        // BEAST Poisson : Input<RealParameter> lambdaInput
        beast.base.inference.distribution.Poisson poisson = new beast.base.inference.distribution.Poisson();
        poisson.setInputValue("lambda", context.getAsRealParameter(generator.getLambda()));
        if (offset != 0) {
            poisson.setInputValue("offset", offset);
            LoggerUtils.log.info("Set Poisson (" + generator.getName() + ") offset = " + offset + " in BEAST XML.");
        }
        poisson.initAndValidate();

        return BEASTContext.createPrior(poisson, (Parameter) value);
    }

    @Override
    public Class<Poisson> getGeneratorClass() {
        return Poisson.class;
    }

    @Override
    public Class<Prior> getBEASTClass() {
        return Prior.class;
    }
}
