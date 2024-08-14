package mascot.lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.evolution.alignment.Taxon;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.tree.TraitSet;
import beast.base.inference.parameter.RealParameter;
import lphy.base.evolution.coalescent.StructuredCoalescent;
import lphy.base.evolution.tree.TimeTree;
import lphy.base.evolution.tree.TimeTreeNode;
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

public class StructuredCoalescentToMascot implements
        GeneratorToBEAST<StructuredCoalescent, mascot.distribution.Mascot> {

    @Override
    public mascot.distribution.Mascot generatorToBEAST(StructuredCoalescent coalescent, BEASTInterface value, BEASTContext context) {

        mascot.distribution.Mascot mascot = new mascot.distribution.Mascot();

        Value<Double[][]> M = coalescent.getM();

        if (!coalescent.isSort())
            throw new IllegalArgumentException("BEAST MASCOT sorts the demes, please set 'sort = true' in StructuredCoalescent !");

        if (M.getGenerator() instanceof MigrationMatrix) {
            Value<Double[]> NeValue = ((MigrationMatrix) M.getGenerator()).getTheta();
            Value<Double[]> backwardsMigrationRates = ((MigrationMatrix) M.getGenerator()).getMigrationRates();
//            Value<Object[]> demes = coalescent.getDemes();

            List<String> uniqueDemes = coalescent.getUniqueDemes();
            String uniqueDemesStr = Arrays.toString(uniqueDemes.toArray(String[]::new))
                    .replace(",", "")  //remove the commas
                    .replace("[", "")  //remove the right bracket
                    .replace("]", "");  //remove the left bracket

            //*** set keys to log location in names ***//

            BEASTInterface ne =  context.getBEASTObject(NeValue);
            BEASTInterface bMR =  context.getBEASTObject(backwardsMigrationRates);

            if ( ! ( (ne instanceof RealParameter) || (bMR instanceof RealParameter) ) )
                throw new IllegalArgumentException("Ne and backwardsMigration have to be KeyRealParameter !");
            RealParameter neParam =  (RealParameter) ne;
            RealParameter bMRParam =  (RealParameter) bMR;

            // set keys to Ne
            if (uniqueDemes.size() != neParam.getDimension())
                throw new IllegalArgumentException("Ne dimension " + neParam.getDimension() +
                        " != " + uniqueDemes.size() + " unique demes !");
            neParam.setInputValue("keys", uniqueDemesStr);
            neParam.initAndValidate();
            System.out.println("Assign locations to Ne : " + uniqueDemes);

            // set keys to Migration rates
            // asymmetric dimension = n*(n-1), symmetric dimension = n*(n-1)/2
            int n = uniqueDemes.size();
            String migRatesStr = "";
            if (bMRParam.getDimension() == n*(n-1)) { // asymmetric

                for(int i = 0; i < uniqueDemes.size(); i++) {
                    for(int j = 0; j < uniqueDemes.size(); j++) {
                        if (i != j) {
                            if (migRatesStr.length() > 1) migRatesStr += " ";
                            // assuming always backwards from LPhy
                            migRatesStr += uniqueDemes.get(i) + "_" + uniqueDemes.get(j);
                        }
                    }
                }
                System.out.println("Assign locations to asymmetric backwards migration rates : " + migRatesStr);

            } else if (bMRParam.getDimension() == n*(n-1)/2) { // symmetric

                for(int i = 0; i < uniqueDemes.size(); i++) {
                    for(int j = i+1; j < uniqueDemes.size(); j++) {
                        if (i != j) {
                            if (migRatesStr.length() > 1) migRatesStr += " ";
                            // assuming always backwards from LPhy
                            migRatesStr += uniqueDemes.get(i) + "_" + uniqueDemes.get(j);
                        }
                    }
                }
                System.out.println("Assign locations to symmetric backwards migration rates : " + migRatesStr);

            } else {
                throw new IllegalArgumentException("Migration rates dimension " + bMRParam.getDimension() +
                        " does not equal to either asymmetric model " + n*(n-1) +
                        " or symmetric model " + (n*(n-1)/2) + " !");
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

            // this will log popsize and rates with postfix of each location
            // but it is duplicated to popsize and rates Loggable.
            // <log idref="Constant"/>
//            context.addExtraLogger(dynamics);
            // <log idref="Mascot"/>
            context.addExtraLoggable(mascot);
            // extra logging
            MascotExtraTreeLogger extraTreeLogger = new MascotExtraTreeLogger(mascot);
            context.addExtraLogger(extraTreeLogger);

            return mascot;
        }
        throw new RuntimeException("Can't convert StructuredCoalescent unless MigrationMatrix function is used to form M matrix");
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
