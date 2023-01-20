package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.evolution.tree.TreeIntervals;
import beast.base.inference.parameter.IntegerParameter;
import beast.base.inference.parameter.RealParameter;
import lphy.evolution.coalescent.SkylineCoalescent;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

import java.util.ArrayList;
import java.util.List;

public class SkylineToBSP implements
        GeneratorToBEAST<SkylineCoalescent, beast.base.evolution.tree.coalescent.BayesianSkyline> {
    @Override
    public beast.base.evolution.tree.coalescent.BayesianSkyline generatorToBEAST(SkylineCoalescent coalescent, BEASTInterface value, BEASTContext context) {

        beast.base.evolution.tree.coalescent.BayesianSkyline bsp = new beast.base.evolution.tree.coalescent.BayesianSkyline();

        TreeIntervals treeIntervals = new TreeIntervals();
        treeIntervals.setInputValue("tree", value);
        treeIntervals.initAndValidate();

        bsp.setInputValue("treeIntervals", treeIntervals);

        RealParameter thetaParam = context.getAsRealParameter(coalescent.getTheta());
        // TODO best way to check the index in the keys
        // keys reply on BEAST Parameter default String getKey(int i)
        // expecting keys = [1, 2, 3, ...]
        int i = Integer.parseInt(thetaParam.getKey(0));
        // https://github.com/LinguaPhylo/LPhyBeast/issues/33
        if (i < 1)
            throw new IllegalArgumentException("Tracer requires the key of pop size parameter to start from 1, which cannot pick up 0");
        // set keys explicitly to show them in XML
        List<String> keys = thetaParam.getKeysList();
        String keysStr = String.join(" ", keys);
        thetaParam.setInputValue("keys", keysStr);

        bsp.setInputValue("popSizes", thetaParam);

        // pop size index has to be same as group size, for Tracer skyline plot
        IntegerParameter groupSizeParam = null;
        if (coalescent.getGroupSizes() != null) {
            groupSizeParam = context.getAsIntegerParameter(coalescent.getGroupSizes());
        } else {
            // classic skyline
            groupSizeParam = new IntegerParameter();
            List<Integer> groupSizes = new ArrayList<>();
            for (int j = 0; j < thetaParam.getDimension(); j++) {
                groupSizes.add(1);
            }
            groupSizeParam.setInputValue("value", groupSizes);
            groupSizeParam.setInputValue("dimension", groupSizes.size());
            groupSizeParam.setID("groupSizes");
        }
        // expecting keys = [1, 2, 3, ...]
        i = Integer.parseInt(groupSizeParam.getKey(0));
        // https://github.com/LinguaPhylo/LPhyBeast/issues/33
        if (i < 1)
            throw new IllegalArgumentException("Tracer requires the key of group size parameter to start from 1, which cannot pick up 0");

        // set keys explicitly to show them in XML
        keys = groupSizeParam.getKeysList();
        keysStr = String.join(" ", keys);
        groupSizeParam.setInputValue("keys", keysStr);

        bsp.setInputValue("groupSizes", groupSizeParam);
        bsp.initAndValidate();

        return bsp;
    }

    @Override
    public Class<SkylineCoalescent> getGeneratorClass() {
        return SkylineCoalescent.class;
    }

    @Override
    public Class<beast.base.evolution.tree.coalescent.BayesianSkyline> getBEASTClass() {
        return beast.base.evolution.tree.coalescent.BayesianSkyline.class;
    }
}
