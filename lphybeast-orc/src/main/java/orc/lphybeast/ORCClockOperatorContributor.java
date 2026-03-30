package orc.lphybeast;

import beast.base.spec.evolution.branchratemodel.UCRelaxedClockModel;
import beast.base.evolution.tree.Tree;
import beast.base.inference.Operator;
import lphybeast.BEASTContext;
import lphybeast.spi.ClockOperatorContributor;
import orc.consoperators.InConstantDistanceOperator;
import orc.consoperators.SimpleDistance;
import orc.consoperators.SmallPulley;

import java.util.ArrayList;
import java.util.List;

import static lphybeast.BEASTContext.getOperatorWeight;

/**
 * Contributes ORC (Optimised Relaxed Clock) operators for relaxed clock analyses.
 */
public class ORCClockOperatorContributor implements ClockOperatorContributor {

    @Override
    public List<Operator> createOperators(Tree tree, UCRelaxedClockModel relaxedClockModel, BEASTContext context) {
        List<Operator> operators = new ArrayList<>();

        var rates = relaxedClockModel.rateInput.get();
        double tWindowSize = tree.getRoot().getHeight() / 10.0;

        InConstantDistanceOperator inConstantDistanceOperator = new InConstantDistanceOperator();
        inConstantDistanceOperator.setInputValue("clockModel", relaxedClockModel);
        inConstantDistanceOperator.setInputValue("tree", tree);
        inConstantDistanceOperator.setInputValue("rates", rates);
        inConstantDistanceOperator.setInputValue("twindowSize", tWindowSize);
        inConstantDistanceOperator.setInputValue("weight", getOperatorWeight(tree.getNodeCount()));
        inConstantDistanceOperator.setID(relaxedClockModel.getID() + ".inConstantDistanceOperator");
        inConstantDistanceOperator.initAndValidate();
        operators.add(inConstantDistanceOperator);

        SimpleDistance simpleDistance = new SimpleDistance();
        simpleDistance.setInputValue("clockModel", relaxedClockModel);
        simpleDistance.setInputValue("tree", tree);
        simpleDistance.setInputValue("rates", rates);
        simpleDistance.setInputValue("twindowSize", tWindowSize);
        simpleDistance.setInputValue("weight", getOperatorWeight(2));
        simpleDistance.setID(relaxedClockModel.getID() + ".simpleDistance");
        simpleDistance.initAndValidate();
        operators.add(simpleDistance);

        SmallPulley smallPulley = new SmallPulley();
        smallPulley.setInputValue("clockModel", relaxedClockModel);
        smallPulley.setInputValue("tree", tree);
        smallPulley.setInputValue("rates", rates);
        smallPulley.setInputValue("dwindowSize", 0.1);
        smallPulley.setInputValue("weight", getOperatorWeight(2));
        smallPulley.setID(relaxedClockModel.getID() + ".smallPulley");
        smallPulley.initAndValidate();
        operators.add(smallPulley);

        System.out.println("ORC extension added " + operators.size() + " relaxed clock operators.");
        return operators;
    }
}
