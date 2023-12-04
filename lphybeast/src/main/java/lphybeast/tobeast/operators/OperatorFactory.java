package lphybeast.tobeast.operators;

import beast.base.core.BEASTInterface;
import beast.base.core.BEASTObject;
import beast.base.evolution.operator.ScaleOperator;
import beast.base.evolution.tree.Tree;
import beast.base.inference.Operator;
import beast.base.inference.StateNode;
import beast.base.inference.operator.BitFlipOperator;
import beast.base.inference.operator.DeltaExchangeOperator;
import beast.base.inference.operator.IntRandomWalkOperator;
import beast.base.inference.parameter.BooleanParameter;
import beast.base.inference.parameter.IntegerParameter;
import beast.base.inference.parameter.RealParameter;
import com.google.common.collect.Multimap;
import lphy.base.distribution.Dirichlet;
import lphy.base.distribution.RandomComposition;
import lphy.core.logger.LoggerUtils;
import lphy.core.model.GenerativeDistribution;
import lphy.core.model.GraphicalModelNode;
import lphy.core.model.RandomVariable;
import lphy.core.vectorization.IID;
import lphybeast.BEASTContext;

import java.util.*;

import static lphybeast.BEASTContext.getOperatorWeight;

/**
 * A class to create all operators
 * @author Walter Xie
 * @author Alexei Drommand
 */
public class OperatorFactory {

    private final BEASTContext context;

    /**
     * @param context               passing all configurations
     */
    public OperatorFactory(BEASTContext context) {
        this.context = context;
    }

    public static Operator createTreeScaleOperator(Tree tree, BEASTContext context) {
        TreeOperatorStrategy treeOperatorStrategy = context.resolveTreeOperatorStrategy(tree);
        Operator operator = treeOperatorStrategy.getScaleOperator();
        operator.setInputValue("tree", tree);
        operator.setInputValue("scaleFactor", 0.75);
        // set the upper of the scale factor
        operator.setInputValue("upper", 0.975);
        operator.setInputValue("weight", getOperatorWeight(tree.getInternalNodeCount()));
        operator.initAndValidate();
        operator.setID(tree.getID() + "." + "scale");
        context.getElements().put(operator, null);
        return operator;
    }

    public static Operator createRootHeightOperator(Tree tree, BEASTContext context) {
        TreeOperatorStrategy treeOperatorStrategy = context.resolveTreeOperatorStrategy(tree);
        Operator operator = treeOperatorStrategy.getScaleOperator();
        operator.setInputValue("tree", tree);
        operator.setInputValue("rootOnly", true);
        operator.setInputValue("scaleFactor", 0.75);
        // set the upper of the scale factor
        operator.setInputValue("upper", 0.975);
        operator.setInputValue("weight", getOperatorWeight(1));
        operator.initAndValidate();
        operator.setID(tree.getID() + "." + "rootAgeScale");
        context.getElements().put(operator, null);
        return operator;
    }

    public static Operator createTreeUniformOperator(Tree tree, BEASTContext context) {
        TreeOperatorStrategy treeOperatorStrategy = context.resolveTreeOperatorStrategy(tree);
        Operator uniform = treeOperatorStrategy.getUniformOperator();
        uniform.setInputValue("tree", tree);
        uniform.setInputValue("weight", getOperatorWeight(tree.getInternalNodeCount()));
        uniform.initAndValidate();
        uniform.setID(tree.getID() + "." + "uniform");
        context.getElements().put(uniform, null);
        return uniform;
    }

    public static Operator createExchangeOperator(Tree tree, BEASTContext context, boolean isNarrow) {
        TreeOperatorStrategy treeOperatorStrategy = context.resolveTreeOperatorStrategy(tree);
        Operator exchange = treeOperatorStrategy.getExchangeOperator();
        exchange.setInputValue("tree", tree);
        double pow = (isNarrow) ? 0.7 : 0.2; // WideExchange size^0.2
        exchange.setInputValue("weight", getOperatorWeight(tree.getInternalNodeCount(), pow));
        exchange.setInputValue("isNarrow", isNarrow);
        exchange.initAndValidate();
        exchange.setID(tree.getID() + "." + ((isNarrow) ? "narrow" : "wide") + "Exchange");
        context.getElements().put(exchange, null);
        return exchange;
    }

    public static Operator createSubtreeSlideOperator(Tree tree, BEASTContext context) {
        TreeOperatorStrategy treeOperatorStrategy = context.resolveTreeOperatorStrategy(tree);
        Operator subtreeSlide = treeOperatorStrategy.getSubtreeSlideOperator();
        subtreeSlide.setInputValue("tree", tree);
        subtreeSlide.setInputValue("weight", getOperatorWeight(tree.getInternalNodeCount()));
        subtreeSlide.setInputValue("size", tree.getRoot().getHeight() / 10.0);
        subtreeSlide.initAndValidate();
        subtreeSlide.setID(tree.getID() + "." + "subtreeSlide");
        context.getElements().put(subtreeSlide, null);
        return subtreeSlide;
    }

    public static Operator createWilsonBaldingOperator(Tree tree, BEASTContext context) {
        TreeOperatorStrategy treeOperatorStrategy = context.resolveTreeOperatorStrategy(tree);
        Operator wilsonBalding = treeOperatorStrategy.getWilsonBaldingOperator();
        wilsonBalding.setInputValue("tree", tree);
        wilsonBalding.setInputValue("weight", getOperatorWeight(tree.getInternalNodeCount(), 0.2));
        wilsonBalding.initAndValidate();
        wilsonBalding.setID(tree.getID() + "." + "wilsonBalding");
        context.getElements().put(wilsonBalding, null);
        return wilsonBalding;
    }

    /**
     * @return  a list of {@link Operator}.
     */
    public List<Operator> createOperators() {

        List<Operator> operators = new ArrayList<>();

        Set<StateNode> skipOperators = context.getSkipOperators();
        for (StateNode stateNode : context.getState()) {
            if (!skipOperators.contains(stateNode)) {
                if (stateNode instanceof RealParameter realParameter) {
                    Operator operator = createBEASTOperator(realParameter);
                    if (operator != null) operators.add(operator);
                } else if (stateNode instanceof IntegerParameter integerParameter) {
                    operators.add(createBEASTOperator(integerParameter));
                } else if (stateNode instanceof BooleanParameter booleanParameter) {
                    operators.add(createBitFlipOperator(booleanParameter));
                } else if (stateNode instanceof Tree tree) {
                    TreeOperatorStrategy treeOperatorStrategy = context.resolveTreeOperatorStrategy(tree);
                    // create operators
                    List<Operator> treeOperators = treeOperatorStrategy.createTreeOperators(tree, context);
                    if (treeOperators.size() < 1)
                        throw new IllegalArgumentException("No operators are created by strategy " +
                                treeOperatorStrategy.getName() + " !");
                    operators.addAll(treeOperators);
                }
            }
        }

        operators.addAll(context.getExtraOperators());
        operators.sort(Comparator.comparing(BEASTObject::getID));

        return operators;
    }

    //*** parameter operators ***//

    private Operator createBEASTOperator(RealParameter parameter) {
        Multimap<BEASTInterface, GraphicalModelNode<?>> elements = context.getElements();
        Collection<GraphicalModelNode<?>> nodes = elements.get(parameter);

        if (nodes.stream().anyMatch(node -> node instanceof RandomVariable)) {

            GraphicalModelNode graphicalModelNode = (GraphicalModelNode)nodes.stream().filter(node -> node instanceof RandomVariable).toArray()[0];

            RandomVariable<?> variable = (RandomVariable<?>) graphicalModelNode;

            Operator operator;
            GenerativeDistribution generativeDistribution = variable.getGenerativeDistribution();

            if (generativeDistribution instanceof Dirichlet ||
                    (generativeDistribution instanceof IID &&
                            ((IID<?>) generativeDistribution).getBaseDistribution() instanceof Dirichlet) ) {
                Double[] value = (Double[]) variable.value();
                operator = new DeltaExchangeOperator();
                operator.setInputValue("parameter", parameter);
                operator.setInputValue("weight", getOperatorWeight(parameter.getDimension() - 1));
                operator.setInputValue("delta", 1.0 / value.length);
                operator.initAndValidate();
                operator.setID(parameter.getID() + ".deltaExchange");
            } else {
                operator = new ScaleOperator();
                operator.setInputValue("parameter", parameter);
                operator.setInputValue("weight", getOperatorWeight(parameter.getDimension()));
                operator.setInputValue("scaleFactor", 0.75);
                operator.initAndValidate();
                operator.setID(parameter.getID() + ".scale");
            }
            elements.put(operator, null);
            return operator;
        } else {
            LoggerUtils.log.severe("No LPhy random variable associated with beast state node " + parameter.getID());
            return null;
        }
    }

    private Operator createBEASTOperator(IntegerParameter parameter) {
        Map<BEASTInterface, GraphicalModelNode<?>> BEASTToLPHYMap = context.getBEASTToLPHYMap();
        // TODO safe cast?
        RandomVariable<?> variable = (RandomVariable<?>) BEASTToLPHYMap.get(parameter);

        Operator operator;
        if (variable.getGenerativeDistribution() instanceof RandomComposition) {
            System.out.println("Constructing operator for randomComposition");

            operator = new DeltaExchangeOperator();
            operator.setInputValue("intparameter", parameter);
            operator.setInputValue("weight", getOperatorWeight(parameter.getDimension() - 1));
            operator.setInputValue("delta", 2.0);
            operator.setInputValue("integer", true);
            operator.initAndValidate();
            operator.setID(parameter.getID() + ".deltaExchange");
        } else {
            operator = new IntRandomWalkOperator();
            operator.setInputValue("parameter", parameter);
            operator.setInputValue("weight", getOperatorWeight(parameter.getDimension()));

            // TODO implement an optimizable int random walk that uses a reflected Poisson distribution for the jump size with the mean of the Poisson being the optimizable parameter
            operator.setInputValue("windowSize", 1);
            operator.initAndValidate();
            operator.setID(parameter.getID() + ".randomWalk");
        }
        Multimap<BEASTInterface, GraphicalModelNode<?>> elements = context.getElements();
        elements.put(operator, null);
        return operator;
    }

    private Operator createBitFlipOperator(BooleanParameter parameter) {
        Operator operator = new BitFlipOperator();
        operator.setInputValue("parameter", parameter);
        operator.setInputValue("weight", getOperatorWeight(parameter.getDimension()));
        operator.initAndValidate();
        operator.setID(parameter.getID() + ".bitFlip");

        return operator;
    }


}
