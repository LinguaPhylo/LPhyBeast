package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.inference.parameter.RealParameter;
import lphy.base.evolution.substitutionmodel.BinaryCovarion;
import lphy.core.model.Value;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

import static lphy.base.evolution.substitutionmodel.BinaryCovarion.*;

public class BinaryCovarionToBEAST implements GeneratorToBEAST<BinaryCovarion, beast.base.evolution.substitutionmodel.BinaryCovarion> {
    @Override
    public beast.base.evolution.substitutionmodel.BinaryCovarion generatorToBEAST(
            BinaryCovarion binaryCovarion, BEASTInterface value, BEASTContext context) {

        beast.base.evolution.substitutionmodel.BinaryCovarion beastBinaryCovarion =
                new beast.base.evolution.substitutionmodel.BinaryCovarion();

        Value a = binaryCovarion.getParams().get(AlphaParamName);
        RealParameter alpha = context.getAsRealParameter(a);
        Value s = binaryCovarion.getParams().get(SwitchRateParamName);
        RealParameter switchRate = context.getAsRealParameter(s);
        Value vf = binaryCovarion.getParams().get(vfreqParamName);
        RealParameter vfrequencies = context.getAsRealParameter(vf);
        Value hf = binaryCovarion.getParams().get(hfreqParamName);
        RealParameter hfrequencies = context.getAsRealParameter(hf);
        beastBinaryCovarion.setInputValue("alpha", alpha);
        beastBinaryCovarion.setInputValue("switchRate", switchRate);
        beastBinaryCovarion.setInputValue("vfrequencies", vfrequencies);
        beastBinaryCovarion.setInputValue("hfrequencies", hfrequencies);

        beastBinaryCovarion.initAndValidate();
        return beastBinaryCovarion;
    }

    @Override
    public Class<BinaryCovarion> getGeneratorClass() { return BinaryCovarion.class; }

    @Override
    public Class<beast.base.evolution.substitutionmodel.BinaryCovarion> getBEASTClass() {
        return beast.base.evolution.substitutionmodel.BinaryCovarion.class;
    }
}
