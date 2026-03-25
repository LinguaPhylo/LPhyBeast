package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.evolution.alignment.Taxon;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.operator.TipDatesRandomWalker;
import beast.base.evolution.tree.TraitSet;
import beast.base.evolution.tree.Tree;
import beast.base.inference.Operator;
import beast.base.inference.StateNode;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.evolution.tree.MRCAPrior;
import beast.base.spec.inference.distribution.*;
import beast.base.spec.inference.parameter.RealScalarParam;
import lphy.base.evolution.tree.LeafCalibrations;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

import java.util.ArrayList;
import java.util.List;

public class LeafCalibrationsToBEAST implements GeneratorToBEAST<LeafCalibrations, MRCAPrior> {

    @Override
    public MRCAPrior generatorToBEAST(LeafCalibrations generator, BEASTInterface value, BEASTContext context) {
        List<LeafCalibrations.TipCalibration> tipCalibrationList = generator.getCalibrations();

        // Remove the tipDates RealParameter from the context, and suppress its auto-generated operator.
        context.getElements().keySet().stream()
                .filter(b -> b instanceof StateNode && "tipDates".equals(b.getID()))
                .findFirst()
                .ifPresent(tipDatesParam -> {
                    // Suppress any auto-generated operator for this state node
                    context.addSkipOperator((StateNode) tipDatesParam);
                    // Remove it from the context entirely
                    context.removeBEASTObject(tipDatesParam);
                });

        // also remove any already-registered extra operator for tipDates
        context.getExtraOperators().removeIf(op ->
                op.getID() != null && op.getID().startsWith("tipDates"));

        // find the tree
        Tree tree = context.getElements().keySet().stream()
                .filter(b -> b instanceof Tree)
                .map(b -> (Tree) b)
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "No tree found in BEASTContext for LeafCalibrations"));

        // rewrite the traisets to date-forward with nominal (point) calendar years
        List<TraitSet> traitSets = (List<TraitSet>) tree.getInputValue("trait");
        if (traitSets != null && !traitSets.isEmpty()) {
            TraitSet traitSet = traitSets.get(0);
            traitSet.traitNameInput.setValue("date-forward", traitSet);

            StringBuilder traitSB = new StringBuilder();
            for (int i = 0; i < tipCalibrationList.size(); i++) {
                LeafCalibrations.TipCalibration cal = tipCalibrationList.get(i);
                if (i > 0) traitSB.append(",\n");
                traitSB.append(cal.taxonName).append("=").append(cal.sampleAge());
            }
            traitSet.traitsInput.setValue(traitSB.toString(), traitSet);
            traitSet.initAndValidate();
        }


        // calc per-walker weight = treeUniformWeight * 0.8 / No.leafCalibrations
        // look up tree.uniform operator (BactrianNodeOperator) by id (since it's automatic)
        long nNonFixed = tipCalibrationList.stream().filter(c -> !c.distributionName.equals("fixed")).count();

        double treeUniformWeight = context.getExtraOperators().stream()
                .filter(op -> op.getID() != null && op.getID().startsWith("tree.uniform"))
                .mapToDouble(Operator::getWeight)
                .findFirst()
                .orElseGet(() -> BEASTContext.getOperatorWeight(tree.getLeafNodeCount()));

        double walkerWeight = (nNonFixed > 0)
                ? (treeUniformWeight * 0.8 / nNonFixed)
                : 1.0;


        // build mrca prior and operator
        MRCAPrior lastPrior = null;

        for (LeafCalibrations.TipCalibration calibration : tipCalibrationList) {
            if (calibration.distributionName.equals("fixed")) {
                continue;
            }

            // --- MRCAPrior ---
            MRCAPrior prior = new MRCAPrior();
            prior.setInputValue("tree", tree);

            List<Taxon> taxonList = new ArrayList<>();
            taxonList.add(tree.getTaxonset().getTaxon(calibration.taxonName));

            TaxonSet taxonSet = new TaxonSet(taxonList);
            taxonSet.setID(calibration.taxonName + ".leaf");

            prior.setInputValue("taxonset", taxonSet);
            prior.setInputValue("monophyletic", true);
            prior.setInputValue("tipsonly", true);
            setDistr(prior, calibration);
            prior.initAndValidate();

            // --- TipDatesRandomWalker with scaled weight ---
            TipDatesRandomWalker walker = new TipDatesRandomWalker();
            walker.setID("TipDatesRandomWalker." + calibration.taxonName + ".leaf");
            walker.setInputValue("taxonset", taxonSet);
            walker.setInputValue("tree", tree);
            walker.setInputValue("weight", walkerWeight);
            walker.setInputValue("windowSize", 1.0);
            walker.initAndValidate();

            context.addExtraOperator(walker);

            if (lastPrior != null) {
                context.addBEASTObject(lastPrior, generator);
            }
            lastPrior = prior;
        }

        // The final MRCAPrior is returned so the framework registers it
        return lastPrior;
    }

    /*
     * Supported distributions:
     * normal, uniform, exponential, offsetexponential,
     * truncatednormal, lognormal, offsetlognormal, gamma, offsetgamma
     */
    private void setDistr(MRCAPrior prior, LeafCalibrations.TipCalibration calibration) {
        String distributionName = calibration.distributionName;
        double[] p = calibration.computedParams;

        if (distributionName.equals("normal")) {
            Normal normalDist = new Normal();
            normalDist.setInputValue("mean", new RealScalarParam<>(p[0], Real.INSTANCE));
            normalDist.setInputValue("sigma", new RealScalarParam<>(p[1], PositiveReal.INSTANCE));
            normalDist.initAndValidate();
            prior.setInputValue("distr", normalDist);

        } else if (distributionName.equals("uniform")) {
            Uniform uniformDist = new Uniform();
            uniformDist.setInputValue("lower", new RealScalarParam<>(p[0], Real.INSTANCE));
            uniformDist.setInputValue("upper", new RealScalarParam<>(p[1], Real.INSTANCE));
            uniformDist.initAndValidate();
            prior.setInputValue("distr", uniformDist);

        } else if (distributionName.equals("exponential")) {
            Exponential expDist = new Exponential();
            expDist.setInputValue("mean", new RealScalarParam<>(p[0], PositiveReal.INSTANCE));
            expDist.initAndValidate();
            prior.setInputValue("distr", expDist);

        } else if (distributionName.equals("offsetexponential")) {
            Exponential expDist = new Exponential();
            expDist.setInputValue("mean", new RealScalarParam<>(p[1], PositiveReal.INSTANCE));
            expDist.initAndValidate();
            OffsetReal offsetDist = new OffsetReal();
            offsetDist.setInputValue("distribution", expDist);
            offsetDist.setInputValue("offset", new RealScalarParam<>(p[0], Real.INSTANCE));
            offsetDist.initAndValidate();
            prior.setInputValue("distr", offsetDist);

        } else if (distributionName.equals("truncatednormal")) {
            Normal truncNormalDist = new Normal();
            truncNormalDist.setInputValue("mean", new RealScalarParam<>(p[1], Real.INSTANCE));
            truncNormalDist.setInputValue("sigma", new RealScalarParam<>(p[2], PositiveReal.INSTANCE));
            truncNormalDist.initAndValidate();
            OffsetReal offsetDist = new OffsetReal(truncNormalDist, p[0]);
            prior.setInputValue("distr", offsetDist);

        } else if (distributionName.equals("lognormal")) {
            LogNormal logNormalDist = new LogNormal();
            logNormalDist.setInputValue("M", new RealScalarParam<>(p[0], Real.INSTANCE));
            logNormalDist.setInputValue("S", new RealScalarParam<>(p[1], PositiveReal.INSTANCE));
            logNormalDist.setInputValue("meanInRealSpace", false);
            logNormalDist.initAndValidate();
            prior.setInputValue("distr", logNormalDist);

        } else if (distributionName.equals("offsetlognormal")) {
            LogNormal logNormalDist = new LogNormal();
            logNormalDist.setInputValue("M", new RealScalarParam<>(p[1], Real.INSTANCE));
            logNormalDist.setInputValue("S", new RealScalarParam<>(p[2], PositiveReal.INSTANCE));
            logNormalDist.setInputValue("meanInRealSpace", false);
            logNormalDist.initAndValidate();
            OffsetReal offsetDist = new OffsetReal();
            offsetDist.setInputValue("distribution", logNormalDist);
            offsetDist.setInputValue("offset", new RealScalarParam<>(p[0], Real.INSTANCE));
            offsetDist.initAndValidate();
            prior.setInputValue("distr", offsetDist);

        } else if (distributionName.equals("gamma")) {
            Gamma gammaDist = new Gamma();
            gammaDist.setInputValue("alpha", new RealScalarParam<>(p[0], PositiveReal.INSTANCE));
            gammaDist.setInputValue("beta", new RealScalarParam<>(p[1], PositiveReal.INSTANCE));
            gammaDist.initAndValidate();
            prior.setInputValue("distr", gammaDist);

        } else if (distributionName.equals("offsetgamma")) {
            Gamma gammaDist = new Gamma();
            gammaDist.setInputValue("alpha", new RealScalarParam<>(p[1], PositiveReal.INSTANCE));
            gammaDist.setInputValue("beta", new RealScalarParam<>(p[2], PositiveReal.INSTANCE));
            gammaDist.initAndValidate();
            OffsetReal offsetDist = new OffsetReal();
            offsetDist.setInputValue("distribution", gammaDist);
            offsetDist.setInputValue("offset", new RealScalarParam<>(p[0], Real.INSTANCE));
            offsetDist.initAndValidate();
            prior.setInputValue("distr", offsetDist);

        } else {
            throw new IllegalArgumentException("Unknown distribution name: " + distributionName);
        }
    }

    @Override
    public Class<LeafCalibrations> getGeneratorClass() {
        return LeafCalibrations.class;
    }
}