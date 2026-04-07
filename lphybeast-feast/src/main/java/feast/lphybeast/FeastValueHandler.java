package feast.lphybeast;

import beast.base.core.BEASTInterface;
import beast.base.core.Function;
import beast.base.inference.StateNode;
import feast.expressions.ExpCalculator;
import lphybeast.spi.ValueHandler;

import java.util.List;

/**
 * ValueHandler for feast types: ExpCalculator.
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
        return null;
    }

    @Override
    public List<StateNode> extractStateNodes(BEASTInterface beastInterface) {
        return null;
    }

    @Override
    public List<Function> extractArguments(BEASTInterface beastInterface) {
        if (beastInterface instanceof ExpCalculator expCalculator)
            return expCalculator.functionsInput.get();
        return null;
    }
}
