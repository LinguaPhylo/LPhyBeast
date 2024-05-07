package lphybeast.spi;

import beast.base.evolution.datatype.DataType;
import jebl.evolution.sequences.SequenceType;
import lphy.base.distribution.DiscretizedGamma;
import lphy.base.evolution.Taxa;
import lphy.base.evolution.alignment.Alignment;
import lphy.base.evolution.tree.TimeTree;
import lphy.core.model.Generator;
import lphy.core.model.Value;
import lphy.core.vectorization.IID;
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
     * @see LPhyBEASTExtImpl#getExcludedValueType()
     */
    static boolean isExcludedValue(Value<?> val) {
        Object ob = val.value();
        // TODO better solution about array ?
        return ob instanceof String[] || // ignore all String, e.g. d = nexus(file="Dengue4.nex"), in a vector
                // exclude the value returned by taxa (and ages) functions
                ( ob instanceof Taxa && !(ob instanceof Alignment) ) ||
                ob instanceof TimeTree[];
    }

    /**
     * For a complex logic, or arrays.
     * @param generator   Generator
     * @return     if the Generator is excluded.
     * @see LPhyBEASTExtImpl#getExcludedGenerator()
     */
    static boolean isExcludedGenerator(Generator generator) {

        return (generator instanceof IID && ((IID<?>) generator).getBaseDistribution() instanceof DiscretizedGamma);
// use || to add more ...
    }

}
