package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.spec.evolution.substitutionmodel.Frequencies;
import beast.base.spec.evolution.substitutionmodel.TN93;
import beast.base.spec.type.Simplex;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class TN93ToBEAST implements GeneratorToBEAST<lphy.base.evolution.substitutionmodel.TN93, TN93> {
    @Override
    public TN93 generatorToBEAST(lphy.base.evolution.substitutionmodel.TN93 tn93, BEASTInterface value, BEASTContext context) {

        TN93 beastTn93 = new TN93();
        beastTn93.setInputValue("kappa1", context.getBEASTObject(tn93.getKappa1()));
        beastTn93.setInputValue("kappa2", context.getBEASTObject(tn93.getKappa2()));
        beastTn93.setInputValue("frequencies",
                new Frequencies((Simplex) context.getBEASTObject(tn93.getFreq())));
        beastTn93.initAndValidate();
        return beastTn93;
    }

    @Override
    public Class<lphy.base.evolution.substitutionmodel.TN93> getGeneratorClass() {
        return lphy.base.evolution.substitutionmodel.TN93.class;
    }

    @Override
    public Class<TN93> getBEASTClass() {
        return TN93.class;
    }
}
