package lphybeast.spi;

import beast.base.inference.CompoundDistribution;
import beast.base.inference.Logger;
import beast.base.inference.MCMC;
import beast.base.inference.Operator;
import beast.base.inference.State;
import beast.base.inference.StateNodeInitialiser;
import lphybeast.LPhyBeastConfig;

import java.util.List;

/**
 * Default strategy that creates a standard BEAST 2 MCMC run element.
 */
public class DefaultMCMCStrategy implements MCMCStrategy {

    @Override
    public boolean isMC3() {
        return false;
    }

    @Override
    public MCMC createRun(CompoundDistribution posterior, List<Operator> operators,
                          List<Logger> loggers, State state, List<StateNodeInitialiser> inits,
                          long chainLength, int preBurnin, boolean sampleFromPrior,
                          LPhyBeastConfig config) {

        MCMC mcmc = new MCMC();
        mcmc.setInputValue("distribution", posterior);
        mcmc.setInputValue("chainLength", chainLength);

        for (int i = 0; i < operators.size(); i++) {
            System.out.println(operators.get(i));
        }
        mcmc.setInputValue("operator", operators);
        mcmc.setInputValue("logger", loggers);
        mcmc.setInputValue("state", state);

        if (!inits.isEmpty()) mcmc.setInputValue("init", inits);

        if (preBurnin > 0)
            mcmc.setInputValue("preBurnin", preBurnin);

        mcmc.initAndValidate();
        if (sampleFromPrior) {
            mcmc.setInputValue("sampleFromPrior", sampleFromPrior);
        }

        return mcmc;
    }
}
