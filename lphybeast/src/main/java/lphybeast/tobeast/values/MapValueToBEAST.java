package lphybeast.tobeast.values;

import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.RealVectorParam;
import lphy.core.model.Value;
import lphybeast.BEASTContext;
import lphybeast.ValueToBEAST;

import java.util.*;

public class MapValueToBEAST implements ValueToBEAST<Map<String, Double>, RealVectorParam> {

    @Override
    public RealVectorParam valueToBEAST(Value<Map<String, Double>> value, BEASTContext context) {

        Map<String, Double> map = value.value();

        SortedMap<String, Double> sortedMap;
        if (map instanceof SortedMap) {
            sortedMap = (SortedMap<String, Double>)map;
        } else {
            sortedMap = new TreeMap<>(map);
        }

        String[] keys = new String[sortedMap.size()];
        List<Double> values = new ArrayList<>();
        for (Map.Entry<String, Double> entry : sortedMap.entrySet()) {
            keys[values.size()] = entry.getKey();
            values.add(entry.getValue());
        }

        StringBuilder builder = new StringBuilder();
        builder.append(keys[0]);
        for (int i = 1; i < keys.length; i++) {
            builder.append(" ");
            builder.append(keys[i]);
        }

        RealVectorParam<Real> parameter = new RealVectorParam<>();
        parameter.setInputValue("value", values);
        parameter.setInputValue("keys", builder.toString());
        parameter.initAndValidate();
        if (!value.isAnonymous()) parameter.setID(value.getCanonicalId());

        return parameter;
    }

    @Override
    public Class getValueClass() {
        return java.util.Map.class;
    }

    @Override
    public Class<RealVectorParam> getBEASTClass() {
        return RealVectorParam.class;
    }

}
