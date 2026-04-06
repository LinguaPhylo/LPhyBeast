package mascot.lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.evolution.alignment.Taxon;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.tree.TraitSet;
import beast.base.spec.inference.parameter.RealVectorParam;
import lphy.base.evolution.coalescent.StructuredCoalescent;
import lphy.base.evolution.tree.TimeTree;
import lphy.base.evolution.tree.TimeTreeNode;
import lphy.base.function.GeneralLinearFunction;
import lphy.base.function.MigrationMatrix;
import lphy.core.model.Value;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;
import lphybeast.tobeast.values.TimeTreeToBEAST;
import mascot.distribution.StructuredTreeIntervals;
import mascot.dynamics.Constant;
import mascot.lphybeast.tobeast.loggers.MascotExtraTreeLogger;

import java.util.Arrays;
import java.util.List;

/**
 * Converts LPhy StructuredCoalescent to BEAST3 MASCOT.
 * <p>
 * If migration rates come from a {@link GeneralLinearFunction}, delegates to
 * {@link StructuredCoalescentToGLM} for GLM-based MASCOT conversion.
 * Otherwise, uses constant dynamics.
 * </p>
 */
public class StructuredCoalescentToMascot implements
        GeneratorToBEAST<StructuredCoalescent, mascot.distribution.Mascot> {

    @Override
    public mascot.distribution.Mascot generatorToBEAST(StructuredCoalescent coalescent, BEASTInterface value, BEASTContext context) {

        // Check if this uses GLM-based migration rates
        if (StructuredCoalescentToGLM.usesGLM(coalescent)) {
            System.out.println("Detected GLM-based migration rates, using MASCOT GLM dynamics");
            StructuredCoalescentToGLM glmConverter = new StructuredCoalescentToGLM();
            return glmConverter.generatorToBEAST(coalescent, value, context);
        }

        mascot.distribution.Mascot mascot = new mascot.distribution.Mascot();

        Value<Double[][]> M = coalescent.getM();

        if (!coalescent.isSort())
            throw new IllegalArgumentException("BEAST MASCOT sorts the demes, please set 'sort = true' in StructuredCoalescent !");

        if (M.getGenerator() instanceof MigrationMatrix) {
            Value<Double[]> NeValue = ((MigrationMatrix) M.getGenerator()).getTheta();
            Value<Double[]> backwardsMigrationRates = ((MigrationMatrix) M.getGenerator()).getMigrationRates();

            List<String> uniqueDemes = coalescent.getUniqueDemes();

            BEASTInterface ne = context.getBEASTObject(NeValue);
            BEASTInterface bMR = context.getBEASTObject(backwardsMigrationRates);

            if (!(ne instanceof RealVectorParam<?>) || !(bMR instanceof RealVectorParam<?>))
                throw new IllegalArgumentException("Ne and backwardsMigration must be RealVectorParam!");
            RealVectorParam<?> neParam = (RealVectorParam<?>) ne;
            RealVectorParam<?> bMRParam = (RealVectorParam<?>) bMR;

            int n = uniqueDemes.size();
            String uniqueDemesStr = String.join(" ", uniqueDemes);

            //*** set keys to log location names ***//

            // set keys to Ne
            if (n != neParam.size())
                throw new IllegalArgumentException("Ne dimension " + neParam.size() +
                        " != " + n + " unique demes!");
            neParam.setInputValue("keys", uniqueDemesStr);
            neParam.initAndValidate();
            System.out.println("Assign locations to Ne : " + uniqueDemes);

            // set keys to migration rates
            int bMRSize = bMRParam.size();
            String migRatesStr;
            if (bMRSize == n * (n - 1)) { // asymmetric
                migRatesStr = buildAsymmetricMigrationKeys(uniqueDemes);
                System.out.println("Assign locations to asymmetric backwards migration rates : " + migRatesStr);
            } else if (bMRSize == n * (n - 1) / 2) { // symmetric
                migRatesStr = buildSymmetricMigrationKeys(uniqueDemes);
                System.out.println("Assign locations to symmetric backwards migration rates : " + migRatesStr);
            } else {
                throw new IllegalArgumentException("Migration rates dimension " + bMRSize +
                        " does not match asymmetric " + n * (n - 1) +
                        " or symmetric " + (n * (n - 1) / 2) + "!");
            }
            bMRParam.setInputValue("keys", migRatesStr);
            bMRParam.initAndValidate();

            Constant dynamics = new Constant();
            dynamics.setInputValue("Ne", neParam);
            dynamics.setInputValue("backwardsMigration", bMRParam);
            dynamics.setInputValue("dimension", NeValue.value().length);

            String popLabel = coalescent.getPopulationLabel();

            TimeTree timeTree = ((Value<TimeTree>)context.getGraphicalModelNode(value)).value();
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

            mascot.setInputValue("dynamics", dynamics);

            StructuredTreeIntervals structuredTreeIntervals = new StructuredTreeIntervals();
            structuredTreeIntervals.setInputValue("tree", value);
            structuredTreeIntervals.initAndValidate();

            mascot.setInputValue("structuredTreeIntervals", structuredTreeIntervals);
            mascot.setInputValue("tree", value);

            mascot.initAndValidate();

            context.addExtraLoggable(mascot);
            MascotExtraTreeLogger extraTreeLogger = new MascotExtraTreeLogger(mascot);
            context.addExtraLogger(extraTreeLogger);

            return mascot;
        }
        throw new RuntimeException("Can't convert StructuredCoalescent unless MigrationMatrix function is used to form M matrix");
    }

    private static String buildAsymmetricMigrationKeys(List<String> demes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < demes.size(); i++) {
            for (int j = 0; j < demes.size(); j++) {
                if (i != j) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(demes.get(i)).append("_").append(demes.get(j));
                }
            }
        }
        return sb.toString();
    }

    private static String buildSymmetricMigrationKeys(List<String> demes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < demes.size(); i++) {
            for (int j = i + 1; j < demes.size(); j++) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(demes.get(i)).append("_").append(demes.get(j));
            }
        }
        return sb.toString();
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
