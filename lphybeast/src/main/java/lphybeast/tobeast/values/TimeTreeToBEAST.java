package lphybeast.tobeast.values;

import beast.base.evolution.alignment.Taxon;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.tree.TraitSet;
import beast.base.evolution.tree.TreeParser;
import lphy.base.evolution.alignment.SimpleAlignment;
import lphy.base.evolution.tree.TimeTree;
import lphy.base.evolution.tree.TimeTreeNode;
import lphy.core.model.RandomVariable;
import lphy.core.model.Value;
import lphy.core.vectorization.operation.ElementsAt;
import lphybeast.BEASTContext;
import lphybeast.ValueToBEAST;

import java.util.ArrayList;
import java.util.List;

public class TimeTreeToBEAST implements ValueToBEAST<TimeTree, TreeParser> {

    /**
     * Call this to use simulated alignment {@link SimpleAlignment}.
     */
    public TimeTreeToBEAST() { }

    @Override
    public TreeParser valueToBEAST(Value<TimeTree> timeTreeValue, BEASTContext context) {

        TimeTree timeTree = timeTreeValue.value();

        TreeParser tree = new TreeParser();
        tree.setInputValue("newick", timeTree.toNewick(false));
        // TODO seems IsLabelledNewick="true" causes the trouble when cares internal node index
        tree.setInputValue("IsLabelledNewick", true);
        // IsLabelledNewick="true" Is the Newick tree labelled (alternatively contains node numbers).
//        tree.setInputValue("IsLabelledNewick", false);

        TaxonSet taxa = new TaxonSet();
        List<Taxon> taxonList = context.createTaxonList(getTaxaNames(timeTree));
        taxa.setInputValue("taxon", taxonList);
        taxa.initAndValidate();
        tree.setInputValue("taxonset", taxa);

        if (!timeTree.isUltrametric()) {

            TraitSet traitSet = new TraitSet();
            traitSet.setInputValue("traitname", TraitSet.DATE_BACKWARD_TRAIT);
            traitSet.setInputValue("value",createAgeTraitString(timeTree));
            traitSet.setInputValue("taxa", taxa);
            traitSet.initAndValidate();

            tree.setInputValue("trait", traitSet);
        }

        tree.initAndValidate();
        tree.setRoot(tree.parseNewick(tree.newickInput.get()));

        // if this is an element of a TimeTree[] random variable
        if (timeTreeValue.isAnonymous() &&
                timeTreeValue.getGenerator() instanceof ElementsAt &&
                ((ElementsAt<?>) timeTreeValue.getGenerator()).array() instanceof RandomVariable) {

            ElementsAt elementsAt = (ElementsAt<?>) timeTreeValue.getGenerator();

            tree.setID(elementsAt.array().getCanonicalId() + "." + ((Integer[])elementsAt.index().value())[0]);
        }

        if (!timeTreeValue.isAnonymous()) tree.setID(timeTreeValue.getCanonicalId());
        return tree;
    }

    public static List<String> getTaxaNames(TimeTree timeTree) {
        List<String> taxaNames = new ArrayList<>();
        for (TimeTreeNode node : timeTree.getNodes()) {
            if (node.isLeaf()) {
                taxaNames.add(node.getId());
            }
        }
        return taxaNames;
    }

    private String createAgeTraitString(TimeTree tree) {

        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (TimeTreeNode node : tree.getNodes()) {
            if (node.isLeaf()) {
                if (count > 0) builder.append(",\n");
                builder.append(node.getId());
                builder.append("=");
                builder.append(node.getAge());
                count += 1;
            }
        }
        return builder.toString();
    }

    @Override
    public Class getValueClass() {
        return TimeTree.class;
    }

    @Override
    public Class<TreeParser> getBEASTClass() {
        return TreeParser.class;
    }
}
