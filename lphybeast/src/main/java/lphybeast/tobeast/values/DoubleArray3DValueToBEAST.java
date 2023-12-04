package lphybeast.tobeast.values;

import beast.base.inference.parameter.RealParameter;
import lphy.core.model.Value;
import lphybeast.BEASTContext;
import lphybeast.ValueToBEAST;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DoubleArray3DValueToBEAST implements ValueToBEAST<Double[][][], RealParameter> {

    @Override
    public RealParameter valueToBEAST(Value<Double[][][]> value, BEASTContext context) {

        RealParameter parameter = new RealParameter();

        Double[][][] val = value.value();

        List<Double> values = new ArrayList<>(val.length * val[0].length * val[0][0].length);
        for (Double[][] doubles : val) {
            for (Double[] doubleArray: doubles) {
                values.addAll(Arrays.asList(doubleArray));
            }
        }
        parameter.setInputValue("value", values);
        parameter.setInputValue("dimension", values.size());
        parameter.setInputValue("minordimension", val[0].length * val[0][0].length); // TODO check this!
        parameter.initAndValidate();
        ValueToParameter.setID(parameter, value);
        return parameter;
    }

    @Override
    public Class getValueClass() {
        return Double[][][].class;
    }

    @Override
    public Class<RealParameter> getBEASTClass() {
        return RealParameter.class;
    }

}
