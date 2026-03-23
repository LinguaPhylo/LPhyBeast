package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.evolution.tree.TreeIntervals;
import beast.base.evolution.tree.coalescent.Coalescent;
import beast.base.spec.evolution.tree.coalescent.ConstantPopulation;
import lphy.base.evolution.coalescent.SerialCoalescent;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class SerialCoalescentToBEAST implements GeneratorToBEAST<SerialCoalescent, Coalescent> {
    @Override
    public Coalescent generatorToBEAST(SerialCoalescent coalescent, BEASTInterface value, BEASTContext context) {

        Coalescent beastCoalescent = new Coalescent();

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
    public Class<Coalescent> getBEASTClass() {
        return Coalescent.class;
    }
}
