//package lphybeast.tobeast.generators;
//
//import beast.base.core.BEASTInterface;
//import beast.base.evolution.alignment.Taxon;
//import beast.base.evolution.alignment.TaxonSet;
//import beast.base.evolution.tree.TraitSet;
//import beast.base.evolution.tree.Tree;
//import beast.util.BEASTVector;
//import lphy.base.evolution.Taxa;
//import lphy.base.evolution.alignment.Alignment;
//import lphy.base.evolution.coalescent.MultispeciesCoalescent;
//import lphy.base.evolution.tree.TimeTree;
//import lphy.core.model.GraphicalModelNode;
//import lphy.core.model.Value;
//import lphybeast.BEASTContext;
//import lphybeast.GeneratorToBEAST;
//import starbeast2.GeneTree;
//import starbeast2.PopulationModel;
//import starbeast2.SpeciesTree;
//import starbeast2.StarBeastTaxonSet;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//
//public class MultispeciesCoalescentToStarBEAST2 implements
//        GeneratorToBEAST<MultispeciesCoalescent, starbeast2.MultispeciesCoalescent> {
//    @Override
//    public starbeast2.MultispeciesCoalescent generatorToBEAST(MultispeciesCoalescent generator, BEASTInterface value, BEASTContext context) {
//
//        starbeast2.MultispeciesCoalescent starbeast = new starbeast2.MultispeciesCoalescent();
//
//        List<Tree> geneTrees = new ArrayList<>();
//
//        if (value == null) {
//            // multiple tree scenario
//            Value v = context.getOutput(generator);
//            if (v.value() instanceof TimeTree[]) {
//
//                TimeTree[] timeTrees = (TimeTree[]) v.value();
//                for (int i = 0; i < timeTrees.length; i++) {
//                    String id = v.getId() + "." + i;
//                    geneTrees.add((Tree) context.getBEASTObject(id));
//                }
//            }
//        } else if (value instanceof Tree) {
//            geneTrees.add((Tree) value);
//        } else if (value instanceof BEASTVector) {
//            for (BEASTInterface beastInterface : ((BEASTVector)value).getObjectList()) {
//                if (beastInterface instanceof Tree) {
//                    geneTrees.add((Tree)beastInterface);
//                }
//            }
//        } else throw new IllegalArgumentException();
//
//        SpeciesTree speciesTree = (SpeciesTree) context.getBEASTObject(generator.getSpeciesTree());
//        StarBeastTaxonSet starBeastTaxonSet = (StarBeastTaxonSet) speciesTree.getTaxonset();
//
//        starbeast2.ConstantPopulations constantPopulations = new starbeast2.ConstantPopulations();
//        constantPopulations.setInputValue("speciesTree", speciesTree);
//        constantPopulations.setInputValue("populationSizes", context.getBEASTObject(generator.getPopulationSizes()));
//        constantPopulations.initAndValidate();
//
//        List<GeneTree> geneTreeDists = new ArrayList<>();
//
//        for (Tree geneTree : geneTrees) {
//
//            starbeast2.GeneTree starbeast2GeneTree = new starbeast2.GeneTree();
//            starbeast2GeneTree.setInputValue("speciesTree", speciesTree);
//            starbeast2GeneTree.setInputValue("tree", geneTree);
//            starbeast2GeneTree.setInputValue("populationModel", constantPopulations);
//            starbeast2GeneTree.initAndValidate();
//            geneTreeDists.add(starbeast2GeneTree);
//            starbeast.setInputValue("distribution", starbeast2GeneTree);
//        }
//
//        starbeast.initAndValidate();
//
//        Value<TimeTree> timeTreeValue = generator.getSpeciesTree();
//
//        starbeast2.StarBeastInitializer starBeastInitializer = createStarBEASTInitializer(timeTreeValue.value(), speciesTree, geneTrees, constantPopulations);
//        starBeastInitializer.setID("SBI");
//
//        context.addInit(starBeastInitializer);
//
//        addOperators(starBeastTaxonSet, speciesTree, geneTrees, geneTreeDists, context);
//
//        return starbeast;
//    }
//
//    public void modifyBEASTValues(MultispeciesCoalescent generator, BEASTInterface value, BEASTContext context) {
//        Tree vanillaSpeciesTree = (Tree) context.getBEASTObject(generator.getSpeciesTree());
//        starbeast2.StarBeastTaxonSet starBeastTaxonSet = createStarBeastTaxonSet(
//                generator.getSpeciesTree().value().getTaxa(),
//                generator.getGeneTreeTaxa(), context);
//
//        TraitSet traitSet = createTraitSet(generator.getSpeciesTree().value(), starBeastTaxonSet);
//
//        SpeciesTree speciesTree = convertToStarBEASTSpeciesTree(vanillaSpeciesTree, traitSet, starBeastTaxonSet);
//        speciesTree.setID(vanillaSpeciesTree.getID());
//        // replace species tree in context with newly converted tree so that tree operators are attached to the correct tree.
//        context.removeBEASTObject(vanillaSpeciesTree);
//        /** call {@link BEASTContext#addToContext(GraphicalModelNode, BEASTInterface)} **/
//        context.putBEASTObject(generator.getSpeciesTree(), speciesTree);
//    }
//
//    private starbeast2.StarBeastInitializer createStarBEASTInitializer(TimeTree speciesTree, SpeciesTree tree, List<Tree> geneTree, PopulationModel populationModel) {
//
//        starbeast2.StarBeastInitializer starBeastInitializer = new starbeast2.StarBeastInitializer();
//        starBeastInitializer.setInputValue("speciesTree", tree);
//        starBeastInitializer.setInputValue("geneTree", geneTree);
//        starBeastInitializer.setInputValue("newick", speciesTree.toNewick(false));
//        starBeastInitializer.setInputValue("estimate", false);
//        starBeastInitializer.setInputValue("populationModel", populationModel);
//        starBeastInitializer.initAndValidate();
//
//        return starBeastInitializer;
//
//    }
//
//    private void addOperators(starbeast2.StarBeastTaxonSet starBeastTaxonSet, SpeciesTree tree, List<Tree> geneTree, List<GeneTree> geneTreeDists, BEASTContext context) {
//
//        int totalNodeCount = tree.getInternalNodeCount();
//        for (Tree gTree : geneTree) {
//            totalNodeCount += gTree.getInternalNodeCount();
//        }
//
//        starbeast2.NodeReheight2 nodeReheight2 = new starbeast2.NodeReheight2();
//        nodeReheight2.setInputValue("taxonset", starBeastTaxonSet);
//        nodeReheight2.setInputValue("tree", tree);
//        nodeReheight2.setInputValue("geneTree", geneTreeDists);
//        nodeReheight2.setInputValue("weight", BEASTContext.getOperatorWeight(totalNodeCount));
//        nodeReheight2.initAndValidate();
//        nodeReheight2.setID(tree.getID() + ".nodeReheight2");
//
//        context.addExtraOperator(nodeReheight2);
//
//        starbeast2.CoordinatedUniform coordinatedUniform = new starbeast2.CoordinatedUniform();
//        coordinatedUniform.setInputValue("speciesTree", tree);
//        coordinatedUniform.setInputValue("geneTree", geneTree);
//        coordinatedUniform.setInputValue("weight", BEASTContext.getOperatorWeight(totalNodeCount));
//        coordinatedUniform.initAndValidate();
//        coordinatedUniform.setID(tree.getID() + ".coordinatedUniform");
//
//        context.addExtraOperator(coordinatedUniform);
//
////        starbeast2.CoordinatedExponential coordinatedExponential = new starbeast2.CoordinatedExponential();
////        coordinatedExponential.setInputValue("speciesTree", tree);
////        coordinatedExponential.setInputValue("geneTree", geneTree);
////        coordinatedExponential.setInputValue("weight", BEASTContext.getOperatorWeight(totalNodeCount));
////        coordinatedExponential.initAndValidate();
////        coordinatedExponential.setID(tree.getID()+".coordinatedExponential");
////
////        context.addExtraOperator(coordinatedExponential);
//    }
//
//    /**
//     * @param speciesTaxa a taxa object containing the species taxa
//     * @param geneTaxa    a taxa object containing the individual taxa in gene tree and associated species.
//     * @return a taxonsuperset using information from the species taxa and gene taxa provided.
//     */
//    private starbeast2.StarBeastTaxonSet createStarBeastTaxonSet(Taxa speciesTaxa, Taxa geneTaxa, BEASTContext context) {
//
//        List<String> speciesTaxaNames = Arrays.asList(speciesTaxa.getTaxaNames());
//
//        List<Value<Alignment>> alignments = context.getAlignments();
//
//        Value<lphy.base.evolution.alignment.Alignment> a = null;
//        for (Value<lphy.base.evolution.alignment.Alignment> candidate : alignments) {
//            List<String> taxaNames = Arrays.asList(candidate.value().getTaxa().getTaxaNames());
//            if (taxaNames.containsAll(speciesTaxaNames)) {
//                a = candidate;
//                break;
//            }
//        }
//
//        // This is the mapping from gene tree taxa to species tree taxa
//        starbeast2.StarBeastTaxonSet sbtaxonSuperSet = new starbeast2.StarBeastTaxonSet();
//        List<Taxon> spTaxonSets = new ArrayList<>();
//        for (String speciesId : speciesTaxa.getTaxaNames()) {
//
//            TaxonSet spTaxonSet = new TaxonSet();
//            List<Taxon> geneTaxonList = new ArrayList<>();
//            for (lphy.base.evolution.Taxon taxon : geneTaxa.getTaxonArray()) {
//
//                if (taxon.getSpecies().equals(speciesId)) {
//                    Taxon geneTaxon = context.getTaxon(taxon.getName());
//                    geneTaxonList.add(geneTaxon);
//                    System.out.println("  taxon " + taxon.getName());
//                }
//            }
//            if (geneTaxonList.size() > 0) {
//                spTaxonSet.setInputValue("taxon", geneTaxonList);
//                spTaxonSet.initAndValidate();
//                spTaxonSet.setID(speciesId);
//                spTaxonSets.add(spTaxonSet);
//            }
//        }
//
//        sbtaxonSuperSet.setInputValue("taxon", spTaxonSets);
//        if (a != null) {
//            sbtaxonSuperSet.setInputValue("alignment", context.getBEASTObject(a));
//        }
//        sbtaxonSuperSet.initAndValidate();
//        return sbtaxonSuperSet;
//    }
//
//    /**
//     * Takes a BEAST tree and, if necessary, converts it to a starbeast2.SpeciesTree
//     *
//     * @param tree
//     * @return
//     */
//    private starbeast2.SpeciesTree convertToStarBEASTSpeciesTree(Tree tree, TraitSet traitSet, TaxonSet taxonSet) {
//
//        tree.m_taxonset.set(taxonSet);
//
//        // TODO might need to update taxon set for each new gene tree
//        if (tree instanceof SpeciesTree) return (SpeciesTree) tree;
//
//        SpeciesTree speciesTree = new SpeciesTree();
//        //speciesTree.setInputValue("initial", tree);
//        speciesTree.setInputValue("trait", traitSet);
//        speciesTree.setInputValue("taxonset", taxonSet);
//        speciesTree.initAndValidate();
//
//        return speciesTree;
//    }
//
//    private TraitSet createTraitSet(TimeTree tree, TaxonSet taxonSuperSet) {
//
//        StringBuilder builder = new StringBuilder();
//        lphy.base.evolution.Taxon[] taxonArray = tree.getTaxa().getTaxonArray();
//        builder.append(taxonArray[0].getName());
//        builder.append("=");
//        builder.append(taxonArray[0].getAge());
//
//        for (int i = 1; i < taxonArray.length; i++) {
//            builder.append(",\n");
//            builder.append(taxonArray[i].getName());
//            builder.append("=");
//            builder.append(taxonArray[i].getAge());
//        }
//        builder.append("\n");
//        String traitValueString = builder.toString();
//
//        TraitSet traitSet = new TraitSet();
//        traitSet.setInputValue("traitname", "date-backward");
//        traitSet.setInputValue("value", traitValueString);
//        traitSet.setInputValue("taxa", taxonSuperSet);
//        traitSet.initAndValidate();
//
//        return traitSet;
//    }
//
//    @Override
//    public Class<MultispeciesCoalescent> getGeneratorClass() {
//        return MultispeciesCoalescent.class;
//    }
//
//    @Override
//    public Class<starbeast2.MultispeciesCoalescent> getBEASTClass() {
//        return starbeast2.MultispeciesCoalescent.class;
//    }
//}
