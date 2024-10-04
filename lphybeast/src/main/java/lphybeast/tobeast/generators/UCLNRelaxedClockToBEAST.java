package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.evolution.branchratemodel.UCRelaxedClockModel;
import beast.base.inference.distribution.LogNormalDistributionModel;
import beast.base.inference.distribution.Prior;
import beast.base.inference.parameter.RealParameter;
import lphy.base.distribution.UCLN;
import lphy.base.evolution.tree.TimeTree;
import lphy.core.model.GraphicalModelNode;
import lphy.core.model.Value;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class UCLNRelaxedClockToBEAST implements GeneratorToBEAST<UCLN, UCRelaxedClockModel> {
    @Override
    public UCRelaxedClockModel generatorToBEAST(UCLN ucln, BEASTInterface value, BEASTContext context) {

        UCRelaxedClockModel ucRelaxedClockModel = new UCRelaxedClockModel();

        LogNormalDistributionModel logNormDist = new LogNormalDistributionModel();
        logNormDist.setInputValue("M", context.getAsRealParameter(ucln.getUclnMean()));
        logNormDist.setInputValue("S", context.getAsRealParameter(ucln.getUclnSigma()));
        logNormDist.initAndValidate();
        ucRelaxedClockModel.setInputValue("distr", logNormDist);

        Value<TimeTree> tree = ucln.getTree();
        ucRelaxedClockModel.setInputValue("tree", context.getBEASTObject(tree));

        if (value instanceof RealParameter rates) {
            ucRelaxedClockModel.setInputValue("rates", rates);

            // rates prior, which is same LogNormal as UCLN
            Prior ratesPrior = BEASTContext.createPrior(logNormDist, rates);
            context.addBEASTObject(ratesPrior, ucln);

        } else throw new IllegalArgumentException("Value sampled from LPhy UCLN should be mapped to RealParameter ! " + value);

        ucRelaxedClockModel.initAndValidate();
        GraphicalModelNode branchRates = context.getGraphicalModelNode(value);
        // in case to duplicate with RandomValue id also called "branchRates"
        ucRelaxedClockModel.setID(branchRates.getUniqueId() + ".model");

        //TODO rm rates from log, replaced by
        // <log id="ORCRatesStat" spec="beast.base.evolution.RateStatistic" branchratemodel="@OptimisedRelaxedClock" tree="@Tree"/>

        return ucRelaxedClockModel;
    }

    @Override
    public Class<UCLN> getGeneratorClass() { return UCLN.class; }

    @Override
    public Class<UCRelaxedClockModel> getBEASTClass() {
        return UCRelaxedClockModel.class;
    }
}
