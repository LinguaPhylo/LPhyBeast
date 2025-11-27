package lphybeast;

import beast.base.core.BEASTInterface;
import beast.base.core.Function;
import beast.base.core.Loggable;
import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.alignment.Taxon;
import beast.base.evolution.datatype.DataType;
import beast.base.evolution.likelihood.GenericTreeLikelihood;
import beast.base.evolution.substitutionmodel.Frequencies;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeInterface;
import beast.base.inference.*;
import beast.base.inference.distribution.ParametricDistribution;
import beast.base.inference.distribution.Prior;
import beast.base.inference.parameter.IntegerParameter;
import beast.base.inference.parameter.Parameter;
import beast.base.inference.parameter.RealParameter;
import beast.base.parser.XMLProducer;
import beastlabs.core.util.Slice;
import beastlabs.util.BEASTVector;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import coupledMCMC.CoupledMCMC;
import feast.expressions.ExpCalculator;
import feast.function.Concatenate;
import jebl.evolution.sequences.SequenceType;
import lphy.core.logger.LoggerUtils;
import lphy.core.model.*;
import lphy.core.parser.LPhyParserDictionary;
import lphy.core.parser.ObservationUtils;
import lphy.core.parser.graphicalmodel.GraphicalModelNodeVisitor;
import lphy.core.parser.graphicalmodel.ValueCreator;
import lphy.core.vectorization.VectorUtils;
import lphy.core.vectorization.VectorizedRandomVariable;
import lphy.core.vectorization.operation.ElementsAt;
import lphy.core.vectorization.operation.SliceValue;
import lphybeast.spi.LPhyBEASTExt;
import lphybeast.tobeast.loggers.LoggerFactory;
import lphybeast.tobeast.loggers.LoggerHelper;
import lphybeast.tobeast.operators.DefaultOperatorStrategy;
import lphybeast.tobeast.operators.DefaultTreeOperatorStrategy;
import lphybeast.tobeast.operators.OperatorStrategy;
import lphybeast.tobeast.operators.TreeOperatorStrategy;
import lphybeast.tobeast.values.ValueToParameter;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import static java.lang.Math.toIntExact;
import static lphybeast.LPhyBeastConfig.NUM_OF_SAMPLES;

/**
 * The central class to keep the configurations,
 * objects mapping between LPhy and BEAST2,
 * and methods to process them and create BEAST2 objects for generating XML.
 */
public class BEASTContext {

    public static final String POSTERIOR_ID = "posterior";
    public static final String PRIOR_ID = "prior";
    public static final String LIKELIHOOD_ID = "likelihood";

    //*** registry ***//

    // contain the Values and Generators parsed from a lphy script.
    LPhyParserDictionary parserDictionary;

    List<ValueToBEAST> valueToBEASTList;
    //use LinkedHashMap to keep inserted ordering, so the first matching converter is used.
    Map<Class, GeneratorToBEAST> generatorToBEASTMap;
    // LPhy SequenceType => BEAST DataType
    Map<SequenceType, DataType> dataTypeMap;

    List<Class<? extends Generator>> excludedGeneratorClasses;
    List<Class> excludedValueTypes;

    //*** to BEAST ***//

    private List<StateNode> state = new ArrayList<>();

    // a list of extra beast elements in the keys,
    // with a pointer to the graphical model node that caused their production
    private Multimap<BEASTInterface, GraphicalModelNode<?>> elements = HashMultimap.create();
    private List<StateNodeInitialiser> inits = new ArrayList<>();

    // a map of graphical model nodes to a list of equivalent BEASTInterface objects
    private Map<GraphicalModelNode<?>, BEASTInterface> beastObjects = new HashMap<>();

    // a map of BEASTInterface to graphical model nodes that they represent
    private Map<BEASTInterface, GraphicalModelNode<?>> BEASTToLPHYMap = new HashMap<>();

    SortedMap<String, Taxon> allTaxa = new TreeMap<>();

    //*** operators ***//
    // a list of beast state nodes to skip the automatic operator creation for.
    private Set<StateNode> skipOperators = new HashSet<>();
    // extra operators either for default or from extensions
    private List<Operator> extraOperators = new ArrayList<>();
    // A list of strategy patterns define how to create operators in extensions,
    // which already exclude DefaultTreeOperatorStrategy
    private List<TreeOperatorStrategy> newTreeOperatorStrategies;

    //*** operators ***//
    // a list of extra loggables in 3 default loggers: parameter logger, screen logger, tree logger.
    private List<Loggable> extraLoggables = new ArrayList<>();
    // a list of extra loggables in 3 default loggers: parameter logger, screen logger, tree logger.
    private List<Loggable> skipLoggables = new ArrayList<>();
    // helper to create extra loggers from extensions
    private List<LoggerHelper> extraLoggers = new ArrayList<>();

    // required
    final private LPhyBeastConfig lPhyBeastConfig;

    @Deprecated
    public BEASTContext(LPhyParserDictionary parserDictionary, LPhyBeastConfig lPhyBeastConfig) {
        this(parserDictionary, null, lPhyBeastConfig);
    }

    /**
     * Find all core classes {@link ValueToBEAST} and {@link GeneratorToBEAST},
     * including {@link DataType} mapped to lphy {@link SequenceType},
     * and then register them for XML creators to use.
     * @param parserDictionary  the parsed lphy commands
     * @param loader to load LPhyBEAST extensions.
     *               Can be null, then initiate here.
     */
    public BEASTContext(LPhyParserDictionary parserDictionary, LPhyBEASTLoader loader, LPhyBeastConfig lPhyBeastConfig) {
        this.parserDictionary = parserDictionary;
        if (loader == null)
            loader = LPhyBEASTLoader.getInstance();
        this.lPhyBeastConfig = lPhyBeastConfig;

        valueToBEASTList = loader.valueToBEASTList;
        generatorToBEASTMap = loader.generatorToBEASTMap;
        dataTypeMap = loader.dataTypeMap;

        excludedGeneratorClasses = loader.excludedGeneratorClasses;
        excludedValueTypes = loader.excludedValueTypes;

        newTreeOperatorStrategies = loader.newTreeOperatorStrategies;
    }

    /**
     * Main method to process configurations to create BEAST 2 XML from LPhy objects.
     *
     * @param logFileStem  log file stem
     * @return BEAST 2 XML in String
     */
    public String toBEASTXML(final String logFileStem) {
        long chainLength = lPhyBeastConfig.getChainLength();
        int preBurnin = lPhyBeastConfig.getPreBurnin();
        boolean sampleFromPrior = lPhyBeastConfig.sampleFromPrior();
        // default to 1M if not specified
        if (chainLength < NUM_OF_SAMPLES)
            throw new IllegalArgumentException("Invalid length for MCMC chain, len = " + chainLength);
        // Will throw an ArithmeticException in case of overflow.
        long logEvery = lPhyBeastConfig.getLogEvery();
        int nsamp = toIntExact(chainLength/logEvery);
        if (nsamp < NUM_OF_SAMPLES/2)
            LoggerUtils.log.warning("The number of logged sample (" + nsamp + ") is too small ! Prefer " + NUM_OF_SAMPLES);

        // this fills in List<StateNode> state
        createBEASTObjects();
        assert state.size() > 0;

        // if preBurnin < 0, then will be defined by all state nodes size
        if (preBurnin < 0)
            preBurnin = getAllStatesSize(state) * 10;

        LoggerUtils.log.info("Set MCMC chain length = " + chainLength + ", log every = " +
                logEvery + ", samples = " + NUM_OF_SAMPLES + ", preBurnin = " + preBurnin + ", sampleFromPrior = " + sampleFromPrior);

        MCMC mcmc = createMCMC(chainLength, logEvery, logFileStem, preBurnin, sampleFromPrior);

        return new XMLProducer().toXML(mcmc, elements.keySet());
    }

    /**
     * Similar to {@link #toBEASTXML}, but sets up an MC³ chain (Metropolis-coupled MCMC),
     * which runs multiple chains at different temperatures for improved mixing.
     * @param logFileStem  log file stem (no extension)
     * @return             XML string configured for MC³
     */

    public String toBEASTXML_MC3(final String logFileStem) {

        long chainLength = lPhyBeastConfig.getChainLength();
        long logEvery = lPhyBeastConfig.getLogEvery();
        int preBurnin = lPhyBeastConfig.getPreBurnin();

        if (chainLength < NUM_OF_SAMPLES) {
            throw new IllegalArgumentException("Invalid length for MC3 chain, len = " + chainLength);
        }

        int nsamp = toIntExact(chainLength / logEvery);
        if (nsamp < NUM_OF_SAMPLES/2) {
            LoggerUtils.log.warning("The number of logged sample (" + nsamp + ") is too small ! Prefer " + NUM_OF_SAMPLES);
        }

        createBEASTObjects();
        assert state.size() > 0;

        if (preBurnin < 0) {
            preBurnin = getAllStatesSize(state) * 10;
        }

        LoggerUtils.log.info("Set MC3 chain length = " + chainLength + ", log every = "
                + logEvery + ", samples = " + NUM_OF_SAMPLES + ", preBurnin = " + preBurnin);


        CoupledMCMC mc3 = createMC3(chainLength, logEvery, logFileStem, preBurnin);

        return new XMLProducer().toXML(mc3, elements.keySet());
    }



    //*** BEAST 2 Parameters ***//

    public static RealParameter createRealParameter(Double[] value) {
        return new RealParameter(value);
    }

    public static RealParameter createRealParameter(double value) {
        return createRealParameter(null, value);
    }

    public static IntegerParameter createIntegerParameter(String id, int value) {
        IntegerParameter parameter = new IntegerParameter();
        parameter.setInputValue("value", value);
        parameter.initAndValidate();
        if (id != null) parameter.setID(id);

        return parameter;
    }

    public static IntegerParameter createIntegerParameter(String id, Integer[] value) {
        IntegerParameter parameter = new IntegerParameter();
        parameter.setInputValue("value", Arrays.asList(value));
        parameter.initAndValidate();
        if (id != null) parameter.setID(id);

        return parameter;
    }

    public static RealParameter createRealParameter(String id, double value) {
        RealParameter parameter = new RealParameter();
        parameter.setInputValue("value", value);
        parameter.initAndValidate();
        if (id != null) parameter.setID(id);

        return parameter;
    }

    public static RealParameter createRealParameter(String id, Double[] value) {
        RealParameter parameter = new RealParameter();
        parameter.setInputValue("value", Arrays.asList(value));
        parameter.initAndValidate();
        if (id != null) parameter.setID(id);

        return parameter;
    }

    /**
     * @param value    IntegerArray/DoubleArray, and set estimate="false" for values that are not RandomVariables.
     * @param lower    Number, can be null
     * @param upper    Number, can be null
     * @param forceToDouble  if true, it will ignore whether component type is Integer or not,
     *                       and always return RealParameter.
     * @return        A {@link IntegerParameter} or {@link RealParameter}
     *                given bounds and array values based on the type of values.
     */
    public static Parameter<? extends Number> createParameterWithBound(
            Value<? extends Number[]> value, Number lower, Number upper, boolean forceToDouble) {

        Parameter.Base parameter;

        // forceToDouble will ignore whether component type is Integer or not
        if ( !forceToDouble &&
                Objects.requireNonNull(value).getType().getComponentType().isAssignableFrom(Integer.class) ) {

            parameter = new IntegerParameter();

            if (lower != null)
                parameter.setInputValue("lower", lower.intValue());
            if (upper != null)
                parameter.setInputValue("upper", upper.intValue());

        } else { // Double and Number
            parameter = new RealParameter();

            if (lower != null)
                parameter.setInputValue("lower", lower.doubleValue());
            if (upper != null)
                parameter.setInputValue("upper", upper.doubleValue());
        }

        List<Number> values = Arrays.asList(value.value());
        parameter.setInputValue("value", values);
        parameter.setInputValue("dimension", values.size());

        // set estimate="false" for IntegerArray/DoubleArray values that are not RandomVariables.
        if (!(value instanceof RandomVariable))
            parameter.setInputValue("estimate", false);

        parameter.initAndValidate();
        ValueToParameter.setID(parameter, value);

        return parameter;
    }

    /**
     * This is used to handle {@link ExpressionNode},
     * and then pass value to {@link #getAsRealParameter} if it is not generated by expressions.
     * @param value   LPhy {@link Value}
     * @return  {@link Function}
     */
    public Function getAsFunctionOrRealParameter(Value value) {
        if (value instanceof SliceValue sliceValue) {
            Value v = sliceValue.getSlicedValue();
                if (v.getGenerator() instanceof ExpressionNode expressionNode) {
                    BEASTInterface beastInterface = beastObjects.get(expressionNode);
                    if (beastInterface instanceof Function function) {
                        addToContext(value, beastInterface);
                        return function;
                    }
                }

        }

        BEASTInterface beastInterface = beastObjects.get(value);
        if (beastInterface == null) {
            // value is generated by ExpressionNode
            if (value.getGenerator() instanceof ExpressionNode expressionNode) {
                beastInterface = beastObjects.get(expressionNode);
                if (beastInterface instanceof Function function) {
                    addToContext(value, beastInterface);
                    return function;
                }
            }
        } else if (beastInterface instanceof ExpCalculator expCalculator)
            return expCalculator;

//        if (value instanceof SliceValue sliceValue)
//            return (Function) handleSliceRequest(sliceValue);
//        else
            return getAsRealParameter(value);
    }

    /**
     * This function will retrieve the beast object for this value and return it if it is a RealParameter,
     * or convert it to a RealParameter if it is an IntegerParameter and replace the original integer parameter in the relevant stores.
     *
     * @param value
     * @return the RealParameter associated with this value if it exists, or can be coerced. Has a side-effect if coercion occurs.
     */
    public RealParameter getAsRealParameter(Value value) {
        if  (value instanceof SliceValue sliceValue) {

            BEASTInterface slicedBEASTValue = beastObjects.get(sliceValue.getSlicedValue());
            if (slicedBEASTValue != null) {
                if (!(slicedBEASTValue instanceof Concatenate)) {
                    String id = lPhyBeastConfig.isLogUnicode() ? sliceValue.getId() : sliceValue.getCanonicalId();
                    Slice slice = SliceFactory.createSlice(slicedBEASTValue, sliceValue.getIndex(), id);
                    addToContext(sliceValue, slice);
                    return slice;
                } else {
                    // handle by concatenating
                    List<Function> parts = ((Concatenate) slicedBEASTValue).functionsInput.get();
                    Function slice = parts.get(sliceValue.getIndex());
                    addToContext(sliceValue, (BEASTInterface) slice);
                    return (BEASTInterface) slice;
                }
            } 
            RealParameter newParam = createRealParameter(sliceValue.getId(), sliceValue.getSlicedValue().value());
            removeBEASTObject((BEASTInterface) param);
            addToContext(value, newParam);

        }

        Parameter param = (Parameter) beastObjects.get(value);

        if (param instanceof RealParameter) return (RealParameter) param;
        if (param instanceof IntegerParameter) {
            if (param.getDimension() == 1) {

                RealParameter newParam = createRealParameter(param.getID(), ((IntegerParameter) param).getValue());
                removeBEASTObject((BEASTInterface) param);
                addToContext(value, newParam);
                return newParam;
            } else {
                Double[] values = new Double[param.getDimension()];
                for (int i = 0; i < values.length; i++) {
                    values[i] = ((IntegerParameter) param).getValue(i).doubleValue();
                }

                RealParameter newParam = createRealParameter(param.getID(), values);
                removeBEASTObject((BEASTInterface) param);
                addToContext(value, newParam);
                return newParam;

            }
        }
        throw new RuntimeException("No coercible parameter found for " + value);
    }

//    public RealParameter convertSliceToRealParameter(lphy.core.vectorization.operation.Slice lphySlice) {
//        SliceValue sliceValue = lphySlice.get
//
//        RealParameter newParam = createRealParameter(param.getID(), ((IntegerParameter) param).getValue());
//        removeBEASTObject((BEASTInterface) param);
//        addToContext(value, newParam);
//        return newParam;
//    }

    public IntegerParameter getAsIntegerParameter(Value value) {
        Parameter param = (Parameter) beastObjects.get(value);
        if (param instanceof IntegerParameter) return (IntegerParameter) param;
        if (param instanceof RealParameter) {
            if (param.getDimension() == 1) {

                IntegerParameter newParam = createIntegerParameter(param.getID(), (int) Math.round(((RealParameter) param).getValue()));
                removeBEASTObject((BEASTInterface) param);
                addToContext(value, newParam);
                return newParam;
            } else {
                Integer[] values = new Integer[param.getDimension()];
                for (int i = 0; i < values.length; i++) {
                    values[i] = ((RealParameter) param).getValue(i).intValue();
                }

                IntegerParameter newParam = createIntegerParameter(param.getID(), values);
                removeBEASTObject((BEASTInterface) param);
                addToContext(value, newParam);
                return newParam;

            }
        }
        throw new RuntimeException("No coercible parameter found for " + value);
    }

    //*** handle BEAST 2 objects ***//

    /**
     * Note: if return RealParameter, must use {@link #getAsRealParameter} not getBEASTObject.
     * @param node  LPhy object
     * @return      the beast object mapped to the given LPhy object
     */
    public BEASTInterface getBEASTObject(GraphicalModelNode<?> node) {

        // Q=jukesCantor(), rateMatrix.getMeanRate() is null
        if (node == null) return null;

        if (node instanceof Value) {
            Value value = (Value)node;
            if (!value.isAnonymous()) {
                BEASTInterface beastInterface = getBEASTObject(value.getId());
                // cannot be Alignment, otherwise getBEASTObject(value.getId()) makes observed alignment not working;
                // it will get simulated Alignment even though data is observed.
                if (beastInterface != null) {
                    if (beastInterface instanceof BEASTVector) {
                        List<BEASTInterface> beastInterfaceList = ((BEASTVector)beastInterface).getObjectList();

                        if ( !(beastInterfaceList.get(0) instanceof Alignment) )
                            return beastInterface;

                    } else if ( !(beastInterface instanceof Alignment) ) {
                        return beastInterface;
                    }
                }
            }
        }

        // have to use this when alignment is observed
        BEASTInterface beastInterface = beastObjects.get(node);

        if (beastInterface != null) {
            return beastInterface;
        } else {
            String id = lPhyBeastConfig.isLogUnicode() ? node.getUniqueId() : Symbols.getCanonical(node.getUniqueId());
            String[] parts = id.split(VectorUtils.INDEX_SEPARATOR);
            if (parts.length == 2) {
                int index = Integer.parseInt(parts[1]);
                Slice slice = createSliceFromVector(node, parts[0], index);
                beastObjects.put(node, slice);
                return slice;
            }
        }

        if (node instanceof SliceValue) {
            SliceValue sliceValue = (SliceValue) node;
            return handleSliceRequest(sliceValue);
        }
        return null;
    }

    public BEASTInterface getBEASTObject(String id) {
        for (BEASTInterface beastInterface : elements.keySet()) {
            if (id.equals(beastInterface.getID())) return beastInterface;
        }

        for (BEASTInterface beastInterface : beastObjects.values()) {
            if (beastInterface.getID() !=  null && id.equals(beastInterface.getID().equals(id))) return beastInterface;
        }
        return null;
    }

    public Slice createSliceFromVector(GraphicalModelNode node, String id, int index) {

        BEASTInterface parentNode = getBEASTObject(Symbols.getCanonical(id));

        Slice slice = SliceFactory.createSlice(parentNode, index,
                Symbols.getCanonical(id) + VectorUtils.INDEX_SEPARATOR + index);
        addToContext(node, slice);
        return slice;

    }

    boolean byslice = false;

    /**
     * returns a logical slice based on the given slice value, as long as the value to be sliced is already available.
     *
     * @param sliceValue the slice value that needs a beast equivalent
     * @return
     */
    public BEASTInterface handleSliceRequest(SliceValue sliceValue) {

        BEASTInterface slicedBEASTValue = beastObjects.get(sliceValue.getSlicedValue());


        if (slicedBEASTValue != null) {
            if (!(slicedBEASTValue instanceof Concatenate)) {
                String id = lPhyBeastConfig.isLogUnicode() ? sliceValue.getId() : sliceValue.getCanonicalId();
                Slice slice = SliceFactory.createSlice(slicedBEASTValue, sliceValue.getIndex(), id);
                addToContext(sliceValue, slice);
                return slice;
            } else {
                // handle by concatenating
                List<Function> parts = ((Concatenate) slicedBEASTValue).functionsInput.get();
                Function slice = parts.get(sliceValue.getIndex());
                addToContext(sliceValue, (BEASTInterface) slice);
                return (BEASTInterface) slice;
            }
        } else return null;
    }

    public GraphicalModelNode getGraphicalModelNode(BEASTInterface beastInterface) {
        return BEASTToLPHYMap.get(beastInterface);
    }

    public void addBEASTObject(BEASTInterface newBEASTObject, GraphicalModelNode graphicalModelNode) {
        elements.put(newBEASTObject, graphicalModelNode);
    }

    /**
     * Directly add to state node list
     * @param stateNode the state node to be added
     * @param graphicalModelNode the graphical model node that this state node corresponds to,
     *                           or represents a part of
     * @param createOperators  whether to create operators for this state node.
     * @see #addToContext(GraphicalModelNode, BEASTInterface) with conditions to determine if add to state node list.
     */
    public void addStateNode(StateNode stateNode, GraphicalModelNode graphicalModelNode, boolean createOperators) {
        if (!state.contains(stateNode)) {
            elements.put(stateNode, graphicalModelNode);
            state.add(stateNode);
        }
        if (!createOperators) skipOperators.add(stateNode);
    }

    public void removeBEASTObject(BEASTInterface beastObject) {
        elements.removeAll(beastObject);
        BEASTToLPHYMap.remove(beastObject);
        if (beastObject instanceof StateNode) state.remove(beastObject);
        if (beastObject instanceof StateNode) skipOperators.remove(beastObject);

        GraphicalModelNode matchingKey = null;
        for (GraphicalModelNode key : beastObjects.keySet()) {
            if (getBEASTObject(key) == beastObject) {
                matchingKey = key;
                break;
            }
        }
        if (matchingKey != null) beastObjects.remove(matchingKey);

        // it may be in extraLoggables
        extraLoggables.remove(beastObject);
    }

    // dealing with -ob "?;?", which can specify any var in lphy to be fixed in beast2 XML.
    public boolean isObserved(Value value) {
        return lPhyBeastConfig.isObserved(value, parserDictionary);
    }

    /**
     * @param id the id of the value
     * @return the value with this id from the data context if it exits, or if not, then the value from the model context if exists, or if neither exist, then returns null.
     */
    public Value getObservedValue(String id) {
        if (id != null) {
            Value observedValue = parserDictionary.getValue(id, LPhyParserDictionary.Context.data);
            if (observedValue != null) {
                return observedValue;
            }
            return parserDictionary.getValue(id, LPhyParserDictionary.Context.model);
        }
        return null;
    }

    public GeneratorToBEAST getGeneratorToBEAST(Generator generator) {
        GeneratorToBEAST toBEAST = generatorToBEASTMap.get(generator.getClass());

        if (toBEAST == null) {
            // else see if there is a compatible to beast
            for (Class c : generatorToBEASTMap.keySet()) {
                // if *ToBEAST exists
                if (c.isAssignableFrom(generator.getClass())) {
                    toBEAST = generatorToBEASTMap.get(c);
                }
            }
        }
        return toBEAST;
    }

    public ValueToBEAST getMatchingValueToBEAST(Value value) {

        for (ValueToBEAST possibleToBEAST : valueToBEASTList) {
            if (possibleToBEAST.match(value)) {
                return possibleToBEAST;
            }
        }
        return null;
    }

    public ValueToBEAST getValueToBEAST(Object rawValue) {
        for (ValueToBEAST possibleToBEAST : valueToBEASTList) {
            // if *ToBEAST exists
            if (possibleToBEAST.match(rawValue)) {
                return possibleToBEAST;
            }
        }
        return null;
    }

    /**
     * The special method to fill in context,
     * use it as a caution.
     * @param node
     * @param beastInterface
     * @see #valueToBEAST(Value)
     */
    public void putBEASTObject(GraphicalModelNode node, BEASTInterface beastInterface) {
        addToContext(node, beastInterface);
    }

    public CompoundDistribution getPosteriorDist() {
        return Objects.requireNonNull(topDist)[0];
    }

    /**
     * The main class to init BEAST 2 MCMC
     */
    private MCMC createMCMC(long chainLength, long logEvery, String logFileStem, int preBurnin, boolean sampleFromPrior) {

        CompoundDistribution posterior = createBEASTPosterior();

        MCMC mcmc = new MCMC();
        mcmc.setInputValue("distribution", posterior);
        mcmc.setInputValue("chainLength", chainLength);

        // TODO eventually all operator related code should go there
        // create XML operator section, with the capability to replace default operators
        OperatorStrategy operatorStrategy = new DefaultOperatorStrategy(this);
        // create all operators, where tree operators strategy can be changed in an extension.
        List<Operator> operators = operatorStrategy.createOperators();
        for (int i = 0; i < operators.size(); i++) {
            System.out.println(operators.get(i));
        }
        mcmc.setInputValue("operator", operators);

        // TODO eventually all logging related code should go there
        // create XML logger section
        topDist = createTopCompoundDist();
        LoggerFactory loggerFactory = new LoggerFactory(this, topDist);
        // 3 default loggers: parameter logger, screen logger, tree logger.
        List<Logger> loggers = loggerFactory.createLoggers(logEvery, logFileStem);
        // extraLoggers processed in LoggerFactory
        mcmc.setInputValue("logger", loggers);

        State state = new State();
        state.setInputValue("stateNode", this.state);
        state.initAndValidate();
        elements.put(state, null);

        // TODO make sure the stateNode list is being correctly populated
        mcmc.setInputValue("state", state);

        if (inits.size() > 0) mcmc.setInputValue("init", inits);

        if (preBurnin > 0)
            mcmc.setInputValue("preBurnin", preBurnin);

        mcmc.initAndValidate();
        if (sampleFromPrior){
            mcmc.setInputValue("sampleFromPrior", sampleFromPrior);
        }

        return mcmc;
    }

    // ----- MC3 Construction -----
    /**
     * Builds the {@link CoupledMCMC} object for multiple chains at different temperatures.
     * This method sets the chain count, temperature increment, and other MC³ parameters.
     */
    private CoupledMCMC createMC3(long chainLength, long logEvery, String logFileStem, int preBurnin) {

        CompoundDistribution posterior = createBEASTPosterior();

        CoupledMCMC mc3 = new CoupledMCMC();
        mc3.setID("mcmcmc");

        mc3.setInputValue("distribution", posterior);
        mc3.setInputValue("chainLength", chainLength);

        // mc3 inputs here
        mc3.setInputValue("chains",          lPhyBeastConfig.getChains());
        mc3.setInputValue("deltaTemperature", lPhyBeastConfig.getDeltaTemperature());
        mc3.setInputValue("resampleEvery",    lPhyBeastConfig.getResampleEvery());
        mc3.setInputValue("target",           lPhyBeastConfig.getTarget());


        // TODO eventually all operator related code should go there
        // create XML operator section, with the capability to replace default operators
        OperatorStrategy operatorStrategy = new DefaultOperatorStrategy(this);
        // create all operators, where tree operators strategy can be changed in an extension.
        List<Operator> operators = operatorStrategy.createOperators();
        for (int i = 0; i < operators.size(); i++) {
            System.out.println(operators.get(i));
        }
        mc3.setInputValue("operator", operators);

        // TODO eventually all logging related code should go there
        // create XML logger section
        topDist = createTopCompoundDist();
        LoggerFactory loggerFactory = new LoggerFactory(this, topDist);
        // 3 default loggers: parameter logger, screen logger, tree logger.
        List<Logger> loggers = loggerFactory.createLoggers(logEvery, logFileStem);
        // extraLoggers processed in LoggerFactory
        mc3.setInputValue("logger", loggers);

        State state = new State();
        state.setInputValue("stateNode", this.state);
        state.initAndValidate();
        elements.put(state, null);

        // TODO make sure the stateNode list is being correctly populated
        mc3.setInputValue("state", state);

        if (inits.size() > 0) mc3.setInputValue("init", inits);

        if (preBurnin > 0)
            mc3.setInputValue("preBurnin", preBurnin);

        mc3.initAndValidate();
        return mc3;
    }

    // posterior, likelihood, prior
    private CompoundDistribution[] topDist;

    // sorted by specific order
    private CompoundDistribution[] createTopCompoundDist() {
        CompoundDistribution[] topDist = new CompoundDistribution[3];
        for (BEASTInterface bI : elements.keySet()) {
            if (bI instanceof CompoundDistribution && bI.getID() != null) {
                if (bI.getID().equals(POSTERIOR_ID))
                    topDist[0] = (CompoundDistribution) bI;
                else if (bI.getID().equals(LIKELIHOOD_ID))
                    topDist[1] = (CompoundDistribution) bI;
                else if (bI.getID().equals(PRIOR_ID))
                    topDist[2] = (CompoundDistribution) bI;
            }
        }
        return topDist;
    }


    /**
     * Make a BEAST2 model from the current model in parser.
     */
    private void createBEASTObjects() {
        // all sinks of the graphical model, including in the data block.
        List<Value<?>> sinks = parserDictionary.getDataModelSinks();

        for (Value<?> value : sinks) {
            createBEASTValueObjects(value);
        }

        Set<Generator> visited = new HashSet<>();
        // 1st traverse calls modifyBEASTValues in each GeneratorToBEAST if implemented,
        // which is to modify/replace the BEASTInterface stored in map, e.g., SliceDoubleArrayToBEAST
        for (Value<?> value : sinks) {
            traverseBEASTGeneratorObjects(value, true, false, visited);
        }

        visited.clear();
        // 2nd traverse converts a generator to an equivalent BEAST object
        for (Value<?> value : sinks) {
            traverseBEASTGeneratorObjects(value, false, true, visited);
        }
    }

    private void updateIDs(Value<?> value) {
        String id = value.getId();
        if (id != null && !id.trim().isEmpty())
            value.setId(Symbols.getCanonical(id));
    }

    /**
     * Creates the beast value objects in a post-order traversal, so that inputs are always created before outputs.
     *
     * @param value the value to convert to a beast value (after doing so for the inputs of its generator, recursively)
     */
    private void createBEASTValueObjects(Value<?> value) {
        // Windows issue that cannot display greek letters correctly
        if (!lPhyBeastConfig.isLogUnicode()) {
            updateIDs(value);
        }

        // do values of inputs recursively first
        Generator<?> generator = value.getGenerator();
        if (generator != null) {

            for (Object inputObject : generator.getParams().values()) {
                Value<?> input = (Value<?>) inputObject;
                createBEASTValueObjects(input);
            }
        }

        // now that the inputs are done we can do this one.
        // TODO && alignment is not observed
        if (beastObjects.get(value) == null && !skipValue(value)) {
            valueToBEAST(value);
        }

    }

    // if alignment is observed, skip valueToBEAST for simulated value in the model block,
    // so that the XML would create duplicated alignment blocks.
    private boolean skipValue(Value<?> value) {
        if (ObservationUtils.isObserved(value.getCanonicalId(), parserDictionary))
            return parserDictionary.getModelValues().contains(value);
        return false;
    }

    private void traverseBEASTGeneratorObjects(Value<?> value, boolean modifyValues, boolean createGenerators, Set<Generator> visited) {

        Generator<?> generator = value.getGenerator();
        if (generator != null) {

            for (Object inputObject : generator.getParams().values()) {
                Value<?> input = (Value<?>) inputObject;
                traverseBEASTGeneratorObjects(input, modifyValues, createGenerators, visited);
            }

            if (!visited.contains(generator)) {
                generatorToBEAST(value, generator, modifyValues, createGenerators);
                visited.add(generator);
            }
        }
    }

    /**
     * This is called after valueToBEAST has been called on both the generated value and the input values.
     * Side-effect of this method is to create an equivalent BEAST object of the generator and put it in the beastObjects map of this BEASTContext.
     *
     * @param value
     * @param generator
     */
    private void generatorToBEAST(Value value, Generator generator, boolean modifyValues, boolean createGenerators) {

        if (getBEASTObject(generator) == null) {

            BEASTInterface beastGenerator = null;

            GeneratorToBEAST toBEAST = getGeneratorToBEAST(generator);

            if (toBEAST != null) {
                BEASTInterface beastValue = beastObjects.get(value);
                // If this is a generative distribution then swap to the observed value if it exists
                if (generator instanceof GenerativeDistribution &&
                        ObservationUtils.isObserved(value.getId(), parserDictionary)) {
                    Value observedValue = getObservedValue(value.getId());
                    beastValue = getBEASTObject(observedValue);
                }

                if (beastValue == null) {
                    LoggerUtils.log.severe("Cannot find beast object given " + value);
                    return;
                }

                if (modifyValues) {
                    toBEAST.modifyBEASTValues(generator, beastValue, this);
                }
                if (createGenerators) {
                    beastGenerator = toBEAST.generatorToBEAST(generator, beastValue, this);
                }
            }

            if (createGenerators) {
                if (beastGenerator == null) {
                    if (!isExcludedGenerator(generator)) {
                        throw new UnsupportedOperationException("Unhandled generator in generatorToBEAST(): " + generator.getClass());
                    }
                } else {
                    addToContext(generator, beastGenerator);
                }
            }
        }
    }

    private boolean isExcludedGenerator(Generator generator) {
        if (LPhyBEASTExt.isExcludedGenerator(generator))
            return true;
        for (Class<? extends Generator> gCls : excludedGeneratorClasses)
            // if generator.getClass() is either the same as, or is a superclass or superinterface of, gCls.
            if (gCls.isAssignableFrom(generator.getClass()))
                return true;
        return false;
    }

    private BEASTInterface valueToBEAST(Value<?> val) {

        BEASTInterface beastValue = null;

        ValueToBEAST toBEAST = getMatchingValueToBEAST(val);

        if (toBEAST != null) {
            beastValue = toBEAST.valueToBEAST(val, this);
        }
        if (beastValue == null) {
            if (!isExcludedValue(val))
                throw new UnsupportedOperationException("Unhandled value" + (!val.isAnonymous() ? " named " + val.getId() : "") + " in valueToBEAST(): \"" +
                        val + "\" of type " + val.value().getClass());
        } else {
            // here is the common way to fill in context,
            // but there is another special method to do this
            /** {@link #putBEASTObject(GraphicalModelNode, BEASTInterface)} **/
            addToContext(val, beastValue);
        }
        return beastValue;
    }

    // handle the classes in excludedValueTypes, and also their types in an array.
    private boolean isExcludedValue(Value value) {
        if (LPhyBEASTExt.isExcludedValue(value)) // takes Value
            return true;
        Class valueType = value.getType();
        // value.value() is array
        if (valueType.isArray()) {
            Class componentClass;
            if (value.value() instanceof Object[] objects) {
                // this can be used to exclude String[][]
                if (LPhyBEASTExt.isExcludedValue(objects[0]))
                    return true;
                // Object[] can be different classes, such as TimeTreeNode[],
                // getComponentType() only returns Object.
                componentClass = objects[0].getClass();
            } else
                componentClass = valueType.getComponentType();

            for (Class vCls : excludedValueTypes) {
                // if vCls is either the same as, or is a superclass or superinterface of value.getType().
                if (vCls != null && vCls.isAssignableFrom(componentClass))
                    return true;
            }
        } else {
            for (Class vCls : excludedValueTypes) {
                // compare the wrapped value's class.
                // if vCls is either the same as, or is a superclass or superinterface of value.getType().
                if (vCls != null && vCls.isAssignableFrom(valueType))
                    return true;
            }
        }
        return false;
    }

    // fill in beastObjects, BEASTToLPHYMap, elements, and state
    private void addToContext(GraphicalModelNode node, BEASTInterface beastInterface) {
        beastObjects.put(node, beastInterface);
        BEASTToLPHYMap.put(beastInterface, node);
        elements.put(beastInterface, node);

        if (isState(node)) {
            Value var = (Value) node;

            if (lPhyBeastConfig.isObserved(var, parserDictionary)) {
                LoggerUtils.log.info("Set the variable " + var.getId() + " in LPhy to be observed.");
                return;
            }

            // var.getOutputs().size() > 0 &&  is removed from below,
            // the sink will be included in the following logic checks.
            if (beastInterface != null && !state.contains(beastInterface)) {
                if (beastInterface instanceof StateNode) {
                    // include MutableAlignment
                    state.add((StateNode) beastInterface);
                } else if (beastInterface instanceof Concatenate) {
                    Concatenate concatenate = (Concatenate) beastInterface;
                    for (Function function : concatenate.functionsInput.get()) {
                        if (function instanceof StateNode && !state.contains(function)) {
                            state.add((StateNode) function);
                        }
                    }
                } else if (beastInterface instanceof BEASTVector) {
                    for (BEASTInterface beastElement : ((BEASTVector) beastInterface).getObjectList()) {
                        // BI obj is wrapped inside BEASTVector, so check existence again
                        if (beastElement instanceof StateNode && !state.contains(beastElement)) {
                            state.add((StateNode) beastElement);
                        }
                    }
                } else if (beastInterface instanceof Slice) {
                    BEASTInterface parent = (BEASTInterface)((Slice)beastInterface).functionInput.get();
                    if (parent instanceof StateNode) {
                        if (!state.contains(parent)) {
                            state.add((StateNode) parent);
                        } else {
                            // parent already in state
                        }
                    } else {
                        throw new RuntimeException("Slice representing random value, but the sliced beast interface is not a state node!");
                    }
                } else if (beastInterface instanceof Alignment) {
                    // Do nothing here

                } else {
                    throw new RuntimeException("Unexpected beastInterface returned true for isState() but can't be added to state");
                }
            }
        }
    }

    private boolean isState(GraphicalModelNode node) {
        if (node instanceof RandomVariable) return true;
        if (node instanceof Value) {
            Value value = (Value) node;
            if (value.isRandom() && (value.getGenerator() instanceof ElementsAt)) {
                ElementsAt elementsAt = (ElementsAt) value.getGenerator();
                if (elementsAt.array() instanceof RandomVariable) {
                    BEASTInterface beastInterface = getBEASTObject(elementsAt.array());
                    if (beastInterface == null) return true;
                }
            }
        }
        return false;
    }

    //*** static methods to init BEAST 2 models ***//

    /**
     * @param freqParameter
     * @param stateNames    the names of the states in a space-delimited string
     * @return
     */
    public static Frequencies createBEASTFrequencies(RealParameter freqParameter, String stateNames) {
        Frequencies frequencies = new Frequencies();
        frequencies.setInputValue("frequencies", freqParameter);
        freqParameter.setInputValue("keys", stateNames);
        freqParameter.initAndValidate();
        frequencies.initAndValidate();
        return frequencies;
    }

    public static Prior createPrior(ParametricDistribution distr, Function function) {
        Prior prior = new Prior();
        prior.setInputValue("distr", distr);
        prior.setInputValue("x", function);
        prior.initAndValidate();
        if (function instanceof BEASTInterface) prior.setID(((BEASTInterface) function).getID() + ".prior");
        return prior;
    }

    public static double getOperatorWeight(int size, double pow) {
        return Math.pow(size, pow);
    }

    public static double getOperatorWeight(int size) {
        return getOperatorWeight(size, 0.7);
    }

    /**
     * Decide which strategy to create tree operators, given a tree.
     * @param tree   a tree
     * @return    final TreeOperatorStrategy
     */
    public TreeOperatorStrategy resolveTreeOperatorStrategy(Tree tree) {
//        TreeOperatorStrategy finalStrategy = new StandardTreeOperatorStrategy();
        TreeOperatorStrategy finalStrategy = new DefaultTreeOperatorStrategy();

        for (TreeOperatorStrategy tOS : newTreeOperatorStrategies) {
            boolean applyThis = tOS.applyStrategyToTree(tree, this);
            // TODO multiple strategies (true) ?
            if (applyThis) {
                if (! (finalStrategy instanceof DefaultTreeOperatorStrategy) )
                    throw new UnsupportedOperationException("Attempted to apply '" + tOS.getName() +
                            "' after already applying tree operator strategy '" + finalStrategy.getName() + "' !");

                finalStrategy = tOS;
                LoggerUtils.log.info("The default strategy to create tree operators is changed into '" +
                        tOS.getName() + "' ! ");
            }
        }
        return finalStrategy;
    }

    private CompoundDistribution createBEASTPosterior() {

        List<Distribution> priorList = new ArrayList<>();

        List<Distribution> likelihoodList = new ArrayList<>();

        for (Map.Entry<GraphicalModelNode<?>, BEASTInterface> entry : beastObjects.entrySet()) {
            if (entry.getValue() instanceof Distribution) {
                if ( !(entry.getKey() instanceof Generator) )
                    throw new IllegalArgumentException("Require likelihood or prior to be Generator !");

                // Now allow function in the key, e.g. GTUnphaseToBEAST
                Generator g = (Generator) entry.getKey();
                Distribution dist = (Distribution) entry.getValue();

                //This is replaced by lphy script
//                if (lPhyBeastConfig.compressAlgId != null && dist instanceof GenericTreeLikelihood treeLikelihood) {
//                    if (parser.getModelSinks().size() > 1)
//                        throw new UnsupportedOperationException("Not support multiple alignments ! " + parser.getModelSinks());
//                    Alignment newAlg = null;
//                    for (Value<?> var : parser.getModelSinks()) {
//                        if (var.getGenerator() != g ) {
//                            // get data given ID
//                            newAlg = getAlignmentFromID(lPhyBeastConfig.compressAlgId);
//                        }
//                    }
//                    if (newAlg == null)
//                        throw new IllegalArgumentException("Cannot find the alignment give ID = " +
//                                lPhyBeastConfig.compressAlgId + ", model sinks = " + parser.getModelSinks());
//                    logOrignalAlignment(treeLikelihood);
//                    // replace data in tree likelihood
//                    treeLikelihood.setInputValue("data", newAlg);
//                    treeLikelihood.setID(newAlg.getID() + ".treeLikelihood");
//                    treeLikelihood.initAndValidate();
//
//                    // add to likelihood
//                    likelihoodList.add(dist);
//
//                } else

                if (generatorOfSink(g))
                    likelihoodList.add(dist);
                else
                    priorList.add(dist);

            }
        }

        for (BEASTInterface beastInterface : elements.keySet()) {
            if (beastInterface instanceof Distribution && !likelihoodList.contains(beastInterface) && !priorList.contains(beastInterface)) {
                priorList.add((Distribution) beastInterface);
            }
        }

        System.out.println("Found " + likelihoodList.size() + " likelihoods.");
        System.out.println("Found " + priorList.size() + " priors.");

        CompoundDistribution priors = new CompoundDistribution();
        priors.setInputValue("distribution", priorList);
        priors.initAndValidate();
        priors.setID(PRIOR_ID);
        elements.put(priors, null);

        CompoundDistribution likelihoods = new CompoundDistribution();
        likelihoods.setInputValue("distribution", likelihoodList);
        likelihoods.initAndValidate();
        likelihoods.setID(LIKELIHOOD_ID);
        elements.put(likelihoods, null);

        List<Distribution> posteriorList = new ArrayList<>();
        posteriorList.add(priors);
        posteriorList.add(likelihoods);

        CompoundDistribution posterior = new CompoundDistribution();
        posterior.setInputValue("distribution", posteriorList);
        posterior.initAndValidate();
        posterior.setID(POSTERIOR_ID);
        elements.put(posterior, null);

        return posterior;
    }

    public Prior getPrior(Function param) {
        for (BEASTInterface beastInterface : elements.keySet()) {
            if (beastInterface instanceof Prior prior && prior.m_x.get().equals(param) ) {
                return prior;
            }
        }
        return null;
    }

    private void logOrignalAlignment(GenericTreeLikelihood treeLikelihood) {
        Alignment alignment = treeLikelihood.dataInput.get();
        String algXML = new XMLProducer().toXML(alignment);
        try (PrintWriter out = new PrintWriter(alignment.getID() + ".xml")) {
            out.println(algXML);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private Alignment getAlignmentFromID(String id) {
//        for (Map.Entry<GraphicalModelNode<?>, BEASTInterface> entry : beastObjects.entrySet()) {
//            if (entry.getKey().getUniqueId().equals(id)) {
//                if (entry.getValue() instanceof Alignment alignment)
//                    return alignment;
//            }
//        }
        BEASTInterface beastInterface = getBEASTObject(id);
        if (beastInterface instanceof Alignment alignment)
            return alignment;
        return null;
    }

    private boolean generatorOfSink(Generator g) {
        for (Value<?> var : parserDictionary.getDataModelSinks()) {
            if (var.getGenerator() == g) {
                return true;
            }
            if (var instanceof VectorizedRandomVariable) {
                VectorizedRandomVariable vv = (VectorizedRandomVariable) var;
                for (int i = 0; i < vv.size(); i++) {
                    RandomVariable rv = vv.getComponentValue(i);
                    if (rv.getGenerator() == g) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    protected int getAllStatesSize(List<StateNode> stateNodes) {
        int size = 0;
        for (StateNode stateNode : stateNodes) {
            if (stateNode instanceof TreeInterface)
                size += ((TreeInterface) stateNode).getInternalNodeCount();
            else
                size += stateNode.getDimension();
        }
        return size;
    }

    public void clear() {
        state.clear();
        elements.clear();
        beastObjects.clear();
        extraOperators.clear();
        skipOperators.clear();
    }

    public void runBEAST(String logFileStem) {

        MCMC mcmc = createMCMC(1000000, 1000, logFileStem, 0, false);

        try {
            mcmc.run();
        } catch (IOException | SAXException | ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    //*** add, setter, getter ***//

    public LPhyParserDictionary getParserDictionary() {
        return parserDictionary;
    }

    public LPhyBeastConfig getLPhyBeastConfig() {
        return lPhyBeastConfig;
    }

    public void addSkipOperator(StateNode stateNode) {
        skipOperators.add(stateNode);
    }

    public void addExtraOperator(Operator operator) {
        extraOperators.add(operator);
    }

    public boolean hasExtraOperator(String opID) {
        return extraOperators.stream().anyMatch(op -> op.getID().equals(opID));
    }

    public List<StateNode> getState() {
        return state;
    }

    public Multimap<BEASTInterface, GraphicalModelNode<?>> getElements() {
        return elements;
    }

    public Map<BEASTInterface, GraphicalModelNode<?>> getBEASTToLPHYMap() {
        return BEASTToLPHYMap;
    }

    public Set<StateNode> getSkipOperators() {
        return skipOperators;
    }

    public List<Operator> getExtraOperators() {
        return extraOperators;
    }

    public List<Loggable> getExtraLoggables() {
        return extraLoggables;
    }

    public List<Loggable> getSkipLoggables() {
        return skipLoggables;
    }

    public List<LoggerHelper> getExtraLoggers() {
        return extraLoggers;
    }

    /**
     * Should only be called for loggable that is based on a random variable or a deterministic function
     * of a random variable.
     * @param loggable
     */
    public void addExtraLoggable(Loggable loggable) {
        extraLoggables.add(loggable);
    }

    public void addSkipLoggable(Loggable loggable) {
        skipLoggables.add(loggable);
    }

    /**
     * {@link LoggerHelper} creates BEAST2 {@link Logger}.
     */
    public void addExtraLogger(LoggerHelper loggerHelper) {
        extraLoggers.add(loggerHelper);
    }

    public void addInit(StateNodeInitialiser beastInitializer) {
        inits.add(beastInitializer);
    }

    public Map<SequenceType, DataType> getDataTypeMap() {
        return this.dataTypeMap;
    }

    public void addTaxon(String taxonID) {
        if (!allTaxa.containsKey(taxonID)) {
            allTaxa.put(taxonID, new Taxon(taxonID));
        }
    }

    /**
     * @param id
     * @return the taxon with this id.
     */
    public Taxon getTaxon(String id) {
        addTaxon(id);
        return allTaxa.get(id);
    }

    public List<Taxon> createTaxonList(List<String> ids) {
        List<Taxon> taxonList = new ArrayList<>();
        for (String id : ids) {
            Taxon taxon = allTaxa.get(id);
            if (taxon == null) {
                addTaxon(id);
                taxonList.add(allTaxa.get(id));
            } else {
                taxonList.add(taxon);
            }
        }
        return taxonList;
    }

    public List<Value<lphy.base.evolution.alignment.Alignment>> getAlignments() {
        ArrayList<Value<lphy.base.evolution.alignment.Alignment>> alignments = new ArrayList<>();
        for (GraphicalModelNode node : beastObjects.keySet()) {
            if (node instanceof Value && node.value() instanceof lphy.base.evolution.alignment.Alignment) {
                alignments.add((Value<lphy.base.evolution.alignment.Alignment>) node);
            }
        }
        return alignments;
    }

    public Value getOutput(Generator generator) {

        final Value[] outputValue = new Value[1];
        for (Value value : parserDictionary.getDataModelSinks()) {

            ValueCreator.traverseGraphicalModel(value, new GraphicalModelNodeVisitor() {
                @Override
                public void visitValue(Value value) {
                    if (value.getGenerator() == generator) {
                        outputValue[0] = value;
                    }
                }

                @Override
                public void visitGenerator(Generator g) {

                }
            }, true);
        }
        return outputValue[0];
    }

}