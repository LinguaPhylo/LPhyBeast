package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.inference.distribution.Prior;
import beast.base.inference.parameter.IntegerParameter;
import beast.base.inference.parameter.RealParameter;
import lphy.base.distribution.Uniform;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class UniformToBEAST implements GeneratorToBEAST<Uniform, Prior> {
    @Override

    public Prior generatorToBEAST(Uniform generator, BEASTInterface value, BEASTContext context) {

        beast.base.inference.distribution.Uniform uniform = new beast.base.inference.distribution.Uniform();

        BEASTInterface lowerB = context.getBEASTObject(generator.getLower());
        BEASTInterface upperB = context.getBEASTObject(generator.getUpper());

        Double lower = Double.NEGATIVE_INFINITY;
        Double upper = Double.POSITIVE_INFINITY;

        if (lowerB instanceof RealParameter) {
            lower = ((RealParameter)lowerB).getValue();
        } else if (lowerB instanceof IntegerParameter) {
            lower = ((IntegerParameter)lowerB).getValue().doubleValue();
        } else {
            throw new IllegalArgumentException("BEAST2 can only have constants for lower and upper of Uniform distribution.");
        }

        if (upperB instanceof RealParameter) {
            upper = ((RealParameter)upperB).getValue();
        } else if (upperB instanceof IntegerParameter) {
            upper = ((IntegerParameter)upperB).getValue().doubleValue();
        } else {
            throw new IllegalArgumentException("BEAST2 can only have constants for lower and upper of Uniform distribution.");
        }

        uniform.setInputValue("lower", lower);
        uniform.setInputValue("upper", upper);
        uniform.initAndValidate();

        return BEASTContext.createPrior(uniform, (RealParameter)value);
    }

    @Override
    public Class<Uniform> getGeneratorClass() {
        return Uniform.class;
    }

    @Override
    public Class<Prior> getBEASTClass() {
        return Prior.class;
    }
}
