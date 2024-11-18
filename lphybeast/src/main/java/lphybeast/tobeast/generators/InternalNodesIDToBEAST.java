package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeDistribution;
import lphy.base.evolution.EvolutionConstants;
import lphy.base.evolution.tree.TimeTree;
import lphy.base.function.tree.InternalNodesID;
import lphy.core.model.Generator;
import lphy.core.model.Value;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

/**
 * setInternalNodesID takes a tree whose internal nodes have no id,
 * and generate the same tree but internal nodes have id.
 */
public class InternalNodesIDToBEAST implements GeneratorToBEAST<InternalNodesID, TreeDistribution> {

    @Override
    public TreeDistribution generatorToBEAST(InternalNodesID generator, BEASTInterface value, BEASTContext context) {
        // ψ2 = setInternalNodesID(ψ);
        if (value instanceof Tree treeInternalNodesWithID) {
//            Value<TimeTree> lphyTreeInternalNodesWithID =
//                    (Value<TimeTree>) context.getGraphicalModelNode(treeInternalNodesWithID);

            Value<TimeTree> lphyTreeInternalNodesNoID =
                    generator.getParams().get(EvolutionConstants.treeParamName);
            Generator treeGenerator = lphyTreeInternalNodesNoID.getGenerator();
            // e.g. Yule
            TreeDistribution treeDistribution = (TreeDistribution) context.getBEASTObject(treeGenerator);
            // replace old tree
            treeDistribution.setInputValue("tree", treeInternalNodesWithID);
            treeDistribution.initAndValidate();

            // rm old beast obj
            Tree treeInternalNodesNoID = (Tree) context.getBEASTObject(lphyTreeInternalNodesNoID);
            context.removeBEASTObject(treeInternalNodesNoID);
            // this creates a new YuleModel mapping in lphybeast
            context.removeBEASTObject(treeDistribution);
            return treeDistribution;

        } else throw new IllegalArgumentException();
    }

    @Override
    public Class<InternalNodesID> getGeneratorClass() {
        return InternalNodesID.class;
    }

    @Override
    public Class<TreeDistribution> getBEASTClass() {
        return TreeDistribution.class;
    }
}
