package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.evolution.alignment.Taxon;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.speciation.CalibratedYuleModel;
import beast.base.evolution.speciation.CalibrationPoint;
import beast.base.evolution.tree.MRCAPrior;
import beast.base.evolution.tree.TreeInterface;
import beast.base.inference.Distribution;
import beast.base.inference.distribution.ParametricDistribution;
import beast.base.inference.distribution.Prior;
import beast.base.inference.parameter.RealParameter;
import lphy.base.evolution.Taxa;
import lphy.base.evolution.birthdeath.CalibratedYule;
import lphy.base.evolution.tree.TimeTreeNode;
import lphy.core.model.*;
import lphy.core.parser.graphicalmodel.GraphicalModel;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

import java.util.ArrayList;
import java.util.List;

import static lphybeast.tobeast.TaxaUtils.getTaxonSet;

/**
 * Only supports single calibrations for internal node at the moment.
 * Only supports age of node generated from distribution.
 */
public class CalibratedYuleToBeast implements GeneratorToBEAST<CalibratedYule, CalibratedYuleModel> {

    @Override
    public CalibratedYuleModel generatorToBEAST(CalibratedYule generator, BEASTInterface value, BEASTContext context) {
        CalibratedYuleModel calibratedYuleModel = new CalibratedYuleModel();

        calibratedYuleModel.setInputValue("tree", value);
        calibratedYuleModel.setInputValue("birthRate", context.getAsRealParameter(generator.getBirthRate()));


        Value<Number[]> cladeMRCAAge = generator.getCladeMRCAAge();

        Value cladeTaxa = generator.getCladeTaxa();

        // unwrapping first element from array, only for parameter generated from distributions.
        // cause error if constant number pass in
        // TODO handle multiple calibrations
        BasicFunction tmp = (BasicFunction) cladeMRCAAge.getInputs().get(0);

        // cladeAgeValue
        Value cladeAgeValue = tmp.getParams().get("0");

        // getting original generator of first element of array
        Generator cladePriorGenerator = cladeAgeValue.getGenerator();

        BEASTInterface beastCladeAgeValue = context.getBEASTObject(cladeAgeValue);
        context.removeBEASTObject(beastCladeAgeValue);

        if (cladePriorGenerator instanceof GenerativeDistribution<?>) {
            // get the distribution for calibration
            Prior cladeCalibrationPrior = (Prior) context.getBEASTObject(cladePriorGenerator);
            ParametricDistribution calibrationDistribution = cladeCalibrationPrior.distInput.get();
            // remove the clade mrca age in the prior section
            context.removeBEASTObject(cladeCalibrationPrior);

            // get TaxonSet for calibrations
            // get taxa names
            String[] cladeNames = new String[((Taxa) cladeTaxa.value()).length()];
            int index = 0;
            for (String name : ((Taxa) cladeTaxa.value()).getTaxaNames()) {
                cladeNames[index++] = name;
            }

            TaxonSet cladeTaxonSet = getTaxonSet((TreeInterface) value, cladeNames);

            CalibrationPoint calibrationPoint = new CalibrationPoint();
            calibrationPoint.setInputValue("taxonset", cladeTaxonSet);
            calibrationPoint.setInputValue("distr", calibrationDistribution);
            calibrationPoint.initAndValidate();

            calibratedYuleModel.setInputValue("calibrations", calibrationPoint);
        } else
            throw new UnsupportedOperationException();

        // calibration for the root (if have root age input)
        if (generator.getRootAge() != null){
            Value<Number> rootAge = generator.getRootAge();
            // remove the root age from the prior section
            context.removeBEASTObject(context.getBEASTObject(rootAge));

            // get all taxa name
            TaxonSet allTaxa = ((TreeInterface)value).getTaxonset();

            if (rootAge.getGenerator() instanceof GenerativeDistribution<?>) {
                // get the distribution for calibration
                Prior rootAgeCalibrationPrior = (Prior) context.getBEASTObject(rootAge.getGenerator());
                ParametricDistribution rootCalibrationDistribution = rootAgeCalibrationPrior.distInput.get();
                // remove the clade mrca age in the prior section
                context.removeBEASTObject(rootAgeCalibrationPrior);

                // create calibration point for the root
                CalibrationPoint calibrationRoot = new CalibrationPoint();
                calibrationRoot.setInputValue("taxonset", allTaxa);
                calibrationRoot.setInputValue("distr", rootCalibrationDistribution);
                calibrationRoot.initAndValidate();
                calibratedYuleModel.setInputValue("calibrations", calibrationRoot);
            }
        }

        calibratedYuleModel.initAndValidate();
        return calibratedYuleModel;
    }

    @Override
    public Class<CalibratedYule> getGeneratorClass() {
        return CalibratedYule.class;
    }

    @Override
    public Class<CalibratedYuleModel> getBEASTClass() {
        return CalibratedYuleModel.class;
    }
}
