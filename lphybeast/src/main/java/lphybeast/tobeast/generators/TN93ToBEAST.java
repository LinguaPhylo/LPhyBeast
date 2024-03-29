package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.inference.parameter.RealParameter;
import lphy.base.evolution.substitutionmodel.TN93;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class TN93ToBEAST implements GeneratorToBEAST<TN93, beast.base.evolution.substitutionmodel.TN93> {
    @Override
    public beast.base.evolution.substitutionmodel.TN93 generatorToBEAST(TN93 tn93, BEASTInterface value, BEASTContext context) {

        beast.base.evolution.substitutionmodel.TN93 beastTn93 = new beast.base.evolution.substitutionmodel.TN93();
        beastTn93.setInputValue("kappa1", context.getBEASTObject(tn93.getKappa1()));
        beastTn93.setInputValue("kappa2", context.getBEASTObject(tn93.getKappa2()));
        beastTn93.setInputValue("frequencies", BEASTContext.createBEASTFrequencies((RealParameter) context.getBEASTObject(tn93.getFreq()),"A C G T"));
        beastTn93.initAndValidate();
        return beastTn93;
    }

    @Override
    public Class<TN93> getGeneratorClass() {
        return TN93.class;
    }

    @Override
    public Class<beast.base.evolution.substitutionmodel.TN93> getBEASTClass() {
        return beast.base.evolution.substitutionmodel.TN93.class;
    }
}
