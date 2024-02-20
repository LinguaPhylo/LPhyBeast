package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.evolution.speciation.YuleModel;
import beast.base.inference.parameter.RealParameter;
import lphy.base.evolution.birthdeath.Yule;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class YuleToBEAST implements GeneratorToBEAST<Yule, YuleModel> {

    @Override
    public YuleModel generatorToBEAST(Yule generator, BEASTInterface value, BEASTContext context) {
        YuleModel yuleModel = new YuleModel();

        yuleModel.setInputValue("tree", value);
        RealParameter b = context.getAsRealParameter(generator.getBirthRate());
        yuleModel.setInputValue("birthDiffRate", b);
        yuleModel.initAndValidate();

        return yuleModel;
    }

    @Override
    public Class<Yule> getGeneratorClass() {
        return Yule.class;
    }

    @Override
    public Class<YuleModel> getBEASTClass() {
        return YuleModel.class;
    }
}
