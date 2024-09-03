package mm.lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import lphy.base.evolution.substitutionmodel.LewisMK;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class LewisMKToBeast implements GeneratorToBEAST<LewisMK, morphmodels.evolution.substitutionmodel.LewisMK> {
    @Override
    public morphmodels.evolution.substitutionmodel.LewisMK generatorToBEAST(LewisMK lewisMK, BEASTInterface value, BEASTContext context) {

        morphmodels.evolution.substitutionmodel.LewisMK beastLewisMK = new morphmodels.evolution.substitutionmodel.LewisMK();
        beastLewisMK.setInputValue("stateNumber", lewisMK.getNumStates().value());
        beastLewisMK.initAndValidate();
        return beastLewisMK;
    }

    @Override
    public Class<LewisMK> getGeneratorClass() { return LewisMK.class; }

    @Override
    public Class<morphmodels.evolution.substitutionmodel.LewisMK> getBEASTClass() {
        return morphmodels.evolution.substitutionmodel.LewisMK.class;
    }
}
