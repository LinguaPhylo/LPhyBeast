package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.inference.parameter.RealParameter;
import lphy.base.evolution.substitutionmodel.WAG;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class WAGToBEAST implements GeneratorToBEAST<WAG, beast.base.evolution.substitutionmodel.WAG> {
    @Override
    public beast.base.evolution.substitutionmodel.WAG generatorToBEAST(WAG wag, BEASTInterface value, BEASTContext context) {

        beast.base.evolution.substitutionmodel.WAG beastWAG = new beast.base.evolution.substitutionmodel.WAG();
        if (wag.getFreq() != null) {
            beastWAG.setInputValue("frequencies", BEASTContext.createBEASTFrequencies((RealParameter) context.getBEASTObject(wag.getFreq()), "A C D E F G H I K L M N P Q R S T V W Y"));
        }
        beastWAG.initAndValidate();
        return beastWAG;
    }

    @Override
    public Class<WAG> getGeneratorClass() { return WAG.class; }

    @Override
    public Class<beast.base.evolution.substitutionmodel.WAG> getBEASTClass() {
        return beast.base.evolution.substitutionmodel.WAG.class;
    }
}
