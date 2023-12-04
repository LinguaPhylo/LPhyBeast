package lphybeast;

import beast.base.core.BEASTInterface;
import lphy.core.model.Generator;

import java.util.List;

public interface GeneratorToBEAST<T extends Generator,S extends BEASTInterface> {

    /**
     * converts a generator to an equivalent BEAST object
     * @param generator the generator to be converted
     * @param value the already-converted value that this generator produced for the conversion
     * @param context the BEASTContext object holding other Beast objects already converted
     * @return a new BEAST object representing this generator
     */
    S generatorToBEAST(T generator, BEASTInterface value, BEASTContext context);

    /**
     * converts a generator to an equivalent BEAST object
     * @param generator the generator to be converted
     * @param value the list of already-converted values that this generator produced for the conversion
     * @param context the BEASTContext object holding other Beast objects already converted
     * @return a new BEAST object representing this generator
     */
    default S generatorToBEAST(T generator, List<BEASTInterface> value, BEASTContext context) {

        if (value.size() > 1) throw new IllegalArgumentException("A multi value version of this generator is not supported!");

        return generatorToBEAST(generator,value.get(0), context);
    }

    /**
     * provides a hook to allow generators that need to, to modify/replace the values that represent their
     * inputs or outputs. This is called after all the automatic beast value creation, but before
     * generatorToBEAST is called on any of the generators.
     * @param generator the generator
     * @param value the already-converted value that this generator produced for the conversion
     * @param context the BEASTContext object holding other Beast objects already converted
     */
    default void modifyBEASTValues(T generator, BEASTInterface value, BEASTContext context) {
        // default do nothing
    }

    /**
     * The class of value that can be converted to BEAST.
     * @return
     */
    Class<T> getGeneratorClass();

    /**
     * The BEAST class to be converted. It is only used for summarising at the moment.
     *
     * @return
     */
    default Class<S> getBEASTClass() {
        return (Class<S>)BEASTInterface.class;
    }

}
