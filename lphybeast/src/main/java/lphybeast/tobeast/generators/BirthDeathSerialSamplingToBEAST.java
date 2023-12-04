package lphybeast.tobeast.generators;

import bdtree.likelihood.BirthDeathSequentialSampling;
import beast.base.core.BEASTInterface;
import beast.base.evolution.tree.MRCAPrior;
import beast.base.evolution.tree.Tree;
import beast.base.inference.distribution.ParametricDistribution;
import beast.base.inference.distribution.Prior;
import beast.base.inference.distribution.Uniform;
import beast.base.inference.parameter.RealParameter;
import lphy.base.evolution.birthdeath.BirthDeathSerialSamplingTree;
import lphy.core.logger.LoggerUtils;
import lphy.core.model.GenerativeDistribution1D;
import lphy.core.model.Value;
import lphy.core.model.ValueUtils;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;
import lphybeast.tobeast.operators.OperatorFactory;

// bdtree (https://github.com/fkmendes/bdtree) is not released as a BEAST2 package,
// but the XML created by this class can run if lphybeast package is installed and loaded by BEAST 2 ClassLoader.
public class BirthDeathSerialSamplingToBEAST implements
        GeneratorToBEAST<BirthDeathSerialSamplingTree, BirthDeathSequentialSampling> {

    @Override
    public BirthDeathSequentialSampling generatorToBEAST(BirthDeathSerialSamplingTree generator, BEASTInterface tree, BEASTContext context) {

        BirthDeathSequentialSampling beastBDSS = new BirthDeathSequentialSampling();
        beastBDSS.setInputValue("birthRate", context.getAsRealParameter(generator.getBirthRate()));
        beastBDSS.setInputValue("deathRate", context.getAsRealParameter(generator.getDeathRate()));
        beastBDSS.setInputValue("rho", context.getAsRealParameter(generator.getRho()));
        beastBDSS.setInputValue("psi", context.getAsRealParameter(generator.getPsi()));
        beastBDSS.setInputValue("tree", tree);

        // 2 scenarios : 1) root age; 2) root age lower and upper bound.
        // Both cannot use getAsRealParameter which will create scale op.
//        Input<Double> rootAgeLowerInput; Input<Double> rootAgeUpperInput;
//        Input<Double> rootAgeInput;
        Value<Number> rootAgeVal = generator.getRootAge();
        if (rootAgeVal.getGenerator() != null) {
            // BirthDeathSequentialSampling requires either set rootAge or both the lower and upper in init,
            // otherwise initAndValidate will throw err

            BEASTInterface beastRootAgeGenerator = context.getBEASTObject(rootAgeVal.getGenerator());
            if (beastRootAgeGenerator instanceof Prior) {
                Prior rootAgePrior = (Prior) beastRootAgeGenerator;
                ParametricDistribution dist = rootAgePrior.distInput.get();

                Double lower = Double.NEGATIVE_INFINITY;
                Double upper = Double.POSITIVE_INFINITY;
                // first check domain bounds
                if (rootAgeVal.getGenerator() instanceof GenerativeDistribution1D geneDist1D) {
                    Object[] bounds = geneDist1D.getDomainBounds();
                    if (bounds[0] instanceof Number number)
                        if (number.doubleValue() > lower)
                            lower = number.doubleValue();
                    if (bounds[1] instanceof Number number)
                        if (number.doubleValue() < upper)
                            upper = number.doubleValue();
                }
                //TODO then, only Uniform has lower upper
                if (dist instanceof Uniform uniform) {
                    lower = uniform.lowerInput.get();
                    upper = uniform.upperInput.get();
                } else
                    LoggerUtils.log.warning("Cannot detect lower and upper for the root age of BDSS, " +
                            "set to its distribution domain bounds [" + lower + ", " + upper + "].");
                // https://github.com/fkmendes/bdtree/blob/master/examples/testing/Shankarappa.xml
                // cannot set rootAge, which will ignore tree op
                // setting rootAge seems easier than setting bound, because the generator may not provide lower and upper to beastRootAge
                beastBDSS.setInputValue("lower", lower);
                beastBDSS.setInputValue("upper", upper);

                //TODO why create MRCAPrior here? BirthDeathSequentialSampling has no input of MRCAPrior.
                MRCAPrior prior = new MRCAPrior();
                prior.setInputValue("distr", dist);
                prior.setInputValue("tree", tree);
                prior.setInputValue("taxonset", ((Tree) tree).getTaxonset());
                prior.initAndValidate();
                context.addBEASTObject(prior, generator.getRootAge().getGenerator());

                RealParameter beastRootAge = context.getAsRealParameter(generator.getRootAge());
                context.removeBEASTObject(beastRootAge);
                context.removeBEASTObject(beastRootAgeGenerator);
            } else {
                throw new RuntimeException("Can't map BirthDeathSamplingTree.rootAge prior to tree in BEAST conversion.");
            }

        } else {
            // https://github.com/fkmendes/bdtree/blob/master/examples/testing/BDSSLikelihood.xml
            Double rootAge = ValueUtils.doubleValue(rootAgeVal);
            beastBDSS.setInputValue("rootAge", rootAge);

            // rm all tree op
            context.addSkipOperator((Tree) tree);
            // add required ones back
            context.addExtraOperator(OperatorFactory.createExchangeOperator((Tree) tree, context, true));
            context.addExtraOperator(OperatorFactory.createExchangeOperator((Tree) tree, context, false));
            context.addExtraOperator(OperatorFactory.createTreeUniformOperator((Tree) tree, context));
            context.addExtraOperator(OperatorFactory.createSubtreeSlideOperator((Tree) tree, context));
            context.addExtraOperator(OperatorFactory.createWilsonBaldingOperator((Tree) tree, context));
        }

        beastBDSS.initAndValidate();

        return beastBDSS;
    }

    @Override
    public Class<BirthDeathSerialSamplingTree> getGeneratorClass() {
        return BirthDeathSerialSamplingTree.class;
    }

    @Override
    public Class<BirthDeathSequentialSampling> getBEASTClass() {
        return BirthDeathSequentialSampling.class;
    }
}
