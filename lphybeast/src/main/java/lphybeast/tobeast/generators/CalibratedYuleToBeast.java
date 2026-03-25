package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.spec.evolution.speciation.CalibratedYuleModel;
import beast.base.spec.evolution.speciation.CalibrationPoint;
import beast.base.evolution.tree.TreeInterface;
import beast.base.inference.Distribution;
import beast.base.spec.inference.distribution.ScalarDistribution;
import lphy.base.evolution.Taxa;
import lphy.base.evolution.birthdeath.CalibratedYule;
import lphy.core.model.*;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

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
        calibratedYuleModel.setInputValue("birthRate", context.getAsRealScalar(generator.getBirthRate()));


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
            // get the spec distribution directly (e.g. LogNormal, Normal)
            Distribution cladeDist = (Distribution) context.getBEASTObject(cladePriorGenerator);
            context.removeBEASTObject(cladeDist);

            // get TaxonSet for calibrations
            String[] cladeNames = new String[((Taxa) cladeTaxa.value()).length()];
            int index = 0;
            for (String name : ((Taxa) cladeTaxa.value()).getTaxaNames()) {
                cladeNames[index++] = name;
            }

            TaxonSet cladeTaxonSet = getTaxonSet((TreeInterface) value, cladeNames);

            CalibrationPoint calibrationPoint = new CalibrationPoint();
            calibrationPoint.setInputValue("taxonset", cladeTaxonSet);
            calibrationPoint.setInputValue("distr", cladeDist);
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
                // get the spec distribution directly
                Distribution rootDist = (Distribution) context.getBEASTObject(rootAge.getGenerator());
                context.removeBEASTObject(rootDist);

                // create calibration point for the root
                CalibrationPoint calibrationRoot = new CalibrationPoint();
                calibrationRoot.setInputValue("taxonset", allTaxa);
                calibrationRoot.setInputValue("distr", rootDist);
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
