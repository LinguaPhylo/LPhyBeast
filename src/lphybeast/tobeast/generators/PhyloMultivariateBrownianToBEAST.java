//package lphybeast.tobeast.generators;
//
//import beast.core.BEASTInterface;
//import lphybeast.BEASTContext;
//import lphybeast.GeneratorToBEAST;
//
//public class PhyloMultivariateBrownianToBEAST implements GeneratorToBEAST<lphy.evolution.continuous.PhyloMultivariateBrownian, multivariatelikelihood.BMPCMShrinkageLikelihood> {
//
//    public multivariatelikelihood.BMPCMShrinkageLikelihood generatorToBEAST(lphy.evolution.continuous.PhyloMultivariateBrownian phyloMultivariateBrownian, BEASTInterface value, BEASTContext context) {
//
//        multivariatelikelihood.BMPCMShrinkageLikelihood treeLikelihood = new multivariatelikelihood.BMPCMShrinkageLikelihood();
//
//        // BEASTInterface value is a RealParameter with keys = taxa names and minordimension = nchar and dimension = ntaxa * nchar
//
//        // TODO setInputValues on treeLikelihood
//        // set tree and so forth as with PhyloCTMCToBEAST
//
//        // TODO call initAndValidate on treeLikelihood
//        // set id
//
//        return treeLikelihood;
//    }
//
//
//    @Override
//    public Class<lphy.evolution.continuous.PhyloMultivariateBrownian> getGeneratorClass() {
//        return lphy.evolution.continuous.PhyloMultivariateBrownian.class;
//    }
//
//    @Override
//    public Class<multivariatelikelihood.BMPCMShrinkageLikelihood> getBEASTClass() {
//        return multivariatelikelihood.BMPCMShrinkageLikelihood.class;
//    }
//}
