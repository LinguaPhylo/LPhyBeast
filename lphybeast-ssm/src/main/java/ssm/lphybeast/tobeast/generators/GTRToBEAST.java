package ssm.lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.spec.evolution.substitutionmodel.Frequencies;
import beast.base.spec.type.Simplex;
import lphy.base.evolution.substitutionmodel.GTR;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class GTRToBEAST implements GeneratorToBEAST<GTR, substmodels.nucleotide.GTR> {
    @Override
    public substmodels.nucleotide.GTR generatorToBEAST(GTR gtr, BEASTInterface value, BEASTContext context) {

        substmodels.nucleotide.GTR beastGTR = new substmodels.nucleotide.GTR();

        beastGTR.setInputValue("rates", context.getBEASTObject(gtr.getRates()));
        beastGTR.setInputValue("frequencies",
                new Frequencies((Simplex) context.getBEASTObject(gtr.getFreq())));
        beastGTR.initAndValidate();
        return beastGTR;
    }

    @Override
    public Class<GTR> getGeneratorClass() { return GTR.class; }

    @Override
    public Class<substmodels.nucleotide.GTR> getBEASTClass() {
        return substmodels.nucleotide.GTR.class;
    }
}
