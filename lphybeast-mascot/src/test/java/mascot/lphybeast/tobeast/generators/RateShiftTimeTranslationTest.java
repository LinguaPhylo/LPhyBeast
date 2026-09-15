package mascot.lphybeast.tobeast.generators;

import beast.base.evolution.alignment.Taxon;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.tree.TraitSet;
import beast.base.evolution.tree.TreeParser;
import beast.base.inference.parameter.BooleanParameter;
import beast.base.inference.parameter.RealParameter;
import mascot.distribution.Mascot;
import mascot.distribution.StructuredTreeIntervals;
import mascot.dynamics.GLM;
import mascot.dynamics.RateShifts;
import mascot.glmmodel.Covariate;
import mascot.glmmodel.CovariateList;
import mascot.glmmodel.LogLinear;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static mascot.lphybeast.tobeast.generators.StructuredCoalescentRateShiftsToGLM.leadingIntervalsBeforePresent;
import static mascot.lphybeast.tobeast.generators.StructuredCoalescentRateShiftsToGLM.toMascotEndTimes;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Proves that LPhy's interval START times are translated into the interval END times
 * MASCOT expects, and that the translated times actually give each epoch its own rates.
 *
 * <h2>What was wrong</h2>
 * LPhy writes {@code rateShiftTimes = [0.0, 5.0]} meaning two epochs, [0,5) and [5,inf).
 * MASCOT reads each value as the END of an epoch and skips every leading value {@code <= 0}
 * ({@code GLM.firstlargerzero}). Passed through unchanged, {@code [0.0, 5.0]} therefore
 * became a single interval whose rates every epoch shared, so the model silently collapsed
 * to a constant one and the shift time stopped affecting the likelihood entirely.
 *
 * <p>{@link #rawPassThroughCollapsesTheModel()} reproduces that collapse against MASCOT
 * directly, so the tests below are anchored to real behaviour rather than to my reading
 * of the code.
 */
public class RateShiftTimeTranslationTest {

    private static final double ROOT = 12.0;

    // 3 predictor rows x 2 demes. Rows are deliberately far apart so a mix-up is obvious.
    private static final double[] NE_ROWS  = {10, 20,   100, 200,   1000, 2000};
    private static final double[] MIG_ROWS = {0.1, 0.1, 0.9, 0.9,   0.5, 0.5};

    // ---------------------------------------------------------------- pure translation

    @Test
    public void translatesStartTimesToEndTimes() {
        // two epochs, boundary at 5: MASCOT needs the boundary plus a sentinel past the root
        Double[] end = toMascotEndTimes(new Double[]{0.0, 5.0}, ROOT);
        assertArrayEquals(new Double[]{5.0, 25.0}, end);
    }

    @Test
    public void translatesThreeEpochs() {
        Double[] end = toMascotEndTimes(new Double[]{0.0, 5.0, 10.0}, ROOT);
        assertArrayEquals(new Double[]{5.0, 10.0, 25.0}, end);
    }

    @Test
    public void singleEpochBecomesOneSentinel() {
        Double[] end = toMascotEndTimes(new Double[]{0.0}, ROOT);
        assertEquals(1, end.length);
        assertTrue(end[0] > ROOT, "sentinel must close the only epoch beyond the root");
    }

    @Test
    public void epochCountIsPreserved() {
        for (int k = 1; k <= 6; k++) {
            Double[] start = new Double[k];
            for (int i = 0; i < k; i++) start[i] = i * 5.0;
            assertEquals(k, toMascotEndTimes(start, ROOT).length,
                    "one MASCOT end time per LPhy epoch, so predictor row i maps to epoch i");
        }
    }

    @Test
    public void sentinelAlwaysClearsBothRootAndLastShift() {
        // the sentinel bounds the oldest epoch; MASCOT reuses the last row beyond it, so it
        // only has to sit past the root -- but it must never land inside the tree.
        for (double root : new double[]{0.5, 12.0, 500.0}) {
            for (double last : new double[]{1.0, 20.0, 400.0}) {
                Double[] end = toMascotEndTimes(new Double[]{0.0, last}, root);
                assertTrue(end[end.length - 1] > root, "sentinel must exceed root " + root);
                assertTrue(end[end.length - 1] > last, "sentinel must exceed last shift " + last);
            }
        }
    }

    @Test
    public void outputIsAlwaysStrictlyPositiveAndIncreasing() {
        // this is the whole point: any value <= 0 re-triggers MASCOT's firstlargerzero path
        Double[][] cases = {
                {0.0, 5.0}, {0.0, 5.0, 10.0}, {0.0}, {-3.0, 0.0, 5.0}, {-10.0, -5.0, 0.0, 2.0}
        };
        for (Double[] start : cases) {
            Double[] end = toMascotEndTimes(start, ROOT);
            for (int i = 0; i < end.length; i++) {
                assertTrue(end[i] > 0.0,
                        "non-positive end time " + end[i] + " from " + Arrays.toString(start));
                if (i > 0) assertTrue(end[i] > end[i - 1], "not increasing: " + Arrays.toString(end));
            }
        }
    }

    @Test
    public void dropsEpochsThatEndBeforeTheMostRecentSample() {
        // LPhy allows negative starts so predictors may begin before the tree; those rows
        // describe time no lineage spans, so they are dropped here rather than left for
        // MASCOT's firstlargerzero (which mis-indexes the oldest epoch when it fires).
        assertEquals(0, leadingIntervalsBeforePresent(new Double[]{0.0, 5.0}));
        assertEquals(1, leadingIntervalsBeforePresent(new Double[]{-3.0, 0.0, 5.0}));
        assertEquals(2, leadingIntervalsBeforePresent(new Double[]{-10.0, -5.0, 0.0, 2.0}));

        assertArrayEquals(new Double[]{5.0, 25.0}, toMascotEndTimes(new Double[]{-3.0, 0.0, 5.0}, ROOT));
    }

    @Test
    public void rejectsEmptyInput() {
        assertThrows(IllegalArgumentException.class, () -> toMascotEndTimes(new Double[0], ROOT));
    }

    // ------------------------------------------------------- semantics against real MASCOT

    @Test
    public void translatedTimesGiveEachEpochItsOwnRates() throws Exception {
        // LPhy [0.0, 5.0, 10.0] = 3 epochs reading predictor rows 0, 1, 2
        GLM glm = glmFor(toMascotEndTimes(new Double[]{0.0, 5.0, 10.0}, ROOT), 3);

        // getIntervals() returns durations; cumulative sums are the boundaries
        double[] durations = glm.getIntervals();
        assertEquals(3, durations.length, "3 epochs in, 3 intervals out");
        assertEquals(5.0, durations[0], 1e-9, "first boundary at 5");
        assertEquals(10.0, durations[0] + durations[1], 1e-9, "second boundary at 10");
        assertTrue(durations[0] + durations[1] + durations[2] > ROOT,
                "oldest epoch must extend past the root");

        assertNe(glm, 0, 10, 20);     // epoch [0,5)   -> row 0
        assertNe(glm, 1, 100, 200);   // epoch [5,10)  -> row 1
        assertMigRow(glm, 0, 0.1);
        assertMigRow(glm, 1, 0.9);
    }

    @Test
    public void neAndMigrationReadTheSameEpoch() throws Exception {
        // the pass-through bug desynced these: Ne came from one row, migration from another
        GLM glm = glmFor(toMascotEndTimes(new Double[]{0.0, 5.0, 10.0}, ROOT), 3);
        for (int epoch = 0; epoch < 2; epoch++) {
            int neRow  = rowOfNe(glm, epoch);
            int migRow = rowOfMig(glm, epoch);
            assertEquals(neRow, migRow,
                    "epoch " + epoch + " reads Ne row " + neRow + " but migration row " + migRow);
        }
    }

    @Test
    public void rawPassThroughCollapsesTheModel() throws Exception {
        // Reproduces the original bug, so the tests above are anchored to real MASCOT
        // behaviour. Feeding LPhy's [0.0, 5.0, 10.0] to MASCOT unchanged:
        GLM glm = glmFor(new Double[]{0.0, 5.0, 10.0}, 3);

        assertEquals(2, glm.getIntervals().length,
                "leading 0.0 is swallowed, so 3 epochs become 2 intervals");

        // every epoch ends up on the same Ne row ...
        assertNe(glm, 0, 100, 200);
        assertNe(glm, 1, 100, 200);
        assertNe(glm, 2, 100, 200);
        // ... while migration walks on to the next one
        assertNotEquals(rowOfNe(glm, 1), rowOfMig(glm, 1),
                "the pass-through desyncs Ne and migration in the oldest epoch");
    }

    @Test
    public void shiftTimeIsVisibleInTheLikelihood() throws Exception {
        // The user-visible symptom: with the raw pass-through, moving the boundary changed
        // nothing, because a partition between identical rates is unobservable.
        double rawNear  = logP(new Double[]{0.0, 2.0, 10.0});
        double rawFar   = logP(new Double[]{0.0, 8.0, 10.0});
        assertEquals(rawNear, rawFar, 1e-2,
                "pass-through: likelihood is flat in the shift time (this is the bug)");

        double fixedNear = logP(toMascotEndTimes(new Double[]{0.0, 2.0, 10.0}, ROOT));
        double fixedFar  = logP(toMascotEndTimes(new Double[]{0.0, 8.0, 10.0}, ROOT));
        assertTrue(Math.abs(fixedNear - fixedFar) > 1.0,
                "after translation the boundary must move the likelihood, got "
                        + fixedNear + " vs " + fixedFar);
    }

    /**
     * Pins a bug in the bundled {@code Mascot.v3.0.8.jar}, NOT in the translation above.
     * <p>
     * 3.0.8 computes the coalescent clamp as {@code dim - 2} instead of
     * {@code dim - firstlargerzero - 1}, so the oldest epoch reads row {@code n-2}. Upstream
     * fixed this in {@code 8e14d8d} ("Fix GLM intervalNr calculation for single-epoch
     * models", #16), released in 3.1.0. The whole 3.1.0 line moved to BEAST 2.8 spec
     * parameter types, so upgrading is a separate migration.
     * <p>
     * When the jar is upgraded, the oldest epoch should read row 2 ({@code Ne=[1000,2000]})
     * and this test should be changed to assert that.
     */
    @Test
    public void oldestEpochStillWrongOnBundledMascot308() throws Exception {
        GLM glm = glmFor(toMascotEndTimes(new Double[]{0.0, 5.0, 10.0}, ROOT), 3);
        assertNe(glm, 2, 100, 200);   // should be 1000, 2000 once on MASCOT >= 3.1.0
    }

    @Test
    public void rateShiftsHandedToMascotCarryTranslatedTimes() {
        // guards the wiring, not just the helper: what MASCOT receives must be end times
        RateShifts shifts = StructuredCoalescentRateShiftsToGLM.buildRateShifts(
                toMascotEndTimes(new Double[]{0.0, 5.0, 10.0}, ROOT));

        assertEquals(3, shifts.getDimension(), "one MASCOT value per LPhy epoch");
        assertEquals(5.0, shifts.getValue(0), 1e-9);
        assertEquals(10.0, shifts.getValue(1), 1e-9);
        assertTrue(shifts.getValue(2) > ROOT, "sentinel closes the oldest epoch past the root");
    }

    @Test
    public void buildRateShiftsRefusesRawLPhyTimes() {
        // a leading 0.0 is precisely what re-triggers GLM.firstlargerzero, so fail loudly
        // rather than silently collapsing the model the way the original code did
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> StructuredCoalescentRateShiftsToGLM.buildRateShifts(new Double[]{0.0, 5.0}));
        assertTrue(e.getMessage().contains("strictly positive"), e.getMessage());
    }

    // ------------------------------------------------------------------------- helpers

    private static void assertNe(GLM glm, int epoch, double ne0, double ne1) {
        double[] coal = glm.getCoalescentRate(epoch);
        assertEquals(ne0, 1 / coal[0], 1e-6, "epoch " + epoch + " Ne[0]");
        assertEquals(ne1, 1 / coal[1], 1e-6, "epoch " + epoch + " Ne[1]");
    }

    /** Which predictor row epoch's Ne came from, by matching against {@link #NE_ROWS}. */
    private static int rowOfNe(GLM glm, int epoch) {
        double ne = 1 / glm.getCoalescentRate(epoch)[0];
        for (int r = 0; r < NE_ROWS.length / 2; r++)
            if (Math.abs(NE_ROWS[2 * r] - ne) < 1e-6) return r;
        return fail("Ne " + ne + " matches no predictor row");
    }

    /**
     * Which predictor row epoch's migration came from. MASCOT returns backwards rates,
     * {@code m * Ne_j / Ne_i}, so recover the forward rate via the Ne ratio it used.
     */
    private static int rowOfMig(GLM glm, int epoch) {
        double[] bm = glm.getBackwardsMigration(epoch);
        double[] coal = glm.getCoalescentRate(epoch);
        // bm is row-major n x n with a zero diagonal; entry [0][1] is index 1 for n=2
        double neRatio = (1 / coal[1]) / (1 / coal[0]);
        double forward = bm[1] / neRatio;
        for (int r = 0; r < MIG_ROWS.length / 2; r++)
            if (Math.abs(MIG_ROWS[2 * r] - forward) < 1e-6) return r;
        // the Ne used for the conversion may itself come from another row; fall back to a
        // direct scan over the candidate ratios rather than guessing
        for (int r = 0; r < MIG_ROWS.length / 2; r++)
            for (int s = 0; s < NE_ROWS.length / 2; s++) {
                double ratio = NE_ROWS[2 * s + 1] / NE_ROWS[2 * s];
                if (Math.abs(MIG_ROWS[2 * r] * ratio - bm[1]) < 1e-6) return r;
            }
        return fail("backwards migration " + bm[1] + " matches no predictor row");
    }

    private static void assertMigRow(GLM glm, int epoch, double expectedForwardRate) {
        int row = rowOfMig(glm, epoch);
        assertEquals(expectedForwardRate, MIG_ROWS[2 * row], 1e-9,
                "epoch " + epoch + " migration row");
    }

    private static LogLinear logLinear(double[] rows, int verticalEntries, int nIntervals, String id)
            throws Exception {
        Double[] logged = new Double[rows.length];
        for (int i = 0; i < rows.length; i++) logged[i] = Math.log(rows[i]);

        CovariateList covariates = new CovariateList();
        covariates.setInputValue("covariates",
                new ArrayList<>(List.of(new Covariate(logged, id + "_intercept"))));
        covariates.initAndValidate();

        RealParameter scaler = new RealParameter();
        scaler.setInputValue("value", "1.0");
        scaler.initAndValidate();
        BooleanParameter indicator = new BooleanParameter();
        indicator.setInputValue("value", "true");
        indicator.initAndValidate();
        RealParameter clock = new RealParameter();
        clock.setInputValue("value", "1.0");
        clock.initAndValidate();

        LogLinear glm = new LogLinear();
        glm.setInputValue("covariateList", covariates);
        glm.setInputValue("scaler", scaler);
        glm.setInputValue("indicator", indicator);
        glm.setInputValue("clock", clock);
        glm.setID(id);
        glm.nrIntervals = nIntervals;
        glm.verticalEntries = verticalEntries;
        glm.initAndValidate();
        return glm;
    }

    /** A 2-deme GLM over {@code nRows} predictor rows, driven by the given MASCOT end times. */
    private static GLM glmFor(Double[] mascotEndTimes, int nRows) throws Exception {
        RateShifts shifts = new RateShifts();
        shifts.setInputValue("value", new ArrayList<>(Arrays.asList(mascotEndTimes)));
        shifts.initAndValidate();

        GLM glm = new GLM();
        glm.setInputValue("NeGLM", logLinear(NE_ROWS, 2, nRows, "Ne"));
        glm.setInputValue("migrationGLM", logLinear(MIG_ROWS, 2, nRows, "mig"));
        glm.setInputValue("rateShifts", shifts);
        glm.setInputValue("dimension", 2);
        glm.setInputValue("fromBeauti", false);
        glm.setInputValue("typeTrait", traitSet());
        glm.initAndValidate();
        return glm;
    }

    private static TraitSet traitSet() throws Exception {
        TaxonSet taxa = new TaxonSet();
        taxa.setInputValue("taxon", new ArrayList<>(List.of(
                new Taxon("t1"), new Taxon("t2"), new Taxon("t3"), new Taxon("t4"))));
        taxa.initAndValidate();

        TraitSet traits = new TraitSet();
        traits.setInputValue("traitname", "deme");
        traits.setInputValue("value", "t1=0,t2=1,t3=0,t4=1");
        traits.setInputValue("taxa", taxa);
        traits.initAndValidate();
        return traits;
    }

    /** MASCOT log likelihood of a fixed tree under the given MASCOT end times. */
    private static double logP(Double[] mascotEndTimes) throws Exception {
        TreeParser tree = new TreeParser();
        tree.setInputValue("newick", "((t1:4.0,t2:4.0):8.0,(t3:6.0,t4:6.0):6.0):0.0;");
        tree.setInputValue("IsLabelledNewick", true);
        tree.setInputValue("adjustTipHeights", false);
        tree.initAndValidate();

        StructuredTreeIntervals intervals = new StructuredTreeIntervals();
        intervals.setInputValue("tree", tree);
        intervals.initAndValidate();

        Mascot mascot = new Mascot();
        mascot.setInputValue("dynamics", glmFor(mascotEndTimes, 3));
        mascot.setInputValue("structuredTreeIntervals", intervals);
        mascot.setInputValue("tree", tree);
        mascot.initAndValidate();
        return mascot.calculateLogP();
    }
}
