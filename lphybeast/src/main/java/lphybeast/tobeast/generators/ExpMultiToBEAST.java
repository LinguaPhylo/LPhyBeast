package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.inference.Distribution;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.inference.distribution.Exponential;
import beast.base.spec.inference.distribution.IID;
import beast.base.spec.type.RealScalar;
import lphy.base.distribution.ExpMulti;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class ExpMultiToBEAST implements GeneratorToBEAST<ExpMulti, Distribution> {
    @Override
    public Distribution generatorToBEAST(ExpMulti generator, BEASTInterface value, BEASTContext context) {
        RealScalar<PositiveReal> mean =
                (RealScalar<PositiveReal>) context.getAsRealScalar(generator.getParams().get("mean"));

        Exponential exponential = new Exponential();
        exponential.setInputValue("mean", mean);
        exponential.initAndValidate();

        IID iid = new IID();
        iid.setInputValue("distr", exponential);
        iid.setInputValue("param", value);
        iid.setID(((BEASTInterface) value).getID() + ".prior");
        iid.initAndValidate();
        return iid;
    }

    @Override
    public Class<ExpMulti> getGeneratorClass() {
        return ExpMulti.class;
    }

    @Override
    public Class<Distribution> getBEASTClass() {
        return Distribution.class;
    }
}
