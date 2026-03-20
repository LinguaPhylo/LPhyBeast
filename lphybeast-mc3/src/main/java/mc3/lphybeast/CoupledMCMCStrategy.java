package mc3.lphybeast;

import beast.base.inference.CompoundDistribution;
import beast.base.inference.Logger;
import beast.base.inference.MCMC;
import beast.base.inference.Operator;
import beast.base.inference.State;
import beast.base.inference.StateNodeInitialiser;
import coupledMCMC.CoupledMCMC;
import lphybeast.LPhyBeastConfig;
import lphybeast.spi.MCMCStrategy;

import java.util.List;

/**
 * MCMCStrategy that creates a CoupledMCMC (MC3) run element
 * with multiple chains at different temperatures.
 */
public class CoupledMCMCStrategy implements MCMCStrategy {

    @Override
    public boolean isMC3() {
        return true;
    }

    @Override
    public MCMC createRun(CompoundDistribution posterior, List<Operator> operators,
                          List<Logger> loggers, State state, List<StateNodeInitialiser> inits,
                          long chainLength, int preBurnin, boolean sampleFromPrior,
                          LPhyBeastConfig config) {

        CoupledMCMC mc3 = new CoupledMCMC();
        mc3.setID("mcmcmc");

        mc3.setInputValue("distribution", posterior);
        mc3.setInputValue("chainLength", chainLength);

        // MC3-specific inputs
        mc3.setInputValue("chains", config.getChains());
        mc3.setInputValue("deltaTemperature", config.getDeltaTemperature());
        mc3.setInputValue("resampleEvery", config.getResampleEvery());
        mc3.setInputValue("target", config.getTarget());

        for (int i = 0; i < operators.size(); i++) {
            System.out.println(operators.get(i));
        }
        mc3.setInputValue("operator", operators);
        mc3.setInputValue("logger", loggers);
        mc3.setInputValue("state", state);

        if (!inits.isEmpty()) mc3.setInputValue("init", inits);

        if (preBurnin > 0)
            mc3.setInputValue("preBurnin", preBurnin);

        mc3.initAndValidate();
        return mc3;
    }
}
