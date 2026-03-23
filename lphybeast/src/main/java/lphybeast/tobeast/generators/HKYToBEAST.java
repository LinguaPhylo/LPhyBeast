package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.spec.evolution.substitutionmodel.Frequencies;
import beast.base.spec.evolution.substitutionmodel.HKY;
import beast.base.spec.type.RealScalar;
import beast.base.spec.type.Simplex;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class HKYToBEAST implements GeneratorToBEAST<lphy.base.evolution.substitutionmodel.HKY, HKY> {
    @Override
    public HKY generatorToBEAST(lphy.base.evolution.substitutionmodel.HKY hky, BEASTInterface value, BEASTContext context) {

        HKY beastHKY = new HKY();
        beastHKY.setInputValue("kappa", context.getBEASTObject(hky.getKappa()));
        beastHKY.setInputValue("frequencies",
                new Frequencies((Simplex) context.getBEASTObject(hky.getFreq())));
        beastHKY.initAndValidate();
        return beastHKY;
    }

    @Override
    public Class<lphy.base.evolution.substitutionmodel.HKY> getGeneratorClass() {
        return lphy.base.evolution.substitutionmodel.HKY.class;
    }

    @Override
    public Class<HKY> getBEASTClass() {
        return HKY.class;
    }
}
