package lphybeast.tobeast;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.CalculationNode;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.type.RealScalar;
import beast.base.spec.type.RealVector;

/**
 * Presents a single element of a {@link RealVector} as a {@link RealScalar}.
 * Used when a spec distribution (e.g. LogNormal) needs a scalar view
 * into a vector parameter (e.g. element 0 of a Markov chain).
 */
@Description("Scalar view of a single element in a RealVector")
public class ScalarSlice extends CalculationNode implements RealScalar<PositiveReal> {

    final public Input<RealVector<?>> vectorInput = new Input<>("vector",
            "the vector to extract an element from", Input.Validate.REQUIRED);
    final public Input<Integer> indexInput = new Input<>("index",
            "index of the element to extract", Input.Validate.REQUIRED);

    private RealVector<?> vector;
    private int index;

    public ScalarSlice() {}

    public ScalarSlice(RealVector<?> vector, int index) {
        initByName("vector", vector, "index", index);
    }

    @Override
    public void initAndValidate() {
        vector = vectorInput.get();
        index = indexInput.get();
        if (index < 0 || index >= vector.size())
            throw new IndexOutOfBoundsException("index " + index + " out of range [0, " + vector.size() + ")");
    }

    @Override
    public double get() {
        return vector.get(index);
    }

    @Override
    public PositiveReal getDomain() {
        return PositiveReal.INSTANCE;
    }
}
