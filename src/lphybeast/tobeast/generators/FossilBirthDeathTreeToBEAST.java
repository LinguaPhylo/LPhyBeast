package lphybeast.tobeast.generators;

import beast.core.BEASTInterface;
import beast.core.parameter.RealParameter;
import beast.evolution.speciation.SABirthDeathModel;
import beast.evolution.tree.Tree;
import lphy.evolution.birthdeath.FossilBirthDeathTree;
import lphy.evolution.tree.TimeTree;
import lphy.graphicalModel.Value;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class FossilBirthDeathTreeToBEAST implements
        GeneratorToBEAST<FossilBirthDeathTree, SABirthDeathModel> {

    public static final String suffix = ".origin";

    @Override
    public SABirthDeathModel generatorToBEAST(FossilBirthDeathTree generator, BEASTInterface tree, BEASTContext context) {

        Value<TimeTree> timeTree = (Value<TimeTree>) context.getGraphicalModelNode(tree);
        Tree beastTree = (Tree) tree;

        if (beastTree.getRoot().getChildCount() == 1) {
            throw new RuntimeException("BEAST tree should not have origin node!");
        }

        if (timeTree.value().getRoot().getChildCount() != 1) {
            throw new IllegalArgumentException("Expecting a lphy.evolution.tree.TimeTree with an origin node!");
        }

        RealParameter originParameter = (RealParameter) context.getBEASTObject(tree.getID() + suffix);
        if (originParameter.getValue() < beastTree.getRoot().getHeight()) {
            originParameter.setValue(beastTree.getRoot().getHeight() + 1.0);
        }
        context.addStateNode(originParameter, timeTree, true);

        SABirthDeathModel saBirthDeathModel = new SABirthDeathModel();
        saBirthDeathModel.setInputValue("birthRate", context.getAsRealParameter(generator.getBirthRate()));
        saBirthDeathModel.setInputValue("deathRate", context.getAsRealParameter(generator.getDeathRate()));
        saBirthDeathModel.setInputValue("rho", context.getAsRealParameter(generator.getRho()));
        saBirthDeathModel.setInputValue("samplingRate", context.getAsRealParameter(generator.getPsi()));
        saBirthDeathModel.setInputValue("removalProbability", BEASTContext.createRealParameter(0.0));
        saBirthDeathModel.setInputValue("origin", originParameter );
        saBirthDeathModel.setInputValue("tree", tree);
        saBirthDeathModel.setInputValue("conditionOnSampling", true);
        saBirthDeathModel.initAndValidate();

        return saBirthDeathModel;
    }

    @Override
    public void modifyBEASTValues(FossilBirthDeathTree generator, BEASTInterface tree, BEASTContext context) {
        // create the origin parameter
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

        RealParameter originParameter = BEASTContext.createRealParameter(originID, origin);
        context.addStateNode(originParameter, timeTree, true);
    }

    @Override
    public Class<FossilBirthDeathTree> getGeneratorClass() {
        return FossilBirthDeathTree.class;
    }

    @Override
    public Class<SABirthDeathModel> getBEASTClass() {
        return SABirthDeathModel.class;
    }
}
