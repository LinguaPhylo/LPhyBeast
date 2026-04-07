package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.inference.Distribution;
import beast.base.spec.type.RealVector;
import lphy.base.distribution.WeightedDirichlet;
import lphy.core.model.Value;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class WeightedDirichletToBEAST implements GeneratorToBEAST<WeightedDirichlet, Distribution> {
    @Override
    public Distribution generatorToBEAST(WeightedDirichlet generator, BEASTInterface value, BEASTContext context) {

        Value<Number[]> concentration = generator.getConcentration();

        RealVector<?> alpha = context.getAsRealVector(concentration);
        RealVector<?> weights = context.getAsRealVector(generator.getWeights());

        beastlabs.math.distributions.WeightedDirichlet beastDirichlet =
                new beastlabs.math.distributions.WeightedDirichlet();
        beastDirichlet.setInputValue("param", value);
        beastDirichlet.setInputValue("alpha", alpha);
        beastDirichlet.setInputValue("weights", weights);
        beastDirichlet.initAndValidate();

        beastDirichlet.setID(((BEASTInterface) value).getID() + ".prior");
        return beastDirichlet;
    }

    @Override
    public Class<WeightedDirichlet> getGeneratorClass() {
        return WeightedDirichlet.class;
    }

    @Override
    public Class<Distribution> getBEASTClass() {
        return Distribution.class;
    }
}
