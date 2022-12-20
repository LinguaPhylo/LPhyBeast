package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import lphy.evolution.substitutionmodel.JukesCantor;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class JukesCantorToBEAST implements
        GeneratorToBEAST<JukesCantor, beast.base.evolution.substitutionmodel.JukesCantor> {
    @Override
    public beast.base.evolution.substitutionmodel.JukesCantor generatorToBEAST(JukesCantor jukesCantor, BEASTInterface value, BEASTContext context) {


        beast.base.evolution.substitutionmodel.JukesCantor beastJC = new beast.base.evolution.substitutionmodel.JukesCantor();
        beastJC.initAndValidate();
        return beastJC;
    }

    @Override
    public Class<JukesCantor> getGeneratorClass() {
        return JukesCantor.class;
    }

    @Override
    public Class<beast.base.evolution.substitutionmodel.JukesCantor> getBEASTClass() {
        return beast.base.evolution.substitutionmodel.JukesCantor.class;
    }
}
