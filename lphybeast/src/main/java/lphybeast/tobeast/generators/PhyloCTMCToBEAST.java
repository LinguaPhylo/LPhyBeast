package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.evolution.branchratemodel.StrictClockModel;
import beast.base.evolution.branchratemodel.UCRelaxedClockModel;
import beast.base.evolution.datatype.DataType;
import beast.base.evolution.datatype.UserDataType;
import beast.base.evolution.likelihood.GenericTreeLikelihood;
import beast.base.evolution.likelihood.ThreadedTreeLikelihood;
import beast.base.evolution.operator.kernel.AdaptableVarianceMultivariateNormalOperator;
import beast.base.evolution.sitemodel.SiteModel;
import beast.base.evolution.substitutionmodel.Frequencies;
import beast.base.evolution.substitutionmodel.SubstitutionModel;
import beast.base.evolution.tree.Tree;
import beast.base.inference.distribution.Prior;
import beast.base.inference.operator.kernel.Transform;
import beast.base.inference.parameter.RealParameter;
import beastclassic.evolution.alignment.AlignmentFromTrait;
import beastclassic.evolution.likelihood.AncestralStateTreeLikelihood;
import beastclassic.evolution.substitutionmodel.SVSGeneralSubstitutionModelLogger;
import consoperators.BigPulley;
import consoperators.InConstantDistanceOperator;
import consoperators.SimpleDistance;
import consoperators.SmallPulley;
import lphy.base.distribution.DiscretizedGamma;
import lphy.base.distribution.LogNormal;
import lphy.base.evolution.branchrate.LocalBranchRates;
import lphy.base.evolution.likelihood.PhyloCTMC;
import lphy.base.evolution.substitutionmodel.RateMatrix;
import lphy.base.evolution.tree.TimeTree;
import lphy.core.model.Generator;
import lphy.core.model.RandomVariable;
import lphy.core.model.Value;
import lphy.core.vectorization.IID;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;
import lphybeast.tobeast.loggers.TraitTreeLogger;
import lphybeast.tobeast.operators.DefaultOperatorStrategy;

import java.util.List;
import java.util.Map;

public class PhyloCTMCToBEAST implements GeneratorToBEAST<PhyloCTMC, GenericTreeLikelihood> {

    private static final String LOCATION = "location";

    public GenericTreeLikelihood generatorToBEAST(PhyloCTMC phyloCTMC, BEASTInterface value, BEASTContext context) {

        if (value instanceof AlignmentFromTrait traitAlignment) {
            // for discrete phylogeography
            return createAncestralStateTreeLikelihood(phyloCTMC, traitAlignment, context);
        } else {
            return createThreadedTreeLikelihood(phyloCTMC, value, context);
        }

    }

    private AncestralStateTreeLikelihood createAncestralStateTreeLikelihood(PhyloCTMC phyloCTMC, AlignmentFromTrait traitAlignment, BEASTContext context) {
        AncestralStateTreeLikelihood treeLikelihood = new AncestralStateTreeLikelihood();
        treeLikelihood.setInputValue("tag", LOCATION);
        treeLikelihood.setInputValue("data", traitAlignment);
//TODO
        constructTreeAndBranchRate(phyloCTMC, treeLikelihood, null, null,
                context, false);

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


    private ThreadedTreeLikelihood createThreadedTreeLikelihood(PhyloCTMC phyloCTMC, BEASTInterface value, BEASTContext context) {
        ThreadedTreeLikelihood treeLikelihood = new ThreadedTreeLikelihood();

        assert value instanceof beast.base.evolution.alignment.Alignment;
        beast.base.evolution.alignment.Alignment alignment = (beast.base.evolution.alignment.Alignment)value;
        treeLikelihood.setInputValue("data", alignment);

        // AVMNOperator for each TreeLikelihood
        AdaptableVarianceMultivariateNormalOperator opAVMNN = DefaultOperatorStrategy.initAVMNOperator();
        opAVMNN.setID(alignment.getID() + ".AVMNOperator");

        Transform.LogConstrainedSumTransform sumTransform = DefaultOperatorStrategy.initAVMNSumTransform(alignment.getID());
        Transform.LogTransform logTransform = DefaultOperatorStrategy.initLogTransform(alignment.getID());
        Transform.NoTransform noTransform = DefaultOperatorStrategy.initNoTransform(alignment.getID());

        // branch models
        constructTreeAndBranchRate(phyloCTMC, treeLikelihood, logTransform, noTransform,
                context, false);

        SiteModel siteModel = constructSiteModel(phyloCTMC, sumTransform, logTransform, context);
        treeLikelihood.setInputValue("siteModel", siteModel);

        treeLikelihood.initAndValidate();
        treeLikelihood.setID(alignment.getID() + ".treeLikelihood");
        // logging
        context.addExtraLoggable(treeLikelihood);

        // AVMNOperator
        sumTransform.initAndValidate();
        logTransform.initAndValidate();
        noTransform.initAndValidate();

        List<Transform> transformList = List.of(sumTransform, logTransform, noTransform);
        opAVMNN.setInputValue("transformations", transformList);
        opAVMNN.initAndValidate();
        context.addBeastObjForOpSamplers(opAVMNN);

        return treeLikelihood;
    }

    /**
     * Create tree and clock rate inside this tree likelihood.
     * @param phyloCTMC
     * @param treeLikelihood
     * @param context
     * @param skipBranchOperators skip constructing branch rates
     */
    public static void constructTreeAndBranchRate(PhyloCTMC phyloCTMC, GenericTreeLikelihood treeLikelihood,
                                                  Transform.LogTransform logTransform, Transform.NoTransform noTransform,
                                                  BEASTContext context, boolean skipBranchOperators) {
        Value<TimeTree> timeTreeValue = phyloCTMC.getTree();
        Tree tree = (Tree) context.getBEASTObject(timeTreeValue);
        //tree.setInputValue("taxa", value);
        //tree.initAndValidate();

        treeLikelihood.setInputValue("tree", tree);

        Value<Double[]> branchRates = phyloCTMC.getBranchRates();

        if (branchRates != null) {

            Generator generator = branchRates.getGenerator();
            if (generator instanceof IID &&
                    ((IID<?>) generator).getBaseDistribution() instanceof LogNormal) {
//TODO migrate to new operators
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
                    addRelaxedClockOperators(tree, relaxedClockModel, beastBranchRates, context);
                }

            } else if (generator instanceof LocalBranchRates) {
                treeLikelihood.setInputValue("branchRateModel", context.getBEASTObject(generator));
            } else {
                throw new UnsupportedOperationException("Only localBranchRates and lognormally distributed branchRates currently supported for LPhyBEAST !");
            }

        } else {
            StrictClockModel clockModel = new StrictClockModel();
            Value<Number> clockRate = phyloCTMC.getClockRate();

            RealParameter clockRatePara;
            if (clockRate != null) {
                clockRatePara = context.getAsRealParameter(clockRate);

            } else {
                clockRatePara =  BEASTContext.createRealParameter(1.0);
            }
            clockModel.setInputValue("clock.rate", clockRatePara);
            treeLikelihood.setInputValue("branchRateModel", clockModel);

            if (clockRate instanceof RandomVariable && timeTreeValue instanceof RandomVariable && skipBranchOperators == false) {
                DefaultOperatorStrategy.addUpDownOperator(tree, clockRatePara, context);
                // will create AdaptableOperatorSampler later
                context.addBeastObjForOpSamplers(clockRatePara);
                // AVMN log Transform
                logTransform.setInputValue("f", clockRatePara);
            }
        }
        // AVMN No Transform
        noTransform.setInputValue("f", tree);
    }


    /**
     * @param phyloCTMC the phyloCTMC object
     * @param context the beast context
     * @return a BEAST SiteModel representing the site model of this LPHY PhyloCTMC
     */
    public static SiteModel constructSiteModel(PhyloCTMC phyloCTMC, Transform.LogConstrainedSumTransform sumTransform,
                                               Transform.LogTransform logTransform, BEASTContext context) {

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
            RealParameter shapeParam = context.getAsRealParameter(shape);
            siteModel.setInputValue("shape", shapeParam);
            // ncat is Integer, do not require to be parameter
            siteModel.setInputValue("gammaCategoryCount", ncat.value());

            //TODO add proportionInvariant

            //TODO need a better solution than rm RandomVariable siteRates
            context.removeBEASTObject(context.getBEASTObject(siteRates));

            // will create AdaptableOperatorSampler later
            context.addBeastObjForOpSamplers(shapeParam);
            // AVMN log Transform
            logTransform.setInputValue("f", shapeParam);
        }

        // Scenario 2: siteRates = NULL
        Generator qGenerator = phyloCTMC.getQValue().getGenerator();
        if (qGenerator == null || !(qGenerator instanceof RateMatrix)) {
            throw new IllegalArgumentException("BEAST2 only supports Q matrices constructed by a RateMatrix function (e.g. hky, gtr, jukeCantor et cetera).");
        } else {
            SubstitutionModel substitutionModel = (SubstitutionModel) context.getBEASTObject(qGenerator);
            if (substitutionModel == null) throw new IllegalArgumentException("Substitution Model was null!");
            siteModel.setInputValue("substModel", substitutionModel);

            if (substitutionModel instanceof SubstitutionModel.Base substBase) {

                Map<String, Input<?>> allInputs = substBase.getInputs();
                // check if any inputs of SubstitutionModel.Base have been added to create AdaptableOperatorSampler,
                // therefore no context.addBeastObjForOpSamplers here
                for (Map.Entry<String, Input<?>> entry : allInputs.entrySet()) {
                    Input<?> input = entry.getValue();
                    if (input.get() instanceof BEASTInterface beastInterface) {
                        if (context.isForOperatorSampler(beastInterface)) {
                            if (beastInterface instanceof Frequencies frequencies) {
                                Function freqParam = frequencies.frequenciesInput.get();
                                // AVMN Log Constrained Sum Transform
                                sumTransform.setInputValue("f", freqParam);
                                sumTransform.setInputValue("sum", "1.0");
                            } else
                                // AVMN log Transform
                                logTransform.setInputValue("f", beastInterface);
                        }
                    }
                }

            }

            RateMatrix rateMatrix = (RateMatrix)qGenerator;
            Value<Double> meanRate = rateMatrix.getMeanRate();
            BEASTInterface mutationRate = meanRate==null ? null : context.getBEASTObject(meanRate);
            if (mutationRate != null) {
                siteModel.setInputValue("mutationRate", mutationRate);

                // will create AdaptableOperatorSampler later
                context.addBeastObjForOpSamplers(mutationRate);
                // AVMN log Transform
                logTransform.setInputValue("f", mutationRate);
            }

            siteModel.initAndValidate();
        }
        // TODO Scenario 3: using SiteModel in PhyloCTMCSiteModel
        return siteModel;
    }

    private static void addRelaxedClockOperators(Tree tree, UCRelaxedClockModel relaxedClockModel, RealParameter rates, BEASTContext context) {

        double tWindowSize = tree.getRoot().getHeight() / 10.0;

        InConstantDistanceOperator inConstantDistanceOperator = new InConstantDistanceOperator();
        inConstantDistanceOperator.setInputValue("clockModel", relaxedClockModel);
        inConstantDistanceOperator.setInputValue("tree", tree);
        inConstantDistanceOperator.setInputValue("rates", rates);
        inConstantDistanceOperator.setInputValue("twindowSize", tWindowSize);
        inConstantDistanceOperator.setInputValue("weight", BEASTContext.getOperatorWeight(tree.getNodeCount()));
        inConstantDistanceOperator.setID(relaxedClockModel.getID() + ".inConstantDistanceOperator");
        inConstantDistanceOperator.initAndValidate();
        context.addExtraOperator(inConstantDistanceOperator);

        SimpleDistance simpleDistance = new SimpleDistance();
        simpleDistance.setInputValue("clockModel", relaxedClockModel);
        simpleDistance.setInputValue("tree", tree);
        simpleDistance.setInputValue("rates", rates);
        simpleDistance.setInputValue("twindowSize", tWindowSize);
        simpleDistance.setInputValue("weight", BEASTContext.getOperatorWeight(2));
        simpleDistance.setID(relaxedClockModel.getID() + ".simpleDistance");
        simpleDistance.initAndValidate();
        context.addExtraOperator(simpleDistance);

        BigPulley bigPulley = new BigPulley();
        bigPulley.setInputValue("tree", tree);
        bigPulley.setInputValue("rates", rates);
        bigPulley.setInputValue("twindowSize", tWindowSize);
        bigPulley.setInputValue("dwindowSize", 0.1);
        bigPulley.setInputValue("weight", BEASTContext.getOperatorWeight(2));
        bigPulley.setID(relaxedClockModel.getID() + ".bigPulley");
        bigPulley.initAndValidate();
        context.addExtraOperator(bigPulley);

        SmallPulley smallPulley = new SmallPulley();
        smallPulley.setInputValue("clockModel", relaxedClockModel);
        smallPulley.setInputValue("tree", tree);
        smallPulley.setInputValue("rates", rates);
        smallPulley.setInputValue("dwindowSize", 0.1);
        smallPulley.setInputValue("weight", BEASTContext.getOperatorWeight(2));
        smallPulley.setID(relaxedClockModel.getID() + ".smallPulley");
        smallPulley.initAndValidate();
        context.addExtraOperator(smallPulley);
    }

    @Override
    public Class<PhyloCTMC> getGeneratorClass() {
        return PhyloCTMC.class;
    }

    @Override
    public Class<GenericTreeLikelihood> getBEASTClass() {
        return GenericTreeLikelihood.class;
    }
}
