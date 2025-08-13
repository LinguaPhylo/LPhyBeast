//package lphybeast.tobeast.generators;
//
//import beast.base.core.BEASTInterface;
//import beast.base.evolution.tree.Tree;
//import beast.base.evolution.tree.TreeInterface;
//import beast.base.evolution.tree.TreeIntervals;
//import beast.base.inference.operator.UpDownOperator;
//import beast.base.inference.parameter.RealParameter;
//import lphy.base.evolution.tree.TimeTree;
//import lphy.base.evolution.continuous.AutoCorrelatedClock;
//import lphy.core.model.Value;
//import lphybeast.BEASTContext;
//import lphybeast.GeneratorToBEAST;
//import lphybeast.tobeast.loggers.MetaDataTreeLogger;
//import rc.beast.evolution.clockmodel.AutoCorrelatedClockModel;
//import rc.beast.evolution.clockmodel.UpDownOp;
//
//public class AutoCorrelatedClockToBEAST implements GeneratorToBEAST<AutoCorrelatedClock, AutoCorrelatedClockModel> {
//
//    @Override
//    public AutoCorrelatedClockModel generatorToBEAST(AutoCorrelatedClock accLphy,
//                                                     BEASTInterface beastValue,
//                                                     BEASTContext context) {
//
//        AutoCorrelatedClockModel accModel = new AutoCorrelatedClockModel();
//
//        Value<TimeTree> treeValue = accLphy.getTree();
//        Value<Double[]> nodeLogRatesValue = accLphy.getNodeLogRates();
//        Value<Double> rootLogRateValue = accLphy.getRootLogRate();
//        Value<Double> sigma2Value = accLphy.getSigma2();
//        Value<Double> meanRateValue = accLphy.getMeanRate();
//        Value<Boolean> normalizeValue = accLphy.getNormalize();
//        Value<Integer> taylorOrderValue = accLphy.getTaylorOrder();
//
//        TreeInterface beastTree = (TreeInterface) context.getBEASTObject(treeValue);
//
//        RealParameter beastNodeLogRates = context.getAsRealParameter(nodeLogRatesValue);
//        RealParameter beastRootLogRate  = context.getAsRealParameter(rootLogRateValue);
//        RealParameter beastSigma2       = context.getAsRealParameter(sigma2Value);
//        RealParameter beastMeanRate     = (meanRateValue != null)
//                ? context.getAsRealParameter(meanRateValue)
//                : new RealParameter("1.0");
//
//        accModel.setInputValue("tree", beastTree);
//        accModel.setInputValue("nodeRates", beastNodeLogRates);
//        accModel.setInputValue("rootLogRate", beastRootLogRate);
//        accModel.setInputValue("sigma2", beastSigma2);
//        accModel.setInputValue("myMeanRate", beastMeanRate);
//
//        if (normalizeValue != null) {
//            accModel.setInputValue("normalize", normalizeValue.value());
//        }
//        if (taylorOrderValue != null) {
//            accModel.setInputValue("taylorOrder", taylorOrderValue.value());
//        }
//
//        if (accModel instanceof UpDownOp) {
//            UpDownOp upDownOp = accModel;
//
//            UpDownOperator op1 = upDownOp.getUpDownOperator1((Tree) beastTree);
//            UpDownOperator op2 = upDownOp.getUpDownOperator2((Tree) beastTree);
//
//            context.addExtraOperator(op1);
//            context.addExtraOperator(op2);
//        }
//
//
//
//
//        MetaDataTreeLogger treeLogger = new MetaDataTreeLogger(accModel, beastTree, context);
//        context.addExtraLogger(treeLogger);
//
//        accModel.initAndValidate();
//        accModel.setID("AutoCorrClockModel." + accLphy.getUniqueId());
//
//        return accModel;
//    }
//
//    @Override
//    public Class<AutoCorrelatedClock> getGeneratorClass() {
//        return AutoCorrelatedClock.class;
//    }
//
//    @Override
//    public Class<AutoCorrelatedClockModel> getBEASTClass() {
//        return AutoCorrelatedClockModel.class;
//    }
//}
