package lphybeast.tobeast.generators;

import beast.core.BEASTInterface;
import beast.util.XMLProducer;
import lphy.core.functions.alignment.Simulate;
import lphy.evolution.alignment.Alignment;
import lphy.evolution.alignment.SimpleAlignment;
import lphy.graphicalModel.Value;
import lphy.util.LoggerUtils;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;
import lphybeast.ValueToBEAST;
import lphybeast.tobeast.values.AlignmentToBEAST;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Map;

public class SimulateToAlignments implements GeneratorToBEAST<Simulate, BEASTInterface> {


    @Override
    public BEASTInterface generatorToBEAST(Simulate generator, BEASTInterface value, BEASTContext context) {

        Map<String, Alignment> intermediateAlignments = generator.getIntermediateAlignments();

        for (Map.Entry<String, Alignment> entry : intermediateAlignments.entrySet()) {
            if (true) {//TODO
                // limit to SimpleAlignment
                if (entry.getValue() instanceof SimpleAlignment simpleAlignment) {
                    Value<SimpleAlignment> algValue =
                            new Value<>(entry.getKey(), simpleAlignment);
                    // ValueToBEAST<SimpleAlignment, beast.evolution.alignment.Alignment>
                    ValueToBEAST toBEAST = context.getMatchingValueToBEAST(algValue);

                    if (toBEAST != null && toBEAST instanceof AlignmentToBEAST alignmentToBEAST) {
                        beast.evolution.alignment.Alignment beastAlg =
                                alignmentToBEAST.valueToBEAST(algValue, context);
                        String algXML = new XMLProducer().toXML(beastAlg);
                        //TODO add replicate prefix
                        try (PrintWriter out = new PrintWriter(entry.getKey() + ".xml")) {
                            out.println(algXML);
                        } catch (FileNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    }

                } else LoggerUtils.log.warning("Skip alignment " + entry.getValue() +
                        " as only SimpleAlignment is processed !");
            }
        } // end for
        return null;
    }

    @Override
    public Class<Simulate> getGeneratorClass() {
        return Simulate.class;
    }
}
