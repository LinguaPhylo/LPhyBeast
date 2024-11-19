package flc.lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.evolution.alignment.Taxon;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.tree.TreeInterface;
import beast.base.inference.parameter.RealParameter;
import lphy.base.evolution.branchrate.LocalClock;
import lphy.base.evolution.tree.TimeTree;
import lphy.base.evolution.tree.TimeTreeNode;
import lphy.core.model.Value;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;
import mf.beast.evolution.branchratemodel.FlexibleLocalClockModel;
import mf.beast.evolution.branchratemodel.StrictCladeModel;
import mf.beast.evolution.branchratemodel.StrictLineageClockModel;

import java.util.ArrayList;
import java.util.List;

import static lphybeast.tobeast.TaxaUtils.getTaxonSet;

public class LocalClockToBeast implements GeneratorToBEAST<LocalClock, FlexibleLocalClockModel> {
    public FlexibleLocalClockModel generatorToBEAST(LocalClock localClock, BEASTInterface value, BEASTContext context) {
        FlexibleLocalClockModel flc = new FlexibleLocalClockModel();

        Value<Double> rootRate = localClock.getRootRate();
        StrictLineageClockModel rootCladeModel = new StrictLineageClockModel();

        RealParameter rootRateParameter = new RealParameter(rootRate.valueToString());
        // specify the parameter has the upper bound at 1.0
        rootRateParameter.setInputValue("upper", 1.0);

        rootCladeModel.initByName("clock.rate", rootRateParameter);

        Value<Object[]> clades = localClock.getClades();
        Value<Double[]> cladeRates = localClock.getCladeRates();
        Value<Boolean> includeStem = localClock.getIncludeStem();
        Value<TimeTree> tree = localClock.getTree();

        StrictCladeModel cladeModel = new StrictCladeModel();
        // TODO: current FLC fix assumes one clade model
        // add multi clade model in the future
        Object[] cladesValue = clades.value();

        for (int i = 0; i < cladesValue.length; i++) {
            TimeTreeNode node = (TimeTreeNode) cladesValue[i];
            List<TimeTreeNode> leaves = node.getAllLeafNodes();
            String[] cladeNames = new String[leaves.size()];

            for (int j = 0; j < leaves.size(); j++) {
                cladeNames[j] = leaves.get(j).getId();
            }

            TaxonSet cladeTaxonSet = getTaxonSet((TreeInterface) context.getBEASTObject(tree.getId()), cladeNames);
            cladeModel.initByName(
                    "taxonset", cladeTaxonSet,
                    "clock.rate", cladeRates.value()[i].toString(),
                    "includeStem", includeStem.value()
            );
        }

        flc.initByName(
                "rootClockModel", rootCladeModel,
                "cladeClockModel", cladeModel,
                // get tree id to link the correct tree
                "tree", context.getBEASTObject(tree.getId())
        );


        flc.initAndValidate();

        return flc;
    }

    @Override
    public Class<LocalClock> getGeneratorClass() {
        return LocalClock.class;
    }

    @Override
    public Class<FlexibleLocalClockModel> getBEASTClass() {
        return FlexibleLocalClockModel.class;
    }
}

