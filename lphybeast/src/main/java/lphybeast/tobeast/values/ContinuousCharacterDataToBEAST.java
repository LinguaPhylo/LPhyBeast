package lphybeast.tobeast.values;

import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.RealVectorParam;
import lphy.base.evolution.alignment.ContinuousCharacterData;
import lphy.core.model.Value;
import lphybeast.BEASTContext;
import lphybeast.ValueToBEAST;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ContinuousCharacterDataToBEAST implements ValueToBEAST<ContinuousCharacterData, RealVectorParam> {

    @Override
    public RealVectorParam valueToBEAST(Value<ContinuousCharacterData> continuousCharacterDataValue, BEASTContext context) {

        ContinuousCharacterData continuousCharacterData = continuousCharacterDataValue.value();
        String[] taxaNames = continuousCharacterData.getTaxa().getTaxaNames();

        StringBuilder keysBuilder = new StringBuilder();
        keysBuilder.append(taxaNames[0]);
        for (int i = 1; i < taxaNames.length; i++) {
            keysBuilder.append(" ");
            keysBuilder.append(taxaNames[i]);
        }

        List<Double> allDataRowByRow = new ArrayList<>();
        for (int i = 0; i < taxaNames.length; i++) {
            allDataRowByRow.addAll(Arrays.asList(continuousCharacterData.getCharacterSequence(taxaNames[i])));
        }

        int nTaxa = taxaNames.length;
        int nTraits = continuousCharacterData.nchar();

        RealVectorParam<Real> beastParameter = new RealVectorParam<>();
        beastParameter.setInputValue("value", allDataRowByRow);
        beastParameter.setInputValue("keys", keysBuilder.toString());
        beastParameter.setInputValue("shape", nTaxa + " " + nTraits);
        beastParameter.setInputValue("domain", Real.INSTANCE);
        beastParameter.initAndValidate();

        if (!continuousCharacterDataValue.isAnonymous()) beastParameter.setID(continuousCharacterDataValue.getCanonicalId());
        return beastParameter;
    }

    @Override
    public Class getValueClass() {
        return ContinuousCharacterData.class;
    }

    @Override
    public Class<RealVectorParam> getBEASTClass() {
        return RealVectorParam.class;
    }
}
