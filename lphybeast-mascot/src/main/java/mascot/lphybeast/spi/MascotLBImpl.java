package mascot.lphybeast.spi;

import beast.base.evolution.datatype.DataType;
import jebl.evolution.sequences.SequenceType;
import lphy.core.model.Generator;
import lphybeast.GeneratorToBEAST;
import lphybeast.ValueToBEAST;
import lphybeast.spi.LPhyBEASTMapping;
import mascot.lphybeast.tobeast.generators.GaussianRandomWalkToBEAST;
import mascot.lphybeast.tobeast.generators.StructuredCoalescentRateShiftsToGLM;
import mascot.lphybeast.tobeast.generators.StructuredCoalescentSkylineToMascot;
import mascot.lphybeast.tobeast.generators.StructuredCoalescentToMascot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The "Container" provider class of SPI
 * which include a list of {@link ValueToBEAST},
 * {@link GeneratorToBEAST}, and {@link DataType}
 * to extend.
 * @author Walter Xie
 */
public class MascotLBImpl implements LPhyBEASTMapping {

    @Override
    public List<Class<? extends ValueToBEAST>> getValuesToBEASTs() {
        return new ArrayList<>();
    }

    @Override
    public List<Class<? extends GeneratorToBEAST>> getGeneratorToBEASTs() {
        return Arrays.asList(
                StructuredCoalescentToMascot.class,
                StructuredCoalescentRateShiftsToGLM.class,
                StructuredCoalescentSkylineToMascot.class,
                GaussianRandomWalkToBEAST.class
        );
    }

    @Override
    public Map<SequenceType, DataType> getDataTypeMap() {
        return new ConcurrentHashMap<>();
    }

    @Override
    public List<Class<? extends Generator>> getExcludedGenerator() {
        return new ArrayList<>();
    }

    @Override
    public List<Class> getExcludedValueType() {
        return new ArrayList<>();
    }

}
