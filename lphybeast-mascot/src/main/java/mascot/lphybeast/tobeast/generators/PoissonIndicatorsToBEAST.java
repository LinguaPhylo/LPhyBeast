package mascot.lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.inference.distribution.Prior;
import beast.base.inference.parameter.RealParameter;
import lphy.base.distribution.PoissonIndicators;
import lphy.core.model.Value;
import lphy.core.model.ValueUtils;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

/**
 * Maps {@link PoissonIndicators} to MASCOT's GLM indicator prior structure:
 * <pre>
 *   &lt;prior&gt;
 *     &lt;x spec="beast.base.evolution.Sum"&gt;
 *       &lt;arg idref="indicators"/&gt;
 *     &lt;/x&gt;
 *     &lt;distr spec="beast.base.inference.distribution.Poisson" lambda="λ"/&gt;
 *   &lt;/prior&gt;
 * </pre>
 * The {@code BitFlipOperator} for the BooleanParameter is created automatically
 * by {@code DefaultOperatorStrategy}.
 */
public class PoissonIndicatorsToBEAST implements GeneratorToBEAST<PoissonIndicators, Prior> {

    @Override
    public Prior generatorToBEAST(PoissonIndicators generator, BEASTInterface value, BEASTContext context) {

        Value<Number> lambdaValue = generator.getLambda();
        double lambdaVal = ValueUtils.doubleValue(lambdaValue);

        // Create BEAST Poisson distribution
        beast.base.inference.distribution.Poisson poisson = new beast.base.inference.distribution.Poisson();
        RealParameter lambdaParam = new RealParameter(String.valueOf(lambdaVal));
        lambdaParam.setID(value.getID() + ".poissonLambda");
        lambdaParam.initAndValidate();
        poisson.setInputValue("lambda", lambdaParam);
        poisson.initAndValidate();

        // Create Sum function on the indicator BooleanParameter
        beast.base.evolution.Sum sumFunction = new beast.base.evolution.Sum();
        sumFunction.setInputValue("arg", value);
        sumFunction.setID(value.getID() + ".sumActive");
        sumFunction.initAndValidate();

        // Create Prior wrapping the Poisson distribution and Sum
        Prior prior = BEASTContext.createPrior(poisson, sumFunction);
        prior.setID(value.getID() + ".prior");

        return prior;
    }

    @Override
    public Class<PoissonIndicators> getGeneratorClass() {
        return PoissonIndicators.class;
    }

    @Override
    public Class<Prior> getBEASTClass() {
        return Prior.class;
    }
}
