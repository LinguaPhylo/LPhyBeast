package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import lphy.core.functions.alignment.Simulate;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

@Deprecated
public class SimulateToAlignments implements GeneratorToBEAST<Simulate, BEASTInterface> {


    @Override
    public BEASTInterface generatorToBEAST(Simulate generator, BEASTInterface value, BEASTContext context) {

//        Map<String, Object> simResMap = generator.getSimResMap();
//
//        Map<String, Alignment> intermediateAlignments = new HashMap<>();
//        //TODO just alignment, what about tree
//        for (Map.Entry<String, Object> entry : simResMap.entrySet()) {
//            if (entry.getValue() instanceof Alignment alignment)
//                intermediateAlignments.put(entry.getKey(), alignment);
//        }
//
//        for (Map.Entry<String, Alignment> entry : intermediateAlignments.entrySet()) {
//            if (context.getLPhyBeastConfig().logAllAlignments) {//TODO
//                // limit to SimpleAlignment
//                if (entry.getValue() instanceof SimpleAlignment simpleAlignment) {
//                    Value<SimpleAlignment> algValue =
//                            new Value<>(entry.getKey(), simpleAlignment);
//                    // ValueToBEAST<SimpleAlignment, beast.base.evolution.alignment.Alignment>
//                    ValueToBEAST toBEAST = context.getMatchingValueToBEAST(algValue);
//
//                    if (toBEAST != null && toBEAST instanceof AlignmentToBEAST alignmentToBEAST) {
//                        beast.base.evolution.alignment.Alignment beastAlg =
//                                alignmentToBEAST.valueToBEAST(algValue, context);
//                        String algXML = new XMLProducer().toXML(beastAlg);
//                        // add replicate prefix
//                        String outFile = context.getLPhyBeastConfig().getXMLFilePath(entry.getKey());
//                        try (PrintWriter out = new PrintWriter(outFile)) {
//                            out.println(algXML);
//                        } catch (FileNotFoundException e) {
//                            throw new RuntimeException(e);
//                        }
//                        LoggerUtils.log.info("Log intermediate alignment " + entry.getValue() +
//                                " to " + outFile);
//                    }
//
//                } else LoggerUtils.log.warning("Skip alignment " + entry.getValue() +
//                        " as only SimpleAlignment is processed !");
//            }
//        } // end for
        return null;
    }

    @Override
    public Class<Simulate> getGeneratorClass() {
        return Simulate.class;
    }
}
