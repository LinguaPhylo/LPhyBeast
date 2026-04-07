package lphybeast.tobeast.values;

import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.RealVectorParam;
import lphy.core.model.Value;
import lphybeast.BEASTContext;
import lphybeast.ValueToBEAST;

import java.util.*;

public class MapStringDoubleArrayValueToBEAST implements ValueToBEAST<Map<String, Double[]>, RealVectorParam> {

    @Override
    public RealVectorParam valueToBEAST(Value<Map<String, Double[]>> value, BEASTContext context) {

        Map<String, Double[]> map = value.value();

        SortedMap<String, Double[]> sortedMap = null;
        if (map instanceof SortedMap) {
            sortedMap = (SortedMap<String, Double[]>) map;
        } else {
            sortedMap = new TreeMap<>();
            sortedMap.putAll(map);
        }

        int ncols = 0;
        String[] keys = new String[sortedMap.size()];
        List<Double> values = new ArrayList<>();
        int keyIndex = 0;
        for (Map.Entry<String, Double[]> entry : sortedMap.entrySet()) {
            keys[keyIndex++] = entry.getKey();
            ncols = entry.getValue().length;
            values.addAll(Arrays.asList(entry.getValue()));
        }

        StringBuilder keysBuilder = new StringBuilder();
        keysBuilder.append(keys[0]);
        for (int i = 1; i < keys.length; i++) {
            keysBuilder.append(" ");
            keysBuilder.append(keys[i]);
        }

        int nrows = sortedMap.size();

        RealVectorParam<Real> parameter = new RealVectorParam<>();
        parameter.setInputValue("value", values);
        parameter.setInputValue("keys", keysBuilder.toString());
        parameter.setInputValue("shape", nrows + " " + ncols);
        parameter.setInputValue("domain", Real.INSTANCE);
        parameter.initAndValidate();
        if (!value.isAnonymous()) parameter.setID(value.getCanonicalId());

        return parameter;
    }

    @Override
    public Class getValueClass() {
        return Map.class;
    }

    @Override
    public Class<RealVectorParam> getBEASTClass() {
        return RealVectorParam.class;
    }

}
