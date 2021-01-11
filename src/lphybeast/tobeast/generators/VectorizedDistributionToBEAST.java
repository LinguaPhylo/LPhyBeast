package lphybeast.tobeast.generators;

import beast.core.BEASTInterface;
import lphy.core.distributions.VectorizedDistribution;
import lphy.graphicalModel.GenerativeDistribution;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;
import outercore.util.BEASTVector;

import java.util.ArrayList;
import java.util.List;

public class VectorizedDistributionToBEAST implements GeneratorToBEAST<VectorizedDistribution, BEASTInterface> {
    @Override
    public BEASTInterface generatorToBEAST(VectorizedDistribution generator, BEASTInterface value, BEASTContext context) {

        List<BEASTInterface> values = null;
        if (value instanceof BEASTVector) {
            values = ((BEASTVector)value).getObjectList();
        } else {
            throw new IllegalArgumentException("Expecting BEASTVector value from VectorizedDistribution");
        }

        List<GenerativeDistribution> generativeDistributionList = generator.getComponentDistributions();

        if (generativeDistributionList.size() != values.size()) throw new IllegalArgumentException("Expecting value and base distribution list sizes to match!");

        List<BEASTInterface> beastGenerators = new ArrayList<>();
        for (int i = 0; i < generativeDistributionList.size(); i++)  {
            GenerativeDistribution generativeDistribution = generativeDistributionList.get(i);
            GeneratorToBEAST toBEAST = context.getGeneratorToBEAST(generativeDistribution);

            BEASTInterface beastGenerator = toBEAST.generatorToBEAST(generativeDistribution, values.get(i), context);
            beastGenerators.add(beastGenerator);
            context.putBEASTObject(generativeDistribution, beastGenerator);
        }

        return new BEASTVector(beastGenerators);
    }

    @Override
    public Class<VectorizedDistribution> getGeneratorClass() {
        return VectorizedDistribution.class;
    }
}