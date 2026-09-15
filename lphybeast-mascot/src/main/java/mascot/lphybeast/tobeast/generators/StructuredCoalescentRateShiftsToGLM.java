package mascot.lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.evolution.alignment.Taxon;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.tree.TraitSet;
import beast.base.inference.parameter.BooleanParameter;
import beast.base.inference.parameter.RealParameter;
import lphy.base.evolution.coalescent.StructuredCoalescentRateShifts;
import lphy.base.evolution.tree.TimeTree;
import lphy.base.evolution.tree.TimeTreeNode;
import lphy.base.function.GeneralLinearFunction;
import lphy.core.model.Generator;
import lphy.core.model.Value;
import lphy.core.vectorization.VectorizedFunction;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;
import lphybeast.tobeast.values.TimeTreeToBEAST;
import mascot.distribution.StructuredTreeIntervals;
import mascot.dynamics.GLM;
import mascot.dynamics.RateShifts;
import mascot.glmmodel.Covariate;
import mascot.glmmodel.CovariateList;
import mascot.glmmodel.LogLinear;
import mascot.lphybeast.tobeast.loggers.MascotExtraTreeLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Converts LPhy {@link StructuredCoalescentRateShifts} to BEAST2 MASCOT GLM.
 * <p>
 * This converter handles the structured coalescent with time-varying (piecewise constant)
 * migration rates and effective population sizes. Rate shifts occur at fixed times
 * specified by the user.
 * </p>
 *
 * <h2>Deme Ordering Contract</h2>
 * <p>
 * Demes are <b>always sorted alphabetically</b> in both LPhy and MASCOT.
 * The theta and m arrays must be provided in the correct flattened order:
 * </p>
 * <ul>
 *   <li>theta: [interval0_deme0, interval0_deme1, ..., interval1_deme0, ...]</li>
 *   <li>m: [interval0_0→1, interval0_0→2, ..., interval1_0→1, ...]</li>
 * </ul>
 *
 * <h2>LPhy Usage</h2>
 * <pre>
 * // Direct rate specification:
 * ψ ~ StructuredCoalescentRateShifts(
 *     theta=theta_data,
 *     m=m_data,
 *     rateShiftTimes=rateShiftTimes,
 *     taxa=taxa,
 *     demes=demes
 * );
 *
 * // GLM-based rates:
 * theta = generalLinearFunction(beta=beta_Ne, x=X_Ne, link="log", scale=Ne_scale);
 * m = generalLinearFunction(beta=beta_m, x=X_m, link="log", scale=m_scale);
 * ψ ~ StructuredCoalescentRateShifts(
 *     theta=theta,
 *     m=m,
 *     rateShiftTimes=rateShiftTimes,
 *     taxa=taxa,
 *     demes=demes
 * );
 * </pre>
 *
 * @author Alexei Drummond
 */
public class StructuredCoalescentRateShiftsToGLM implements
        GeneratorToBEAST<StructuredCoalescentRateShifts, mascot.distribution.Mascot> {

    /**
     * GLM parameters extracted from GeneralLinearFunction.
     */
    private static record GLMParams(
            Value<Double[]> beta,
            Value<?> x,
            Value<String> link,
            Value<Double> scale,
            Value<Boolean[]> indicator,
            Value<Double> error,
            boolean isVectorized
    ) {}

    @Override
    public mascot.distribution.Mascot generatorToBEAST(StructuredCoalescentRateShifts coalescent,
                                                        BEASTInterface value,
                                                        BEASTContext context) {

        Value<Double[]> thetaValue = coalescent.getTheta();
        Value<Double[]> mValue = coalescent.getM();
        Value<Double[]> rateShiftTimesValue = coalescent.getRateShiftTimes();

        List<String> uniqueDemes = coalescent.getUniqueDemes();
        int nDemes = coalescent.getNDemes();
        int nIntervals = coalescent.getNIntervals();
        int nMigRates = nDemes * (nDemes - 1);

        Double[] rateShiftTimes = rateShiftTimesValue.value();

        String popLabel = StructuredCoalescentRateShifts.populationLabel;
        TimeTree timeTree = ((Value<TimeTree>) context.getGraphicalModelNode(value)).value();

        // LPhy gives interval START times beginning at 0.0; MASCOT wants interval END
        // times that are strictly positive. Translate before wiring anything up --
        // see toMascotEndTimes for why a straight pass-through collapses the model.
        int intervalOffset = leadingIntervalsBeforePresent(rateShiftTimes);
        int nEffIntervals = nIntervals - intervalOffset;
        Double[] endTimes = toMascotEndTimes(rateShiftTimes, timeTree.rootAge());

        // Create MASCOT components
        mascot.distribution.Mascot mascot = new mascot.distribution.Mascot();

        // === Migration GLM ===
        LogLinear migrationGLM = createMigrationGLM(mValue, nDemes, nEffIntervals, intervalOffset, context);

        // === Ne GLM ===
        LogLinear neGLM = createNeGLM(thetaValue, nDemes, nEffIntervals, intervalOffset, context);

        // === Rate Shifts ===
        RateShifts rateShifts = buildRateShifts(endTimes);

        // === GLM Dynamics ===
        GLM dynamics = new GLM();
        dynamics.setInputValue("migrationGLM", migrationGLM);
        dynamics.setInputValue("NeGLM", neGLM);
        dynamics.setInputValue("rateShifts", rateShifts);
        dynamics.setInputValue("dimension", nDemes);
        dynamics.setInputValue("fromBeauti", false);

        // Create type trait set
        String traitStr = createTraitString(timeTree, popLabel);
        List<Taxon> taxonList = context.createTaxonList(TimeTreeToBEAST.getTaxaNames(timeTree));

        TraitSet traitSet = new TraitSet();
        traitSet.setInputValue("traitname", popLabel);
        traitSet.setInputValue("value", traitStr);

        TaxonSet taxa = new TaxonSet();
        taxa.setInputValue("taxon", taxonList);
        taxa.initAndValidate();

        traitSet.setInputValue("taxa", taxa);
        traitSet.initAndValidate();

        dynamics.setInputValue("typeTrait", traitSet);
        dynamics.initAndValidate();

        // === Mascot distribution ===
        mascot.setInputValue("dynamics", dynamics);

        StructuredTreeIntervals structuredTreeIntervals = new StructuredTreeIntervals();
        structuredTreeIntervals.setInputValue("tree", value);
        structuredTreeIntervals.initAndValidate();

        mascot.setInputValue("structuredTreeIntervals", structuredTreeIntervals);
        mascot.setInputValue("tree", value);
        mascot.initAndValidate();

        // Add loggers
        context.addExtraLoggable(mascot);
        MascotExtraTreeLogger extraTreeLogger = new MascotExtraTreeLogger(mascot);
        context.addExtraLogger(extraTreeLogger);

        return mascot;
    }

    /**
     * Wrap already-translated MASCOT end times in a {@link RateShifts}. Kept separate so a
     * test can assert that what reaches MASCOT is translated rather than raw LPhy input.
     */
    static RateShifts buildRateShifts(Double[] mascotEndTimes) {
        for (Double t : mascotEndTimes) {
            if (t <= 0.0)
                throw new IllegalArgumentException(
                        "MASCOT rate shift times must be strictly positive (a value <= 0 triggers " +
                        "GLM.firstlargerzero, which collapses the epochs): " + Arrays.toString(mascotEndTimes));
        }
        RateShifts rateShifts = new RateShifts();
        rateShifts.setInputValue("value", new ArrayList<>(Arrays.asList(mascotEndTimes)));
        rateShifts.initAndValidate();
        return rateShifts;
    }

    /**
     * Number of leading LPhy intervals that end at or before the most recent sample.
     * <p>
     * LPhy allows negative start times so that a predictor series may begin before the
     * tree does (see {@link StructuredCoalescentRateShifts}). Those leading rows describe
     * time the tree never spans. MASCOT has its own mechanism for this
     * ({@code GLM.firstlargerzero}), but it mis-indexes the oldest epoch whenever it
     * fires -- {@code getCoalescentRate} clamps to {@code n-firstlargerzero-1} while
     * {@code getBackwardsMigration} clamps to {@code n-1}, so Ne and migration end up one
     * row apart. We therefore drop those rows here and hand MASCOT only strictly positive
     * times, which keeps {@code firstlargerzero == 0} and the two clamps in agreement.
     */
    static int leadingIntervalsBeforePresent(Double[] startTimes) {
        int drop = 0;
        // interval i runs from startTimes[i] to startTimes[i+1]; it is invisible to the
        // tree when it has already ended by the time of the most recent sample.
        while (drop < startTimes.length - 1 && startTimes[drop + 1] <= 0.0) drop++;
        return drop;
    }

    /**
     * Translate LPhy interval START times into the interval END times MASCOT expects.
     * <p>
     * LPhy: {@code [0.0, 5.0]} means two epochs, {@code [0,5)} and {@code [5,inf)}, using
     * predictor rows 0 and 1.
     * <p>
     * MASCOT reads each value as the END of an epoch and skips every leading value
     * {@code <= 0} (GLM.java: {@code if (getValue(i) > 0) { firstlargerzero = i; break; }}).
     * Passing {@code [0.0, 5.0]} through unchanged therefore yields
     * {@code firstlargerzero == 1} and a single interval, and the clamp in
     * {@code getCoalescentRate} pins every epoch to row 0 -- so both epochs get identical
     * rates and the shift time becomes unobservable in the likelihood.
     * <p>
     * The correct encoding of the same model is {@code [5.0, T]}: epoch i then covers
     * {@code (endTimes[i-1], endTimes[i]]} and reads row i, one-to-one with LPhy. The
     * trailing sentinel T only has to sit past the root, because MASCOT reuses the last
     * row for everything older than the final shift -- which is exactly LPhy's
     * "last epoch extends to infinity". A tree that later grows past T is still handled
     * correctly for the same reason.
     *
     * @param startTimes LPhy interval start times, strictly increasing, normally [0.0, ...]
     * @param rootAge    age of the root of the simulated tree, used to place the sentinel
     * @return strictly positive, strictly increasing END times, one per surviving epoch
     */
    static Double[] toMascotEndTimes(Double[] startTimes, double rootAge) {
        if (startTimes.length == 0)
            throw new IllegalArgumentException("rateShiftTimes must not be empty");

        int drop = leadingIntervalsBeforePresent(startTimes);
        int nEff = startTimes.length - drop;

        // sentinel closes the oldest epoch; must exceed the root and every shift time
        double last = startTimes[startTimes.length - 1];
        double sentinel = Math.max(Math.max(rootAge, last), 0.0) * 2.0 + 1.0;

        Double[] endTimes = new Double[nEff];
        for (int i = 0; i < nEff - 1; i++)
            endTimes[i] = startTimes[drop + i + 1];
        endTimes[nEff - 1] = sentinel;

        for (int i = 0; i < endTimes.length; i++) {
            if (endTimes[i] <= 0.0)
                throw new IllegalArgumentException(
                        "rateShiftTimes produced a non-positive MASCOT end time at index " + i +
                        ": " + Arrays.toString(endTimes) + " from " + Arrays.toString(startTimes));
            if (i > 0 && endTimes[i] <= endTimes[i - 1])
                throw new IllegalArgumentException(
                        "rateShiftTimes must be strictly increasing, got " + Arrays.toString(startTimes));
        }
        return endTimes;
    }

    /**
     * Create the migration GLM model.
     * If m comes from GeneralLinearFunction, use the GLM parameters.
     * Otherwise, treat the m values as intercept covariates.
     */
    private LogLinear createMigrationGLM(Value<Double[]> mValue, int nDemes, int nIntervals,
                                         int intervalOffset, BEASTContext context) {

        int nMigRates = nDemes * (nDemes - 1);

        GLMParams params = extractGLMParams(mValue.getGenerator());

        if (params != null) {
            // m comes from GeneralLinearFunction - use full GLM
            String link = (params.link() != null) ? params.link().value() : "identity";
            if (!"log".equalsIgnoreCase(link)) {
                throw new IllegalArgumentException("MASCOT GLM requires log link function for migration, got: " + link);
            }
            return createGLMFromParams(params, nMigRates, nIntervals, intervalOffset, "migration", context);
        } else {
            // m is constant data - create intercept-only GLM
            return createInterceptGLM(mValue.value(), nMigRates, nIntervals, intervalOffset, "migration");
        }
    }

    /**
     * Create the Ne GLM model.
     * If theta comes from GeneralLinearFunction, use the GLM parameters.
     * Otherwise, treat the theta values as intercept covariates.
     */
    private LogLinear createNeGLM(Value<Double[]> thetaValue, int nDemes, int nIntervals,
                                  int intervalOffset, BEASTContext context) {

        GLMParams params = extractGLMParams(thetaValue.getGenerator());

        if (params != null) {
            // theta comes from GeneralLinearFunction - use full GLM
            String link = (params.link() != null) ? params.link().value() : "identity";
            if (!"log".equalsIgnoreCase(link)) {
                throw new IllegalArgumentException("MASCOT GLM requires log link function for Ne, got: " + link);
            }
            return createGLMFromParams(params, nDemes, nIntervals, intervalOffset, "Ne", context);
        } else {
            // theta is constant data - create intercept-only GLM
            return createInterceptGLM(thetaValue.value(), nDemes, nIntervals, intervalOffset, "Ne");
        }
    }

    /**
     * Create a LogLinear GLM from GeneralLinearFunction parameters.
     */
    private LogLinear createGLMFromParams(GLMParams params, int verticalEntries, int nIntervals,
                                           int intervalOffset, String namePrefix, BEASTContext context) {

        Double[] beta = params.beta().value();
        int nPredictors = beta.length;

        // Extract covariates from design matrix
        List<Covariate> covariates = extractCovariates(params.x(), nPredictors, verticalEntries,
                nIntervals, intervalOffset, namePrefix);

        CovariateList covariateList = new CovariateList();
        covariateList.setInputValue("covariates", covariates);
        covariateList.initAndValidate();

        // Get or create scaler parameter (beta coefficients)
        RealParameter scalerParam = (RealParameter) context.getBEASTObject(params.beta());
        scalerParam.setID(namePrefix + "ScalerGLM");

        // Create indicator parameter - use LPhy indicators if provided, otherwise all true
        BooleanParameter indicatorParam;
        if (params.indicator() != null) {
            indicatorParam = (BooleanParameter) context.getBEASTObject(params.indicator());
            indicatorParam.setID(namePrefix + "IndicatorGLM");
        } else {
            Boolean[] indicators = new Boolean[nPredictors];
            Arrays.fill(indicators, true);
            indicatorParam = new BooleanParameter();
            indicatorParam.setInputValue("value", Arrays.asList(indicators));
            indicatorParam.setID(namePrefix + "IndicatorGLM");
            indicatorParam.initAndValidate();
        }

        // Create clock parameter from scale value, or default 1.0
        RealParameter clockParam;
        if (params.scale() != null) {
            clockParam = (RealParameter) context.getBEASTObject(params.scale());
            clockParam.setID(namePrefix + "ClockGLM");
        } else {
            clockParam = new RealParameter();
            clockParam.setInputValue("value", "1.0");
            clockParam.setID(namePrefix + "ClockGLM");
            clockParam.initAndValidate();
        }

        // Create LogLinear GLM model
        LogLinear glm = new LogLinear();
        glm.setInputValue("covariateList", covariateList);
        glm.setInputValue("scaler", scalerParam);
        glm.setInputValue("indicator", indicatorParam);
        glm.setInputValue("clock", clockParam);

        // Wire error term if provided
        if (params.error() != null && intervalOffset > 0)
            throw new UnsupportedOperationException(
                    "An error term combined with negative rateShiftTimes is not supported yet: the " +
                    "per-row error parameter would have to be trimmed by " + intervalOffset +
                    " interval(s) alongside the covariates.");
        if (params.error() != null) {
            RealParameter errorParam = (RealParameter) context.getBEASTObject(params.error());
            errorParam.setID(namePrefix + "ErrorGLM");
            glm.setInputValue("error", errorParam);
        }

        glm.setID(namePrefix + "GLM");

        glm.nrIntervals = nIntervals;
        glm.verticalEntries = verticalEntries;
        glm.initAndValidate();

        return glm;
    }

    /**
     * Create an intercept-only GLM for constant rate values.
     * The rate values become the covariate, with scaler=1.0.
     */
    private LogLinear createInterceptGLM(Double[] values, int verticalEntries, int nIntervals,
                                         int intervalOffset, String namePrefix) {

        values = dropLeadingIntervals(values, verticalEntries, intervalOffset);
        int expectedLength = nIntervals * verticalEntries;
        if (values.length != expectedLength) {
            throw new IllegalArgumentException(
                namePrefix + " array has " + values.length + " values, expected " + expectedLength +
                " (nIntervals=" + nIntervals + " * verticalEntries=" + verticalEntries + ")");
        }

        // Create a single "intercept" covariate with the log of the rate values
        // (since MASCOT uses log-linear, we need exp(log(value)) = value)
        Double[] logValues = new Double[values.length];
        for (int i = 0; i < values.length; i++) {
            logValues[i] = Math.log(values[i]);
        }
        Covariate cov = new Covariate(logValues, namePrefix + "_intercept");

        List<Covariate> covariates = new ArrayList<>();
        covariates.add(cov);

        CovariateList covariateList = new CovariateList();
        covariateList.setInputValue("covariates", covariates);
        covariateList.initAndValidate();

        // Scaler = 1.0 (coefficient for the intercept)
        RealParameter scalerParam = new RealParameter();
        scalerParam.setInputValue("value", "1.0");
        scalerParam.setID(namePrefix + "ScalerGLM");
        scalerParam.initAndValidate();

        // Indicator = true
        BooleanParameter indicatorParam = new BooleanParameter();
        indicatorParam.setInputValue("value", "true");
        indicatorParam.setID(namePrefix + "IndicatorGLM");
        indicatorParam.initAndValidate();

        // Clock = 1.0
        RealParameter clockParam = new RealParameter();
        clockParam.setInputValue("value", "1.0");
        clockParam.setID(namePrefix + "ClockGLM");
        clockParam.initAndValidate();

        LogLinear glm = new LogLinear();
        glm.setInputValue("covariateList", covariateList);
        glm.setInputValue("scaler", scalerParam);
        glm.setInputValue("indicator", indicatorParam);
        glm.setInputValue("clock", clockParam);
        glm.setID(namePrefix + "GLM");

        glm.nrIntervals = nIntervals;
        glm.verticalEntries = verticalEntries;
        glm.initAndValidate();

        return glm;
    }

    /**
     * Extract covariates from the design matrix.
     * Each column of the design matrix becomes one Covariate.
     */
    private List<Covariate> extractCovariates(Value<?> xValue, int nPredictors, int verticalEntries,
                                               int nIntervals, int intervalOffset, String namePrefix) {
        List<Covariate> covariates = new ArrayList<>();
        Object x = xValue.value();
        int expectedRows = nIntervals * verticalEntries;
        int rowOffset = intervalOffset * verticalEntries;

        if (x instanceof Double[][] matrix) {
            if (rowOffset > 0) matrix = Arrays.copyOfRange(matrix, rowOffset, matrix.length);
            if (matrix.length != expectedRows) {
                throw new IllegalArgumentException(
                    "Design matrix has " + matrix.length + " rows, expected " + expectedRows);
            }
            if (matrix.length > 0 && matrix[0].length != nPredictors) {
                throw new IllegalArgumentException(
                    "Design matrix has " + matrix[0].length + " columns, expected " + nPredictors);
            }

            // Create one covariate per predictor (column)
            for (int p = 0; p < nPredictors; p++) {
                Double[] covValues = new Double[expectedRows];
                for (int i = 0; i < expectedRows; i++) {
                    covValues[i] = matrix[i][p];
                }
                Covariate cov = new Covariate(covValues, namePrefix + "_predictor_" + p);
                covariates.add(cov);
            }
        } else if (x instanceof Double[] array) {
            // Flattened design matrix: values are row-major [row0_col0, row0_col1, ..., row1_col0, ...]
            if (rowOffset > 0) array = Arrays.copyOfRange(array, rowOffset * nPredictors, array.length);
            if (array.length != expectedRows * nPredictors) {
                throw new IllegalArgumentException(
                    "Flattened design matrix has " + array.length + " values, expected " +
                    (expectedRows * nPredictors));
            }

            for (int p = 0; p < nPredictors; p++) {
                Double[] covValues = new Double[expectedRows];
                for (int row = 0; row < expectedRows; row++) {
                    covValues[row] = array[row * nPredictors + p];
                }
                Covariate cov = new Covariate(covValues, namePrefix + "_predictor_" + p);
                covariates.add(cov);
            }
        } else {
            throw new IllegalArgumentException("Unexpected type for design matrix: " + x.getClass());
        }

        return covariates;
    }

    /**
     * Extract GLM parameters from either a GeneralLinearFunction or VectorizedFunction.
     */
    private GLMParams extractGLMParams(Generator<?> generator) {
        if (generator instanceof GeneralLinearFunction glm) {
            return new GLMParams(
                    glm.getParams().get(GeneralLinearFunction.betaParamName),
                    glm.getParams().get(GeneralLinearFunction.xParamName),
                    glm.getParams().get(GeneralLinearFunction.linkParamName),
                    glm.getParams().get(GeneralLinearFunction.scaleParamName),
                    glm.getParams().get(GeneralLinearFunction.indicatorParamName),
                    glm.getParams().get(GeneralLinearFunction.errorParamName),
                    false
            );
        }
        if (generator instanceof VectorizedFunction<?> vf &&
            !vf.getComponentFunctions().isEmpty() &&
            vf.getComponentFunctions().get(0) instanceof GeneralLinearFunction) {
            return new GLMParams(
                    vf.getParams().get(GeneralLinearFunction.betaParamName),
                    vf.getParams().get(GeneralLinearFunction.xParamName),
                    vf.getParams().get(GeneralLinearFunction.linkParamName),
                    vf.getParams().get(GeneralLinearFunction.scaleParamName),
                    vf.getParams().get(GeneralLinearFunction.indicatorParamName),
                    vf.getParams().get(GeneralLinearFunction.errorParamName),
                    true
            );
        }
        return null;
    }

    /** Drop the first {@code intervalOffset} intervals worth of rows from a flattened rate array. */
    private static Double[] dropLeadingIntervals(Double[] values, int verticalEntries, int intervalOffset) {
        if (intervalOffset <= 0) return values;
        return Arrays.copyOfRange(values, intervalOffset * verticalEntries, values.length);
    }

    private String createTraitString(TimeTree tree, String traitName) {
        StringBuilder builder = new StringBuilder();
        int leafCount = 0;
        for (TimeTreeNode node : tree.getNodes()) {
            if (node.isLeaf()) {
                if (leafCount > 0) builder.append(", ");
                builder.append(node.getId());
                builder.append("=");
                builder.append(node.getMetaData(traitName));
                leafCount += 1;
            }
        }
        return builder.toString();
    }

    @Override
    public Class<StructuredCoalescentRateShifts> getGeneratorClass() {
        return StructuredCoalescentRateShifts.class;
    }

    @Override
    public Class<mascot.distribution.Mascot> getBEASTClass() {
        return mascot.distribution.Mascot.class;
    }
}
