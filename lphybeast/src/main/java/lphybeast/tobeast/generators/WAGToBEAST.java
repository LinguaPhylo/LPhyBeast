package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.spec.evolution.substitutionmodel.Frequencies;
import beast.base.spec.evolution.substitutionmodel.WAG;
import beast.base.spec.type.Simplex;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class WAGToBEAST implements GeneratorToBEAST<lphy.base.evolution.substitutionmodel.WAG, WAG> {
    @Override
    public WAG generatorToBEAST(lphy.base.evolution.substitutionmodel.WAG wag, BEASTInterface value, BEASTContext context) {

        WAG beastWAG = new WAG();
        if (wag.getFreq() != null) {
            beastWAG.setInputValue("frequencies",
                    new Frequencies((Simplex) context.getBEASTObject(wag.getFreq())));
        }
        beastWAG.initAndValidate();
        return beastWAG;
    }

    @Override
    public Class<lphy.base.evolution.substitutionmodel.WAG> getGeneratorClass() { return lphy.base.evolution.substitutionmodel.WAG.class; }

    @Override
    public Class<WAG> getBEASTClass() {
        return WAG.class;
    }
}
