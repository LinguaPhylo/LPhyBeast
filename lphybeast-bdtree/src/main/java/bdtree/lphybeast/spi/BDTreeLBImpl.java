package bdtree.lphybeast.spi;

import beast.base.evolution.datatype.DataType;
import jebl.evolution.sequences.SequenceType;
import lphy.core.model.Generator;
import lphybeast.GeneratorToBEAST;
import lphybeast.ValueToBEAST;
import lphybeast.spi.LPhyBEASTExt;
import lphybeast.tobeast.operators.DefaultTreeOperatorStrategy;
import lphybeast.tobeast.operators.TreeOperatorStrategy;
import bdtree.lphybeast.tobeast.generators.BirthDeathSerialSamplingToBEAST;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BDTreeLBImpl implements LPhyBEASTExt {

    @Override
    public List<Class<? extends ValueToBEAST>> getValuesToBEASTs() {
        return Collections.emptyList();
    }

    @Override
    public List<Class<? extends GeneratorToBEAST>> getGeneratorToBEASTs() {
        return List.of(BirthDeathSerialSamplingToBEAST.class);
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
