package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.spec.evolution.substitutionmodel.JukesCantor;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class JukesCantorToBEAST implements GeneratorToBEAST<lphy.base.evolution.substitutionmodel.JukesCantor, JukesCantor> {
    @Override
    public JukesCantor generatorToBEAST(lphy.base.evolution.substitutionmodel.JukesCantor jukesCantor, BEASTInterface value, BEASTContext context) {

        JukesCantor beastJC = new JukesCantor();
        beastJC.initAndValidate();
        return beastJC;
    }

    @Override
    public Class<lphy.base.evolution.substitutionmodel.JukesCantor> getGeneratorClass() {
        return lphy.base.evolution.substitutionmodel.JukesCantor.class;
    }

    @Override
    public Class<JukesCantor> getBEASTClass() {
        return JukesCantor.class;
    }
}
