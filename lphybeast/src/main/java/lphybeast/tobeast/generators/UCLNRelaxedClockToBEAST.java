package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.evolution.RateStatistic;
import beast.base.evolution.tree.TreeInterface;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.evolution.branchratemodel.UCRelaxedClockModel;
import beast.base.spec.inference.distribution.LogNormal;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.type.RealScalar;
import beast.base.spec.type.RealVector;
import lphy.base.distribution.UCLNMean1;
import lphy.base.evolution.likelihood.PhyloCTMC;
import lphy.base.evolution.tree.TimeTree;
import lphy.core.model.GraphicalModelNode;
import lphy.core.model.Value;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;
import lphybeast.tobeast.LoggerUtils;
import lphybeast.tobeast.loggers.MetaDataTreeLogger;

public class UCLNRelaxedClockToBEAST implements GeneratorToBEAST<UCLNMean1, UCRelaxedClockModel> {
    @Override
    public UCRelaxedClockModel generatorToBEAST(UCLNMean1 ucln, BEASTInterface value, BEASTContext context) {

        UCRelaxedClockModel ucRelaxedClockModel = new UCRelaxedClockModel();

        Value<TimeTree> tree = ucln.getTree();
        TreeInterface beastTree = (TreeInterface) context.getBEASTObject(tree);
        ucRelaxedClockModel.setInputValue("tree", beastTree);

        // set clock.rate to UclnMean
        for (GraphicalModelNode treeOut : tree.getOutputs()) {
            if (treeOut instanceof PhyloCTMC phyloCTMC) {
                Value mu = phyloCTMC.getClockRate();
                if (mu != null)
                    ucRelaxedClockModel.setInputValue("clock.rate", context.getBEASTObject(mu));
            }
        }

        GraphicalModelNode branchRates = context.getGraphicalModelNode(value);
        if (value instanceof RealVector<?> rates) {
            ucRelaxedClockModel.setInputValue("rates", rates);

            // spec LogNormal: mean fixed to 1 in real space
            RealScalarParam<Real> M = new RealScalarParam<>(1.0, Real.INSTANCE);
            RealScalar<PositiveReal> S = (RealScalar<PositiveReal>) context.getBEASTObject(ucln.getUclnSigma());

            LogNormal logNormDist = new LogNormal();
            logNormDist.initByName("M", M, "meanInRealSpace", true, "S", S);
            logNormDist.setID("LogNormalDistr." + branchRates.getUniqueId());
            ucRelaxedClockModel.setInputValue("distr", logNormDist);

            // rm rates from log
            if (rates instanceof beast.base.core.Loggable loggable)
                context.addSkipLoggable(loggable);
            // replaced by <log id="" ... branchratemodel="@" tree="@"/>
            RateStatistic rateStatistic = LoggerUtils.createRateStatistic("RatesStat." + branchRates.getUniqueId(), ucRelaxedClockModel, beastTree);
            context.addExtraLoggable(rateStatistic);

        } else throw new IllegalArgumentException("Value sampled from LPhy UCLN should be mapped to RealVector ! " + value);

        ucRelaxedClockModel.initAndValidate();
        ucRelaxedClockModel.setID(branchRates.getUniqueId() + ".model");

        // Extra Logger <log ... branchratemodel="@..." tree="@..."/>
        MetaDataTreeLogger metaDataTreeLogger = new MetaDataTreeLogger(ucRelaxedClockModel, beastTree, context);
        context.addExtraLogger(metaDataTreeLogger);

        return ucRelaxedClockModel;
    }

    @Override
    public Class<UCLNMean1> getGeneratorClass() { return UCLNMean1.class; }

    @Override
    public Class<UCRelaxedClockModel> getBEASTClass() {
        return UCRelaxedClockModel.class;
    }
}
