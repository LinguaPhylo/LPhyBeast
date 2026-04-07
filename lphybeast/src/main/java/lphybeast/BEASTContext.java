package lphybeast;

import beast.base.core.BEASTInterface;
import beast.base.core.Function;
import beast.base.core.Loggable;
import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.alignment.Taxon;
import beast.base.evolution.datatype.DataType;
import beast.base.spec.evolution.likelihood.GenericTreeLikelihood;

import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeInterface;
import beast.base.inference.*;
import beast.base.parser.XMLProducer;
import beast.base.spec.inference.parameter.VectorElement;
import beast.base.spec.type.RealVector;
import beastlabs.util.BEASTVector;
import lphybeast.tobeast.VectorSlice;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
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
import lphybeast.spi.*;
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
    // cached loader for SPI access
    private LPhyBEASTLoader loader;

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
        this.loader = loader;
        this.lPhyBeastConfig = lPhyBeastConfig;

        valueToBEASTList = loader.valueToBEASTList;
        generatorToBEASTMap = loader.generatorToBEASTMap;
        dataTypeMap = loader.dataTypeMap;

        excludedGeneratorClasses = loader.excludedGeneratorClasses;
        excludedValueTypes = loader.excludedValueTypes;

        newTreeOperatorStrategies = loader.newTreeOperatorStrategies;
    }

    //*** SPI accessors ***//

    public List<ValueHandler> getValueHandlers() {
        return loader.valueHandlers;
    }

    public List<TreeLikelihoodStrategy> getTreeLikelihoodStrategies() {
        return loader.treeLikelihoodStrategies;
    }

    public List<OperatorContributor> getOperatorContributors() {
        return loader.operatorContributors;
    }

    public List<ClockOperatorContributor> getClockOperatorContributors() {
        return loader.clockOperatorContributors;
    }

    public List<AlignmentHandler> getAlignmentHandlers() {
        return loader.alignmentHandlers;
    }

    /**
     * Resolve the MCMCStrategy to use. If MC3 is requested, find an MC3 strategy
     * from extensions. Otherwise use the default.
     */
    private MCMCStrategy resolveMCMCStrategy() {
        if (lPhyBeastConfig.isUseMC3()) {
            for (MCMCStrategy strategy : loader.mcmcStrategies) {
                if (strategy.isMC3()) return strategy;
            }
            throw new UnsupportedOperationException(
                    "MC3 (Coupled MCMC) requested but no MC3 strategy found. " +
                    "Install the lphybeast-mc3 extension package.");
        }
        // Return first non-MC3 strategy, or the default
        for (MCMCStrategy strategy : loader.mcmcStrategies) {
            if (!strategy.isMC3()) return strategy;
        }
        return new DefaultMCMCStrategy();
    }

    /**
     * Ask registered ValueHandlers if beastInterface is a function expression.
     * @return the Function, or null if no handler recognizes it
     */
    public Function valueHandlerAsFunction(BEASTInterface beastInterface) {
        for (ValueHandler handler : getValueHandlers()) {
            Function f = handler.asFunction(beastInterface);
            if (f != null) return f;
        }
        return null;
    }

    /**
     * Ask registered ValueHandlers to extract parts from a compound value.
     * @return the parts, or null if no handler recognizes it
     */
    public List<Function> valueHandlerExtractParts(BEASTInterface beastInterface) {
        for (ValueHandler handler : getValueHandlers()) {
            List<Function> parts = handler.extractParts(beastInterface);
            if (parts != null) return parts;
        }
        return null;
    }

    /**
     * Ask registered ValueHandlers to extract state nodes from a compound value.
     * @return the state nodes, or null if no handler recognizes it
     */
    public List<StateNode> valueHandlerExtractStateNodes(BEASTInterface beastInterface) {
        for (ValueHandler handler : getValueHandlers()) {
            List<StateNode> nodes = handler.extractStateNodes(beastInterface);
            if (nodes != null) return nodes;
        }
        return null;
    }

    /**
     * Ask registered ValueHandlers to extract function arguments from an expression.
     * @return the arguments, or null if no handler recognizes it
     */
    public List<Function> valueHandlerExtractArguments(BEASTInterface beastInterface) {
        for (ValueHandler handler : getValueHandlers()) {
            List<Function> args = handler.extractArguments(beastInterface);
            if (args != null) return args;
        }
        return null;
    }

    /**
     * Main method to process configurations to create BEAST 2 XML from LPhy objects.
     * Uses {@link MCMCStrategy} to create either standard MCMC or MC3 run element.
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
     * Returns the spec RealScalar for this value, coercing from IntScalarParam if needed.
     */
    public beast.base.spec.type.RealScalar<?> getAsRealScalar(Value value) {
        Object obj = beastObjects.get(value);
        if (obj instanceof beast.base.spec.type.RealScalar<?> rs) return rs;
        if (obj instanceof beast.base.spec.inference.parameter.IntScalarParam<?> isp) {
            var scalar = new beast.base.spec.inference.parameter.RealScalarParam<>(
                    (double) isp.get(), beast.base.spec.domain.Real.INSTANCE);
            scalar.setID(isp.getID());
            removeBEASTObject((BEASTInterface) isp);
            addToContext(value, scalar);
            return scalar;
        }
        throw new RuntimeException("No coercible RealScalar found for " + value + " (got " + obj.getClass().getSimpleName() + ")");
    }

    /**
     * Returns a spec RealVector for this value, coercing from IntVectorParam if needed.
     */
    public beast.base.spec.type.RealVector<?> getAsRealVector(Value value) {
        Object obj = beastObjects.get(value);
        if (obj instanceof beast.base.spec.type.RealVector<?> rv) return rv;
        if (obj instanceof beast.base.spec.inference.parameter.IntVectorParam ivp) {
            double[] values = new double[ivp.size()];
            for (int i = 0; i < values.length; i++) values[i] = ivp.get(i);
            var vec = new beast.base.spec.inference.parameter.RealVectorParam<>(values, beast.base.spec.domain.Real.INSTANCE);
            vec.setID(ivp.getID());
            vec.setInputValue("estimate", false);
            removeBEASTObject(ivp);
            addToContext(value, vec);
            return vec;
        }
        throw new RuntimeException("No coercible RealVector found for " + value + " (got " + obj.getClass().getSimpleName() + ")");
    }

    //*** handle BEAST 2 objects ***//

    /**
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
                VectorElement<?> element = createVectorElementFromVector(node, parts[0], index);
                beastObjects.put(node, element);
                return element;
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

    public VectorElement<?> createVectorElementFromVector(GraphicalModelNode node, String id, int index) {

        BEASTInterface parentNode = getBEASTObject(Symbols.getCanonical(id));

        VectorElement<?> element = new VectorElement<>((RealVector) parentNode, index);
        element.setID(Symbols.getCanonical(id) + VectorUtils.INDEX_SEPARATOR + index);
        addToContext(node, element);
        return element;

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
            String id = lPhyBeastConfig.isLogUnicode() ? sliceValue.getId() : sliceValue.getCanonicalId();
            VectorElement<?> element = new VectorElement<>((RealVector) slicedBEASTValue, sliceValue.getIndex());
            element.setID(id);
            addToContext(sliceValue, element);
            return element;
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
     * Create the MCMC run element using the resolved {@link MCMCStrategy}.
     */
    private MCMC createMCMC(long chainLength, long logEvery, String logFileStem, int preBurnin, boolean sampleFromPrior) {

        CompoundDistribution posterior = createBEASTPosterior();

        // create operators
        OperatorStrategy operatorStrategy = new DefaultOperatorStrategy(this);
        List<Operator> operators = operatorStrategy.createOperators();

        // create loggers
        topDist = createTopCompoundDist();
        LoggerFactory loggerFactory = new LoggerFactory(this, topDist);
        List<Logger> loggers = loggerFactory.createLoggers(logEvery, logFileStem);

        // create state
        State beastState = new State();
        beastState.setInputValue("stateNode", this.state);
        beastState.initAndValidate();
        elements.put(beastState, null);

        // delegate to strategy
        MCMCStrategy strategy = resolveMCMCStrategy();
        return strategy.createRun(posterior, operators, loggers, beastState, inits,
                chainLength, preBurnin, sampleFromPrior, lPhyBeastConfig);
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
                        throw new UnsupportedOperationException(buildMissingMappingMessage(
                                "generator", null, generator.getClass(), null));
                    }
                } else {
                    addToContext(generator, beastGenerator);
                }
            }
        }
    }

    private boolean isExcludedGenerator(Generator generator) {
        if (LPhyBEASTMapping.isExcludedGenerator(generator))
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
            if (!isExcludedValue(val)) {
                throw new UnsupportedOperationException(buildMissingMappingMessage(
                        "value", val.isAnonymous() ? null : val.getId(),
                        val.value().getClass(), val.getGenerator()));
            }
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
        if (LPhyBEASTMapping.isExcludedValue(value)) // takes Value
            return true;
        Class valueType = value.getType();
        // value.value() is array
        if (valueType.isArray()) {
            Class componentClass;
            if (value.value() instanceof Object[] objects) {
                // this can be used to exclude String[][]
                if (LPhyBEASTMapping.isExcludedValue(objects[0]))
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
                } else if (beastInterface instanceof BEASTVector) {
                    for (BEASTInterface beastElement : ((BEASTVector) beastInterface).getObjectList()) {
                        // BI obj is wrapped inside BEASTVector, so check existence again
                        if (beastElement instanceof StateNode && !state.contains(beastElement)) {
                            state.add((StateNode) beastElement);
                        }
                    }
                } else if (beastInterface instanceof VectorElement<?> ve) {
                    BEASTInterface parent = (BEASTInterface) ve.vectorInput.get();
                    if (parent instanceof StateNode sn) {
                        if (!state.contains(sn)) state.add(sn);
                    } else {
                        throw new RuntimeException("VectorElement representing random value, but the vector is not a state node!");
                    }
                } else if (beastInterface instanceof VectorSlice<?> vs) {
                    BEASTInterface parent = (BEASTInterface) vs.vectorInput.get();
                    if (parent instanceof StateNode sn) {
                        if (!state.contains(sn)) state.add(sn);
                    } else {
                        throw new RuntimeException("VectorSlice representing random value, but the vector is not a state node!");
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
            else if (stateNode instanceof Function)
                size += ((Function) stateNode).getDimension();
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

    /**
     * Build a diagnostic message for a missing generator or value mapping,
     * listing which extensions are loaded and what generators they provide.
     *
     * @param kind      "value" or "generator"
     * @param name      the variable name (nullable)
     * @param cls       the class of the value or generator
     * @param generator the generator that produced the value (nullable, only for values)
     */
    private String buildMissingMappingMessage(String kind, String name, Class<?> cls,
                                              lphy.core.model.Generator generator) {
        StringBuilder sb = new StringBuilder();
        sb.append("Unhandled ").append(kind);
        if (name != null) sb.append(" '").append(name).append("'");
        sb.append(" of type ").append(cls.getSimpleName());

        if (generator != null) {
            sb.append("\n  Generated by: ").append(generator.getClass().getSimpleName());
        }

        // Show what's loaded
        if (loader != null) {
            var summary = loader.getExtensionGeneratorSummary();
            if (!summary.isEmpty()) {
                sb.append("\n  Loaded extensions and their generators:");
                for (var entry : summary.entrySet()) {
                    // Show short extension name (last part of class name)
                    String extShort = entry.getKey();
                    int dot = extShort.lastIndexOf('.');
                    if (dot >= 0) extShort = extShort.substring(dot + 1);
                    sb.append("\n    ").append(extShort).append(": ")
                            .append(String.join(", ", entry.getValue()));
                }
            }

            // Check if the generator class has a loaded mapping
            Class<?> genClass = generator != null ? generator.getClass() : cls;
            String source = loader.getGeneratorSource(genClass);
            if (source != null) {
                sb.append("\n  Note: ").append(genClass.getSimpleName())
                        .append(" has a GeneratorToBEAST in ").append(source)
                        .append(" but no ValueToBEAST matched the output value.");
            } else if (generator != null) {
                sb.append("\n  No loaded extension provides a mapping for ")
                        .append(genClass.getSimpleName()).append(".");
                sb.append("\n  Ensure the required extension is loaded via -vf <extension>/version.xml");
            }
        }

        return sb.toString();
    }

}