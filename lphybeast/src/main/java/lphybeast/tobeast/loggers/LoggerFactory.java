package lphybeast.tobeast.loggers;

import beast.core.BEASTInterface;
import beast.core.Loggable;
import beast.core.Logger;
import beast.core.StateNode;
import beast.core.util.CompoundDistribution;
import beast.evolution.tree.Tree;
import beast.evolution.tree.TreeInterface;
import beast.evolution.tree.TreeStatLogger;
import beast.evolution.tree.TreeWithMetaDataLogger;
import com.google.common.collect.Multimap;
import lphy.evolution.coalescent.SkylineCoalescent;
import lphy.evolution.coalescent.StructuredCoalescent;
import lphy.graphicalModel.Generator;
import lphy.graphicalModel.GraphicalModelNode;
import lphy.graphicalModel.RandomVariable;
import lphybeast.BEASTContext;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A class to create all operators
 * @author Walter Xie
 * @author Alexei Drommand
 */
public class LoggerFactory implements LoggerHelper {

    private final BEASTContext context;
    private final CompoundDistribution[] topDist;

    String fileName = null;

    public LoggerFactory(BEASTContext context, CompoundDistribution[] topDist) {
        this.context = context;
        this.topDist = topDist;
    }

    /**
     * @param logEvery  Number of the samples logged
     * @param logFileStem  null for screen logger
     * @return    3 default loggers: parameter logger, screen logger, tree logger.
     * @see Logger
     */
    public List<Logger> createLoggers(int logEvery, String logFileStem) {
        Multimap<BEASTInterface, GraphicalModelNode<?>> elements = context.getElements();

        List<Logger> loggers = new ArrayList<>();
        // reduce screen logging
        setFileName(null, false);
        loggers.add(createLogger(logEvery * 100, elements));
        // parameter logger
        setFileName(logFileStem, false);
        loggers.add(createLogger(logEvery, elements));

        // tree logger
        List<TreeInterface> trees = getTrees();
        boolean multipleTrees = trees.size() > 1;
        for (TreeInterface tree : trees) {
            TreeLoggerHelper treeLogger = new TreeLoggerCreator(tree);
            treeLogger.setFileName(logFileStem, multipleTrees);

            loggers.add(treeLogger.createLogger(logEvery, elements));
        }

        // extraLoggers, create a seperated logger each time
        for (LoggerHelper loggerHelper : context.getExtraLoggers()) {
            // implement to set a different file name to the default names.
            loggerHelper.setFileName(logFileStem, multipleTrees);
            Logger logger = loggerHelper.createLogger(logEvery, elements);
            loggers.add(logger);
        }

        return loggers;
    }

    //*** default parameter/screen loggers ***//

    // screen logger if fileName is null
    public Logger createLogger(int logEvery, Multimap<BEASTInterface, GraphicalModelNode<?>> elements) {
        Logger logger = new Logger();
        logger.setInputValue("logEvery", logEvery);
        logger.setInputValue("log", getLoggables());
        if (getFileName() == null) {
            logger.setID("ScreenLogger"); // only 1 screen logger
        } else {
            logger.setInputValue("fileName", getFileName());
        }
        logger.initAndValidate();
        elements.put(logger, null);
        return logger;
    }

    public List<Loggable> getLoggables() {
        List<StateNode> state = context.getState();
        List<Loggable> nonTrees = state.stream()
                .filter(stateNode -> !(stateNode instanceof Tree))
                .collect(Collectors.toList());

        // tree height, but not in screen logging
        if ( getFileName() != null ) {
            List<TreeInterface> trees = getTrees();
            for (TreeInterface tree : trees) {
                TreeStatLogger treeStatLogger = new TreeStatLogger();
                // use default, which will log the length
                treeStatLogger.initByName("tree", tree);
                nonTrees.add(treeStatLogger);
            }
        }

        // not in screen logging
        if ( getFileName() != null ) {
            nonTrees.addAll(context.getExtraLoggables());
        }

//        for (Loggable loggable : extraLoggables) {
//            // fix StructuredCoalescent log
//            if (loggable instanceof Constant) {
//                // Constant includes Ne and m
//                RealParameter ne = ((Constant) loggable).NeInput.get();
//                nonTrees.remove(ne);
//                RealParameter m = ((Constant) loggable).b_mInput.get();
//                nonTrees.remove(m);
//            }
//        }

        // add them in the end to avoid sorting
        nonTrees.addAll(0, Arrays.asList(topDist));

        return nonTrees;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public void setFileName(String fileStem, boolean isMultiple) {
        if (fileStem == null) fileName = null;
        else fileName = Objects.requireNonNull(fileStem) + ".log";
    }

    /**
     * @return   a list of {@link TreeInterface}
     */
    public List<TreeInterface> getTrees() {
        //TODO get trees from CTMC?
        List<StateNode> state = context.getState();
        return state.stream()
                .filter(stateNode -> stateNode instanceof TreeInterface)
                .map(stateNode -> (TreeInterface) stateNode)
                .sorted(Comparator.comparing(TreeInterface::getID))
                .collect(Collectors.toList());
    }

    //*** default tree loggers ***//
    class TreeLoggerCreator implements TreeLoggerHelper {

        final TreeInterface tree;
        String fileName;

        public TreeLoggerCreator(TreeInterface tree) {
            this.tree = tree;
        }

        private boolean logMetaData() {
            TreeInterface tree = getTree();
            Map<BEASTInterface, GraphicalModelNode<?>> BEASTToLPHYMap = context.getBEASTToLPHYMap();
            GraphicalModelNode graphicalModelNode = BEASTToLPHYMap.get(tree);
            Generator generator = ((RandomVariable) graphicalModelNode).getGenerator();

            // TODO more general?
            return generator instanceof SkylineCoalescent ||
                    generator instanceof StructuredCoalescent;
        }

        public Logger createLogger(int logEvery, Multimap<BEASTInterface, GraphicalModelNode<?>> elements) {
            Logger logger = new Logger();
            logger.setInputValue("logEvery", logEvery);
            if (logMetaData()) {
                TreeWithMetaDataLogger treeWithMetaDataLogger = new TreeWithMetaDataLogger();
                treeWithMetaDataLogger.setInputValue("tree", getTree());
                logger.setInputValue("log", treeWithMetaDataLogger);
            } else
                logger.setInputValue("log", getTree());

            logger.setInputValue("fileName", getFileName());
            logger.setInputValue("mode", "tree");
            logger.initAndValidate();
            logger.setID(getTree().getID() + ".treeLogger");
            elements.put(logger, null);

            return logger;
        }

        @Override
        public String getFileName() {
            return fileName;
        }

        @Override
        public void setFileName(String fileStem, boolean isMultiple) {
            if (isMultiple) // multi-partitions and unlink trees
                fileName = fileStem + "." + getTree().getID() + ".trees";
            else
                fileName = fileStem + ".trees";
        }

        @Override
        public TreeInterface getTree() {
            return tree;
        }

    }

}
