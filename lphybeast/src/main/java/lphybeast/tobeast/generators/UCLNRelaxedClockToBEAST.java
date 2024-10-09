package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.evolution.RateStatistic;
import beast.base.evolution.branchratemodel.UCRelaxedClockModel;
import beast.base.evolution.tree.TreeInterface;
import beast.base.inference.distribution.LogNormalDistributionModel;
import beast.base.inference.distribution.Prior;
import beast.base.inference.parameter.RealParameter;
import lphy.base.distribution.UCLNMean1;
import lphy.base.evolution.likelihood.PhyloCTMC;
import lphy.base.evolution.tree.TimeTree;
import lphy.core.model.GraphicalModelNode;
import lphy.core.model.Value;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;
import lphybeast.tobeast.LoggerUtils;
import lphybeast.tobeast.loggers.MetaDataTreeLogger;

/**
 * For ORC package.
 */
public class UCLNRelaxedClockToBEAST implements GeneratorToBEAST<UCLNMean1, UCRelaxedClockModel> {
    @Override
    public UCRelaxedClockModel generatorToBEAST(UCLNMean1 ucln, BEASTInterface value, BEASTContext context) {

        UCRelaxedClockModel ucRelaxedClockModel = new UCRelaxedClockModel();

        LogNormalDistributionModel logNormDist = new LogNormalDistributionModel();
        // Note: the mean of log-normal distr on branch rates in real space must be fixed to 1.
        logNormDist.setInputValue("M", new RealParameter("1.0"));
        logNormDist.setInputValue("meanInRealSpace", "true");
        logNormDist.setInputValue("S", context.getAsRealParameter(ucln.getUclnSigma()));
        logNormDist.initAndValidate();
        ucRelaxedClockModel.setInputValue("distr", logNormDist);

        Value<TimeTree> tree = ucln.getTree();
        TreeInterface beastTree = (TreeInterface) context.getBEASTObject(tree);
        ucRelaxedClockModel.setInputValue("tree", beastTree);

        // set clock.rate to UclnMean
        for (GraphicalModelNode treeOut : tree.getOutputs()) {
            if (treeOut instanceof PhyloCTMC phyloCTMC) {
                Value mu = phyloCTMC.getClockRate();
                if (mu != null) // BEAST clock.rate default to 1.0
                    ucRelaxedClockModel.setInputValue("clock.rate", context.getAsRealParameter(mu));
//                else
//                    ucRelaxedClockModel.setInputValue("clock.rate", new RealParameter("1.0"));
            }
        }

        if (value instanceof RealParameter rates) {
            ucRelaxedClockModel.setInputValue("rates", rates);

            // branch rates LogNormal prior, which is same LogNormal as UCLN
            Prior ratesPrior = BEASTContext.createPrior(logNormDist, rates);
            context.addBEASTObject(ratesPrior, ucln);

        } else throw new IllegalArgumentException("Value sampled from LPhy UCLN should be mapped to RealParameter ! " + value);

        ucRelaxedClockModel.initAndValidate();
        GraphicalModelNode branchRates = context.getGraphicalModelNode(value);
        // in case to duplicate with RandomValue id also called "branchRates"
        ucRelaxedClockModel.setID(branchRates.getUniqueId() + ".model");

        //TODO rm rates from log, replaced by <log id="" ... branchratemodel="@" tree="@"/>
        RateStatistic rateStatistic = LoggerUtils.createRateStatistic("RatesStat." + branchRates.getUniqueId(), ucRelaxedClockModel, beastTree);
        context.addExtraLoggable(rateStatistic);

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
