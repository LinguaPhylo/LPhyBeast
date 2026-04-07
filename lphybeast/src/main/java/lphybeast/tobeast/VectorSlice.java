package lphybeast.tobeast;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.CalculationNode;
import beast.base.spec.domain.Real;
import beast.base.spec.type.RealVector;

import java.util.AbstractList;
import java.util.List;

/**
 * Vector view of a contiguous sub-range of a {@link RealVector}.
 * Used when an LPhy range-slice (e.g. {@code array[2:5]}) maps to a
 * contiguous sub-vector of a BEAST parameter.
 * <p>
 * Can be promoted to beast3's {@code beast.base.spec.inference.parameter}
 * alongside {@link beast.base.spec.inference.parameter.VectorElement}.
 *
 * @param <D> the real domain type, inherited from the underlying vector
 */
@Description("Vector view of a contiguous sub-range of a RealVector")
public class VectorSlice<D extends Real> extends CalculationNode implements RealVector<D> {

    final public Input<RealVector<?>> vectorInput = new Input<>("vector",
            "the vector to extract a sub-range from", Input.Validate.REQUIRED);
    final public Input<Integer> indexInput = new Input<>("index",
            "start index of the sub-range", Input.Validate.REQUIRED);
    final public Input<Integer> countInput = new Input<>("count",
            "number of elements to extract", Input.Validate.REQUIRED);

    private RealVector<?> vector;
    private int index;
    private int count;

    public VectorSlice() {}

    public VectorSlice(RealVector<D> vector, int index, int count) {
        initByName("vector", vector, "index", index, "count", count);
    }

    @Override
    public void initAndValidate() {
        vector = vectorInput.get();
        index = indexInput.get();
        count = countInput.get();
        if (index < 0 || count < 1 || index + count > vector.size())
            throw new IndexOutOfBoundsException(
                    "slice [" + index + ", " + (index + count) + ") out of range [0, " + vector.size() + ")");
    }

    @Override
    public double get(int i) {
        if (i < 0 || i >= count)
            throw new IndexOutOfBoundsException("index " + i + " out of range [0, " + count + ")");
        return vector.get(index + i);
    }

    @Override
    public Double get(int... idx) {
        if (idx.length != 1)
            throw new IndexOutOfBoundsException("Vector access requires exactly 1 index, but got " + idx.length);
        return get(idx[0]);
    }

    @Override
    public List<Double> getElements() {
        return new AbstractList<>() {
            @Override
            public Double get(int i) {
                return VectorSlice.this.get(i);
            }

            @Override
            public int size() {
                return count;
            }
        };
    }

    @Override
    public int size() {
        return count;
    }

    @SuppressWarnings("unchecked")
    @Override
    public D getDomain() {
        return (D) vector.getDomain();
    }
}
