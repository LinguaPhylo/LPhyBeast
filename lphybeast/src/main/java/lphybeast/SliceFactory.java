package lphybeast;

import beast.base.core.BEASTInterface;
import beastlabs.core.util.Slice;

/**
 * Utils class to create frequently used BEAST objects
 * @author Alexei Drummond
 * @author Walter Xie
 */
public final class SliceFactory {

    /**
     * @param slicedBEASTObj  map to "arg" input, argument to extract element from.
     * @param index           map to "index" input, index of first element to extract.
     * @param id              Slice ID
     * @return   {@link Slice}
     */
    public static Slice createSlice(BEASTInterface slicedBEASTObj, int index, String id) {
        return SliceFactory.createSlice(slicedBEASTObj, index, 1, id);
    }

    /**
     * @param slicedBEASTObj  map to "arg" input, argument to extract element from.
     * @param index           map to "index" input, index of first element to extract.
     * @param count           map to "count" input, Number of elements to extract, default to 1 in BEAST.
     * @param id              Slice ID
     * @return   {@link Slice}
     */
    public static Slice createSlice(BEASTInterface slicedBEASTObj, int index, int count, String id) {
        Slice slice = new Slice();
        slice.setInputValue("arg", slicedBEASTObj);
        slice.setInputValue("index", index);
        if (count != 1) slice.setInputValue("count", count);
        slice.initAndValidate();
        slice.setID(id);
        return slice;
    }


}
