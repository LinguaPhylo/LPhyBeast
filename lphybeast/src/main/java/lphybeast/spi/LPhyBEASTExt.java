package lphybeast.spi;

import beast.base.evolution.datatype.DataType;
import jebl.evolution.sequences.SequenceType;
import lphy.base.distribution.DiscretizedGamma;
import lphy.base.distribution.RandomComposition;
import lphy.base.distribution.Sample;
import lphy.base.distribution.WeightedDirichlet;
import lphy.base.evolution.Taxa;
import lphy.base.evolution.alignment.Alignment;
import lphy.base.evolution.tree.TimeTree;
import lphy.base.function.*;
import lphy.base.function.alignment.*;
import lphy.base.function.datatype.BinaryDatatypeFunction;
import lphy.base.function.datatype.NucleotidesFunction;
import lphy.base.function.datatype.StandardDatatypeFunction;
import lphy.base.function.io.WriteFasta;
import lphy.base.function.taxa.*;
import lphy.base.function.tree.ExtantTree;
import lphy.base.function.tree.MigrationCount;
import lphy.base.function.tree.NodeCount;
import lphy.core.model.ExpressionNode;
import lphy.core.model.Generator;
import lphy.core.model.Value;
import lphy.core.parser.function.ExpressionNodeWrapper;
import lphy.core.parser.function.MapFunction;
import lphy.core.parser.function.MethodCall;
import lphy.core.simulator.Simulate;
import lphy.core.vectorization.IID;
import lphy.core.vectorization.array.ArrayFunction;
import lphy.core.vectorization.operation.ElementsAt;
import lphy.core.vectorization.operation.Range;
import lphy.core.vectorization.operation.RangeList;
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

    List<Class> getExcludedValueType();

    default TreeOperatorStrategy getTreeOperatorStrategy() {
        return new DefaultTreeOperatorStrategy();
    }


    /**
     * exclude {@link lphy.core.model.Value}
     * or {@link lphy.core.model.Generator} to skip the validation
     * so not to throw UnsupportedOperationException
     * in either <code>BEASTContext#valueToBEAST(Value)<code/> or
     * <code>BEASTContext#generatorToBEAST(Value, Generator)<code/>.
     */

    /**
     * For a complex logic, or arrays.
     * @param val  Value
     * @return     if the value type is excluded.
     */
    static boolean isExcludedValue(Value<?> val) {
        Object ob = val.value();
        return ob instanceof String[] || // ignore all String, e.g. d = nexus(file="Dengue4.nex"), in a vector
                // exclude the value returned by taxa (and ages) functions
                ( ob instanceof Taxa && !(ob instanceof Alignment) ) ||
                ob instanceof TimeTree[];
    }

    static boolean isExcludedGenerator(Generator generator) {

        return generator instanceof WeightedDirichlet || generator instanceof ArrayFunction ||
                generator instanceof ExpressionNode || generator instanceof RandomComposition ||
                generator instanceof NTaxaFunction || generator instanceof NCharFunction ||
                generator instanceof TaxaFunction || generator instanceof NodeCount ||
                generator instanceof CreateTaxa || generator instanceof SpeciesTaxa ||
                generator instanceof TaxaAgesFromFunction || generator instanceof Get<?> ||
                generator instanceof MissingSites || generator instanceof SelectSitesByMissingFraction ||
                generator instanceof InvariableSites || generator instanceof VariableSites ||
                generator instanceof CopySites || generator instanceof Sample<?> ||
                generator instanceof Simulate || generator instanceof WriteFasta ||
//                generator instanceof ReadNexus || generator instanceof ReadFasta ||
                generator instanceof ExtractTrait || generator instanceof Unique ||
                generator instanceof Intersect || generator instanceof RepArray ||
                generator instanceof Sort<?> || generator instanceof ConcatArray ||
                generator instanceof ARange || generator instanceof Range ||
                generator instanceof MapFunction || generator instanceof MethodCall ||
                generator instanceof RangeList || generator instanceof ElementsAt || generator instanceof Rep ||
                generator instanceof MigrationMatrix || generator instanceof MigrationCount ||
                generator instanceof Length || generator instanceof Select || generator instanceof SumBoolean ||
                generator instanceof DiscretizedGamma || generator instanceof ExtantTree ||
                // ignore all data types
                generator instanceof NucleotidesFunction || generator instanceof StandardDatatypeFunction ||
                generator instanceof BinaryDatatypeFunction || //generator instanceof PhasedGenotypeFunction ||
                (generator instanceof IID && ((IID<?>) generator).getBaseDistribution() instanceof DiscretizedGamma) ||
                generator instanceof ExpressionNodeWrapper;
    }

}
