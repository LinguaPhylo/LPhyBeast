package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.evolution.tree.TreeStatLogger;
import lphy.base.function.tree.TreeLength;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class TreeLengthToBEAST implements GeneratorToBEAST<TreeLength, TreeStatLogger> {

    public TreeStatLogger generatorToBEAST(TreeLength treeLength, BEASTInterface tree, BEASTContext context) {

        TreeStatLogger treeStatLogger = new TreeStatLogger();
        treeStatLogger.setInputValue("logLength", true);
        treeStatLogger.setInputValue("logHeight", false);
        treeStatLogger.setInputValue("tree", tree);
        treeStatLogger.initAndValidate();

        if (treeLength.hasRandomParameters()) {
            context.addExtraLoggable(treeStatLogger);
        }

        return treeStatLogger;
    }

    @Override
    public Class<TreeLength> getGeneratorClass() {
        return TreeLength.class;
    }

    @Override
    public Class<TreeStatLogger> getBEASTClass() {
        return TreeStatLogger.class;
    }
}
