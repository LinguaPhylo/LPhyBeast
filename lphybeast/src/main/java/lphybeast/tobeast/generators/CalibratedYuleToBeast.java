package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.evolution.speciation.CalibratedYuleModel;
import beast.base.inference.parameter.RealParameter;
import lphy.base.evolution.birthdeath.CalibratedYule;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class CalibratedYuleToBeast implements GeneratorToBEAST<CalibratedYule, CalibratedYuleModel> {

    @Override
    public CalibratedYuleModel generatorToBEAST(CalibratedYule generator, BEASTInterface value, BEASTContext context) {
        CalibratedYuleModel calibratedYuleModel = new CalibratedYuleModel();

        calibratedYuleModel.setInputValue("tree", value);

        RealParameter birthRate = context.getAsRealParameter(generator.getBirthRate());
        calibratedYuleModel.setInputValue("birthRate", birthRate);

        calibratedYuleModel.initAndValidate();

        return calibratedYuleModel;
    }

    @Override
    public Class<CalibratedYule> getGeneratorClass() {
        return CalibratedYule.class;
    }

    @Override
    public Class<CalibratedYuleModel> getBEASTClass() {
        return CalibratedYuleModel.class;
    }
}
