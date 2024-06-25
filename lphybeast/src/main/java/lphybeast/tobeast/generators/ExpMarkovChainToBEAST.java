package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.inference.distribution.MarkovChainDistribution;
import beastlabs.core.util.Slice;
import lphy.base.distribution.ExpMarkovChain;
import lphy.core.model.Generator;
import lphy.core.model.GraphicalModelNode;
import lphy.core.model.Value;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;
import lphybeast.SliceFactory;

import static lphy.base.distribution.ExpMarkovChain.firstValueParamName;
import static lphy.base.distribution.ExpMarkovChain.initialMeanParamName;

public class ExpMarkovChainToBEAST implements GeneratorToBEAST<ExpMarkovChain, MarkovChainDistribution> {
    @Override
    public MarkovChainDistribution generatorToBEAST(ExpMarkovChain generator, BEASTInterface value, BEASTContext context) {

        MarkovChainDistribution mcd = new MarkovChainDistribution();
        mcd.setInputValue("shape", 1.0);
        mcd.setInputValue("parameter", value);

        Value firstValue = generator.getParams().get(firstValueParamName);
        if (firstValue != null) {
            BEASTInterface firstV = context.getBEASTObject(firstValue);
            // rm firstValue from maps
            context.removeBEASTObject(firstV);

            // create theta[0]
            Slice feastSlice = SliceFactory.createSlice(value,0, firstValue.getCanonicalId());

            // replace Prior x = theta[0]
            Generator dist = firstValue.getGenerator();
            BEASTInterface prior = context.getBEASTObject(dist);
            prior.setInputValue("x", feastSlice);
            /** call {@link BEASTContext#addToContext(GraphicalModelNode, BEASTInterface)} **/
            context.putBEASTObject(dist, prior);

        } else {
            Value initialMean = generator.getParams().get(initialMeanParamName);
            mcd.setInputValue("initialMean", context.getBEASTObject(initialMean));
        }
        mcd.initAndValidate();

//        Value<Double> initialMean = generator.getInitialMean();
//        GenerativeDistribution initialMeanGenerator = (GenerativeDistribution)initialMean.getGenerator();
//
//        // replace prior on initialMean with excludable prior on the first element of value
//        beast.base.inference.distribution.Prior prior = (beast.base.inference.distribution.Prior)context.getBEASTObject(initialMeanGenerator);
//
//        ExcludablePrior excludablePrior = new ExcludablePrior();
//        BooleanParameter include = new BooleanParameter();
//        List<Boolean> includeList = new ArrayList<>();
//        int n = generator.getN().value();
//        includeList.add(true);
//        for (int i = 1; i < n; i++) {
//            includeList.add(false);
//        }
//        include.setInputValue("value", includeList);
//        include.setInputValue("dimension", n);
//        include.initAndValidate();
//        excludablePrior.setInputValue("xInclude", include);
//        excludablePrior.setInputValue("x", value);
//        excludablePrior.setInputValue("distr",prior.distInput.get());
//        excludablePrior.initAndValidate();
//
//        context.putBEASTObject(initialMeanGenerator, excludablePrior);

        return mcd;
    }

    @Override
    public Class<ExpMarkovChain> getGeneratorClass() {
        return ExpMarkovChain.class;
    }

    @Override
    public Class<MarkovChainDistribution> getBEASTClass() {
        return MarkovChainDistribution.class;
    }
}
