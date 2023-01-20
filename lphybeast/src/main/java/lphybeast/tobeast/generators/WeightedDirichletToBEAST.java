package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.core.Function;
import beast.base.inference.distribution.Prior;
import lphy.core.distributions.WeightedDirichlet;
import lphy.graphicalModel.Value;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class WeightedDirichletToBEAST implements GeneratorToBEAST<WeightedDirichlet, Prior> {
    @Override
    public Prior generatorToBEAST(WeightedDirichlet generator, BEASTInterface value, BEASTContext context) {

        Value<Number[]> concentration = generator.getConcentration();

        beastlabs.math.distributions.WeightedDirichlet beastDirichlet =
                new beastlabs.math.distributions.WeightedDirichlet();
        beastDirichlet.setInputValue("alpha", context.getAsRealParameter(concentration));
        beastDirichlet.setInputValue("weights", context.getAsIntegerParameter(generator.getWeights()));
        beastDirichlet.initAndValidate();

        return BEASTContext.createPrior(beastDirichlet, (Function) value);
    }

    @Override
    public Class<WeightedDirichlet> getGeneratorClass() {
        return WeightedDirichlet.class;
    }

    @Override
    public Class<Prior> getBEASTClass() {
        return Prior.class;
    }

//    private boolean allOne(Value<Number[]> concentration) {
//        Number[] conc = concentration.value();
//        for (Number num : conc)
//            if (num.doubleValue() != 1.0) return false;
//        return true;
//    }
}
