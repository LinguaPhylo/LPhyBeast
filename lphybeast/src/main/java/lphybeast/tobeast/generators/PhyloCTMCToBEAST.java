package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.core.Function;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.branchratemodel.StrictClockModel;
import beast.base.evolution.branchratemodel.UCRelaxedClockModel;
import beast.base.evolution.datatype.DataType;
import beast.base.evolution.datatype.UserDataType;
import beast.base.evolution.likelihood.GenericTreeLikelihood;
import beast.base.evolution.likelihood.ThreadedTreeLikelihood;
import beast.base.evolution.operator.AdaptableOperatorSampler;
import beast.base.evolution.operator.Exchange;
import beast.base.evolution.operator.kernel.BactrianScaleOperator;
import beast.base.evolution.sitemodel.SiteModel;
import beast.base.evolution.substitutionmodel.SubstitutionModel;
import beast.base.evolution.tree.Tree;
import beast.base.inference.StateNode;
import beast.base.inference.distribution.LogNormalDistributionModel;
import beast.base.inference.distribution.ParametricDistribution;
import beast.base.inference.distribution.Prior;
import beast.base.inference.operator.kernel.BactrianRandomWalkOperator;
import beast.base.inference.parameter.RealParameter;
import beastclassic.evolution.alignment.AlignmentFromTrait;
import beastclassic.evolution.likelihood.AncestralStateTreeLikelihood;
import beastclassic.evolution.substitutionmodel.SVSGeneralSubstitutionModelLogger;
import beastlabs.evolution.tree.RNNIMetric;
import lphy.base.distribution.DiscretizedGamma;
import lphy.base.distribution.LogNormal;
import lphy.base.distribution.UCLNMean1;
import lphy.base.evolution.branchrate.LocalBranchRates;
import lphy.base.evolution.branchrate.LocalClock;
import lphy.base.evolution.continuous.AutoCorrelatedClock;
import lphy.base.evolution.likelihood.PhyloCTMC;
import lphy.base.evolution.substitutionmodel.RateMatrix;
import lphy.base.evolution.tree.TimeTree;
import lphy.core.logger.LoggerUtils;
import lphy.core.model.Generator;
import lphy.core.model.RandomVariable;
import lphy.core.model.Value;
import lphy.core.vectorization.IID;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;
import lphybeast.tobeast.loggers.TraitTreeLogger;
import lphybeast.tobeast.operators.DefaultOperatorStrategy;
import mutablealignment.MATreeLikelihood;
import orc.consoperators.InConstantDistanceOperator;
import orc.consoperators.SimpleDistance;
import orc.consoperators.SmallPulley;
import orc.consoperators.UcldScalerOperator;
import orc.ner.NEROperator_dAE_dBE_dCE;
import orc.operators.SampleFromPriorOperator;
import rc.beast.evolution.clockmodel.AutoCorrelatedClockModel;

import java.util.Map;

public class PhyloCTMCToBEAST implements GeneratorToBEAST<PhyloCTMC, GenericTreeLikelihood> {

    private static final String LOCATION = "location";

    public GenericTreeLikelihood generatorToBEAST(PhyloCTMC phyloCTMC, BEASTInterface value, BEASTContext context) {

        if (value instanceof AlignmentFromTrait traitAlignment) {
            // for discrete phylogeography
            return createAncestralStateTreeLikelihood(phyloCTMC, traitAlignment, context);
        } else {
            return createGenericTreeLikelihood(phyloCTMC, value, context);
        }

    }

    private AncestralStateTreeLikelihood createAncestralStateTreeLikelihood(PhyloCTMC phyloCTMC, AlignmentFromTrait traitAlignment, BEASTContext context) {
        AncestralStateTreeLikelihood treeLikelihood = new AncestralStateTreeLikelihood();
        treeLikelihood.setInputValue("tag", LOCATION);
        treeLikelihood.setInputValue("data", traitAlignment);

        constructTreeAndBranchRate(phyloCTMC, treeLikelihood, context);

        DataType userDataType = traitAlignment.getDataType();
        if (! (userDataType instanceof UserDataType) )
            throw new IllegalArgumentException("Expect BEAST user defined datatype ! But find " +
                    userDataType.getTypeDescription());

        SiteModel siteModel = constructGeoSiteModel(phyloCTMC, context, (UserDataType) userDataType);
        treeLikelihood.setInputValue("siteModel", siteModel);

        treeLikelihood.initAndValidate();
        treeLikelihood.setID(traitAlignment.getID() + ".treeLikelihood");

        // <log idref="D_trait.treeLikelihood"/> in parameters
        context.addExtraLoggable(treeLikelihood);

        // Extra Logger <logger id="TreeWithTraitLogger" fileName="h5n1_with_trait.trees"
        TraitTreeLogger traitTreeLogger = new TraitTreeLogger(treeLikelihood, context);
        context.addExtraLogger(traitTreeLogger);

        return treeLikelihood;
    }

    private SiteModel constructGeoSiteModel(PhyloCTMC phyloCTMC, BEASTContext context, UserDataType userDataType) {
        SiteModel siteModel = new SiteModel();

        Value<Double[]> siteRates = phyloCTMC.getSiteRates();
        // only 1 site
        if (siteRates == null) {
            siteModel.setInputValue("gammaCategoryCount", 1);
        } else {
            throw new UnsupportedOperationException("Discrete traits will only have 1 site !");
        }

        Generator qGenerator = phyloCTMC.getQValue().getGenerator();
        if (qGenerator == null || !(qGenerator instanceof RateMatrix)) {
            throw new RuntimeException("BEAST2 only supports Q matrices constructed by a RateMatrix function.");
        } else {
            RateMatrix rateMatrix = (RateMatrix)qGenerator;

            BEASTInterface mutationRate = context.getBEASTObject(rateMatrix.getMeanRate());

            SubstitutionModel substitutionModel = (SubstitutionModel) context.getBEASTObject(qGenerator);

            if (substitutionModel == null) throw new IllegalArgumentException("Substitution Model was null!");

            siteModel.setInputValue("substModel", substitutionModel);
            if (mutationRate != null) siteModel.setInputValue("mutationRate", mutationRate);
            siteModel.initAndValidate();

            // add SVSGeneralSubstitutionModelLogger
            SVSGeneralSubstitutionModelLogger svsLogger = new SVSGeneralSubstitutionModelLogger();
            svsLogger.setInputValue("dataType", userDataType);
            svsLogger.setInputValue("model", substitutionModel);
            svsLogger.initAndValidate();

            if (svsLogger.getID() == null)
                svsLogger.setID(svsLogger.toString().substring(0, 3));

            context.addExtraLoggable(svsLogger);
        }
        siteModel.setID("geo." + siteModel.toString());
        return siteModel;
    }


    private GenericTreeLikelihood createGenericTreeLikelihood(PhyloCTMC phyloCTMC, BEASTInterface value, BEASTContext context) {
        GenericTreeLikelihood treeLikelihood = null;

        assert value instanceof beast.base.evolution.alignment.Alignment;
        beast.base.evolution.alignment.Alignment alignment = (beast.base.evolution.alignment.Alignment)value;

        Value alignmentValue = (Value)context.getBEASTToLPHYMap().get(alignment);

        if (!context.isObserved(alignmentValue)) {
            // MutableAlignment
            treeLikelihood = new MATreeLikelihood();
            treeLikelihood.setInputValue("useAmbiguities", false);

        } else {
            // normal Alignment
            treeLikelihood = new ThreadedTreeLikelihood();
            treeLikelihood.setInputValue("useAmbiguities", true);
        }



        treeLikelihood.setInputValue("data", alignment);


        constructTreeAndBranchRate(phyloCTMC, treeLikelihood, context);

        SiteModel siteModel = constructSiteModel(phyloCTMC, context);
        treeLikelihood.setInputValue("siteModel", siteModel);
        treeLikelihood.initAndValidate();
        treeLikelihood.setID(alignment.getID() + ".treeLikelihood");
        // logging
        context.addExtraLoggable(treeLikelihood);

        return treeLikelihood;
    }


    /**
     * Create tree and clock rate inside this tree likelihood.
     * @param phyloCTMC
     * @param treeLikelihood
     * @param context
     */
    public static void constructTreeAndBranchRate(PhyloCTMC phyloCTMC, GenericTreeLikelihood treeLikelihood, BEASTContext context) {
        constructTreeAndBranchRate(phyloCTMC, treeLikelihood, context, false);
    }

    /**
     * Create tree and clock rate inside this tree likelihood.
     * @param phyloCTMC
     * @param treeLikelihood
     * @param context
     * @param skipBranchOperators skip constructing branch rates
     */
    public static void constructTreeAndBranchRate(PhyloCTMC phyloCTMC, GenericTreeLikelihood treeLikelihood, BEASTContext context, boolean skipBranchOperators) {
        Value<TimeTree> timeTreeValue = phyloCTMC.getTree();
        Tree tree = (Tree) context.getBEASTObject(timeTreeValue);
        //tree.setInputValue("taxa", value);
        //tree.initAndValidate();

        treeLikelihood.setInputValue("tree", tree);

        Value<Number> clockRateValue = phyloCTMC.getClockRate();
        // clock.rate
        RealParameter clockRateParam = getClockRateParam(clockRateValue, context);
        // updown op when estimating clock.rate
        if (clockRateValue instanceof RandomVariable && timeTreeValue instanceof RandomVariable && skipBranchOperators == false) {
            DefaultOperatorStrategy.addUpDownOperator(tree, clockRateParam, context);
        }

        // relaxed or local clock
        Value<Double[]> branchRates = phyloCTMC.getBranchRates();

        if (branchRates != null) {
            /**
             * 1. Use ORC package (relaxed clock) where UCLNMean1 is used in LPhy
             * 2. Keep the alternative option to use IID LogNormal on branch rates in LPhy, but XML is not recommended.
             */
            Generator generator = branchRates.getGenerator();
            if (generator instanceof UCLNMean1 ucln) {

                // UCLNRelaxedClockToBEAST: the mean of log-normal distr on branch rates in real space is fixed to 1.
                UCRelaxedClockModel relaxedClockModel = (UCRelaxedClockModel) context.getBEASTObject(generator);
                treeLikelihood.setInputValue("branchRateModel", relaxedClockModel);

                if (skipBranchOperators == false) {
                    addORCOperators(tree, relaxedClockModel, context);
                }

            } else if (generator instanceof IID &&
                    ((IID<?>) generator).getBaseDistribution() instanceof LogNormal) {
                LoggerUtils.log.warning("To use ORC package, please use UCLN_Mean1 in your lphy script !");

                // simpleRelaxedClock.lphy
                UCRelaxedClockModel relaxedClockModel = new UCRelaxedClockModel();

                Prior logNormalPrior = (Prior) context.getBEASTObject(generator);

                RealParameter beastBranchRates = context.getAsRealParameter(branchRates);

                relaxedClockModel.setInputValue("rates", beastBranchRates);
                relaxedClockModel.setInputValue("tree", tree);
                relaxedClockModel.setInputValue("distr", logNormalPrior.distInput.get());
                relaxedClockModel.setID(branchRates.getCanonicalId() + ".model");
                relaxedClockModel.initAndValidate();
                treeLikelihood.setInputValue("branchRateModel", relaxedClockModel);

                if (skipBranchOperators == false) {
                    addORCOperators(tree, relaxedClockModel, context);
                }

            } else if (generator instanceof LocalBranchRates) {
                treeLikelihood.setInputValue("branchRateModel", context.getBEASTObject(generator));
            } else if (generator instanceof LocalClock) {
                treeLikelihood.setInputValue("branchRateModel", context.getBEASTObject(generator));
            } else if (generator instanceof AutoCorrelatedClock) {
                AutoCorrelatedClockModel acModel = (AutoCorrelatedClockModel) context.getBEASTObject(generator);
                treeLikelihood.setInputValue("branchRateModel", acModel);
            } else {
                throw new UnsupportedOperationException("Only localBranchRates and lognormally distributed branchRates currently supported for LPhyBEAST !");
            }

        } else {
            StrictClockModel clockModel = new StrictClockModel();
            clockModel.setInputValue("clock.rate", clockRateParam);
            treeLikelihood.setInputValue("branchRateModel", clockModel);
        }

    }

    private static RealParameter getClockRateParam(Value<Number> clockRateValue, BEASTContext context) {
        RealParameter clockRateParam;
        if (clockRateValue != null) {
            clockRateParam = context.getAsRealParameter(clockRateValue);

        } else {
            clockRateParam =  BEASTContext.createRealParameter(1.0);
        }
        return clockRateParam;
    }

    /**
     * @param phyloCTMC the phyloCTMC object
     * @param context the beast context
     * @return a BEAST SiteModel representing the site model of this LPHY PhyloCTMC
     */
    public static SiteModel constructSiteModel(PhyloCTMC phyloCTMC, BEASTContext context) {

        SiteModel siteModel = new SiteModel();

        Value<Double[]> siteRates = phyloCTMC.getSiteRates();

        // Scenario 1: using siteRates
        if (siteRates != null) {
            Generator generator = siteRates.getGenerator();

            Value shape;
            Value ncat;
            if (generator instanceof DiscretizedGamma discretizedGamma) {
                shape = discretizedGamma.getShape();
                ncat = discretizedGamma.getNcat();
            } else if (generator instanceof IID iid) {
                if (iid.getBaseDistribution() instanceof DiscretizedGamma) {
                    Map<String, Value> params = iid.getParams();
                    shape = params.get("shape");
                    ncat = params.get("ncat");
                } else
                    throw new UnsupportedOperationException("Only discretized gamma site rates are supported by LPhyBEAST !");
            } else {
                throw new UnsupportedOperationException("Only discretized gamma site rates are supported by LPhyBEAST !");
            }
            siteModel.setInputValue("shape", context.getAsRealParameter(shape));
            siteModel.setInputValue("gammaCategoryCount", ncat.value());

            //TODO need a better solution than rm RandomVariable siteRates
            context.removeBEASTObject(context.getBEASTObject(siteRates));
        }

        // Scenario 2: siteRates = NULL
        Generator qGenerator = phyloCTMC.getQValue().getGenerator();
        if (qGenerator == null || !(qGenerator instanceof RateMatrix)) {
            throw new IllegalArgumentException("BEAST2 only supports Q matrices constructed by a RateMatrix function (e.g. hky, gtr, jukeCantor et cetera).");
        } else {
            SubstitutionModel substitutionModel = (SubstitutionModel) context.getBEASTObject(qGenerator);
            if (substitutionModel == null) throw new IllegalArgumentException("Substitution Model was null!");
            siteModel.setInputValue("substModel", substitutionModel);

            RateMatrix rateMatrix = (RateMatrix)qGenerator;
            Value<Double> meanRate = rateMatrix.getMeanRate();
            BEASTInterface mutationRate = meanRate==null ? null : context.getBEASTObject(meanRate);
            if (mutationRate != null) siteModel.setInputValue("mutationRate", mutationRate);

            siteModel.initAndValidate();
        }
        // TODO Scenario 3: using SiteModel in PhyloCTMCSiteModel
        return siteModel;
    }

    /**
     * Assume ORCRates and ORCsigma are StateNode.
     * TODO uclnMean operator ?
     */
    private static void addORCOperators(Tree tree, UCRelaxedClockModel relaxedClockModel, BEASTContext context) {
        // assume rates to Tree is 1 to 1 mapping, when setting ID
        RealParameter rates = relaxedClockModel.rateInput.get();

        ParametricDistribution distr = relaxedClockModel.getDistribution();
        // get ORCsigma from LogNormal
        Function orcSigma;
        if (distr instanceof LogNormalDistributionModel logNDistr) {
            orcSigma = logNDistr.SParameterInput.get();
        } else throw new UnsupportedOperationException("LPhyBeast only supports LogNormal distribution for relaxed model in ORC !");

        /**
         * Skip ORCRates and ORCsigma operators created from the default.
         * All operators insider AdaptableOperatorSampler use weight 1.0
         */
        context.addSkipOperator(rates);

        Prior sigmaPrior = context.getPrior(orcSigma);
        // if ORCsigma is estimated, then add operators
        if (orcSigma instanceof StateNode stateNode && sigmaPrior != null) {
            context.addSkipOperator(stateNode);

            // 1.1 ORCucldStdevScaler
            UcldScalerOperator ucldScalerOperator = new UcldScalerOperator();
            ucldScalerOperator.initByName("rates", rates, "distr", distr, "stdev", orcSigma,
                    "weight", 1.0, "scaleFactor", 0.5);
            ucldScalerOperator.setID("ORCucldStdevScaler." + stateNode.getID());

            // 1.2 ORCUcldStdevRandomWalk
            BactrianRandomWalkOperator randomWalkOperator = new BactrianRandomWalkOperator();
            randomWalkOperator.initByName("parameter", orcSigma,
                    "weight", 1.0, "scaleFactor", 0.1);
            randomWalkOperator.setID("ORCUcldStdevRandomWalk." + stateNode.getID());

            // 1.3 ORCUcldStdevScale
            BactrianScaleOperator scaleOperator = new BactrianScaleOperator();
            scaleOperator.initByName("parameter", orcSigma, "upper", 10.0,  // TODO why 10.0?
                    "weight", 1.0, "scaleFactor", 0.5);
            scaleOperator.setID("ORCUcldStdevScale." + stateNode.getID());

            // 1.4 ORCSampleFromPriorOperator_sigma
            SampleFromPriorOperator sampleFromPriorOperator = new SampleFromPriorOperator();
            sampleFromPriorOperator.initByName("parameter", orcSigma, "prior2", sigmaPrior, "weight", 1.0);
            sampleFromPriorOperator.setID("ORCSampleFromPriorOperator." + stateNode.getID());

            // 1. ORCsigma
            AdaptableOperatorSampler sigmaAOSampler = new AdaptableOperatorSampler();
            // weight="3.0"
            sigmaAOSampler.initByName("weight", BEASTContext.getOperatorWeight(tree.getNodeCount()) / 5,
                    // <parameter idref="ORCsigma"/>
                    "parameter", orcSigma, "operator", ucldScalerOperator, "operator", randomWalkOperator,
                    "operator", scaleOperator, "operator", sampleFromPriorOperator);
            sigmaAOSampler.setID("ORCAdaptableOperatorSampler." + stateNode.getID());

            context.addExtraOperator(sigmaAOSampler);
        }

        // 2. rates_root
        SimpleDistance simpleDistance = getSimpleDistance(tree, relaxedClockModel, rates);
        SmallPulley smallPulley = getSmallPulley(tree, relaxedClockModel, rates);

        AdaptableOperatorSampler ratesRootAOSampler = new AdaptableOperatorSampler();
        // <parameter idref="ORCRates"/>   <tree idref="Tree"/>
        ratesRootAOSampler.initByName("weight", 1.0, "parameter", rates, "tree", tree,
                "operator", simpleDistance, "operator", smallPulley);
        ratesRootAOSampler.setID("ORCAdaptableOperatorSampler.ratesRoot." + rates.getID());

        context.addExtraOperator(ratesRootAOSampler);

        // 3. rates_internal
        InConstantDistanceOperator inConstantDistanceOperator =
                getInConstantDistanceOperator(tree, relaxedClockModel, rates);

        BactrianRandomWalkOperator randomWalkOperator2 = new BactrianRandomWalkOperator();
        randomWalkOperator2.initByName("parameter", rates,
                "weight", 1.0, "scaleFactor", 0.1);
        randomWalkOperator2.setID("ORCUcldStdevRandomWalk." + rates.getID());

        // 1.3 ORCUcldStdevScale
        BactrianScaleOperator scaleOperator2 = new BactrianScaleOperator();
        scaleOperator2.initByName("parameter", rates, "upper", 10.0,  // TODO why 10.0?
                "weight", 1.0, "scaleFactor", 0.5);
        scaleOperator2.setID("ORCUcldStdevScale." + rates.getID());

        Prior ratesPrior = context.getPrior(rates);
        if (ratesPrior == null) throw new IllegalArgumentException("Cannot find BEAST prior of " + rates + " !");

        // 1.4 ORCSampleFromPriorOperator_rates
        SampleFromPriorOperator sampleFromPriorOperator2 = new SampleFromPriorOperator();
        sampleFromPriorOperator2.initByName("parameter", rates, "prior2", ratesPrior, "weight", 1.0);
        sampleFromPriorOperator2.setID("ORCSampleFromPriorOperator." + rates.getID());

        AdaptableOperatorSampler ratesInternalAOSampler = new AdaptableOperatorSampler();
        // weight="20.0"
        ratesInternalAOSampler.initByName("weight", BEASTContext.getOperatorWeight(tree.getNodeCount()),
                // <parameter idref="ORCRates"/> <tree idref="Tree"/>
                "parameter", rates, "tree", tree, "operator", inConstantDistanceOperator,
                "operator", randomWalkOperator2, "operator", scaleOperator2, "operator", sampleFromPriorOperator2);
        ratesInternalAOSampler.setID("ORCAdaptableOperatorSampler.ratesInternal." + rates.getID());

        context.addExtraOperator(ratesInternalAOSampler);

        // 4. NER
        // ORCNER_Exchange
        Exchange exchange = new Exchange();
        exchange.initByName("tree", tree, "weight", 1.0);
        exchange.setID("ORCNER_Exchange.NER." + tree.getID());

        //TODO disable <operator id="YuleModelNarrow" spec="Exchange" tree="@Tree" weight="0.0"/>

        // ORCNER_dAE_dBE_dCE
        NEROperator_dAE_dBE_dCE nerOperator = new NEROperator_dAE_dBE_dCE();
        nerOperator.initByName("rates", rates, "tree", tree, "weight", 1.0);
        nerOperator.setID("NEROperator.NER." + tree.getID());
        // RNNIMetric taxonset="@TaxonSet"
        RNNIMetric rnniMetric = new RNNIMetric();
        TaxonSet taxonSet = tree.getTaxonset();
        rnniMetric.initByName("taxonset", taxonSet);
        nerOperator.setID("RNNIMetric.NER." + tree.getID());

        AdaptableOperatorSampler nerAOSampler = new AdaptableOperatorSampler();
        // weight="10.0"
        nerAOSampler.initByName("weight", BEASTContext.getOperatorWeight(tree.getNodeCount()) / 2,
                // <tree idref="Tree"/>
                "tree", tree, "operator", exchange, "operator", nerOperator, "metric", rnniMetric);
        nerAOSampler.setID("ORCAdaptableOperatorSampler.NER." + rates.getID());

        context.addExtraOperator(nerAOSampler);

    }

    private static InConstantDistanceOperator getInConstantDistanceOperator(Tree tree, UCRelaxedClockModel relaxedClockModel, RealParameter rates) {
        double tWindowSize = tree.getRoot().getHeight() / 10.0;

        InConstantDistanceOperator inConstDistOperator = new InConstantDistanceOperator();
        inConstDistOperator.initByName("clockModel", relaxedClockModel, "tree", tree, "rates", rates,
                "twindowSize", tWindowSize, "weight", 1.0);
        //        inConstantDistanceOperator.setInputValue("weight", BEASTContext.getOperatorWeight(tree.getNodeCount()));
        inConstDistOperator.setID(relaxedClockModel.getID() + ".inConstantDistanceOperator");
        return inConstDistOperator;
    }

    private static SmallPulley getSmallPulley(Tree tree, UCRelaxedClockModel relaxedClockModel, RealParameter rates) {
        SmallPulley smallPulley = new SmallPulley();
        smallPulley.initByName("clockModel", relaxedClockModel, "tree", tree, "rates", rates,
                "dwindowSize", 0.1, "weight", 1.0);
        //        smallPulley.setInputValue("weight", BEASTContext.getOperatorWeight(2));
        smallPulley.setID(relaxedClockModel.getID() + ".smallPulley");
        smallPulley.initAndValidate();
        return smallPulley;
    }

    private static SimpleDistance getSimpleDistance(Tree tree, UCRelaxedClockModel relaxedClockModel, RealParameter rates) {
        double tWindowSize = tree.getRoot().getHeight() / 10.0;

        SimpleDistance simpleDistance = new SimpleDistance();
        simpleDistance.initByName("clockModel", relaxedClockModel, "tree", tree, "rates", rates,
                "twindowSize", tWindowSize, "weight", 1.0);
        //        simpleDistance.setInputValue("weight", BEASTContext.getOperatorWeight(2));
        simpleDistance.setID(relaxedClockModel.getID() + ".simpleDistance");
        simpleDistance.initAndValidate();
        return simpleDistance;
    }


    /**
     * @deprecated this will be replaced by ORC soon
     */
    @Deprecated
    //    private static void addRelaxedClockOperators(Tree tree, UCRelaxedClockModel relaxedClockModel, BEASTContext context) {
    //
    //        RealParameter rates = relaxedClockModel.rateInput.get();
    //
    //        double tWindowSize = tree.getRoot().getHeight() / 10.0;
    //
    //        InConstantDistanceOperator inConstantDistanceOperator = new InConstantDistanceOperator();
    //        inConstantDistanceOperator.setInputValue("clockModel", relaxedClockModel);
    //        inConstantDistanceOperator.setInputValue("tree", tree);
    //        inConstantDistanceOperator.setInputValue("rates", rates);
    //        inConstantDistanceOperator.setInputValue("twindowSize", tWindowSize);
    //        inConstantDistanceOperator.setInputValue("weight", BEASTContext.getOperatorWeight(tree.getNodeCount()));
    //        inConstantDistanceOperator.setID(relaxedClockModel.getID() + ".inConstantDistanceOperator");
    //        inConstantDistanceOperator.initAndValidate();
    //        context.addExtraOperator(inConstantDistanceOperator);
    //
    //        SimpleDistance simpleDistance = new SimpleDistance();
    //        simpleDistance.setInputValue("clockModel", relaxedClockModel);
    //        simpleDistance.setInputValue("tree", tree);
    //        simpleDistance.setInputValue("rates", rates);
    //        simpleDistance.setInputValue("twindowSize", tWindowSize);
    //        simpleDistance.setInputValue("weight", BEASTContext.getOperatorWeight(2));
    //        simpleDistance.setID(relaxedClockModel.getID() + ".simpleDistance");
    //        simpleDistance.initAndValidate();
    //        context.addExtraOperator(simpleDistance);
    //
    //        BigPulley bigPulley = new BigPulley();
    //        bigPulley.setInputValue("tree", tree);
    //        bigPulley.setInputValue("rates", rates);
    //        bigPulley.setInputValue("twindowSize", tWindowSize);
    //        bigPulley.setInputValue("dwindowSize", 0.1);
    //        bigPulley.setInputValue("weight", BEASTContext.getOperatorWeight(2));
    //        bigPulley.setID(relaxedClockModel.getID() + ".bigPulley");
    //        bigPulley.initAndValidate();
    //        context.addExtraOperator(bigPulley);
    //
    //        SmallPulley smallPulley = new SmallPulley();
    //        smallPulley.setInputValue("clockModel", relaxedClockModel);
    //        smallPulley.setInputValue("tree", tree);
    //        smallPulley.setInputValue("rates", rates);
    //        smallPulley.setInputValue("dwindowSize", 0.1);
    //        smallPulley.setInputValue("weight", BEASTContext.getOperatorWeight(2));
    //        smallPulley.setID(relaxedClockModel.getID() + ".smallPulley");
    //        smallPulley.initAndValidate();
    //        context.addExtraOperator(smallPulley);
    //    }

    @Override
    public Class<PhyloCTMC> getGeneratorClass() {
        return PhyloCTMC.class;
    }

    @Override
    public Class<GenericTreeLikelihood> getBEASTClass() {
        return GenericTreeLikelihood.class;
    }
}