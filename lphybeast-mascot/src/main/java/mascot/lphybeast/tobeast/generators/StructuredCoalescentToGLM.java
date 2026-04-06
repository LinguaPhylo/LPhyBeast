package mascot.lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.evolution.alignment.Taxon;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.tree.TraitSet;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.BoolVectorParam;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.inference.parameter.RealVectorParam;
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
 * Converts LPhy StructuredCoalescent with GLM-based migration rates to BEAST3 MASCOT GLM.
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
            return isGeneralLinearFunction(migRates.getGenerator()) ||
                   isGeneralLinearFunction(theta.getGenerator());
        }
        return false;
    }

    private static boolean isGeneralLinearFunction(Generator<?> generator) {
        if (generator instanceof GeneralLinearFunction) {
            return true;
        }
        if (generator instanceof VectorizedFunction<?> vf) {
            return !vf.getComponentFunctions().isEmpty() &&
                   vf.getComponentFunctions().get(0) instanceof GeneralLinearFunction;
        }
        return false;
    }

    private static record GLMParams(
            Value<Double[]> beta,
            Value<?> x,
            Value<String> link,
            Value<Double> scale,
            Value<Boolean[]> indicator,
            Value<Double> error,
            boolean isVectorized
    ) {}

    private static GLMParams extractGLMParams(Generator<?> generator) {
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
            neGLM = createSimpleNeGLM(NeValue, nDemes, context);
        }

        // Verify consistent number of intervals
        int nIntervals = Math.max(migIntervals, neIntervals);
        if (migIntervals > 1 && neIntervals > 1 && migIntervals != neIntervals) {
            throw new IllegalArgumentException(
                "Migration and Ne have different numbers of intervals: " + migIntervals + " vs " + neIntervals);
        }

        // === Rate Shifts ===
        RateShifts rateShifts = new RateShifts();
        if (nIntervals == 1) {
            rateShifts.setInputValue("value", Double.POSITIVE_INFINITY);
        } else {
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

    private record GLMResult(LogLinear glm, int nIntervals) {}

    private GLMResult createMigrationGLM(GLMParams params, int nDemes, BEASTContext context) {

        Double[] beta = params.beta().value();
        int nPredictors = beta.length;
        int nMigRates = nDemes * (nDemes - 1);

        CovariateResult covResult = extractCovariates(params.x(), nPredictors, nMigRates, "migration_predictor_");
        int nIntervals = covResult.nIntervals();

        CovariateList covariateList = new CovariateList();
        covariateList.setInputValue("covariates", covResult.covariates());
        covariateList.initAndValidate();

        RealVectorParam<?> scalerParam = (RealVectorParam<?>) context.getBEASTObject(params.beta());
        scalerParam.setID("migrationScalerGLM");

        BoolVectorParam indicatorParam;
        if (params.indicator() != null) {
            indicatorParam = (BoolVectorParam) context.getBEASTObject(params.indicator());
            indicatorParam.setID("migrationIndicatorGLM");
        } else {
            boolean[] indicators = new boolean[nPredictors];
            Arrays.fill(indicators, true);
            indicatorParam = new BoolVectorParam(indicators);
            indicatorParam.setID("migrationIndicatorGLM");
            indicatorParam.initAndValidate();
        }

        RealScalarParam<Real> clockParam;
        if (params.scale() != null) {
            clockParam = (RealScalarParam<Real>) context.getBEASTObject(params.scale());
            clockParam.setID("migrationClockGLM");
        } else {
            clockParam = new RealScalarParam<>(1.0, Real.INSTANCE);
            clockParam.setID("migrationClockGLM");
            clockParam.initAndValidate();
        }

        LogLinear glm = new LogLinear();
        glm.setInputValue("covariateList", covariateList);
        glm.setInputValue("scaler", scalerParam);
        glm.setInputValue("indicator", indicatorParam);
        glm.setInputValue("clock", clockParam);

        if (params.error() != null) {
            RealVectorParam<?> errorParam = (RealVectorParam<?>) context.getBEASTObject(params.error());
            errorParam.setID("migrationErrorGLM");
            glm.setInputValue("error", errorParam);
        }

        glm.setID("migrationGLM");

        glm.nrIntervals = nIntervals;
        glm.verticalEntries = nMigRates;
        glm.initAndValidate();

        return new GLMResult(glm, nIntervals);
    }

    private record CovariateResult(List<Covariate> covariates, int nIntervals) {}

    private CovariateResult extractCovariates(Value<?> xValue, int nPredictors, int verticalEntries, String namePrefix) {
        List<Covariate> covariates = new ArrayList<>();
        Object x = xValue.value();
        int nIntervals = 1;

        if (x instanceof Double[][] matrix) {
            int nRows = matrix.length;

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

            for (int p = 0; p < nPredictors; p++) {
                Double[] covValues = new Double[nRows];
                for (int i = 0; i < nRows; i++) {
                    covValues[i] = matrix[i][p];
                }
                Covariate cov = new Covariate(covValues, namePrefix + p);
                covariates.add(cov);
            }
        } else if (x instanceof Double[] array) {
            if (array.length == nPredictors) {
                for (int p = 0; p < nPredictors; p++) {
                    Double[] covValues = new Double[verticalEntries];
                    Arrays.fill(covValues, array[p]);
                    Covariate cov = new Covariate(covValues, namePrefix + p);
                    covariates.add(cov);
                }
            } else if (array.length >= verticalEntries * nPredictors) {
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

    private LogLinear createSimpleMigrationGLM(Value<Double[]> migRatesValue, int nDemes, BEASTContext context) {

        Double[] migRates = migRatesValue.value();
        int nMigRates = nDemes * (nDemes - 1);

        Covariate cov = new Covariate(migRates, "migration_intercept");

        List<Covariate> covariates = new ArrayList<>();
        covariates.add(cov);

        CovariateList covariateList = new CovariateList();
        covariateList.setInputValue("covariates", covariates);
        covariateList.initAndValidate();

        RealVectorParam<Real> scalerParam = new RealVectorParam<>(new double[]{1.0}, Real.INSTANCE);
        scalerParam.setID("migrationScalerGLM");
        scalerParam.initAndValidate();

        BoolVectorParam indicatorParam = new BoolVectorParam(new boolean[]{true});
        indicatorParam.setID("migrationIndicatorGLM");
        indicatorParam.initAndValidate();

        RealScalarParam<Real> clockParam = new RealScalarParam<>(1.0, Real.INSTANCE);
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

    private GLMResult createNeGLM(GLMParams params, int nDemes, BEASTContext context) {

        Double[] beta = params.beta().value();
        int nPredictors = beta.length;

        CovariateResult covResult = extractCovariates(params.x(), nPredictors, nDemes, "Ne_predictor_");
        int nIntervals = covResult.nIntervals();

        CovariateList covariateList = new CovariateList();
        covariateList.setInputValue("covariates", covResult.covariates());
        covariateList.initAndValidate();

        RealVectorParam<?> scalerParam = (RealVectorParam<?>) context.getBEASTObject(params.beta());
        scalerParam.setID("neScalerGLM");

        BoolVectorParam indicatorParam;
        if (params.indicator() != null) {
            indicatorParam = (BoolVectorParam) context.getBEASTObject(params.indicator());
            indicatorParam.setID("neIndicatorGLM");
        } else {
            boolean[] indicators = new boolean[nPredictors];
            Arrays.fill(indicators, true);
            indicatorParam = new BoolVectorParam(indicators);
            indicatorParam.setID("neIndicatorGLM");
            indicatorParam.initAndValidate();
        }

        RealScalarParam<Real> clockParam;
        if (params.scale() != null) {
            clockParam = (RealScalarParam<Real>) context.getBEASTObject(params.scale());
            clockParam.setID("neClockGLM");
        } else {
            clockParam = new RealScalarParam<>(1.0, Real.INSTANCE);
            clockParam.setID("neClockGLM");
            clockParam.initAndValidate();
        }

        LogLinear glm = new LogLinear();
        glm.setInputValue("covariateList", covariateList);
        glm.setInputValue("scaler", scalerParam);
        glm.setInputValue("indicator", indicatorParam);
        glm.setInputValue("clock", clockParam);

        if (params.error() != null) {
            RealVectorParam<?> errorParam = (RealVectorParam<?>) context.getBEASTObject(params.error());
            errorParam.setID("neErrorGLM");
            glm.setInputValue("error", errorParam);
        }

        glm.setID("neGLM");

        glm.nrIntervals = nIntervals;
        glm.verticalEntries = nDemes;
        glm.initAndValidate();

        return new GLMResult(glm, nIntervals);
    }

    private LogLinear createSimpleNeGLM(Value<Double[]> NeValue, int nDemes, BEASTContext context) {

        Double[] ne = NeValue.value();

        Covariate cov = new Covariate(ne, "Ne_intercept");

        List<Covariate> covariates = new ArrayList<>();
        covariates.add(cov);

        CovariateList covariateList = new CovariateList();
        covariateList.setInputValue("covariates", covariates);
        covariateList.initAndValidate();

        RealVectorParam<Real> scalerParam = new RealVectorParam<>(new double[]{1.0}, Real.INSTANCE);
        scalerParam.setID("neScalerGLM");
        scalerParam.initAndValidate();

        BoolVectorParam indicatorParam = new BoolVectorParam(new boolean[]{true});
        indicatorParam.setID("neIndicatorGLM");
        indicatorParam.initAndValidate();

        RealScalarParam<Real> clockParam = new RealScalarParam<>(1.0, Real.INSTANCE);
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
