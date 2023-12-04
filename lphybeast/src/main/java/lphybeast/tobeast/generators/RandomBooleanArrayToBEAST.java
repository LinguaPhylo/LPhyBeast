package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.inference.distribution.ParametricDistribution;
import beast.base.inference.distribution.Prior;
import beast.base.inference.parameter.IntegerParameter;
import lphy.base.distribution.RandomBooleanArray;
import lphy.core.model.Generator;
import lphy.core.model.Value;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

import java.util.Objects;

public class RandomBooleanArrayToBEAST implements GeneratorToBEAST<RandomBooleanArray, Prior> {
    @Override
    public Prior generatorToBEAST(RandomBooleanArray generator, BEASTInterface value, BEASTContext context) {
        // Poisson => S => RandomBooleanArray => I
        Value s = Objects.requireNonNull(generator.getParams().get("hammingWeight"));
        BEASTInterface sI = context.getBEASTObject(s);
        if ( !(sI instanceof IntegerParameter) )
            throw new IllegalStateException("Expecting the mapped BEAST IntegerParameter S from hammingWeight !");
        // rm IntegerParameter S
        context.removeBEASTObject(sI);

        // Poisson
        Generator poissonGenerator = s.getGenerator();
        Prior poissonPrior = (Prior) context.getBEASTObject(poissonGenerator);
        // need distr not prior
        context.removeBEASTObject(poissonPrior);

        ParametricDistribution dist = poissonPrior.distInput.get();
        beast.base.evolution.Sum x = new beast.base.evolution.Sum();
        x.setInputValue("arg", value);
        Prior prior = BEASTContext.createPrior(dist, x);
        prior.setID(value.getID() + ".prior");
        return prior;
    }

    @Override
    public Class<RandomBooleanArray> getGeneratorClass() {
        return RandomBooleanArray.class;
    }

    @Override
    public Class<Prior> getBEASTClass() {
        return Prior.class;
    }
}
