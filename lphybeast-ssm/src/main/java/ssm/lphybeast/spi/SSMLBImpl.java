package ssm.lphybeast.spi;

import beast.base.evolution.datatype.DataType;
import jebl.evolution.sequences.SequenceType;
import lphy.core.model.Generator;
import lphybeast.GeneratorToBEAST;
import lphybeast.ValueToBEAST;
import lphybeast.spi.LPhyBEASTMapping;
import ssm.lphybeast.tobeast.generators.GTRToBEAST;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SSMLBImpl implements LPhyBEASTMapping {

    @Override
    public List<Class<? extends ValueToBEAST>> getValuesToBEASTs() {
        return new ArrayList<>();
    }

    @Override
    public List<Class<? extends GeneratorToBEAST>> getGeneratorToBEASTs() {
        return Arrays.asList(GTRToBEAST.class);
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
