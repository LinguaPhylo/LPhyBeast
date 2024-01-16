package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.evolution.substitutionmodel.Frequencies;
import beast.base.inference.parameter.RealParameter;
import lphy.base.evolution.substitutionmodel.HKY;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class HKYToBEAST implements GeneratorToBEAST<HKY, beast.base.evolution.substitutionmodel.HKY> {
    @Override
    public beast.base.evolution.substitutionmodel.HKY generatorToBEAST(HKY hky, BEASTInterface value, BEASTContext context) {

        RealParameter kappa = (RealParameter) context.getBEASTObject(hky.getKappa());
        RealParameter freqParam = (RealParameter) context.getBEASTObject(hky.getFreq());
        Frequencies frequencies = BEASTContext.createBEASTFrequencies(freqParam,"A C G T");

        beast.base.evolution.substitutionmodel.HKY beastHKY = new beast.base.evolution.substitutionmodel.HKY();
        beastHKY.setInputValue("kappa", kappa);
        beastHKY.setInputValue("frequencies", frequencies);
        beastHKY.initAndValidate();

//        <operator id="KappaScaler.s:$(n)" spec="beast.base.evolution.operator.AdaptableOperatorSampler" weight="0.05">
//            <parameter idref="kappa.s:$(n)"/>
//        	<operator idref="AVMNOperator.$(n)"/>
//		    <operator id='KappaScalerX.s:$(n)' spec='kernel.BactrianScaleOperator' scaleFactor="0.1" weight="0.1" parameter="@kappa.s:$(n)"/>
//        </operator>
//
//        <operator id="FrequenciesExchanger.s:$(n)" spec="beast.base.evolution.operator.AdaptableOperatorSampler" weight="0.05">
//            <parameter idref="freqParameter.s:$(n)"/>
//        	<operator idref="AVMNOperator.$(n)"/>
//	        <operator id='FrequenciesExchangerX.s:$(n)' spec='kernel.BactrianDeltaExchangeOperator' delta="0.01" weight="0.1" parameter="@freqParameter.s:$(n)"/>
//        </operator>

        // they will create AdaptableOperatorSampler later
        context.addBeastObjForOpSamplers(kappa);
        context.addBeastObjForOpSamplers(frequencies);

        return beastHKY;
    }

    @Override
    public Class<HKY> getGeneratorClass() {
        return HKY.class;
    }

    @Override
    public Class<beast.base.evolution.substitutionmodel.HKY> getBEASTClass() {
        return beast.base.evolution.substitutionmodel.HKY.class;
    }
}
