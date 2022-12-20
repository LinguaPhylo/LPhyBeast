package lphybeast.tobeast.loggers;

import beast.base.core.Loggable;
import beast.base.evolution.tree.TreeInterface;

import java.util.List;

/**
 * Helper to create a tree logger, may contain a simple tree,
 * or customised logger e.g. {@link beast.base.evolution.TreeWithMetaDataLogger}.
 * Usually one tree one logger, which is different convention with logging parameters.
 * @author Walter Xie
 */
public interface TreeLoggerHelper extends LoggerHelper {


    default List<Loggable> getLoggables() {
        return null; // use getTree() in createLogger(...)
    }

    /**
     * @return  the tree to log, usually one tree in one logger
     */
    TreeInterface getTree();
}
