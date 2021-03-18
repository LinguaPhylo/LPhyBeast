package lphybeast.tobeast.generators;

import beast.core.BEASTInterface;
import beast.evolution.tree.coalescent.TreeIntervals;
import lphy.evolution.coalescent.SkylineCoalescent;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;
import outercore.parameter.KeyIntegerParameter;
import outercore.parameter.KeyRealParameter;

import java.util.ArrayList;
import java.util.List;

public class SkylineToBSP implements
        GeneratorToBEAST<SkylineCoalescent, beast.evolution.tree.coalescent.BayesianSkyline> {
    @Override
    public beast.evolution.tree.coalescent.BayesianSkyline generatorToBEAST(SkylineCoalescent coalescent, BEASTInterface value, BEASTContext context) {

        beast.evolution.tree.coalescent.BayesianSkyline bsp = new beast.evolution.tree.coalescent.BayesianSkyline();

        TreeIntervals treeIntervals = new TreeIntervals();
        treeIntervals.setInputValue("tree", value);
        treeIntervals.initAndValidate();

        bsp.setInputValue("treeIntervals", treeIntervals);

        // https://github.com/LinguaPhylo/LPhyBeast/issues/33
        // pop size index has to be same as group size, for Tracer skyline plot
        BEASTInterface popSizes = context.getBEASTObject(coalescent.getTheta());
        if ( !(popSizes instanceof KeyRealParameter) )
            throw new IllegalArgumentException("Expecting KeyRealParameter for Skyline pop size ! ");
        bsp.setInputValue("popSizes", popSizes);

        KeyIntegerParameter groupSizeParameter = null;
        if (coalescent.getGroupSizes() != null) {
            groupSizeParameter = (KeyIntegerParameter)context.getBEASTObject(coalescent.getGroupSizes());
        } else {
            // classic skyline
            groupSizeParameter = new KeyIntegerParameter();
            List<Integer> groupSizes = new ArrayList<>();
            for (int i = 0; i < coalescent.getTheta().value().length; i++) {
                groupSizes.add(1);
            }
            groupSizeParameter.setInputValue("value", groupSizes);
            groupSizeParameter.setInputValue("dimension", groupSizes.size());
            groupSizeParameter.initAndValidate();
            groupSizeParameter.setID("groupSizes");
        }

        bsp.setInputValue("groupSizes", groupSizeParameter);
        bsp.initAndValidate();

        return bsp;
    }

    @Override
    public Class<SkylineCoalescent> getGeneratorClass() {
        return SkylineCoalescent.class;
    }

    @Override
    public Class<beast.evolution.tree.coalescent.BayesianSkyline> getBEASTClass() {
        return beast.evolution.tree.coalescent.BayesianSkyline.class;
    }
}
