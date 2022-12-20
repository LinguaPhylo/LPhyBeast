package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.inference.distribution.LogNormalDistributionModel;
import beast.base.inference.distribution.Prior;
import beast.base.inference.parameter.RealParameter;
import lphy.core.distributions.LogNormal;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class LogNormalToBEAST implements GeneratorToBEAST<LogNormal, Prior> {
    @Override
    public Prior generatorToBEAST(LogNormal generator, BEASTInterface value, BEASTContext context) {
        LogNormalDistributionModel logNormalDistributionModel = new LogNormalDistributionModel();
        logNormalDistributionModel.setInputValue("M", context.getAsRealParameter(generator.getMeanLog()));
        logNormalDistributionModel.setInputValue("S", context.getAsRealParameter(generator.getSDLog()));
        logNormalDistributionModel.initAndValidate();

        return BEASTContext.createPrior(logNormalDistributionModel, (RealParameter) value);
    }

    @Override
    public Class<LogNormal> getGeneratorClass() {
        return LogNormal.class;
    }

    @Override
    public Class<Prior> getBEASTClass() {
        return Prior.class;
    }
}
