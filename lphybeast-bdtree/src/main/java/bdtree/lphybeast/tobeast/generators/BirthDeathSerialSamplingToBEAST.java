package bdtree.lphybeast.tobeast.generators;

import bdtree.likelihood.BirthDeathSequentialSampling;
import beast.base.core.BEASTInterface;
import beast.base.evolution.tree.MRCAPrior;
import beast.base.evolution.tree.Tree;
import beast.base.inference.Distribution;
import beast.base.spec.type.RealScalar;
import lphy.base.evolution.birthdeath.BirthDeathSerialSamplingTree;
import lphy.core.logger.LoggerUtils;
import lphy.core.model.GenerativeDistribution1D;
import lphy.core.model.Value;
import lphy.core.model.ValueUtils;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;
import lphybeast.tobeast.operators.TreeOperatorStrategy;

public class BirthDeathSerialSamplingToBEAST implements
        GeneratorToBEAST<BirthDeathSerialSamplingTree, BirthDeathSequentialSampling> {

    @Override
    public BirthDeathSequentialSampling generatorToBEAST(BirthDeathSerialSamplingTree generator, BEASTInterface tree, BEASTContext context) {

        BirthDeathSequentialSampling beastBDSS = new BirthDeathSequentialSampling();
        beastBDSS.setInputValue("birthRate", context.getAsRealScalar(generator.getBirthRate()));
        beastBDSS.setInputValue("deathRate", context.getAsRealScalar(generator.getDeathRate()));
        beastBDSS.setInputValue("rho", context.getAsRealScalar(generator.getRho()));
        beastBDSS.setInputValue("psi", context.getAsRealScalar(generator.getPsi()));
        beastBDSS.setInputValue("tree", tree);

        Value<Number> rootAgeVal = generator.getRootAge();
        if (rootAgeVal.getGenerator() != null) {
            BEASTInterface beastRootAge = context.getBEASTObject(rootAgeVal);
            BEASTInterface beastRootAgeGenerator = context.getBEASTObject(rootAgeVal.getGenerator());

            if (beastRootAge instanceof RealScalar && beastRootAgeGenerator instanceof Distribution) {
                Distribution rootAgeDist = (Distribution) beastRootAgeGenerator;

                Double lower = Double.NEGATIVE_INFINITY;
                Double upper = Double.POSITIVE_INFINITY;
                if (rootAgeVal.getGenerator() instanceof GenerativeDistribution1D geneDist1D) {
                    Object[] bounds = geneDist1D.getDomainBounds();
                    if (bounds[0] instanceof Number number)
                        if (number.doubleValue() > lower)
                            lower = number.doubleValue();
                    if (bounds[1] instanceof Number number)
                        if (number.doubleValue() < upper)
                            upper = number.doubleValue();
                }
                if (rootAgeDist instanceof beast.base.spec.inference.distribution.Uniform uniform) {
                    lower = uniform.lowerInput.get().get();
                    upper = uniform.upperInput.get().get();
                } else
                    LoggerUtils.log.warning("Cannot detect lower and upper for the root age of BDSS, " +
                            "set to its distribution domain bounds [" + lower + ", " + upper + "].");
                beastBDSS.setInputValue("lower", lower);
                beastBDSS.setInputValue("upper", upper);

                MRCAPrior prior = new MRCAPrior();
                prior.setInputValue("distr", rootAgeDist);
                prior.setInputValue("tree", tree);
                prior.setInputValue("taxonset", ((Tree) tree).getTaxonset());
                prior.initAndValidate();
                context.addBEASTObject(prior, rootAgeVal.getGenerator());
                context.removeBEASTObject(beastRootAge);
                context.removeBEASTObject(beastRootAgeGenerator);
            } else {
                throw new RuntimeException("Can't map BirthDeathSamplingTree.rootAge prior to tree in BEAST conversion.");
            }

        } else {
            Double rootAge = ValueUtils.doubleValue(rootAgeVal);
            beastBDSS.setInputValue("rootAge", rootAge);

            context.addSkipOperator((Tree) tree);
            context.addExtraOperator(TreeOperatorStrategy.createExchangeOperator((Tree) tree, context, true));
            context.addExtraOperator(TreeOperatorStrategy.createExchangeOperator((Tree) tree, context, false));
            context.addExtraOperator(TreeOperatorStrategy.createTreeUniformOperator((Tree) tree, context));
            context.addExtraOperator(TreeOperatorStrategy.createSubtreeSlideOperator((Tree) tree, context));
            context.addExtraOperator(TreeOperatorStrategy.createWilsonBaldingOperator((Tree) tree, context));
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
