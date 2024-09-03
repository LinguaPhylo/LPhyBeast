package mascot.lphybeast.tobeast.loggers;

import beast.base.core.BEASTInterface;
import beast.base.evolution.tree.TreeInterface;
import beast.base.inference.Logger;
import com.google.common.collect.Multimap;
import lphy.core.model.GraphicalModelNode;
import lphybeast.tobeast.loggers.TreeLoggerHelper;
import mascot.distribution.Mascot;
import mascot.logger.StructuredTreeLogger;

/**
 * @author Walter Xie
 */
public class MascotExtraTreeLogger implements TreeLoggerHelper {
    // Mascot is a TreeDistribution
    final protected Mascot mascot;
    String fileName;

    public MascotExtraTreeLogger(Mascot mascot) {
        this.mascot = mascot;
    }

    @Override
    public Logger createLogger(long logEvery, final Multimap<BEASTInterface, GraphicalModelNode<?>> elements) {
        // Mascot StructuredTreeLogger
        TreeInterface tree = getTree();

        StructuredTreeLogger structuredTreeLogger = new StructuredTreeLogger();
        // not logging tree directly
        structuredTreeLogger.setInputValue("mascot", mascot);

        Logger logger = new Logger();
        // must convert to int
        logger.setInputValue("logEvery", Math.toIntExact(logEvery));
        logger.setInputValue("log", structuredTreeLogger);

        logger.setInputValue("fileName", getFileName());
        logger.setID("StructuredTreeLogger" + tree.getID());

        logger.setInputValue("mode", "tree");
        logger.initAndValidate();

        return logger;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public void setFileName(String fileStem, boolean isMultiple) {
        if (isMultiple) // multi-partitions and unlink trees
            fileName = fileStem + "_" + getTree().getID() + ".mascot.trees";
        else
            fileName = fileStem + ".mascot.trees";
    }

    @Override
    public TreeInterface getTree() {
        return mascot.treeInput.get();
    }
}
