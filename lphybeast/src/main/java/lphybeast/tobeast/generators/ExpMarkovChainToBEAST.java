package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.inference.Distribution;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.inference.distribution.MarkovChainDistribution;
import beast.base.spec.type.RealVector;
import lphy.base.distribution.ExpMarkovChain;
import lphy.core.model.Generator;
import lphy.core.model.Value;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;
import beast.base.spec.inference.parameter.VectorElement;

import static lphy.base.distribution.ExpMarkovChain.firstValueParamName;
import static lphy.base.distribution.ExpMarkovChain.initialMeanParamName;

public class ExpMarkovChainToBEAST implements GeneratorToBEAST<ExpMarkovChain, Distribution> {
    @Override
    public Distribution generatorToBEAST(ExpMarkovChain generator, BEASTInterface value, BEASTContext context) {

        MarkovChainDistribution mcd = new MarkovChainDistribution();
        mcd.setInputValue("shape", 1.0);
        mcd.setInputValue("param", value);

        Value firstValue = generator.getParams().get(firstValueParamName);
        if (firstValue != null) {
            BEASTInterface firstV = context.getBEASTObject(firstValue);
            // rm firstValue from maps
            context.removeBEASTObject(firstV);

            // create scalar view of element 0
            VectorElement<PositiveReal> element = new VectorElement<>(
                    (RealVector<PositiveReal>) value, 0);
            element.setID(firstValue.getCanonicalId());

            // replace prior's param with scalar view of chain[0]
            Generator dist = firstValue.getGenerator();
            BEASTInterface prior = context.getBEASTObject(dist);
            prior.setInputValue("param", element);
            context.putBEASTObject(dist, prior);

        } else {
            Value initialMean = generator.getParams().get(initialMeanParamName);
            mcd.setInputValue("initialMean", context.getBEASTObject(initialMean));
        }
        mcd.initAndValidate();
        mcd.setID(((BEASTInterface) value).getID() + ".prior");

        return mcd;
    }

    @Override
    public Class<ExpMarkovChain> getGeneratorClass() {
        return ExpMarkovChain.class;
    }

    @Override
    public Class<Distribution> getBEASTClass() {
        return Distribution.class;
    }
}
