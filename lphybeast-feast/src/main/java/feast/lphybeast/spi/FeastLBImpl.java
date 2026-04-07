package feast.lphybeast.spi;

import beast.base.evolution.datatype.DataType;
import feast.lphybeast.tobeast.generators.ExpressionNodeToBEAST;
import feast.lphybeast.tobeast.generators.ExpressionNodeWrapperToFEAST;
import jebl.evolution.sequences.SequenceType;
import lphy.core.model.Generator;
import lphybeast.GeneratorToBEAST;
import lphybeast.ValueToBEAST;
import lphybeast.spi.LPhyBEASTMapping;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class FeastLBImpl implements LPhyBEASTMapping {

    @Override
    public List<Class<? extends ValueToBEAST>> getValuesToBEASTs() {
        return Collections.emptyList();
    }

    @Override
    public List<Class<? extends GeneratorToBEAST>> getGeneratorToBEASTs() {
        return Arrays.asList(
                ExpressionNodeToBEAST.class,
                ExpressionNodeWrapperToFEAST.class
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
