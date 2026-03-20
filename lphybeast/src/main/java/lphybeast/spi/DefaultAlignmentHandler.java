package lphybeast.spi;

import beast.base.evolution.alignment.Alignment;

/**
 * Default handler that creates standard BEAST 2 Alignment objects.
 * Used for observed data.
 */
public class DefaultAlignmentHandler implements AlignmentHandler {

    @Override
    public boolean appliesTo(boolean isObserved) {
        return isObserved;
    }

    @Override
    public Alignment createAlignment() {
        return new Alignment();
    }
}
