package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.evolution.tree.TreeIntervals;
import beast.base.inference.parameter.IntegerParameter;
import beast.base.inference.parameter.RealParameter;
import lphy.base.evolution.coalescent.BICEPS;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class BICEPSToBEAST implements GeneratorToBEAST<BICEPS, biceps.BICEPS> {

    @Override
    public biceps.BICEPS generatorToBEAST(BICEPS generator, BEASTInterface value, BEASTContext context) {

        biceps.BICEPS bicepsBeast = new biceps.BICEPS();

        TreeIntervals treeIntervals = new TreeIntervals();
        treeIntervals.setInputValue("tree", value);
        treeIntervals.initAndValidate();

        bicepsBeast.setInputValue("treeIntervals", treeIntervals);

        RealParameter populationShape = context.getAsRealParameter(generator.getPopulationShape());
        bicepsBeast.setInputValue("populationShape", populationShape);

        RealParameter populationMean = context.getAsRealParameter(generator.getPopulationMean());
        bicepsBeast.setInputValue("populationMean", populationMean);

        if (generator.getGroupSizes() != null) {
            IntegerParameter groupSizes = context.getAsIntegerParameter(generator.getGroupSizes());
            bicepsBeast.setInputValue("groupSizes", groupSizes);
            bicepsBeast.setInputValue("groupCount", groupSizes.getDimension());
        }

        if (generator.getPloidy2() != null) {
            bicepsBeast.setInputValue("ploidy", generator.getPloidy2().value());
        }

        bicepsBeast.initAndValidate();

        return bicepsBeast;
    }

    @Override
    public Class<BICEPS> getGeneratorClass() {
        return BICEPS.class;
    }

    @Override
    public Class<biceps.BICEPS> getBEASTClass() {
        return biceps.BICEPS.class;
    }
}
