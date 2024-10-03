package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.evolution.speciation.CalibratedYuleModel;
import lphy.base.evolution.birthdeath.CalibratedYule;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class CalibratedYuleToBeast implements GeneratorToBEAST<CalibratedYule, CalibratedYuleModel> {

    @Override
    public CalibratedYuleModel generatorToBEAST(CalibratedYule generator, BEASTInterface value, BEASTContext context) {
        CalibratedYuleModel calibratedYuleModel = new CalibratedYuleModel();

        calibratedYuleModel.setInputValue("tree", value);
        calibratedYuleModel.setInputValue("birthRate", context.getAsRealParameter(generator.getBirthRate()));

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
