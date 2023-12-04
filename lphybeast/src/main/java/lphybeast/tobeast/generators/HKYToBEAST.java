package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.inference.parameter.RealParameter;
import lphy.base.evolution.substitutionmodel.HKY;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class HKYToBEAST implements GeneratorToBEAST<HKY, beast.base.evolution.substitutionmodel.HKY> {
    @Override
    public beast.base.evolution.substitutionmodel.HKY generatorToBEAST(HKY hky, BEASTInterface value, BEASTContext context) {

        beast.base.evolution.substitutionmodel.HKY beastHKY = new beast.base.evolution.substitutionmodel.HKY();
        beastHKY.setInputValue("kappa", context.getBEASTObject(hky.getKappa()));
        beastHKY.setInputValue("frequencies", BEASTContext.createBEASTFrequencies((RealParameter) context.getBEASTObject(hky.getFreq()),"A C G T"));
        beastHKY.initAndValidate();
        return beastHKY;
    }

    @Override
    public Class<HKY> getGeneratorClass() {
        return HKY.class;
    }

    @Override
    public Class<beast.base.evolution.substitutionmodel.HKY> getBEASTClass() {
        return beast.base.evolution.substitutionmodel.HKY.class;
    }
}
