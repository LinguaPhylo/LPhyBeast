package lphybeast.spi;

import beast.base.evolution.datatype.DataType;
import jebl.evolution.sequences.SequenceType;
import lphy.graphicalModel.Generator;
import lphy.graphicalModel.Value;
import lphybeast.GeneratorToBEAST;
import lphybeast.ValueToBEAST;
import lphybeast.tobeast.operators.DefaultTreeOperatorStrategy;
import lphybeast.tobeast.operators.TreeOperatorStrategy;

import java.util.List;
import java.util.Map;

/**
 * The service interface defined for SPI.
 * Implement this interface to create one "Container" provider class
 * for each module of LPhyBEAST or its extensions,
 * which should include {@link ValueToBEAST}, {@link GeneratorToBEAST},
 * and {@link DataType}.
 *
 * @author Walter Xie
 */
public interface LPhyBEASTExt {

    List<Class<? extends ValueToBEAST>> getValuesToBEASTs();

    List<Class<? extends GeneratorToBEAST>> getGeneratorToBEASTs();

    Map<SequenceType, DataType> getDataTypeMap();

    List<Class<? extends Generator>> getExcludedGenerator();

    List<Class<? extends Value>> getExcludedValue();

    default TreeOperatorStrategy getTreeOperatorStrategy() {
        return new DefaultTreeOperatorStrategy();
    }

}
