package mascot.lphybeast.spi;

import beast.base.evolution.datatype.DataType;
import jebl.evolution.sequences.SequenceType;
import lphy.core.model.Generator;
import lphybeast.GeneratorToBEAST;
import lphybeast.ValueToBEAST;
import lphybeast.spi.LPhyBEASTExt;
import mascot.lphybeast.tobeast.generators.StructuredCoalescentToMascot;
import mascot.lphybeast.tobeast.generators.StructuredCoalescentToGLM;
import mascot.lphybeast.tobeast.generators.StructuredCoalescentRateShiftsToGLM;

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
public class MascotLBImpl implements LPhyBEASTExt {

    @Override
    public List<Class<? extends ValueToBEAST>> getValuesToBEASTs() {
        return new ArrayList<>();
    }

    @Override
    public List<Class<? extends GeneratorToBEAST>> getGeneratorToBEASTs() {
        return Arrays.asList(
                StructuredCoalescentToMascot.class,
                StructuredCoalescentRateShiftsToGLM.class
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
        // For a complex logic, or arrays, use isExcludedValue
        return new ArrayList<>();
    }

}
