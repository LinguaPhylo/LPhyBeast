package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.inference.Distribution;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.type.RealScalar;
import lphy.base.distribution.Normal;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class NormalToBEAST implements GeneratorToBEAST<Normal, Distribution> {
    @Override
    public Distribution generatorToBEAST(Normal generator, BEASTInterface value, BEASTContext context) {

        RealScalar<Real> mean =
                (RealScalar<Real>) context.getAsRealScalar(generator.getMean());
        RealScalar<PositiveReal> sigma =
                (RealScalar<PositiveReal>) context.getAsRealScalar(generator.getSd());

        beast.base.spec.inference.distribution.Normal dist =
                new beast.base.spec.inference.distribution.Normal();
        dist.setInputValue("mean", mean);
        dist.setInputValue("sigma", sigma);
        if (value != null) {
            dist.setInputValue("param", value);
            dist.setID(((BEASTInterface) value).getID() + ".prior");
        }
        dist.initAndValidate();
        return dist;
    }

    @Override
    public Class<Normal> getGeneratorClass() {
        return Normal.class;
    }

    @Override
    public Class<Distribution> getBEASTClass() {
        return Distribution.class;
    }
}
