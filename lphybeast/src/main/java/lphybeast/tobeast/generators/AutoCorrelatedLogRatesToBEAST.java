//package lphybeast.tobeast.generators;
//
//import beast.base.core.BEASTInterface;
//import beast.base.evolution.tree.TreeInterface;
//import beast.base.inference.parameter.RealParameter;
//import lphy.base.evolution.continuous.AutoCorrelatedLogRates;
//import lphybeast.BEASTContext;
//import lphybeast.GeneratorToBEAST;
//import rc.beast.evolution.clockmodel.AutoCorrelatedPrior;
//
//public class AutoCorrelatedLogRatesToBEAST implements GeneratorToBEAST<AutoCorrelatedLogRates, AutoCorrelatedPrior> {
//
//    @Override
//    public AutoCorrelatedPrior generatorToBEAST(AutoCorrelatedLogRates dist,
//                                                BEASTInterface beastValue,
//                                                BEASTContext context) {
//        if (!(beastValue instanceof RealParameter)) {
//            throw new IllegalArgumentException("AutoCorrelatedLogRates2 must map to a RealParameter!");
//        }
//
//        RealParameter beastNodeLogRate = (RealParameter) beastValue;
//
//        TreeInterface beastTree = (TreeInterface) context.getBEASTObject(dist.getTree());
//        RealParameter beastSigma2      = context.getAsRealParameter(dist.getSigma2());
//        RealParameter beastRootLogRate = context.getAsRealParameter(dist.getRootLogRate());
//
//        AutoCorrelatedPrior acPrior = new AutoCorrelatedPrior();
//        acPrior.setID("AutoCorrPrior." + dist.getUniqueId());
//
//        acPrior.setInputValue("tree",        beastTree);
//        acPrior.setInputValue("sigma2",      beastSigma2);
//        acPrior.setInputValue("rootLogRate", beastRootLogRate);
//        acPrior.setInputValue("nodeRates",   beastNodeLogRate);
//
//        context.addBEASTObject(acPrior, dist);
//
////       context.addSkipLoggable(beastNodeLogRate);
//
//        acPrior.initAndValidate();
//        return acPrior;
//    }
//
//    @Override
//    public Class<AutoCorrelatedLogRates> getGeneratorClass() {
//        return AutoCorrelatedLogRates.class;
//    }
//
//    @Override
//    public Class<AutoCorrelatedPrior> getBEASTClass() {
//        return AutoCorrelatedPrior.class;
//    }
//}
