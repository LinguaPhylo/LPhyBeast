package lphybeast.tobeast.loggers;

import beast.core.BEASTInterface;
import beast.core.BEASTObject;
import beast.core.Logger;
import beast.evolution.likelihood.AncestralStateTreeLikelihood;
import beast.evolution.tree.TreeInterface;
import beast.evolution.tree.TreeWithTraitLogger;
import com.google.common.collect.Multimap;
import lphy.graphicalModel.GraphicalModelNode;
import lphybeast.BEASTContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The extra tree logger for discrete phylogeography.
 * @see TreeWithTraitLogger
 * @author Walter Xie
 */
public class TraitTreeLogger implements TreeLoggerHelper {
    // add extra tree logger for AncestralStateTreeLikelihood
    final protected AncestralStateTreeLikelihood treeLikelihood;
    final protected BEASTContext context;
    String fileName;

    public TraitTreeLogger(AncestralStateTreeLikelihood treeLikelihood, BEASTContext context) {
        this.treeLikelihood = Objects.requireNonNull(treeLikelihood);
        this.context = context;
    }

    @Override
    public Logger createLogger(int logEvery, Multimap<BEASTInterface, GraphicalModelNode<?>> elements) {
        TreeInterface tree = getTree();

        TreeWithTraitLogger treeWithTraitLogger = new TreeWithTraitLogger();
        treeWithTraitLogger.setInputValue("tree", tree);

        List<BEASTObject> metadata = new ArrayList<>();
        metadata.add(context.getPosteriorDist());
        // <metadata idref="D_trait.treeLikelihood"/> is compulsory,
        // otherwise location trait would not be logged.
        metadata.add(treeLikelihood);
        treeWithTraitLogger.setInputValue("metadata", metadata);

        Logger logger = new Logger();
        logger.setInputValue("logEvery", logEvery);
        logger.setInputValue("log", treeWithTraitLogger);
        logger.setInputValue("fileName", getFileName());
        logger.setInputValue("mode", "tree");
        logger.initAndValidate();

        logger.setID("TreeWithTraitLogger." + tree.getID());
        elements.put(logger, null);
        return logger;
    }

    @Override
    public TreeInterface getTree() {
        return Objects.requireNonNull(treeLikelihood.treeInput.get());
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public void setFileName(String fileStem, boolean isMultiple) {
        if (isMultiple) // multi-partitions and unlink trees
            fileName = fileStem + "_with_trait." + getTree().getID() + ".trees";
        else
            fileName = fileStem + "_with_trait.trees";
    }
}
