package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.evolution.tree.TreeIntervals;
import beast.base.evolution.tree.coalescent.ConstantPopulation;
import lphy.base.evolution.coalescent.SerialCoalescent;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class SerialCoalescentToBEAST implements
        GeneratorToBEAST<SerialCoalescent, beast.base.evolution.tree.coalescent.Coalescent> {
    @Override
    public beast.base.evolution.tree.coalescent.Coalescent generatorToBEAST(SerialCoalescent coalescent, BEASTInterface value, BEASTContext context) {

        beast.base.evolution.tree.coalescent.Coalescent beastCoalescent = new beast.base.evolution.tree.coalescent.Coalescent();

        TreeIntervals treeIntervals = new TreeIntervals();
        treeIntervals.setInputValue("tree", value);
        treeIntervals.initAndValidate();

        beastCoalescent.setInputValue("treeIntervals", treeIntervals);

        ConstantPopulation populationFunction = new ConstantPopulation();
        populationFunction.setInputValue("popSize", context.getBEASTObject(coalescent.getTheta()));
        populationFunction.initAndValidate();

        beastCoalescent.setInputValue("populationModel", populationFunction);

        beastCoalescent.initAndValidate();

        return beastCoalescent;
    }

    @Override
    public Class<SerialCoalescent> getGeneratorClass() {
        return SerialCoalescent.class;
    }

    @Override
    public Class<beast.base.evolution.tree.coalescent.Coalescent> getBEASTClass() {
        return beast.base.evolution.tree.coalescent.Coalescent.class;
    }
}
