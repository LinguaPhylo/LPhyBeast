package lphybeast.tobeast.generators;

import bdtree.likelihood.BirthDeathSequentialSampling;
import beast.base.core.BEASTInterface;
import beast.base.evolution.tree.MRCAPrior;
import beast.base.evolution.tree.Tree;
import beast.base.inference.distribution.Prior;
import beast.base.inference.parameter.RealParameter;
import lphy.evolution.birthdeath.BirthDeathSerialSamplingTree;
import lphy.graphicalModel.Value;
import lphy.graphicalModel.ValueUtils;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

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
            // https://github.com/fkmendes/bdtree/blob/master/examples/testing/Shankarappa.xml
            RealParameter beastRootAge = context.getAsRealParameter(generator.getRootAge());
            // this should be only for init, it must have operator to sample from dist
            beastBDSS.setInputValue("rootAge", beastRootAge.getValue());
// setting rootAge seems easier than setting bound, because the generator may not provide lower and upper to beastRootAge
//            beastBDSS.setInputValue("lower", lower);
//            beastBDSS.setInputValue("upper", upper);

            BEASTInterface beastRootAgeGenerator = context.getBEASTObject(generator.getRootAge().getGenerator());
            if (beastRootAgeGenerator instanceof Prior) {
                Prior rootAgePrior = (Prior) beastRootAgeGenerator;

                MRCAPrior prior = new MRCAPrior();
                prior.setInputValue("distr", rootAgePrior.distInput.get());
                prior.setInputValue("tree", tree);
                prior.setInputValue("taxonset", ((Tree) tree).getTaxonset());
                prior.initAndValidate();
                context.addBEASTObject(prior, generator.getRootAge().getGenerator());
                context.removeBEASTObject(beastRootAge);
                context.removeBEASTObject(beastRootAgeGenerator);
            } else {
                throw new RuntimeException("Can't map BirthDeathSamplingTree.rootAge prior to tree in BEAST conversion.");
            }

        } else {
            // https://github.com/fkmendes/bdtree/blob/master/examples/testing/BDSSLikelihood.xml
            Double rootAge = ValueUtils.doubleValue(rootAgeVal);
            beastBDSS.setInputValue("rootAge", rootAge);
            //TODO how to remove tree root op: spec="ScaleOperator" rootOnly="true" tree="@psi"
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
