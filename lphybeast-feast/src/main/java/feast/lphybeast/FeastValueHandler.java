package feast.lphybeast;

import beast.base.core.BEASTInterface;
import beast.base.core.Function;
import beast.base.inference.StateNode;
import feast.expressions.ExpCalculator;
import feast.function.Concatenate;
import lphybeast.spi.ValueHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * ValueHandler for feast types: ExpCalculator and Concatenate.
 */
public class FeastValueHandler implements ValueHandler {

    @Override
    public Function asFunction(BEASTInterface beastInterface) {
        if (beastInterface instanceof ExpCalculator expCalculator)
            return expCalculator;
        return null;
    }

    @Override
    public List<Function> extractParts(BEASTInterface beastInterface) {
        if (beastInterface instanceof Concatenate concatenate)
            return concatenate.functionsInput.get();
        return null;
    }

    @Override
    public List<StateNode> extractStateNodes(BEASTInterface beastInterface) {
        if (beastInterface instanceof Concatenate concatenate) {
            List<StateNode> nodes = new ArrayList<>();
            for (Function function : concatenate.functionsInput.get()) {
                if (function instanceof StateNode sn)
                    nodes.add(sn);
            }
            return nodes;
        }
        return null;
    }

    @Override
    public List<Function> extractArguments(BEASTInterface beastInterface) {
        if (beastInterface instanceof ExpCalculator expCalculator)
            return expCalculator.functionsInput.get();
        return null;
    }
}
