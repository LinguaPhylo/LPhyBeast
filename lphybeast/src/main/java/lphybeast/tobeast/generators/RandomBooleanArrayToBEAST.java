package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.inference.Distribution;
import beast.base.inference.StateNode;
import beast.base.spec.inference.distribution.OffsetInt;
import lphy.base.distribution.RandomBooleanArray;
import lphy.core.model.Generator;
import lphy.core.model.Value;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

import java.util.Objects;

public class RandomBooleanArrayToBEAST implements GeneratorToBEAST<RandomBooleanArray, Distribution> {
    @Override
    public Distribution generatorToBEAST(RandomBooleanArray generator, BEASTInterface value, BEASTContext context) {
        // Poisson => S => RandomBooleanArray => I
        Value s = Objects.requireNonNull(generator.getParams().get("hammingWeight"));
        BEASTInterface sI = context.getBEASTObject(s);
        if ( !(sI instanceof StateNode) )
            throw new IllegalStateException("Expecting a mapped BEAST StateNode from hammingWeight !");
        // rm the S parameter
        context.removeBEASTObject(sI);

        // Get the Poisson (or OffsetInt) distribution for S
        Generator poissonGenerator = s.getGenerator();
        Distribution poissonDist = (Distribution) context.getBEASTObject(poissonGenerator);
        context.removeBEASTObject(poissonDist);

        // Re-target the distribution to Sum(I) instead of S
        beast.base.evolution.Sum x = new beast.base.evolution.Sum();
        x.setInputValue("arg", value);

        // The spec OffsetInt/Poisson has param input — replace it with Sum(I)
        // Since Sum implements Function (not a spec type), use the old Prior as bridge
        beast.base.inference.distribution.Prior prior = new beast.base.inference.distribution.Prior();
        if (poissonDist instanceof OffsetInt offsetInt) {
            prior.setInputValue("distr", offsetInt);
        } else {
            prior.setInputValue("distr", poissonDist);
        }
        prior.setInputValue("x", x);
        prior.setID(value.getID() + ".prior");
        prior.initAndValidate();
        return prior;
    }

    @Override
    public Class<RandomBooleanArray> getGeneratorClass() {
        return RandomBooleanArray.class;
    }

    @Override
    public Class<Distribution> getBEASTClass() {
        return Distribution.class;
    }
}
