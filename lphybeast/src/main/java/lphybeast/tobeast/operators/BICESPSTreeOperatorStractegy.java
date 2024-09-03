package lphybeast.tobeast.operators;

import beast.base.evolution.tree.Tree;
import beast.base.inference.Operator;
import lphybeast.BEASTContext;

import static lphybeast.BEASTContext.getOperatorWeight;

/**
 * BICESPS operators, to replace tree scale operator.
 * @author Walter Xie
 */
public interface BICESPSTreeOperatorStractegy {

    Operator getBICEPSEpochTopOrAll();

    Operator getBICEPSTreeFlex();

    static Operator createBICEPSEpochTop(Tree tree, BEASTContext context) {
        TreeOperatorStrategy treeOperatorStrategy = context.resolveTreeOperatorStrategy(tree);
        Operator operator = treeOperatorStrategy.getBICEPSEpochTopOrAll();
        operator.setInputValue("tree", tree);
        // weight="2.0" scaleFactor="0.1"
        operator.setInputValue("scaleFactor", 0.1);
        operator.setInputValue("weight", getOperatorWeight(1));
        operator.initAndValidate();
        operator.setID(tree.getID() + "." + "BICEPSEpochTop");
        context.getElements().put(operator, null);
        return operator;
    }

    static Operator createBICEPSEpochAll(Tree tree, BEASTContext context) {
        TreeOperatorStrategy treeOperatorStrategy = context.resolveTreeOperatorStrategy(tree);
        Operator operator = treeOperatorStrategy.getBICEPSEpochTopOrAll();
        operator.setInputValue("tree", tree);
        // weight="2.0" scaleFactor="0.1" fromOldestTipOnly="false"
        operator.setInputValue("scaleFactor", 0.1);
        operator.setInputValue("weight", getOperatorWeight(2)); // TODO check ?
        operator.setInputValue("fromOldestTipOnly", false);
        operator.initAndValidate();
        operator.setID(tree.getID() + "." + "BICEPSEpochAll");
        context.getElements().put(operator, null);
        return operator;
    }

    static Operator createBICEPSTreeFlex(Tree tree, BEASTContext context) {
        TreeOperatorStrategy treeOperatorStrategy = context.resolveTreeOperatorStrategy(tree);
        Operator operator = treeOperatorStrategy.getBICEPSTreeFlex();
        operator.setInputValue("tree", tree);
        // weight="2.0" scaleFactor="0.01"
        operator.setInputValue("scaleFactor", 0.01); // TODO used to be 0.75 ?
        operator.setInputValue("weight", getOperatorWeight(tree.getInternalNodeCount()));
        operator.initAndValidate();
        operator.setID(tree.getID() + "." + "BICEPSTreeFlex");
        context.getElements().put(operator, null);
        return operator;
    }
}
