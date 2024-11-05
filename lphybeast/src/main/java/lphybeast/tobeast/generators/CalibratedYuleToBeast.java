package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.evolution.alignment.Taxon;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.speciation.CalibratedYuleModel;
import beast.base.evolution.speciation.CalibrationPoint;
import beast.base.evolution.tree.MRCAPrior;
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

/**
 * Only supports single calibrations at the moment.
 */
public class CalibratedYuleToBeast implements GeneratorToBEAST<CalibratedYule, CalibratedYuleModel> {

    @Override
    public CalibratedYuleModel generatorToBEAST(CalibratedYule generator, BEASTInterface value, BEASTContext context) {
        CalibratedYuleModel calibratedYuleModel = new CalibratedYuleModel();

        calibratedYuleModel.setInputValue("tree", value);
        calibratedYuleModel.setInputValue("birthRate", context.getAsRealParameter(generator.getBirthRate()));


        Value<Number[]> cladeMCRAAge = generator.getCladeMRCAAge();

        Value calibrations = generator.getCladeTaxa();

        // unwrapping first element from array
        // TODO handle multiple calibrations
        BasicFunction tmp = (BasicFunction) cladeMCRAAge.getInputs().get(0);

        // cladeAgeValue
        Value cladeAgeValue = tmp.getParams().get("0");

        // getting original generator of first element of array
        Generator priorGenerator = cladeAgeValue.getGenerator();

        BEASTInterface beastCladeAgeValue = context.getBEASTObject(cladeAgeValue);
        context.removeBEASTObject(beastCladeAgeValue);

        if (priorGenerator instanceof GenerativeDistribution<?>) {
            // get the distribution for calibration
            Prior calibrationPrior = (Prior) context.getBEASTObject(priorGenerator);
            //
            ParametricDistribution calibrationDistribution = calibrationPrior.distInput.get();

            context.removeBEASTObject(calibrationPrior);

            if (Taxa.class.isAssignableFrom(calibrations.getType())) {
                TaxonSet cladeTaxa = new TaxonSet();
                ArrayList<Taxon> taxa = new ArrayList<>();
                Taxa lphyTaxa = (Taxa) calibrations.value();
                for (String tN : lphyTaxa.getTaxaNames()) {
                    Taxon taxon = new Taxon(tN);
                    taxa.add(taxon);
                }
                cladeTaxa.initByName("taxon", taxa);


                CalibrationPoint calibrationPoint = new CalibrationPoint();
                calibrationPoint.setInputValue("taxonset", cladeTaxa);
                calibrationPoint.setInputValue("distr", calibrationDistribution);
                calibrationPoint.initAndValidate();

                calibratedYuleModel.setInputValue("calibrations", calibrationPoint);
            } else
                throw new UnsupportedOperationException();

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
