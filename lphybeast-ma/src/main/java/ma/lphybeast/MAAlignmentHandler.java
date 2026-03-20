package ma.lphybeast;

import beast.base.evolution.alignment.Alignment;
import lphybeast.spi.AlignmentHandler;
import mutablealignment.MutableAlignment;

/**
 * Creates MutableAlignment for unobserved data.
 */
public class MAAlignmentHandler implements AlignmentHandler {

    @Override
    public boolean appliesTo(boolean isObserved) {
        return !isObserved;
    }

    @Override
    public Alignment createAlignment() {
        return new MutableAlignment();
    }
}
