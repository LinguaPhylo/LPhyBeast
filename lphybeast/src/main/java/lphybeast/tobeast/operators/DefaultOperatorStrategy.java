package lphybeast.tobeast.operators;

import beast.base.core.BEASTInterface;
import beast.base.core.BEASTObject;
import beast.base.core.Function;
import beast.base.evolution.operator.Exchange;
import beast.base.evolution.operator.TreeOperator;
import beast.base.evolution.tree.Tree;
import beast.base.inference.Operator;
import beast.base.inference.Scalable;
import beast.base.inference.StateNode;

import beast.base.spec.evolution.operator.AdaptableOperatorSampler;
import beast.base.spec.evolution.operator.UpDownOperator;
import beast.base.spec.inference.operator.DeltaExchangeOperator;
import beast.base.spec.inference.parameter.BoolVectorParam;
import beast.base.spec.inference.parameter.IntSimplexParam;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.spec.inference.parameter.SimplexParam;
import com.google.common.collect.Multimap;
import lphy.base.distribution.WeightedDirichlet;
import lphy.core.logger.LoggerUtils;
import lphy.core.model.*;
import lphybeast.BEASTContext;
import lphybeast.spi.OperatorContributor;

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
                // beast3 spec types
                if (stateNode instanceof SimplexParam simplex) {
                    operators.add(createSimplexOperator(simplex));
                } else if (stateNode instanceof IntSimplexParam<?> intSimplex) {
                    operators.add(createIntSimplexOperator(intSimplex));
                } else if (stateNode instanceof RealVectorParam<?> realVector) {
                    // DeltaExchangeOperator takes priority: skip ScaleOperator if one is already
                    // registered in extraOperators for this parameter (e.g. from WeightedDirichlet).
                    if (!hasDeltaExchangeOperator(realVector, extraOperators))
                        operators.add(createRealVectorOperator(realVector));
                } else if (stateNode instanceof RealScalarParam<?> realScalar) {
                    operators.add(createRealScalarOperator(realScalar));
                } else if (stateNode instanceof BoolVectorParam boolVector) {
                    operators.add(createBoolVectorOperator(boolVector));
                } else if (stateNode instanceof Tree tree) {
                    TreeOperatorStrategy treeOperatorStrategy = context.resolveTreeOperatorStrategy(tree);
                    // create operators
                    List<Operator> treeOperators = treeOperatorStrategy.createTreeOperators(tree, context);
                    if (treeOperators.size() < 1)
                        throw new IllegalArgumentException("No operators are created by strategy " +
                                treeOperatorStrategy.getName() + " !");

                    // rm the tree operator (e.g. Narrow Exchange by ORC) duplicated to extraOperators
                    List<Operator> noDuplicatedOperators = getNoDuplicatedOperators(treeOperators, extraOperators);

                    operators.addAll(noDuplicatedOperators);
                } else {
                    // Delegate to OperatorContributors (e.g., MutableAlignment from MA extension)
                    boolean handled = false;
                    for (OperatorContributor contributor : context.getOperatorContributors()) {
                        if (contributor.canHandle(stateNode)) {
                            operators.addAll(contributor.createOperators(stateNode, context));
                            handled = true;
                            break;
                        }
                    }
                    if (!handled) {
                        LoggerUtils.log.warning("No operator created for state node: " + stateNode.getID() +
                                " of type " + stateNode.getClass().getSimpleName());
                    }
                }
            }
        }

        // then add all extra
        operators.addAll(extraOperators);
        operators.sort(Comparator.comparing(BEASTObject::getID,
                Comparator.nullsLast(String::compareTo) // Handle null IDs
                ));
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


    //*** beast3 spec parameter operators ***//

    private Operator createSimplexOperator(SimplexParam simplex) {
        var operator = new beast.base.spec.inference.operator.DeltaExchangeOperator();
        operator.setInputValue("rvparameter", simplex);
        operator.setInputValue("weight", getOperatorWeight(simplex.size() - 1));
        operator.setInputValue("delta", 1.0 / simplex.size());
        operator.initAndValidate();
        operator.setID(simplex.getID() + ".deltaExchange");
        Multimap<BEASTInterface, GraphicalModelNode<?>> elements = context.getElements();
        elements.put(operator, null);
        return operator;
    }

    private Operator createIntSimplexOperator(IntSimplexParam<?> intSimplex) {
        var operator = new beast.base.spec.inference.operator.DeltaExchangeOperator();
        operator.setInputValue("ivparameter", intSimplex);
        operator.setInputValue("weight", getOperatorWeight(intSimplex.size() - 1));
        operator.setInputValue("delta", 2.0);
        operator.setInputValue("integer", true);
        operator.initAndValidate();
        operator.setID(intSimplex.getID() + ".deltaExchange");
        Multimap<BEASTInterface, GraphicalModelNode<?>> elements = context.getElements();
        elements.put(operator, null);
        return operator;
    }

    private Operator createRealVectorOperator(RealVectorParam<?> realVector) {
        var operator = new beast.base.spec.inference.operator.ScaleOperator();
        operator.setInputValue("parameter", realVector);
        operator.setInputValue("weight", getOperatorWeight(realVector.size()));
        operator.setInputValue("scaleFactor", 0.75);
        operator.initAndValidate();
        operator.setID(realVector.getID() + ".scale");
        Multimap<BEASTInterface, GraphicalModelNode<?>> elements = context.getElements();
        elements.put(operator, null);
        return operator;
    }

    private Operator createRealScalarOperator(RealScalarParam<?> realScalar) {
        var operator = new beast.base.spec.inference.operator.ScaleOperator();
        operator.setInputValue("parameter", realScalar);
        operator.setInputValue("weight", getOperatorWeight(1));
        operator.setInputValue("scaleFactor", 0.75);
        operator.initAndValidate();
        operator.setID(realScalar.getID() + ".scale");
        Multimap<BEASTInterface, GraphicalModelNode<?>> elements = context.getElements();
        elements.put(operator, null);
        return operator;
    }

    private Operator createBoolVectorOperator(BoolVectorParam boolVector) {
        var operator = new beast.base.spec.inference.operator.BitFlipOperator();
        operator.setInputValue("parameter", boolVector);
        operator.setInputValue("weight", getOperatorWeight(boolVector.size()));
        operator.initAndValidate();
        operator.setID(boolVector.getID() + ".bitFlip");
        Multimap<BEASTInterface, GraphicalModelNode<?>> elements = context.getElements();
        elements.put(operator, null);
        return operator;
    }

    /**
     * Returns true if a {@link DeltaExchangeOperator} targeting {@code param} already exists
     * in {@code extraOperators}. Used to prevent a redundant {@link beast.base.spec.inference.operator.ScaleOperator}
     * from being created for the same parameter.
     * <p>
     * Note: it is assumed that any parameter assigned a {@link DeltaExchangeOperator} is a
     * {@link RealVectorParam}, because the operator works on a vector whose elements are
     * individually represented as scalars summing to a constrained total.
     */
    private boolean hasDeltaExchangeOperator(RealVectorParam<?> param, List<Operator> extraOperators) {
        return extraOperators.stream()
                .anyMatch(op -> op instanceof DeltaExchangeOperator deltaOp
                        && param.equals(deltaOp.getInput("rvparameter").get()));
    }

    //*** static methods ***//

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

    // when both mu and tree are random var
    public static void addUpDownOperator(Tree tree, StateNode clockRate, BEASTContext context) {
        String idStr = clockRate.getID() + "Up" + tree.getID() + "DownOperator";
        // avoid to duplicate updown ops from the same pair of rate and tree
        if (!context.hasExtraOperator(idStr)) {
            Operator upDownOperator = new UpDownOperator();
            upDownOperator.setID(idStr);
            upDownOperator.setInputValue("up", clockRate);
            upDownOperator.setInputValue("down", tree);
            upDownOperator.setInputValue("scaleFactor", 0.9);
            upDownOperator.setInputValue("weight", BEASTContext.getOperatorWeight(tree.getInternalNodeCount()+1));
            context.addExtraOperator(upDownOperator);
        }
    }

    /**
     * Add an up-down operator when the clock rate is computed by an expression
     * (e.g., ExpCalculator from feast). The underlying function arguments
     * are scaled upward against the tree.
     *
     * @param tree       the tree to scale down
     * @param upArgs     the function arguments to scale up
     * @param expression the expression BEASTInterface (for ID)
     * @param context    the BEAST context
     */
    public static void addUpDownOperator(Tree tree, List<Function> upArgs, BEASTInterface expression, BEASTContext context) {
        String idStr = expression.getID() + "Up" + tree.getID() + "DownOperator";
        // avoid to duplicate updown ops from the same pair of rate and tree
        if (!context.hasExtraOperator(idStr)) {
            Operator upDownOperator = new UpDownOperator();
            upDownOperator.setID(idStr);

            for (Function function : upArgs) {
                if (function instanceof Scalable) {
                    upDownOperator.setInputValue("up", function);
                } else {
                    LoggerUtils.log.warning("Cannot add " + function + " to up-down operator: not Scalable");
                }
            }
            LoggerUtils.log.warning("The clock rate is computed by expression " +
                    expression.getID() +
                    ", where all arguments are assumed to scale upward in the up-down operator !");
            upDownOperator.setInputValue("down", tree);
            upDownOperator.setInputValue("scaleFactor", 0.9);
            upDownOperator.setInputValue("weight", BEASTContext.getOperatorWeight(tree.getInternalNodeCount()+1));
            context.addExtraOperator(upDownOperator);
        }
    }

    public static void addDeltaExchangeOperator(Value<Double[]> value, RealVectorParam<?> param, BEASTContext context) {
        WeightedDirichlet weightedDirichlet = (WeightedDirichlet) value.getGenerator();
        BEASTInterface weightsObj = context.getBEASTObject(weightedDirichlet.getWeights());

        DeltaExchangeOperator operator = new DeltaExchangeOperator();
        operator.setInputValue("rvparameter", param);
        operator.setInputValue("weight", BEASTContext.getOperatorWeight(param.size() - 1));
        operator.setInputValue("weightvector", weightsObj);
        operator.setInputValue("delta", 1.0 / value.value().length);
        operator.initAndValidate();
        operator.setID(value.getCanonicalId() + ".deltaExchange");
        context.addExtraOperator(operator);
    }
}
