package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.inference.Distribution;
import beast.base.inference.StateNode;
import beast.base.spec.evolution.IntSum;
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

        // Re-target the distribution to IntSum(I) instead of S
        IntSum x = new IntSum();
        x.setInputValue("arg", value);
        x.initAndValidate();

        // Replace param on the spec distribution with IntSum(I)
        if (poissonDist instanceof OffsetInt offsetInt) {
            offsetInt.setInputValue("param", x);
            offsetInt.setID(value.getID() + ".prior");
            offsetInt.initAndValidate();
            return offsetInt;
        } else {
            poissonDist.setInputValue("param", x);
            poissonDist.setID(value.getID() + ".prior");
            poissonDist.initAndValidate();
            return poissonDist;
        }
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
