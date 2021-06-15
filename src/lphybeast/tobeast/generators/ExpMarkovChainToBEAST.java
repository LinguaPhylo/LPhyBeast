package lphybeast.tobeast.generators;

import beast.core.BEASTInterface;
import beast.math.distributions.MarkovChainDistribution;
import feast.function.Slice;
import lphy.core.distributions.ExpMarkovChain;
import lphy.graphicalModel.Generator;
import lphy.graphicalModel.GraphicalModelNode;
import lphy.graphicalModel.Value;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;
import lphybeast.SliceFactory;

public class ExpMarkovChainToBEAST implements GeneratorToBEAST<ExpMarkovChain, MarkovChainDistribution> {
    @Override
    public MarkovChainDistribution generatorToBEAST(ExpMarkovChain generator, BEASTInterface value, BEASTContext context) {

        MarkovChainDistribution mcd = new MarkovChainDistribution();
        mcd.setInputValue("shape", 1.0);
        mcd.setInputValue("parameter", value);

        Value<Double> firstValue = generator.getFirstValue();
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
            mcd.setInputValue("initialMean", context.getBEASTObject(generator.getInitialMean()));
        }
        mcd.initAndValidate();

//        Value<Double> initialMean = generator.getInitialMean();
//        GenerativeDistribution initialMeanGenerator = (GenerativeDistribution)initialMean.getGenerator();
//
//        // replace prior on initialMean with excludable prior on the first element of value
//        beast.math.distributions.Prior prior = (beast.math.distributions.Prior)context.getBEASTObject(initialMeanGenerator);
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
