package lphybeast.tobeast.values;

import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.RealVectorParam;
import lphy.core.model.Value;
import lphybeast.BEASTContext;
import lphybeast.ValueToBEAST;

public class DoubleArray2DValueToBEAST implements ValueToBEAST<Double[][], RealVectorParam> {

    @Override
    public RealVectorParam valueToBEAST(Value<Double[][]> value, BEASTContext context) {
        Double[][] val = value.value();
        int total = val.length * val[0].length;
        double[] dvals = new double[total];
        int k = 0;
        for (Double[] row : val)
            for (Double d : row)
                dvals[k++] = d;
        RealVectorParam<Real> param = new RealVectorParam<>(dvals, Real.INSTANCE);
        param.setID(value.getCanonicalId());
        return param;
    }

    @Override
    public Class getValueClass() {
        return Double[][].class;
    }

    @Override
    public Class<RealVectorParam> getBEASTClass() {
        return RealVectorParam.class;
    }

}
