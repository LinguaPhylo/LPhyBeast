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

        // Create MASCOT components
        mascot.distribution.Mascot mascot = new mascot.distribution.Mascot();

        // === Migration GLM ===
        LogLinear migrationGLM = createMigrationGLM(mValue, nDemes, nIntervals, context);

        // === Ne GLM ===
        LogLinear neGLM = createNeGLM(thetaValue, nDemes, nIntervals, context);

        // === Rate Shifts ===
        RateShifts rateShifts = new RateShifts();
        List<Double> shiftTimesList = new ArrayList<>(Arrays.asList(rateShiftTimes));
        rateShifts.setInputValue("value", shiftTimesList);
        rateShifts.initAndValidate();

        // === GLM Dynamics ===
        GLM dynamics = new GLM();
        dynamics.setInputValue("migrationGLM", migrationGLM);
        dynamics.setInputValue("NeGLM", neGLM);
        dynamics.setInputValue("rateShifts", rateShifts);
        dynamics.setInputValue("dimension", nDemes);
        dynamics.setInputValue("fromBeauti", false);

        // Create type trait set
        String popLabel = StructuredCoalescentRateShifts.populationLabel;
        TimeTree timeTree = ((Value<TimeTree>) context.getGraphicalModelNode(value)).value();
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
     * Create the migration GLM model.
     * If m comes from GeneralLinearFunction, use the GLM parameters.
     * Otherwise, treat the m values as intercept covariates.
     */
    private LogLinear createMigrationGLM(Value<Double[]> mValue, int nDemes, int nIntervals, BEASTContext context) {

        int nMigRates = nDemes * (nDemes - 1);
        int totalMigRates = nIntervals * nMigRates;

        GLMParams params = extractGLMParams(mValue.getGenerator());

        if (params != null) {
            // m comes from GeneralLinearFunction - use full GLM
            String link = (params.link() != null) ? params.link().value() : "identity";
            if (!"log".equalsIgnoreCase(link)) {
                throw new IllegalArgumentException("MASCOT GLM requires log link function for migration, got: " + link);
            }
            return createGLMFromParams(params, nMigRates, nIntervals, "migration", context);
        } else {
            // m is constant data - create intercept-only GLM
            return createInterceptGLM(mValue.value(), nMigRates, nIntervals, "migration");
        }
    }

    /**
     * Create the Ne GLM model.
     * If theta comes from GeneralLinearFunction, use the GLM parameters.
     * Otherwise, treat the theta values as intercept covariates.
     */
    private LogLinear createNeGLM(Value<Double[]> thetaValue, int nDemes, int nIntervals, BEASTContext context) {

        int totalNe = nIntervals * nDemes;

        GLMParams params = extractGLMParams(thetaValue.getGenerator());

        if (params != null) {
            // theta comes from GeneralLinearFunction - use full GLM
            String link = (params.link() != null) ? params.link().value() : "identity";
            if (!"log".equalsIgnoreCase(link)) {
                throw new IllegalArgumentException("MASCOT GLM requires log link function for Ne, got: " + link);
            }
            return createGLMFromParams(params, nDemes, nIntervals, "Ne", context);
        } else {
            // theta is constant data - create intercept-only GLM
            return createInterceptGLM(thetaValue.value(), nDemes, nIntervals, "Ne");
        }
    }

    /**
     * Create a LogLinear GLM from GeneralLinearFunction parameters.
     */
    private LogLinear createGLMFromParams(GLMParams params, int verticalEntries, int nIntervals,
                                           String namePrefix, BEASTContext context) {

        Double[] beta = params.beta().value();
        int nPredictors = beta.length;

        // Extract covariates from design matrix
        List<Covariate> covariates = extractCovariates(params.x(), nPredictors, verticalEntries, nIntervals, namePrefix);

        CovariateList covariateList = new CovariateList();
        covariateList.setInputValue("covariates", covariates);
        covariateList.initAndValidate();

        // Get or create scaler parameter (beta coefficients)
        RealParameter scalerParam = (RealParameter) context.getBEASTObject(params.beta());
        scalerParam.setID(namePrefix + "ScalerGLM");

        // Create indicator parameter (all true = all predictors included)
        Boolean[] indicators = new Boolean[nPredictors];
        Arrays.fill(indicators, true);
        BooleanParameter indicatorParam = new BooleanParameter();
        indicatorParam.setInputValue("value", Arrays.asList(indicators));
        indicatorParam.setID(namePrefix + "IndicatorGLM");
        indicatorParam.initAndValidate();

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
    private LogLinear createInterceptGLM(Double[] values, int verticalEntries, int nIntervals, String namePrefix) {

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
                                               int nIntervals, String namePrefix) {
        List<Covariate> covariates = new ArrayList<>();
        Object x = xValue.value();
        int expectedRows = nIntervals * verticalEntries;

        if (x instanceof Double[][] matrix) {
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
                    true
            );
        }
        return null;
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
