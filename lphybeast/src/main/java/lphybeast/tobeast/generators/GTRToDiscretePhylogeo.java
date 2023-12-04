package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.evolution.substitutionmodel.Frequencies;
import beast.base.inference.parameter.BooleanParameter;
import beast.base.inference.parameter.RealParameter;
import beastclassic.evolution.substitutionmodel.SVSGeneralSubstitutionModel;
import lphy.base.evolution.substitutionmodel.GeneralTimeReversible;
import lphy.base.function.Select;
import lphy.core.model.Generator;
import lphy.core.model.GraphicalModelNode;
import lphy.core.model.Value;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

import java.util.Arrays;
import java.util.Map;

public class GTRToDiscretePhylogeo implements
        GeneratorToBEAST<GeneralTimeReversible, SVSGeneralSubstitutionModel> {

    @Override
    public SVSGeneralSubstitutionModel generatorToBEAST(GeneralTimeReversible gtr,
                                                        BEASTInterface value, BEASTContext context) {

        SVSGeneralSubstitutionModel svs = new SVSGeneralSubstitutionModel();
        // only symmetric
        svs.setInputValue("symmetric", Boolean.TRUE);

        Generator ratesGenerator = gtr.getRates().getGenerator();
        // rates=select(x=trait_rates, indicator=trait_indicators)
        Map<String, Value> selectFunParams = ratesGenerator.getParams();
        if (selectFunParams.size() != 2)
            throw new IllegalStateException("Expecting 'select' function to produce traits rates, given rates and boolean indicators");

        GraphicalModelNode<?> rateNode = (GraphicalModelNode<?>) selectFunParams.get(Select.valueParamName);
        RealParameter rates = (RealParameter) context.getBEASTObject(rateNode);
        svs.setInputValue("rates", rates);

        GraphicalModelNode<?> indicatorNode = (GraphicalModelNode<?>) selectFunParams.get(Select.indicatorParamName);
        BooleanParameter rateIndicators = (BooleanParameter) context.getBEASTObject(indicatorNode);

        RealParameter traitfrequencies = (RealParameter) context.getBEASTObject(gtr.getFreq());
        Frequencies traitfreqs = new Frequencies();
        traitfreqs.setInputValue("frequencies", traitfrequencies);
        traitfreqs.initAndValidate();
        svs.setInputValue("frequencies", traitfreqs);

        // frequencies dim = number of states
        int stateCount = traitfrequencies.getDimension();
        validateIndicators(rateIndicators, stateCount);
        svs.setInputValue("rateIndicator", rateIndicators);

        svs.initAndValidate();

        return svs;
    }

    // To avoid initialization issue #31
    private void validateIndicators(BooleanParameter rateIndicators, int stateCount) {
        // symmetric rates
        if (rateIndicators.getDimension() != stateCount * (stateCount - 1) / 2)
            throw new IllegalStateException("In symmetric rates model, the rate indicators should have the dimension of " +
                    "stateCount * (stateCount - 1) / 2 !\nBut stateCount = " + stateCount +
                    ", indicators dimension = " + rateIndicators.getDimension());

        long numOfTrue = Arrays.stream(rateIndicators.getValues()).filter(indicator -> indicator).count();

        if (numOfTrue < stateCount)
            throw new IllegalArgumentException("Invalid init value of the trait rate indicators, where " +
                    numOfTrue + " 'true' " + " is less than number of states " + stateCount +
                    "! Set all to be 'true' in XML !");

    }

    @Override
    public Class<GeneralTimeReversible> getGeneratorClass() {
        return GeneralTimeReversible.class;
    }

    @Override
    public Class<SVSGeneralSubstitutionModel> getBEASTClass() {
        return SVSGeneralSubstitutionModel.class;
    }
}
