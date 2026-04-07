package lphybeast.util;

import beast.base.core.BEASTInterface;
import beast.base.core.BEASTObject;
import beast.base.core.Description;
import beast.base.core.Input;

import java.util.ArrayList;
import java.util.List;

/**
 * Array of BEAST objects. Used internally by LPhyBeast to group
 * vectorized values and generators during XML construction.
 * Vendored from BEASTLabs (originally written for LPhyBeast).
 *
 * @author Alexei Drummond
 */
@Description("array of beast objects")
public class BEASTVector extends BEASTObject {

    public Input<List<BEASTInterface>> vectorInput = new Input<>("element", "a vector of beast objects.", new ArrayList<>());

    public BEASTVector(List<BEASTInterface> elements, String id) {
        setInputValue("element", elements);
        initAndValidate();
        setID(id);
    }

    public BEASTVector(List<BEASTInterface> elements) {
        setInputValue("element", elements);
        initAndValidate();
    }

    @Override
    public void initAndValidate() {}

    public List<BEASTInterface> getObjectList() {
        return vectorInput.get();
    }
}
