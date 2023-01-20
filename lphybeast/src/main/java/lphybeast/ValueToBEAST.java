package lphybeast;

import beast.base.core.BEASTInterface;
import lphy.graphicalModel.Value;

public interface ValueToBEAST<T, S extends BEASTInterface> {

    /**
     * @param value the value to be converted
     * @param context all beast objects already converted by the value-inorder generator-postorder traversal.
     * @return
     */
    S valueToBEAST(Value<T> value, BEASTContext context);

    /**
     * The type of value that can be consumed but this ValueToBEAST.
     * @return a class representing the type of value that can be consumed.
     */
    @Deprecated
    Class getValueClass();

    /**
     * @param value a value to be tested for consumption by this ValueToBEAST
     * @return true if this value can be converted by this ValueToBEAST class, false otherwise.
     */
    default boolean match(Value value) {
        return getValueClass().isAssignableFrom(value.value().getClass());
    }

    /**
     * @param rawValue a raw value to be tested for consumption by this ValueToBEAST
     * @return true if this value can be converted by this ValueToBEAST class, false otherwise.
     */
    default boolean match(Object rawValue) {
        return getValueClass().isAssignableFrom(rawValue.getClass());
    }

    /**
     * The BEAST class to be converted. It is only used for summarising at the moment.
     *
     * @return
     */
    default Class<S> getBEASTClass() {
        return (Class<S>)BEASTInterface.class;
    }
}
