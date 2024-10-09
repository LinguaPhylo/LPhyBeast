package lphybeast.tobeast.loggers;

import beast.base.core.BEASTInterface;
import beast.base.evolution.TreeWithMetaDataLogger;
import beast.base.evolution.branchratemodel.BranchRateModel;
import beast.base.evolution.tree.TreeInterface;
import beast.base.inference.Logger;
import com.google.common.collect.Multimap;
import lphy.core.model.GraphicalModelNode;
import lphybeast.BEASTContext;

import java.util.Objects;

import static java.lang.Math.toIntExact;

/**
 * Log branch rates with tree.
 * @see TreeWithMetaDataLogger
 * @author Walter Xie
 */
public class MetaDataTreeLogger implements TreeLoggerHelper {
    // add extra tree logger for AncestralStateTreeLikelihood
    final protected BranchRateModel branchRateModel;
    final protected BEASTContext context;
    final TreeInterface tree;
    String fileName;

    public MetaDataTreeLogger(BranchRateModel branchRateModel, TreeInterface tree, BEASTContext context) {
        this.branchRateModel = Objects.requireNonNull(branchRateModel);
        this.tree = Objects.requireNonNull(tree);
        this.context = context;
    }

    @Override
    public Logger createLogger(long logEvery, Multimap<BEASTInterface, GraphicalModelNode<?>> elements) {
        Logger logger = new Logger();
        // Must convert to int
        logger.setInputValue("logEvery", toIntExact(logEvery));
        logger.setInputValue("fileName", getFileName());
        logger.setInputValue("mode", "tree");

        TreeWithMetaDataLogger treeWithMetaDataLogger = new TreeWithMetaDataLogger();
        treeWithMetaDataLogger.setInputValue("tree", tree);
        treeWithMetaDataLogger.setInputValue("branchratemodel", branchRateModel);

        logger.setInputValue("log", treeWithMetaDataLogger);
        logger.initAndValidate();

        logger.setID("TreeWithMetaDataLogger." + tree.getID());
        elements.put(logger, null);
        return logger;
    }

    @Override
    public TreeInterface getTree() {
        return tree;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public void setFileName(String fileStem, boolean isMultiple) {
        if (isMultiple) // multi-partitions and unlink trees
            fileName = fileStem + "_with_rates." + getTree().getID() + ".trees";
        else
            fileName = fileStem + "_with_rates.trees";
    }
}
