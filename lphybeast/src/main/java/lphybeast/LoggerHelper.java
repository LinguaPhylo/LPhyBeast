package lphybeast;

import beast.core.BEASTInterface;
import beast.core.Loggable;
import beast.core.Logger;
import com.google.common.collect.Multimap;
import lphy.graphicalModel.GraphicalModelNode;

import java.util.List;

/**
 * Helper to create a {@link Logger} from a list of loggable.
 * It can be used to create a customized logger different to the default loggers.
 * @author Walter Xie
 */
public interface LoggerHelper {

    /**
     * Create a logger containing the loggable parameters.
     * @param logEvery        number of the samples logged.
     * @param elements        require the code <code>elements.put(logger, null)</code>
     *                        to make sure not mapping logger to any GraphicalModelNode.
     * @return a parameter or screen {@link Logger}. Or tree logger
     */
    Logger createLogger(int logEvery, final Multimap<BEASTInterface, GraphicalModelNode<?>> elements);

    /**
     * @return  a list of loggable parameters.
     */
    List<Loggable> getLoggables();


    /**
     * @return    log file name
     */
    String getFileName();

    /**
     * Set the log file name by a given file stem.
     * @param fileStem    log file stem without file extension
     * @param isMultiple  if true, for example multi-partitions and unlink trees,
     *                    then add the unique identifier (e.g. tree id) into the file name.
     */
    void setFileName(String fileStem, boolean isMultiple);

}
