package lphybeast.tobeast.generators;

import bdtree.likelihood.BirthDeathSequentialSampling;
import beast.base.core.BEASTInterface;
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
            throw new UnsupportedOperationException("The development of specifying lower and upper bounds for the root age is still in progress !");

            /*TODO Alexei: what is RootAgeGenerator? I assume you want to assign the lower and upper here
            // https://github.com/fkmendes/bdtree/blob/master/examples/testing/Shankarappa.xml
            BEASTInterface beastRootAgeGenerator = context.getBEASTObject(generator.getRootAge().getGenerator());
//            beastBDSS.setInputValue("lower", lower);
//            beastBDSS.setInputValue("upper", upper);

            if (beastRootAgeGenerator instanceof Prior) {
                Prior rootAgePrior = (Prior) beastRootAgeGenerator;

                MRCAPrior prior = new MRCAPrior();
                prior.setInputValue("distr", rootAgePrior.distInput.get());
                prior.setInputValue("tree", tree);
                prior.setInputValue("taxonset", ((Tree) tree).getTaxonset());
                prior.initAndValidate();
                context.addBEASTObject(prior, generator.getRootAge().getGenerator());
//                context.removeBEASTObject(beastRootAge);
                context.removeBEASTObject(beastRootAgeGenerator);
            } else {
                throw new RuntimeException("Can't map BirthDeathSamplingTree.rootAge prior to tree in BEAST conversion.");
            }*/

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
