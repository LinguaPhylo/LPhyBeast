package feast.lphybeast.spi;

import beast.base.evolution.datatype.DataType;
import feast.lphybeast.tobeast.generators.ExpressionNodeToBEAST;
import feast.lphybeast.tobeast.generators.ExpressionNodeWrapperToFEAST;
import feast.lphybeast.tobeast.generators.SliceDoubleArrayToBEAST;
import feast.lphybeast.tobeast.values.DoubleArrayValueToBEAST;
import jebl.evolution.sequences.SequenceType;
import lphy.core.model.Generator;
import lphybeast.GeneratorToBEAST;
import lphybeast.ValueToBEAST;
import lphybeast.spi.LPhyBEASTExt;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class FeastLBImpl implements LPhyBEASTExt {

    @Override
    public List<Class<? extends ValueToBEAST>> getValuesToBEASTs() {
        return List.of(DoubleArrayValueToBEAST.class);
    }

    @Override
    public List<Class<? extends GeneratorToBEAST>> getGeneratorToBEASTs() {
        return Arrays.asList(
                ExpressionNodeToBEAST.class,
                ExpressionNodeWrapperToFEAST.class,
                SliceDoubleArrayToBEAST.class
        );
    }

    @Override
    public Map<SequenceType, DataType> getDataTypeMap() {
        return Collections.emptyMap();
    }

    @Override
    public List<Class<? extends Generator>> getExcludedGenerator() {
        return Collections.emptyList();
    }

    @Override
    public List<Class> getExcludedValueType() {
        return Collections.emptyList();
    }
}
