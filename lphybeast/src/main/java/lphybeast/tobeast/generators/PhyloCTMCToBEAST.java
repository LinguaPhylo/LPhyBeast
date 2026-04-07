package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.core.Function;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.spec.evolution.branchratemodel.StrictClockModel;
import beast.base.spec.evolution.branchratemodel.UCRelaxedClockModel;
import beast.base.evolution.datatype.DataType;
import beast.base.evolution.datatype.UserDataType;
import beast.base.inference.Distribution;
import beast.base.spec.evolution.likelihood.ThreadedTreeLikelihood;
import beast.base.spec.evolution.sitemodel.SiteModel;
import beast.base.evolution.substitutionmodel.SubstitutionModel;
import beast.base.evolution.tree.Tree;
import beast.base.inference.StateNode;
import beast.base.inference.parameter.RealParameter;
import beastclassic.evolution.alignment.AlignmentFromTrait;
import beastclassic.evolution.likelihood.AncestralStateTreeLikelihood;
import beastclassic.evolution.substitutionmodel.SVSGeneralSubstitutionModelLogger;
import lphybeast.spi.TreeLikelihoodStrategy;
import lphy.base.distribution.DiscretizedGamma;
import lphy.base.distribution.LogNormal;
import lphy.base.distribution.UCLNMean1;
import lphy.base.evolution.likelihood.PhyloCTMC;
import lphy.base.evolution.likelihood.PhyloLikelihood;
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

import java.util.Map;

public class PhyloCTMCToBEAST implements GeneratorToBEAST<PhyloCTMC, BEASTInterface> {

    private static final String LOCATION = "location";

    public BEASTInterface generatorToBEAST(PhyloCTMC phyloCTMC, BEASTInterface value, BEASTContext context) {

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
        context.addExtraLoggable((beast.base.core.Loggable) treeLikelihood);

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


    private BEASTInterface createGenericTreeLikelihood(PhyloCTMC phyloCTMC, BEASTInterface value, BEASTContext context) {
        BEASTInterface treeLikelihood = null;

        assert value instanceof beast.base.evolution.alignment.Alignment;
        beast.base.evolution.alignment.Alignment alignment = (beast.base.evolution.alignment.Alignment)value;

        Value alignmentValue = (Value)context.getBEASTToLPHYMap().get(alignment);
        boolean isObserved = context.isObserved(alignmentValue);

        // Delegate to TreeLikelihoodStrategy (e.g., MA extension provides MATreeLikelihood)
        for (TreeLikelihoodStrategy strategy : context.getTreeLikelihoodStrategies()) {
            if (strategy.appliesTo(alignment, isObserved)) {
                treeLikelihood = strategy.createTreeLikelihood(alignment, isObserved);
                break;
            }
        }
        // Default: ThreadedTreeLikelihood for observed data
        if (treeLikelihood == null) {
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
        context.addExtraLoggable((beast.base.core.Loggable) treeLikelihood);

        return treeLikelihood;
    }


    /**
     * Create tree and clock rate inside this tree likelihood.
     * @param phyloCTMC
     * @param treeLikelihood
     * @param context
     */
    public static void constructTreeAndBranchRate(PhyloCTMC phyloCTMC, BEASTInterface treeLikelihood, BEASTContext context) {
        constructTreeAndBranchRate(phyloCTMC, treeLikelihood, context, false);
    }

    /**
     * Create tree and clock rate inside this tree likelihood.
     * @param phyloCTMC
     * @param treeLikelihood
     * @param context
     * @param skipBranchOperators skip constructing branch rates
     */
    public static void constructTreeAndBranchRate(PhyloLikelihood phyloCTMC, BEASTInterface treeLikelihood, BEASTContext context, boolean skipBranchOperators) {
        Value<TimeTree> timeTreeValue = phyloCTMC.getTree();
        Tree tree = (Tree) context.getBEASTObject(timeTreeValue);
        //tree.setInputValue("taxa", value);
        //tree.initAndValidate();

        treeLikelihood.setInputValue("tree", tree);

        Value<Number> clockRateValue = phyloCTMC.getClockRate();
        // clock.rate
        BEASTInterface clockRateParam = getClockRateParam(clockRateValue, context);
        // add updown op when estimating clock.rate
        if (timeTreeValue instanceof RandomVariable && skipBranchOperators == false) {
            if (clockRateValue instanceof RandomVariable && clockRateParam instanceof StateNode clockRate) {
                // clockRate must be state node here
                DefaultOperatorStrategy.addUpDownOperator(tree, clockRate, context);
            } else if (clockRateParam instanceof BEASTInterface bi) {
                // clockRate may be computed by an expression (e.g., ExpCalculator from feast)
                java.util.List<Function> args = context.valueHandlerExtractArguments(bi);
                if (args != null && !args.isEmpty())
                    DefaultOperatorStrategy.addUpDownOperator(tree, args, bi, context);
            }
        }

        // relaxed or local clock
        Value<Double[]> branchRates = phyloCTMC.getBranchRates();

        if (branchRates != null) {
            /**
             * 1. Use ORC package (relaxed clock) where UCLNMean1 is used in LPhy
             * 2. Keep the alternative option to use IID LogNormal on branch rates in LPhy, but XML is not recommended.
             */
            Generator generator = branchRates.getGenerator();
            // get the BEAST obj, and check its type below
            BEASTInterface beastBranchModel = context.getBEASTObject(generator);

            if (generator instanceof UCLNMean1 ucln) {

                // UCLNRelaxedClockToBEAST: the mean of log-normal distr on branch rates in real space is fixed to 1.
                UCRelaxedClockModel relaxedClockModel = (UCRelaxedClockModel) context.getBEASTObject(generator);
                treeLikelihood.setInputValue("branchRateModel", relaxedClockModel);

                if (skipBranchOperators == false) {
                    addClockOperators(tree, relaxedClockModel, context);
                }

            } else if (generator instanceof IID &&
                    ((IID<?>) generator).getBaseDistribution() instanceof LogNormal) {
                LoggerUtils.log.warning("To use ORC package, please use UCLN_Mean1 in your lphy script !");

                // simpleRelaxedClock.lphy
                UCRelaxedClockModel relaxedClockModel = new UCRelaxedClockModel();

                // Extract the base ScalarDistribution from the spec IID
                Object baseDist;
                if (beastBranchModel instanceof beast.base.spec.inference.distribution.IID<?,?,?> iid) {
                    baseDist = iid.distInput.get();
                } else {
                    throw new RuntimeException("Expected spec IID for IID(LogNormal) branch rates, got " +
                            beastBranchModel.getClass().getSimpleName());
                }

                BEASTInterface beastBranchRates = context.getBEASTObject(branchRates);

                relaxedClockModel.setInputValue("rates", beastBranchRates);
                relaxedClockModel.setInputValue("tree", tree);
                relaxedClockModel.setInputValue("distr", baseDist);
                relaxedClockModel.setID(branchRates.getCanonicalId() + ".model");
                relaxedClockModel.initAndValidate();
                treeLikelihood.setInputValue("branchRateModel", relaxedClockModel);

                if (skipBranchOperators == false) {
                    addClockOperators(tree, relaxedClockModel, context);
                }

            } else if (beastBranchModel instanceof beast.base.spec.evolution.branchratemodel.Base specBranchRateModel) {
                // this replaces generator instanceof LocalBranchRates, generator instanceof LocalClock
                treeLikelihood.setInputValue("branchRateModel", specBranchRateModel);
            } else {
                throw new UnsupportedOperationException("Only localBranchRates and lognormally distributed branchRates currently supported for LPhyBEAST !");
            }

        } else {
            StrictClockModel clockModel = new StrictClockModel();
            clockModel.setInputValue("clock.rate", clockRateParam);
            treeLikelihood.setInputValue("branchRateModel", clockModel);
        }

    }

    public static BEASTInterface getClockRateParam(Value<Number> clockRateValue, BEASTContext context) {
        if (clockRateValue != null) {
            return context.getBEASTObject(clockRateValue);
        } else {
            return new beast.base.spec.inference.parameter.RealScalarParam<>(1.0,
                    beast.base.spec.domain.PositiveReal.INSTANCE);
        }
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
            siteModel.setInputValue("shape", context.getBEASTObject(shape));
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
            // spec SiteModel expects RealScalar<PositiveReal> for mutationRate
            if (mutationRate instanceof RealParameter rp) {
                mutationRate = BEASTContext.toRealScalar(rp,
                        beast.base.spec.domain.PositiveReal.INSTANCE);
            }
            if (mutationRate != null) siteModel.setInputValue("mutationRate", mutationRate);

            siteModel.initAndValidate();
        }
        // TODO Scenario 3: using SiteModel in PhyloCTMCSiteModel
        return siteModel;
    }

    /**
     * Delegate to ClockOperatorContributor SPI implementations (e.g., ORC extension).
     * If no contributors are registered, no extra clock operators are added.
     */
    private static void addClockOperators(Tree tree, UCRelaxedClockModel relaxedClockModel, BEASTContext context) {
        for (var contributor : context.getClockOperatorContributors()) {
            for (var operator : contributor.createOperators(tree, relaxedClockModel, context)) {
                context.addExtraOperator(operator);
            }
        }
    }

    @Override
    public Class<PhyloCTMC> getGeneratorClass() {
        return PhyloCTMC.class;
    }

    @Override
    public Class<BEASTInterface> getBEASTClass() {
        return BEASTInterface.class;
    }
}