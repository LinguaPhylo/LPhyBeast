package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.spec.evolution.substitutionmodel.Frequencies;
import beast.base.spec.evolution.substitutionmodel.HKY;
import beast.base.spec.inference.parameter.SimplexParam;
import lphy.base.evolution.substitutionmodel.K80;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class K80ToBEAST implements GeneratorToBEAST<K80, HKY> {
    @Override
    public HKY generatorToBEAST(K80 k80, BEASTInterface value, BEASTContext context) {

        SimplexParam equalFreqs = new SimplexParam(new double[]{0.25, 0.25, 0.25, 0.25});

        HKY beastHKY = new HKY();
        beastHKY.setInputValue("kappa", context.getBEASTObject(k80.getKappa()));
        beastHKY.setInputValue("frequencies", new Frequencies(equalFreqs));
        beastHKY.initAndValidate();
        return beastHKY;
    }

    @Override
    public Class<K80> getGeneratorClass() {
        return K80.class;
    }

    @Override
    public Class<HKY> getBEASTClass() {
        return HKY.class;
    }
}
