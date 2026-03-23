package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.evolution.substitutionmodel.Frequencies;
import beast.base.inference.parameter.BooleanParameter;
import beast.base.inference.parameter.RealParameter;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.spec.inference.parameter.SimplexParam;
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
        BEASTInterface ratesBeast = context.getBEASTObject(rateNode);
        // SVSGeneralSubstitutionModel (beast-classic) expects Function for rates.
        // Bridge spec SimplexParam back to RealParameter until beast-classic is migrated.
        if (ratesBeast instanceof SimplexParam simplex) {
            RealParameter rp = toRealParameter(simplex);
            context.putBEASTObject(rateNode, rp);
            ratesBeast = rp;
        }
        svs.setInputValue("rates", ratesBeast);

        GraphicalModelNode<?> indicatorNode = (GraphicalModelNode<?>) selectFunParams.get(Select.indicatorParamName);
        BooleanParameter rateIndicators = (BooleanParameter) context.getBEASTObject(indicatorNode);

        BEASTInterface freqBeast = context.getBEASTObject(gtr.getFreq());
        // Bridge spec SimplexParam to old Frequencies for beast-classic
        if (freqBeast instanceof SimplexParam simplex) {
            RealParameter freqRP = toRealParameter(simplex);
            context.putBEASTObject(gtr.getFreq(), freqRP);
            Frequencies traitfreqs = new Frequencies();
            traitfreqs.setInputValue("frequencies", freqRP);
            traitfreqs.initAndValidate();
            svs.setInputValue("frequencies", traitfreqs);
        } else if (freqBeast instanceof RealParameter rp) {
            Frequencies traitfreqs = new Frequencies();
            traitfreqs.setInputValue("frequencies", rp);
            traitfreqs.initAndValidate();
            svs.setInputValue("frequencies", traitfreqs);
        }

        int stateCount = ((Frequencies) svs.frequenciesInput.get()).getFreqs().length;
        validateIndicators(rateIndicators, stateCount);
        svs.setInputValue("rateIndicator", rateIndicators);

        svs.initAndValidate();

        return svs;
    }

    /** Bridge: convert spec SimplexParam back to legacy RealParameter for beast-classic */
    private static RealParameter toRealParameter(RealVectorParam<?> vec) {
        Double[] values = new Double[vec.size()];
        for (int i = 0; i < values.length; i++)
            values[i] = vec.get(i);
        RealParameter rp = new RealParameter(values);
        rp.setID(vec.getID());
        return rp;
    }

    private void validateIndicators(BooleanParameter rateIndicators, int stateCount) {
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
