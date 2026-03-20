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
 * Strategy for creating the MCMC run element.
 * The default creates standard MCMC; extensions can provide
 * alternative run elements (e.g., CoupledMCMC for MC3).
 */
public interface MCMCStrategy {

    /**
     * @return true if this strategy requires specific config (e.g., MC3 params)
     */
    boolean isMC3();

    /**
     * Create and configure the MCMC (or MC3, etc.) run object.
     *
     * @param posterior      the posterior distribution
     * @param operators      the list of operators
     * @param loggers        the list of loggers
     * @param state          the MCMC state
     * @param inits          state node initialisers
     * @param chainLength    MCMC chain length
     * @param preBurnin      pre-burnin steps
     * @param sampleFromPrior whether to sample from prior
     * @param config         LPhyBeast configuration
     * @return the configured MCMC run object
     */
    MCMC createRun(CompoundDistribution posterior, List<Operator> operators,
                   List<Logger> loggers, State state, List<StateNodeInitialiser> inits,
                   long chainLength, int preBurnin, boolean sampleFromPrior,
                   LPhyBeastConfig config);
}
