package lphybeast.tobeast.generators;

import beast.core.BEASTInterface;
import beast.core.parameter.RealParameter;
import beast.evolution.datatype.DataType;
import beast.evolution.errormodel.ErrorModel;
import beast.evolution.likelihood.TreeLikelihoodWithError;
import beast.evolution.likelihood.TreeLikelihoodWithErrorSlow;
import beast.evolution.sitemodel.SiteModel;
import lphy.evolution.alignment.Alignment;
import lphy.evolution.alignment.GT16ErrorModel;
import lphy.evolution.likelihood.PhyloCTMC;
import lphy.graphicalModel.GraphicalModelNode;
import lphy.graphicalModel.Value;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

/**
 * This has to create TreeLikelihood
 * @author Walter Xie
 */
public class GT16ErrorModelToBEAST implements GeneratorToBEAST<GT16ErrorModel, TreeLikelihoodWithError>  {

    @Override
    public TreeLikelihoodWithError generatorToBEAST(GT16ErrorModel generator, BEASTInterface value, BEASTContext context) {

        assert value instanceof beast.evolution.alignment.Alignment;
        beast.evolution.alignment.Alignment errAlignment = (beast.evolution.alignment.Alignment)value;

        // the allelic drop out probability
        double delta = generator.getDelta();
        // the sequencing/amplification error rate
        double epsilon = generator.getEpsilon();

        beast.evolution.errormodel.GT16ErrorModel gt16ErrorModel = new beast.evolution.errormodel.GT16ErrorModel();

        DataType beastDataType = errAlignment.getDataType();
        // Input<DataType> datatypeInput
        gt16ErrorModel.setInputValue("datatype", beastDataType);

        RealParameter deltaParam = new RealParameter(String.valueOf(delta));
        gt16ErrorModel.setInputValue("delta", deltaParam);
        RealParameter epsilonParam = new RealParameter(String.valueOf(epsilon));
        gt16ErrorModel.setInputValue("epsilon", epsilonParam);

        gt16ErrorModel.initAndValidate();


        // TODO temp solution to rm parent alignment if there is a child alignment created from it,
        // e.g. original alignment creates err alignment

        // A ~ PhyloCTMC(); E ~ ErrorModel(A);
        PhyloCTMC phyloCTMC = null;
        Value<Alignment> origAlignmentInput = null;
        for (GraphicalModelNode<?> input : generator.getInputs()) {
            if (input instanceof Value && input.value() instanceof Alignment) {
                origAlignmentInput = (Value<Alignment>) input;
                phyloCTMC = (PhyloCTMC) origAlignmentInput.getGenerator();
                break;
            }
        }

        // TODO not working for additional  D = unphase(E);
        if (phyloCTMC == null)
            throw new IllegalArgumentException("Cannot find err alignment and PhyloCTMC !");

        TreeLikelihoodWithError treeLikelihoodWithError =
                getTreeLikelihoodWithError(errAlignment, gt16ErrorModel, phyloCTMC, context);
        // logging
        context.addExtraLogger(treeLikelihoodWithError);

        removeOriginalTreeLikelihood(origAlignmentInput, phyloCTMC, context);

        return treeLikelihoodWithError;
    }

    private void removeOriginalTreeLikelihood(Value<Alignment> origAlignmentInput, PhyloCTMC phyloCTMC, BEASTContext context) {
        BEASTInterface beastOrigAlignment = context.getBEASTObject(origAlignmentInput);
        context.removeBEASTObject(beastOrigAlignment);

        BEASTInterface treeLikelihood = context.getBEASTObject(phyloCTMC);
        context.removeBEASTObject(treeLikelihood);

    }


    private TreeLikelihoodWithError getTreeLikelihoodWithError(beast.evolution.alignment.Alignment errAlignment,
                                                               ErrorModel errorModel, PhyloCTMC phyloCTMC, BEASTContext context) {
        //TODO why TreeLikelihoodWithErrorSlow?
        TreeLikelihoodWithErrorSlow treeLikelihoodWithError = new TreeLikelihoodWithErrorSlow();

        treeLikelihoodWithError.setInputValue("data", errAlignment);

        PhyloCTMCToBEAST.constructTreeAndBranchRate(phyloCTMC, treeLikelihoodWithError, context);

        SiteModel siteModel = PhyloCTMCToBEAST.constructSiteModel(phyloCTMC, context);
        treeLikelihoodWithError.setInputValue("siteModel", siteModel);

        treeLikelihoodWithError.setInputValue("errorModel", errorModel);
        // TODO use tip ambiguities from data
        treeLikelihoodWithError.setInputValue("useTipsEmpirical", false);

        treeLikelihoodWithError.initAndValidate();
        treeLikelihoodWithError.setID(errAlignment.getID() + ".treeLikelihood");

        return treeLikelihoodWithError;
    }



    @Override
    public Class<GT16ErrorModel> getGeneratorClass() {
        return GT16ErrorModel.class;
    }

    @Override
    public Class<TreeLikelihoodWithError> getBEASTClass() {
        return TreeLikelihoodWithError.class;
    }
}
