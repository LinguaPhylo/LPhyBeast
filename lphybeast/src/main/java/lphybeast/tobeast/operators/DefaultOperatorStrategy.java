package lphybeast.tobeast.operators;

import beast.base.core.BEASTInterface;
import beast.base.core.BEASTObject;
import beast.base.evolution.operator.AdaptableOperatorSampler;
import beast.base.evolution.operator.kernel.AdaptableVarianceMultivariateNormalOperator;
import beast.base.evolution.operator.kernel.BactrianScaleOperator;
import beast.base.evolution.tree.Tree;
import beast.base.inference.Operator;
import beast.base.inference.StateNode;
import beast.base.inference.operator.BitFlipOperator;
import beast.base.inference.operator.IntRandomWalkOperator;
import beast.base.inference.operator.kernel.BactrianDeltaExchangeOperator;
import beast.base.inference.operator.kernel.BactrianRandomWalkOperator;
import beast.base.inference.operator.kernel.BactrianUpDownOperator;
import beast.base.inference.operator.kernel.Transform;
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
public class DefaultOperatorStrategy implements OperatorStrategy {

    // store all <f idref= /> inside each AVMNOperator,
    // which maps to each data partition (tree likelihood) in beast.
    protected Map<StateNode, String> idrefAVMNN = new HashMap<>();

    protected final BEASTContext context;

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

        for (BEASTInterface beastInterface : context.getBeastObjForOpSamplers()) {
            if (beastInterface instanceof AdaptableVarianceMultivariateNormalOperator opAVMNN) {
                operators.add(opAVMNN);

                // TODO fill in Map<StateNode, String> idrefAVMNN, value is opAVMNN.getID()
            }
        }

        Set<StateNode> skipOperators = context.getSkipOperators();
        for (StateNode stateNode : context.getState()) {
            if (!skipOperators.contains(stateNode)) {
                // The default template to create operators
                if (stateNode instanceof RealParameter realParameter) {
                    Operator operator = createBEASTOperator(realParameter);
                    if (operator != null)
                        addOperatorOrSampler(stateNode, operator, operators);
                } else if (stateNode instanceof IntegerParameter integerParameter) {
                    Operator operator = createBEASTOperator(integerParameter);
                    addOperatorOrSampler(stateNode, operator, operators);
                } else if (stateNode instanceof BooleanParameter booleanParameter) {
                    Operator operator = createBitFlipOperator(booleanParameter);
                    addOperatorOrSampler(stateNode, operator, operators);
                } else if (stateNode instanceof Tree tree) {
                    TreeOperatorStrategy treeOperatorStrategy = context.resolveTreeOperatorStrategy(tree);
                    // create operators
                    List<Operator> treeOperators = treeOperatorStrategy.createTreeOperators(tree, context);
                    if (treeOperators.size() < 1)
                        throw new IllegalArgumentException("No operators are created by strategy " +
                                treeOperatorStrategy.getName() + " !");
                    //TODO or samplers?
                    operators.addAll(treeOperators);
                }
            }
        }

        operators.addAll(context.getExtraOperators());
        operators.sort(Comparator.comparing(BEASTObject::getID));

        return operators;
    }

    protected void addOperatorOrSampler(StateNode stateNode, Operator operator, List<Operator> operators) {
        // frequencies, site and substitution model parameters, trees
        if (context.isForOperatorSampler(stateNode)) {
            AdaptableOperatorSampler operatorSampler = new AdaptableOperatorSampler();

            operatorSampler.setInputValue("weight", "0.05");
            // TODO not only parameter
            operatorSampler.setInputValue("parameter", stateNode);
            // add operator here
            operatorSampler.setInputValue("operator", operator);

            //TODO  <operator idref="AVMNOperator.$(n)"/>

            operatorSampler.setID(stateNode.getID() + ".OperatorSampler");
            operatorSampler.initAndValidate();
            operators.add(operatorSampler);

        } else operators.add(operator);

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

    // AVMNOperator for each TreeLikelihood
    public static AdaptableVarianceMultivariateNormalOperator initAVMNOperator() {
        AdaptableVarianceMultivariateNormalOperator opAVMNN = new AdaptableVarianceMultivariateNormalOperator();
        opAVMNN.setInputValue("weight", "0.1");
        opAVMNN.setInputValue("coefficient", "1.0");
        opAVMNN.setInputValue("scaleFactor", "1");
        opAVMNN.setInputValue("beta", "0.05");
        opAVMNN.setInputValue("initial", "800");
        opAVMNN.setInputValue("burnin", "400");
        opAVMNN.setInputValue("every", "1");
        opAVMNN.setInputValue("allowNonsense", "true");
        // require initAndValidate later for adding more input in runtime
        return opAVMNN;
    }

    public static Transform.LogConstrainedSumTransform initAVMNSumTransform(String idSteam) {
        Transform.LogConstrainedSumTransform transform = new Transform.LogConstrainedSumTransform();
        transform.setID(idSteam + ".AVMNSumTransform");
        return transform;
    }

    public static Transform.LogTransform initLogTransform(String idSteam) {
        Transform.LogTransform transform = new Transform.LogTransform();
        transform.setID(idSteam + ".AVMNLogTransform");
        return transform;
    }

    public static Transform.NoTransform initNoTransform(String idSteam) {
        Transform.NoTransform transform = new Transform.NoTransform();
        transform.setID(idSteam + ".AVMNNoTransform");
        return transform;
    }

    // when both mu and tree are random var
    public static void addUpDownOperator(Tree tree, RealParameter clockRate, BEASTContext context) {
        String idStr = clockRate.getID() + "Up" + tree.getID() + "DownOperator";
        // avoid to duplicate updown ops from the same pair of rate and tree
        if (!context.hasExtraOperator(idStr)) {
            BactrianUpDownOperator upDownOperator = new BactrianUpDownOperator();
            upDownOperator.setID(idStr);
            upDownOperator.setInputValue("up", clockRate);
            upDownOperator.setInputValue("down", tree);
            upDownOperator.setInputValue("scaleFactor", 0.9);
            upDownOperator.setInputValue("weight", BEASTContext.getOperatorWeight(tree.getInternalNodeCount()+1));
            context.addExtraOperator(upDownOperator);
        }
    }
}
