package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.evolution.substitutionmodel.Frequencies;
import beast.base.spec.evolution.substitutionmodel.HKY;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.type.Simplex;
import lphy.base.evolution.substitutionmodel.F81;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class F81ToBEAST implements GeneratorToBEAST<F81, HKY> {
    @Override
    public HKY generatorToBEAST(F81 f81, BEASTInterface value, BEASTContext context) {

        RealScalarParam<PositiveReal> kappa = new RealScalarParam<>(1.0, PositiveReal.INSTANCE);

        HKY beastF81 = new HKY();
        beastF81.setInputValue("kappa", kappa);
        beastF81.setInputValue("frequencies",
                new Frequencies((Simplex) context.getBEASTObject(f81.getFreq())));
        beastF81.initAndValidate();
        return beastF81;
    }

    @Override
    public Class<F81> getGeneratorClass() { return F81.class; }

    @Override
    public Class<HKY> getBEASTClass() {
        return HKY.class;
    }
}
