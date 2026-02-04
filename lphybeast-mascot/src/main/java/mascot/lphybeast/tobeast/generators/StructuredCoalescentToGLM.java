package mascot.lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.evolution.alignment.Taxon;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.tree.TraitSet;
import beast.base.inference.parameter.BooleanParameter;
import beast.base.inference.parameter.RealParameter;
import lphy.base.evolution.coalescent.StructuredCoalescent;
import lphy.base.evolution.tree.TimeTree;
import lphy.base.evolution.tree.TimeTreeNode;
import lphy.base.function.GeneralLinearFunction;
import lphy.base.function.MigrationMatrix;
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
 * Converts LPhy StructuredCoalescent with GLM-based migration rates to BEAST2 MASCOT GLM.
 * <p>
 * This converter handles the case where migration rates and/or population sizes are computed via
 * {@link GeneralLinearFunction} with a log link, enabling GLM-based phylogeography.
 * </p>
 * <p>
 * LPhy usage (migration GLM with scale):
 * <pre>
 * beta ~ Normal(mean=0.0, sd=1.0, replicates=nPredictors);
 * migrationScale ~ LogNormal(meanlog=-2.0, sdlog=1.0);
 * m = generalLinearFunction(beta=beta, x=X, link="log", scale=migrationScale);
 * M = migrationMatrix(theta=Θ, m=m);
 * ψ ~ StructuredCoalescent(M=M, ...);
 * </pre>
 * </p>
 * <p>
 * LPhy usage (full GLM for both Ne and migration):
 * <pre>
 * beta_Ne ~ Normal(mean=0.0, sd=1.0, replicates=nPredictors);
 * Ne_scale ~ LogNormal(meanlog=0.0, sdlog=1.0);
 * Theta = generalLinearFunction(beta=beta_Ne, x=X_Ne, link="log", scale=Ne_scale);
 *
 * beta_m ~ Normal(mean=0.0, sd=1.0, replicates=nPredictors);
 * m_scale ~ LogNormal(meanlog=-2.0, sdlog=1.0);
 * m = generalLinearFunction(beta=beta_m, x=X_m, link="log", scale=m_scale);
 *
 * M = migrationMatrix(theta=Theta, m=m);
 * ψ ~ StructuredCoalescent(M=M, ...);
 * </pre>
 * </p>
 *
 * @author Alexei Drummond
 */
public class StructuredCoalescentToGLM implements
        GeneratorToBEAST<StructuredCoalescent, mascot.distribution.Mascot> {

    /**
     * Check if the StructuredCoalescent uses GLM-based migration rates or population sizes.
     */
    public static boolean usesGLM(StructuredCoalescent coalescent) {
        Value<Double[][]> M = coalescent.getM();
        if (M.getGenerator() instanceof MigrationMatrix migMatrix) {
            Value<Double[]> migRates = migMatrix.getMigrationRates();
            Value<Double[]> theta = migMatrix.getTheta();
            // Use GLM converter if either migration rates or theta come from GeneralLinearFunction
            // (either directly or via VectorizedFunction)
            return isGeneralLinearFunction(migRates.getGenerator()) ||
                   isGeneralLinearFunction(theta.getGenerator());
        }
        return false;
    }

    /**
     * Check if a generator is a GeneralLinearFunction (either directly or vectorized).
     */
    private static boolean isGeneralLinearFunction(Generator<?> generator) {
        if (generator instanceof GeneralLinearFunction) {
            return true;
        }
        if (generator instanceof VectorizedFunction<?> vf) {
            // Check if the component functions are GeneralLinearFunction
            return !vf.getComponentFunctions().isEmpty() &&
                   vf.getComponentFunctions().get(0) instanceof GeneralLinearFunction;
        }
        return false;
    }

    /**
     * Extract GLM parameters from either a GeneralLinearFunction or VectorizedFunction.
     * Returns null if not a GLM.
     */
    private static record GLMParams(
            Value<Double[]> beta,
            Value<?> x,  // Could be Double[] or Double[][]
            Value<String> link,
            Value<Double> scale,
            boolean isVectorized
    ) {}

    private static GLMParams extractGLMParams(Generator<?> generator) {
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
            // Get params from the VectorizedFunction - these are the original (non-sliced) values
            return new GLMParams(
                    vf.getParams().get(GeneralLinearFunction.betaParamName),
                    vf.getParams().get(GeneralLinearFunction.xParamName),  // This is the full matrix
                    vf.getParams().get(GeneralLinearFunction.linkParamName),
                    vf.getParams().get(GeneralLinearFunction.scaleParamName),
                    true
            );
        }
        return null;
    }

    @Override
    public mascot.distribution.Mascot generatorToBEAST(StructuredCoalescent coalescent,
                                                        BEASTInterface value,
                                                        BEASTContext context) {

        if (!coalescent.isSort())
            throw new IllegalArgumentException("BEAST MASCOT sorts the demes, please set 'sort = true' in StructuredCoalescent!");

        Value<Double[][]> M = coalescent.getM();
        if (!(M.getGenerator() instanceof MigrationMatrix migMatrix))
            throw new RuntimeException("GLM converter requires MigrationMatrix function for M matrix");

        Value<Double[]> migRatesValue = migMatrix.getMigrationRates();
        Value<Double[]> NeValue = migMatrix.getTheta();
        List<String> uniqueDemes = coalescent.getUniqueDemes();
        int nDemes = uniqueDemes.size();

        // Create MASCOT GLM components
        mascot.distribution.Mascot mascot = new mascot.distribution.Mascot();

        // === Migration GLM ===
        LogLinear migrationGLM;
        int migIntervals = 1;
        GLMParams migParams = extractGLMParams(migRatesValue.getGenerator());
        if (migParams != null) {
            String link = (migParams.link() != null) ? migParams.link().value() : "identity";
            if (!"log".equalsIgnoreCase(link)) {
                throw new IllegalArgumentException("MASCOT GLM requires log link function for migration, but got: " + link);
            }
            GLMResult migResult = createMigrationGLM(migParams, nDemes, context);
            migrationGLM = migResult.glm();
            migIntervals = migResult.nIntervals();
        } else {
            // Migration rates are not from GLM - create simple constant migration GLM
            migrationGLM = createSimpleMigrationGLM(migRatesValue, nDemes, context);
        }

        // === Ne GLM ===
        LogLinear neGLM;
        int neIntervals = 1;
        GLMParams neParams = extractGLMParams(NeValue.getGenerator());
        if (neParams != null) {
            String link = (neParams.link() != null) ? neParams.link().value() : "identity";
            if (!"log".equalsIgnoreCase(link)) {
                throw new IllegalArgumentException("MASCOT GLM requires log link function for Ne, but got: " + link);
            }
            GLMResult neResult = createNeGLM(neParams, nDemes, context);
            neGLM = neResult.glm();
            neIntervals = neResult.nIntervals();
        } else {
            // Ne is not from GLM - create simple constant Ne GLM
            neGLM = createSimpleNeGLM(NeValue, nDemes, context);
        }

        // Verify consistent number of intervals
        int nIntervals = Math.max(migIntervals, neIntervals);
        if (migIntervals > 1 && neIntervals > 1 && migIntervals != neIntervals) {
            throw new IllegalArgumentException(
                "Migration and Ne have different numbers of intervals: " + migIntervals + " vs " + neIntervals);
        }

        // === Rate Shifts ===
        // Create rate shifts based on detected number of intervals
        RateShifts rateShifts = new RateShifts();
        if (nIntervals == 1) {
            // Single epoch - use Infinity
            rateShifts.setInputValue("value", Double.POSITIVE_INFINITY);
        } else {
            // Multiple intervals - create evenly spaced rate shifts starting at 0
            // Times are: 0.0, 1.0, 2.0, ... (nIntervals-1).0
            // These are placeholder times; actual times should come from LPhy data block
            List<Double> shiftTimes = new ArrayList<>();
            for (int i = 0; i < nIntervals; i++) {
                shiftTimes.add((double) i);
            }
            rateShifts.setInputValue("value", shiftTimes);
        }
        rateShifts.initAndValidate();

        // === GLM Dynamics ===
        GLM dynamics = new GLM();
        dynamics.setInputValue("migrationGLM", migrationGLM);
        dynamics.setInputValue("NeGLM", neGLM);
        dynamics.setInputValue("rateShifts", rateShifts);
        dynamics.setInputValue("dimension", nDemes);
        dynamics.setInputValue("fromBeauti", false);

        // Create type trait set
        String popLabel = coalescent.getPopulationLabel();
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
     * Result of creating a GLM, including detected number of intervals.
     */
    private record GLMResult(LogLinear glm, int nIntervals) {}

    /**
     * Create the migration GLM model from LPhy GeneralLinearFunction parameters.
     */
    private GLMResult createMigrationGLM(GLMParams params, int nDemes, BEASTContext context) {

        Double[] beta = params.beta().value();
        int nPredictors = beta.length;
        int nMigRates = nDemes * (nDemes - 1);

        // Extract covariate matrix - detect time-varying if present
        CovariateResult covResult = extractCovariates(params.x(), nPredictors, nMigRates, "migration_predictor_");
        int nIntervals = covResult.nIntervals();

        CovariateList covariateList = new CovariateList();
        covariateList.setInputValue("covariates", covResult.covariates());
        covariateList.initAndValidate();

        // Get or create scaler parameter (beta coefficients)
        RealParameter scalerParam = (RealParameter) context.getBEASTObject(params.beta());
        scalerParam.setID("migrationScalerGLM");

        // Create indicator parameter (all 1s = all predictors included)
        Boolean[] indicators = new Boolean[nPredictors];
        Arrays.fill(indicators, true);
        BooleanParameter indicatorParam = new BooleanParameter();
        indicatorParam.setInputValue("value", Arrays.asList(indicators));
        indicatorParam.setID("migrationIndicatorGLM");
        indicatorParam.initAndValidate();

        // Create clock parameter from scale value, or default 1.0
        RealParameter clockParam;
        if (params.scale() != null) {
            clockParam = (RealParameter) context.getBEASTObject(params.scale());
            clockParam.setID("migrationClockGLM");
        } else {
            clockParam = new RealParameter();
            clockParam.setInputValue("value", "1.0");
            clockParam.setID("migrationClockGLM");
            clockParam.initAndValidate();
        }

        // Create LogLinear GLM model
        LogLinear glm = new LogLinear();
        glm.setInputValue("covariateList", covariateList);
        glm.setInputValue("scaler", scalerParam);
        glm.setInputValue("indicator", indicatorParam);
        glm.setInputValue("clock", clockParam);
        glm.setID("migrationGLM");

        // Set nrIntervals and verticalEntries before initAndValidate
        glm.nrIntervals = nIntervals;
        glm.verticalEntries = nMigRates;
        glm.initAndValidate();

        return new GLMResult(glm, nIntervals);
    }

    /**
     * Result of extracting covariates, including detected number of intervals.
     */
    private record CovariateResult(List<Covariate> covariates, int nIntervals) {}

    /**
     * Extract covariates from the design matrix.
     * Handles both vectorized (Double[][]) and non-vectorized (Double[]) cases.
     * Detects time-varying covariates when matrix has more rows than single epoch.
     *
     * @param xValue the design matrix value
     * @param nPredictors number of predictor columns
     * @param verticalEntries expected rows per interval (nMigRates or nDemes)
     * @param namePrefix prefix for covariate IDs
     * @return CovariateResult with extracted covariates and detected nIntervals
     */
    private CovariateResult extractCovariates(Value<?> xValue, int nPredictors, int verticalEntries, String namePrefix) {
        List<Covariate> covariates = new ArrayList<>();
        Object x = xValue.value();
        int nIntervals = 1;

        if (x instanceof Double[][] matrix) {
            // Vectorized case: x is a 2D matrix
            int nRows = matrix.length;

            // Detect time-varying: if nRows is a multiple of verticalEntries
            if (nRows > verticalEntries && nRows % verticalEntries == 0) {
                nIntervals = nRows / verticalEntries;
            } else if (nRows != verticalEntries) {
                throw new IllegalArgumentException(
                    "Design matrix has " + nRows + " rows, expected " + verticalEntries +
                    " (single epoch) or a multiple for rate shifts");
            }

            if (matrix.length > 0 && matrix[0].length != nPredictors) {
                throw new IllegalArgumentException(
                    "Design matrix has " + matrix[0].length + " columns, expected " + nPredictors);
            }

            // Create one covariate per predictor (column)
            for (int p = 0; p < nPredictors; p++) {
                Double[] covValues = new Double[nRows];
                for (int i = 0; i < nRows; i++) {
                    covValues[i] = matrix[i][p];
                }
                Covariate cov = new Covariate(covValues, namePrefix + p);
                covariates.add(cov);
            }
        } else if (x instanceof Double[] array) {
            // Non-vectorized case: single row or flattened matrix
            if (array.length == nPredictors) {
                // Same predictor values for all rows (intercept-like)
                for (int p = 0; p < nPredictors; p++) {
                    Double[] covValues = new Double[verticalEntries];
                    Arrays.fill(covValues, array[p]);
                    Covariate cov = new Covariate(covValues, namePrefix + p);
                    covariates.add(cov);
                }
            } else if (array.length >= verticalEntries * nPredictors) {
                // Flattened design matrix - detect time-varying
                int totalValues = array.length / nPredictors;
                if (totalValues > verticalEntries && totalValues % verticalEntries == 0) {
                    nIntervals = totalValues / verticalEntries;
                }
                int nRows = nIntervals * verticalEntries;

                for (int p = 0; p < nPredictors; p++) {
                    Double[] covValues = new Double[nRows];
                    for (int i = 0; i < nRows; i++) {
                        covValues[i] = array[i * nPredictors + p];
                    }
                    Covariate cov = new Covariate(covValues, namePrefix + p);
                    covariates.add(cov);
                }
            } else {
                throw new IllegalArgumentException(
                    "Design matrix x has wrong dimensions. Expected " + nPredictors +
                    " or " + (verticalEntries * nPredictors) + " values, got " + array.length);
            }
        } else {
            throw new IllegalArgumentException("Unexpected type for design matrix: " + x.getClass());
        }

        return new CovariateResult(covariates, nIntervals);
    }

    /**
     * Create a simple migration GLM model for constant migration rates (not from GeneralLinearFunction).
     */
    private LogLinear createSimpleMigrationGLM(Value<Double[]> migRatesValue, int nDemes, BEASTContext context) {

        Double[] migRates = migRatesValue.value();
        int nMigRates = nDemes * (nDemes - 1);

        // Create a single covariate with the migration rate values
        // Note: Do NOT call initAndValidate() - it would overwrite values from empty valuesInput
        Covariate cov = new Covariate(migRates, "migration_intercept");

        List<Covariate> covariates = new ArrayList<>();
        covariates.add(cov);

        CovariateList covariateList = new CovariateList();
        covariateList.setInputValue("covariates", covariates);
        covariateList.initAndValidate();

        // Scaler = 1.0
        RealParameter scalerParam = new RealParameter();
        scalerParam.setInputValue("value", "1.0");
        scalerParam.setID("migrationScalerGLM");
        scalerParam.initAndValidate();

        // Indicator = true
        BooleanParameter indicatorParam = new BooleanParameter();
        indicatorParam.setInputValue("value", "true");
        indicatorParam.setID("migrationIndicatorGLM");
        indicatorParam.initAndValidate();

        // Clock = 1.0
        RealParameter clockParam = new RealParameter();
        clockParam.setInputValue("value", "1.0");
        clockParam.setID("migrationClockGLM");
        clockParam.initAndValidate();

        LogLinear glm = new LogLinear();
        glm.setInputValue("covariateList", covariateList);
        glm.setInputValue("scaler", scalerParam);
        glm.setInputValue("indicator", indicatorParam);
        glm.setInputValue("clock", clockParam);
        glm.setID("migrationGLM");

        glm.nrIntervals = 1;
        glm.verticalEntries = nMigRates;
        glm.initAndValidate();

        return glm;
    }

    /**
     * Create the Ne GLM model from LPhy GeneralLinearFunction parameters.
     */
    private GLMResult createNeGLM(GLMParams params, int nDemes, BEASTContext context) {

        Double[] beta = params.beta().value();
        int nPredictors = beta.length;

        // Extract covariate matrix - detect time-varying if present
        CovariateResult covResult = extractCovariates(params.x(), nPredictors, nDemes, "Ne_predictor_");
        int nIntervals = covResult.nIntervals();

        CovariateList covariateList = new CovariateList();
        covariateList.setInputValue("covariates", covResult.covariates());
        covariateList.initAndValidate();

        // Get or create scaler parameter (beta coefficients)
        RealParameter scalerParam = (RealParameter) context.getBEASTObject(params.beta());
        scalerParam.setID("neScalerGLM");

        // Create indicator parameter (all 1s = all predictors included)
        Boolean[] indicators = new Boolean[nPredictors];
        Arrays.fill(indicators, true);
        BooleanParameter indicatorParam = new BooleanParameter();
        indicatorParam.setInputValue("value", Arrays.asList(indicators));
        indicatorParam.setID("neIndicatorGLM");
        indicatorParam.initAndValidate();

        // Create clock parameter from scale value, or default 1.0
        RealParameter clockParam;
        if (params.scale() != null) {
            clockParam = (RealParameter) context.getBEASTObject(params.scale());
            clockParam.setID("neClockGLM");
        } else {
            clockParam = new RealParameter();
            clockParam.setInputValue("value", "1.0");
            clockParam.setID("neClockGLM");
            clockParam.initAndValidate();
        }

        // Create LogLinear GLM model
        LogLinear glm = new LogLinear();
        glm.setInputValue("covariateList", covariateList);
        glm.setInputValue("scaler", scalerParam);
        glm.setInputValue("indicator", indicatorParam);
        glm.setInputValue("clock", clockParam);
        glm.setID("neGLM");

        // Set nrIntervals and verticalEntries before initAndValidate
        glm.nrIntervals = nIntervals;
        glm.verticalEntries = nDemes;
        glm.initAndValidate();

        return new GLMResult(glm, nIntervals);
    }

    /**
     * Create a simple Ne GLM model for constant Ne values (not from GeneralLinearFunction).
     */
    private LogLinear createSimpleNeGLM(Value<Double[]> NeValue, int nDemes, BEASTContext context) {

        Double[] ne = NeValue.value();

        // Create a single "intercept" covariate with Ne values
        // Note: Do NOT call initAndValidate() - it would overwrite values from empty valuesInput
        Covariate cov = new Covariate(ne, "Ne_intercept");

        List<Covariate> covariates = new ArrayList<>();
        covariates.add(cov);

        CovariateList covariateList = new CovariateList();
        covariateList.setInputValue("covariates", covariates);
        covariateList.initAndValidate();

        // Scaler = 1.0 (just use the Ne values directly)
        RealParameter scalerParam = new RealParameter();
        scalerParam.setInputValue("value", "1.0");
        scalerParam.setID("neScalerGLM");
        scalerParam.initAndValidate();

        // Indicator = true
        BooleanParameter indicatorParam = new BooleanParameter();
        indicatorParam.setInputValue("value", "true");
        indicatorParam.setID("neIndicatorGLM");
        indicatorParam.initAndValidate();

        // Clock = 1.0
        RealParameter clockParam = new RealParameter();
        clockParam.setInputValue("value", "1.0");
        clockParam.setID("neClockGLM");
        clockParam.initAndValidate();

        LogLinear glm = new LogLinear();
        glm.setInputValue("covariateList", covariateList);
        glm.setInputValue("scaler", scalerParam);
        glm.setInputValue("indicator", indicatorParam);
        glm.setInputValue("clock", clockParam);
        glm.setID("neGLM");

        glm.nrIntervals = 1;
        glm.verticalEntries = nDemes;
        glm.initAndValidate();

        return glm;
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
    public Class<StructuredCoalescent> getGeneratorClass() {
        return StructuredCoalescent.class;
    }

    @Override
    public Class<mascot.distribution.Mascot> getBEASTClass() {
        return mascot.distribution.Mascot.class;
    }
}
