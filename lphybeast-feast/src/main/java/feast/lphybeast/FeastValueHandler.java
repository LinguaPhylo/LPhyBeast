package feast.lphybeast;

import beast.base.core.BEASTInterface;
import beast.base.inference.StateNode;
import beast.base.spec.domain.Real;
import beast.base.spec.type.RealVector;
import feast.expressions.ExpCalculator;
import lphybeast.spi.ValueHandler;

import java.util.List;

/**
 * ValueHandler for feast types: ExpCalculator.
 */
public class FeastValueHandler implements ValueHandler {

//    @Override
//    public Function asFunction(BEASTInterface beastInterface) {
//        if (beastInterface instanceof ExpCalculator expCalculator)
//            return expCalculator;
//        return null;
//    }

    @Override
    public List<BEASTInterface> extractParts(BEASTInterface beastInterface) {
        return null;
    }

    @Override
    public List<StateNode> extractStateNodes(BEASTInterface beastInterface) {
        return null;
    }

    @Override
    public List<RealVector<? extends Real>> extractArguments(BEASTInterface beastInterface) {
        if (beastInterface instanceof ExpCalculator expCalculator)
            return expCalculator.realVectorsInput.get();
        return null;
    }
}

