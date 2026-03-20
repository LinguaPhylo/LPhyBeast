package ma.lphybeast.spi;

import beast.base.evolution.datatype.DataType;
import jebl.evolution.sequences.SequenceType;
import lphy.core.model.Generator;
import lphybeast.GeneratorToBEAST;
import lphybeast.ValueToBEAST;
import lphybeast.spi.LPhyBEASTExt;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MALBImpl implements LPhyBEASTExt {

    @Override
    public List<Class<? extends ValueToBEAST>> getValuesToBEASTs() {
        return Collections.emptyList();
    }

    @Override
    public List<Class<? extends GeneratorToBEAST>> getGeneratorToBEASTs() {
        return Collections.emptyList();
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
