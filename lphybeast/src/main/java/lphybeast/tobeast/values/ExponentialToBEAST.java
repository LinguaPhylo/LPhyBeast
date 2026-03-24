package lphybeast.tobeast.values;

import beast.base.spec.evolution.tree.coalescent.ExponentialGrowth;
import lphy.base.evolution.coalescent.populationmodel.ExponentialPopulation;
import lphy.base.evolution.coalescent.populationmodel.ExponentialPopulationFunction;
import lphy.core.model.Value;
import lphybeast.BEASTContext;
import lphybeast.ValueToBEAST;


public class ExponentialToBEAST implements ValueToBEAST<ExponentialPopulation, ExponentialGrowth> {

    public ExponentialGrowth valueToBEAST(Value<ExponentialPopulation> lphyPopFuncVal, BEASTContext context) {

        ExponentialPopulationFunction gen = (ExponentialPopulationFunction) lphyPopFuncVal.getGenerator();

        ExponentialGrowth beastPopFunc = new ExponentialGrowth();
        beastPopFunc.setInputValue("growthRate", context.getBEASTObject(gen.getGrowthRate()));
        beastPopFunc.setInputValue("popSize", context.getBEASTObject(gen.getN0()));
        beastPopFunc.initAndValidate();

        ValueToParameter.setID(beastPopFunc, lphyPopFuncVal);

        return beastPopFunc;
    }

    public Class getValueClass() {
        return ExponentialPopulation.class;
    }

    public Class<ExponentialGrowth> getBEASTClass() {
        return ExponentialGrowth.class;
    }

}
