package lphybeast.tobeast.operators;

import beast.base.core.BEASTInterface;
import beast.base.core.BEASTObject;
import beast.base.core.Function;
import beast.base.evolution.operator.AdaptableOperatorSampler;
import beast.base.evolution.operator.Exchange;
import beast.base.evolution.operator.TreeOperator;
import beast.base.evolution.operator.kernel.BactrianScaleOperator;
import beast.base.evolution.tree.Tree;
import beast.base.inference.Operator;
import beast.base.inference.StateNode;
import beast.base.inference.operator.BitFlipOperator;
import beast.base.inference.operator.IntRandomWalkOperator;
import beast.base.inference.operator.kernel.BactrianDeltaExchangeOperator;
import beast.base.inference.operator.kernel.BactrianRandomWalkOperator;
import beast.base.inference.operator.kernel.BactrianUpDownOperator;
import beast.base.inference.parameter.BooleanParameter;
import beast.base.inference.parameter.IntegerParameter;
import beast.base.inference.parameter.RealParameter;
import com.google.common.collect.Multimap;
import lphy.base.distribution.Dirichlet;
import lphy.base.distribution.RandomComposition;
import lphy.base.distribution.WeightedDirichlet;
import lphy.core.logger.LoggerUtils;
import lphy.core.model.*;
import lphy.core.vectorization.IID;
import lphybeast.BEASTContext;

import java.util.*;

import static lphybeast.BEASTContext.getOperatorWeight;

/**
 * A class to create all default operators
 * @author Walter Xie
 * @author Alexei Drommand
 */
public class DefaultOperatorStrategy implements OperatorStrategy {

    private final BEASTContext context;

    /**
     * @param context               passing all configurations
     */
    public DefaultOperatorStrategy(BEASTContext context) {
        this.context = context;
    }


    @Override
    public Operator getScaleOperator() {
        return new BactrianScaleOperator();
    }

    @Override
    public Operator getDeltaExchangeOperator() {
        return new BactrianDeltaExchangeOperator();
    }

    @Override
    public Operator getRandomWalkOperator() {
        return new BactrianRandomWalkOperator();
    }

    @Override
    public Operator getIntRandomWalkOperator() {
        return new IntRandomWalkOperator();
    }

    @Override
    public Operator getBitFlipOperator() {
        return new BitFlipOperator();
    }

    /**
     * @return  a list of {@link Operator}.
     */
    public List<Operator> createOperators() {
        List<Operator> operators = new ArrayList<>();
        // extra operators
        List<Operator> extraOperators = context.getExtraOperators();

        Set<StateNode> skipOperators = context.getSkipOperators();
        for (StateNode stateNode : context.getState()) {
            if (!skipOperators.contains(stateNode)) {
                // The default template to create operators
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

                    List<Operator> noDuplicatedOperators = getNoDuplicatedOperators(treeOperators, extraOperators);

                    operators.addAll(noDuplicatedOperators);
                }
            }
        }

        // then add all extra
        operators.addAll(extraOperators);
        operators.sort(Comparator.comparing(BEASTObject::getID));
        return operators;
    }

    /**
     * It is expecting to remove the standard op, if an extra operator is same as a standard op,
     * and they are both working on the same state node.
     * But Operator cannot locate StateNode, so it is only working on TreeOperator at the moment.
     */
    private List<Operator> getNoDuplicatedOperators(List<Operator> operators, List<Operator> extraOperators) {
        List<Operator> newOperators = new ArrayList<>();
        for (Operator stdOp : operators) {
            boolean add = true;
            for (Operator extraOperator : extraOperators) {
                // only consider extra operator could be AdaptableOperatorSampler, but standard op is not
                for (Operator extra : getActualOperators(extraOperator)) {
                    // same operators operate on the same tree
                    if (stdOp instanceof TreeOperator stdTreeOP && extra instanceof TreeOperator extraTreeOP
                            && stdTreeOP.getClass().equals(extraTreeOP.getClass())
                            && stdTreeOP.treeInput.get().equals(extraTreeOP.treeInput.get())) {
                        // narrow or wide Exchange
                        if (stdOp instanceof Exchange exchange && extra instanceof Exchange exchange2
                            && exchange2.isNarrowInput.get().equals(exchange.isNarrowInput.get())) {
                            // cannot remove it from original list
                            LoggerUtils.log.info("Skip the duplicated operator: " + stdOp + " for " + stdTreeOP.treeInput.get().getID());
                            add = false;
                        }
                    }
                }
            }
            // after the duplication check
            if (add)
                newOperators.add(stdOp);
        }
        return newOperators;
    }

    private List<Operator> getActualOperators(Operator operator) {
        if (operator instanceof AdaptableOperatorSampler sampler)
            return sampler.operatorsInput.get();
        else return List.of(operator);
    }


    //*** parameter operators ***//

    public Operator createBEASTOperator(RealParameter parameter) {
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
                operator = getDeltaExchangeOperator();
                operator.setInputValue("parameter", parameter);
                operator.setInputValue("weight", getOperatorWeight(parameter.getDimension() - 1));
                operator.setInputValue("delta", 1.0 / value.length);
                operator.initAndValidate();
                operator.setID(parameter.getID() + ".deltaExchange");

            } else if (supportNegativeValues(generativeDistribution)) {
                // any distribution with support in negative values, e.g. Normal, Laplace.
                operator = getRandomWalkOperator();
                operator.setInputValue("parameter", parameter);
                operator.setInputValue("weight", getOperatorWeight(parameter.getDimension()));
                operator.setInputValue("scaleFactor", 0.75);
                operator.initAndValidate();
                operator.setID(parameter.getID() + ".randomWalk");

            } else {
                operator = getScaleOperator();
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

    // for RandomWalkOperator
    public static boolean supportNegativeValues(GenerativeDistribution generativeDistribution) {
        if ( generativeDistribution instanceof GenerativeDistribution1D<?> oneD) {
            Object[] bounds = oneD.getDomainBounds();
            if ( bounds[0] instanceof Number lower && bounds[1] instanceof Number upper ) {
                return lower.doubleValue() < 0 && upper.doubleValue() > 0;
            }
            return false;
        }
        return false;
    }

    public Operator createBEASTOperator(IntegerParameter parameter) {
        Map<BEASTInterface, GraphicalModelNode<?>> BEASTToLPHYMap = context.getBEASTToLPHYMap();
        // TODO safe cast?
        RandomVariable<?> variable = (RandomVariable<?>) BEASTToLPHYMap.get(parameter);

        Operator operator;
        if (variable.getGenerativeDistribution() instanceof RandomComposition) {
            System.out.println("Constructing operator for randomComposition");

            operator = getDeltaExchangeOperator();
            operator.setInputValue("intparameter", parameter);
            operator.setInputValue("weight", getOperatorWeight(parameter.getDimension() - 1));
            operator.setInputValue("delta", 2.0);
            operator.setInputValue("integer", true);
            operator.initAndValidate();
            operator.setID(parameter.getID() + ".deltaExchange");
        } else {
            operator = getIntRandomWalkOperator();
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
        Operator operator = getBitFlipOperator();
        operator.setInputValue("parameter", parameter);
        operator.setInputValue("weight", getOperatorWeight(parameter.getDimension()));
        operator.initAndValidate();
        operator.setID(parameter.getID() + ".bitFlip");

        return operator;
    }

    //*** static methods ***//

    // when both mu and tree are random var
    public static void addUpDownOperator(Tree tree, RealParameter clockRate, BEASTContext context) {
        String idStr = clockRate.getID() + "Up" + tree.getID() + "DownOperator";
        // avoid to duplicate updown ops from the same pair of rate and tree
        if (!context.hasExtraOperator(idStr)) {
            Operator upDownOperator = new BactrianUpDownOperator();
            upDownOperator.setID(idStr);
            upDownOperator.setInputValue("up", clockRate);
            upDownOperator.setInputValue("down", tree);
            upDownOperator.setInputValue("scaleFactor", 0.9);
            upDownOperator.setInputValue("weight", BEASTContext.getOperatorWeight(tree.getInternalNodeCount()+1));
            context.addExtraOperator(upDownOperator);
        }
    }

    public static void addDeltaExchangeOperator(Value<Double[]> value, List<Function> args, BEASTContext context) {
        WeightedDirichlet weightedDirichlet = (WeightedDirichlet) value.getGenerator();
        IntegerParameter weightIntParam = context.getAsIntegerParameter(weightedDirichlet.getWeights());

        Operator operator = new BactrianDeltaExchangeOperator();
        operator.setInputValue("parameter", args);
        operator.setInputValue("weight", BEASTContext.getOperatorWeight(args.size() - 1));
        operator.setInputValue("weightvector", weightIntParam);
        operator.setInputValue("delta", 1.0 / value.value().length);
        operator.initAndValidate();
        operator.setID(value.getCanonicalId() + ".deltaExchange");
        context.addExtraOperator(operator);
    }
}
