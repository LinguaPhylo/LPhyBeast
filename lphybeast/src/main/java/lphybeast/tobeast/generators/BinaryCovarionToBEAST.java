package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.spec.evolution.substitutionmodel.BinaryCovarion;
import lphy.core.model.Value;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

import static lphy.base.evolution.substitutionmodel.BinaryCovarion.*;

public class BinaryCovarionToBEAST implements GeneratorToBEAST<lphy.base.evolution.substitutionmodel.BinaryCovarion, BinaryCovarion> {
    @Override
    public BinaryCovarion generatorToBEAST(
            lphy.base.evolution.substitutionmodel.BinaryCovarion binaryCovarion, BEASTInterface value, BEASTContext context) {

        BinaryCovarion beastBinaryCovarion = new BinaryCovarion();

        beastBinaryCovarion.setInputValue("alpha", context.getBEASTObject(binaryCovarion.getParams().get(AlphaParamName)));
        beastBinaryCovarion.setInputValue("switchRate", context.getBEASTObject(binaryCovarion.getParams().get(SwitchRateParamName)));
        beastBinaryCovarion.setInputValue("vfrequencies", context.getBEASTObject(binaryCovarion.getParams().get(vfreqParamName)));
        beastBinaryCovarion.setInputValue("hfrequencies", context.getBEASTObject(binaryCovarion.getParams().get(hfreqParamName)));

        beastBinaryCovarion.initAndValidate();
        return beastBinaryCovarion;
    }

    @Override
    public Class<lphy.base.evolution.substitutionmodel.BinaryCovarion> getGeneratorClass() { return lphy.base.evolution.substitutionmodel.BinaryCovarion.class; }

    @Override
    public Class<BinaryCovarion> getBEASTClass() {
        return BinaryCovarion.class;
    }
}
