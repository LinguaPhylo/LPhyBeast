package lphybeast.tobeast;

import beast.base.evolution.RateStatistic;
import beast.base.evolution.branchratemodel.BranchRateModel;
import beast.base.evolution.tree.TreeInterface;

public class LoggerUtils {

    public static RateStatistic createRateStatistic(String id, BranchRateModel branchRateModel, TreeInterface tree) {
        // <log id="" ... branchratemodel="@" tree="@"/>
        RateStatistic rateStatistic = new RateStatistic();
        rateStatistic.setInputValue("branchratemodel", branchRateModel);
        rateStatistic.setInputValue("tree", tree);
        rateStatistic.initAndValidate();
        rateStatistic.setID(id);
        return rateStatistic;
    }

}
