package sa.lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.evolution.tree.Tree;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.UnitInterval;
import beast.base.spec.inference.parameter.RealScalarParam;
import lphy.base.evolution.birthdeath.SimFBDAgeDT;
import lphy.base.evolution.tree.TimeTree;
import lphy.core.model.Value;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;
import sa.evolution.speciation.SABirthDeathModel;

public class SimFBDAgeDTToBEAST implements GeneratorToBEAST<SimFBDAgeDT, SABirthDeathModel> {

    public static final String suffix = ".origin";

    @Override
    public SABirthDeathModel generatorToBEAST(SimFBDAgeDT generator, BEASTInterface tree, BEASTContext context) {

        Value<TimeTree> timeTree = (Value<TimeTree>) context.getGraphicalModelNode(tree);
        Tree beastTree = (Tree) tree;

        if (beastTree.getRoot().getChildCount() == 1) {
            throw new RuntimeException("BEAST tree should not have origin node!");
        }

        if (timeTree.value().getRoot().getChildCount() != 1) {
            throw new IllegalArgumentException("Expecting a lphy.evolution.tree.TimeTree with an origin node!");
        }

        RealScalarParam<PositiveReal> originParameter = (RealScalarParam<PositiveReal>) context.getBEASTObject(tree.getID() + suffix);
        if (originParameter.get() < beastTree.getRoot().getHeight()) {
            originParameter.set(beastTree.getRoot().getHeight() + 1.0);
        }
        context.addStateNode(originParameter, timeTree, true);

        SABirthDeathModel saBirthDeathModel = new SABirthDeathModel();
        saBirthDeathModel.setInputValue("diversificationRate", context.getAsRealScalar(generator.getDiversificationRate()));
        saBirthDeathModel.setInputValue("turnover", context.getAsRealScalar(generator.getTurnover()));
        saBirthDeathModel.setInputValue("rho", context.getAsRealScalar(generator.getFrac()));
        saBirthDeathModel.setInputValue("samplingProportion", context.getAsRealScalar(generator.getSamplingProportion()));
        saBirthDeathModel.setInputValue("removalProbability", new RealScalarParam<>(0.0, UnitInterval.INSTANCE));
        saBirthDeathModel.setInputValue("origin", originParameter);
        saBirthDeathModel.setInputValue("tree", tree);
        saBirthDeathModel.setInputValue("conditionOnSampling", true);
        saBirthDeathModel.initAndValidate();

        return saBirthDeathModel;
    }

    @Override
    public void modifyBEASTValues(SimFBDAgeDT generator, BEASTInterface tree, BEASTContext context) {
        String treeId = tree.getID();
        String originID = treeId + suffix;

        Value<TimeTree> timeTree = (Value<TimeTree>) context.getGraphicalModelNode(tree);
        if (timeTree.value().getRoot().getChildCount() != 1) {
            throw new IllegalArgumentException("Expecting a lphy.evolution.tree.TimeTree with an origin node!");
        }

        double origin = timeTree.value().getRoot().getAge();

        // hack to ensure that origin is older than the root height for tree initializers like the SBI
        // TODO remove this when sampled-ancestors have been fixed.
        origin += timeTree.value().getTaxa().ntaxa();

        RealScalarParam<PositiveReal> originParameter = new RealScalarParam<>(origin, PositiveReal.INSTANCE);
        originParameter.setID(originID);
        context.addStateNode(originParameter, timeTree, true);
    }

    @Override
    public Class<SimFBDAgeDT> getGeneratorClass() {
        return SimFBDAgeDT.class;
    }

    @Override
    public Class<SABirthDeathModel> getBEASTClass() {
        return SABirthDeathModel.class;
    }
}
