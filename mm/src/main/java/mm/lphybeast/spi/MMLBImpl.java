package mm.lphybeast.spi;

import beast.base.evolution.datatype.DataType;
import jebl.evolution.sequences.SequenceType;
import lphy.graphicalModel.Generator;
import lphy.graphicalModel.Value;
import lphybeast.GeneratorToBEAST;
import lphybeast.ValueToBEAST;
import lphybeast.spi.LPhyBEASTExt;
import mm.lphybeast.tobeast.generators.LewisMKToBeast;

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
public class MMLBImpl implements LPhyBEASTExt {

    @Override
    public List<Class<? extends ValueToBEAST>> getValuesToBEASTs() {
        return new ArrayList<>();
    }

    @Override
    public List<Class<? extends GeneratorToBEAST>> getGeneratorToBEASTs() {
        return Arrays.asList( LewisMKToBeast.class );
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
    public List<Class<? extends Value>> getExcludedValue() {
        return new ArrayList<>();
    }


}
