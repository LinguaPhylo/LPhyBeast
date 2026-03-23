package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.evolution.tree.TreeIntervals;
import beast.base.spec.domain.PositiveInt;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.evolution.tree.coalescent.BayesianSkyline;
import beast.base.spec.inference.parameter.IntSimplexParam;
import beast.base.spec.type.IntSimplex;
import beast.base.spec.type.RealVector;
import lphy.base.evolution.coalescent.SkylineCoalescent;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class SkylineToBSP implements GeneratorToBEAST<SkylineCoalescent, BayesianSkyline> {
    @Override
    public BayesianSkyline generatorToBEAST(SkylineCoalescent coalescent, BEASTInterface value, BEASTContext context) {

        BayesianSkyline bsp = new BayesianSkyline();

        TreeIntervals treeIntervals = new TreeIntervals();
        treeIntervals.setInputValue("tree", value);
        treeIntervals.initAndValidate();

        bsp.setInputValue("treeIntervals", treeIntervals);

        @SuppressWarnings("unchecked")
        RealVector<PositiveReal> thetaParam =
                (RealVector<PositiveReal>) context.getBEASTObject(coalescent.getTheta());
        bsp.setInputValue("popSizes", thetaParam);

        IntSimplex<?> groupSizeParam;
        if (coalescent.getGroupSizes() != null) {
            groupSizeParam = (IntSimplex<?>) context.getBEASTObject(coalescent.getGroupSizes());
        } else {
            // classic skyline: one interval per group
            int dim = thetaParam.size();
            int[] ones = new int[dim];
            for (int j = 0; j < dim; j++) ones[j] = 1;
            IntSimplexParam<PositiveInt> defaultGroups =
                    new IntSimplexParam<>(ones, PositiveInt.INSTANCE, dim);
            defaultGroups.setID("groupSizes");
            groupSizeParam = defaultGroups;
        }
        bsp.setInputValue("groupSizes", groupSizeParam);
        bsp.initAndValidate();

        return bsp;
    }

    @Override
    public Class<SkylineCoalescent> getGeneratorClass() {
        return SkylineCoalescent.class;
    }

    @Override
    public Class<BayesianSkyline> getBEASTClass() {
        return BayesianSkyline.class;
    }
}
