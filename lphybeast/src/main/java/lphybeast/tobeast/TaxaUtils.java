package lphybeast.tobeast;

import beast.base.evolution.alignment.Taxon;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.tree.TreeInterface;

import java.util.ArrayList;
import java.util.List;

/**
 * Utils
 * @author Walter Xie
 */
public class TaxaUtils {

    /**
     * @param beastTree     Beast tree
     * @param taxonNames    taxon names of that clade
     * @return    the clade (TaxonSet) in BEAST given a tree and the taxon names of that clade
     */
    public static TaxonSet getTaxonSet(TreeInterface beastTree, String[] taxonNames) {
        TaxonSet allTaxa = beastTree.getTaxonset();
        // pull out the taxon from given beastTree
        List<Taxon> taxonList = new ArrayList<>();
        for (String taxonName : taxonNames) {
            Taxon taxon = allTaxa.getTaxon(taxonName);
            if (taxon == null) throw new IllegalArgumentException("Cannot find taxon " + taxonName +
                    " in the tree " + beastTree.getID() + " !");
            taxonList.add(taxon);
        }
        TaxonSet taxonSet = new TaxonSet();
        taxonSet.initByName("taxon", taxonList);
        return taxonSet;
    }

}
