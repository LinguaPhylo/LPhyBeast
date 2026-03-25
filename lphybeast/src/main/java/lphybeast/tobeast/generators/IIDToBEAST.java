package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.inference.Distribution;
import beast.base.spec.inference.distribution.IID;
import beast.base.spec.inference.distribution.ScalarDistribution;
import beast.base.spec.type.Vector;
import beastlabs.util.BEASTVector;
import lphy.base.distribution.Bernoulli;
import lphy.core.logger.LoggerUtils;
import lphy.core.model.GenerativeDistribution;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

import java.util.ArrayList;
import java.util.List;

public class IIDToBEAST implements GeneratorToBEAST<lphy.core.vectorization.IID, BEASTInterface> {
    @Override
    public BEASTInterface generatorToBEAST(lphy.core.vectorization.IID generator, BEASTInterface value, BEASTContext context) {
        GenerativeDistribution baseDistribution = generator.getBaseDistribution();
        GeneratorToBEAST toBEAST = context.getGeneratorToBEAST(baseDistribution);

        if (toBEAST == null) {
            if (generator.getBaseDistribution() instanceof Bernoulli) {
                throw new UnsupportedOperationException("in dev");
            }
            LoggerUtils.log.warning("Ignoring IID distribution " + generator.getBaseDistribution().getName());
            return null;
        }

        if (value instanceof Vector<?, ?> vectorParam) {
            // Create the base distribution by calling sub-generator with null value.
            // The sub-generator creates a ScalarDistribution with only hyperparameters (no param attached).
            // Then wrap in spec IID which provides the vector param.
            BEASTInterface baseDist = toBEAST.generatorToBEAST(baseDistribution, (BEASTInterface) null, context);

            if (baseDist instanceof ScalarDistribution<?, ?> scalarDist) {
                IID iid = new IID(vectorParam, scalarDist);
                iid.setID(((BEASTInterface) value).getID() + ".prior");
                return iid;
            }
            if (baseDist instanceof Distribution) {
                return baseDist;
            }
            throw new IllegalArgumentException("Expecting ScalarDistribution or Distribution from sub-generator, " +
                    "but got " + baseDist.getClass().getSimpleName());

        } else if (value instanceof BEASTVector) {

            List<BEASTInterface> values = ((BEASTVector) value).getObjectList();

            if (generator.size() != values.size())
                throw new IllegalArgumentException("Expecting value and base distribution list sizes to match!");

            List<BEASTInterface> beastGenerators = new ArrayList<>();
            for (int i = 0; i < values.size(); i++) {
                BEASTInterface beastGenerator = toBEAST.generatorToBEAST(baseDistribution, values.get(i), context);
                beastGenerators.add(beastGenerator);
                context.putBEASTObject(baseDistribution, beastGenerator);
            }
            return new BEASTVector(beastGenerators);

        } else {
            throw new IllegalArgumentException("Expecting Vector or BEASTVector value from IID, " +
                    "but getting " + value.getClass().getSimpleName());
        }
    }

    @Override
    public Class<lphy.core.vectorization.IID> getGeneratorClass() {
        return lphy.core.vectorization.IID.class;
    }

}
