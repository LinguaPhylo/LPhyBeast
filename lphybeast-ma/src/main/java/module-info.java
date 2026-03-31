open module lphy.beast.ma {
    requires lphy.beast;
    requires beast.base;
    requires mutable.alignment;
    requires lphy.base;

    exports ma.lphybeast;
    exports ma.lphybeast.spi;

    provides lphybeast.spi.LPhyBEASTMapping with ma.lphybeast.spi.MALBImpl;
    provides lphybeast.spi.TreeLikelihoodStrategy with ma.lphybeast.MATreeLikelihoodStrategy;
    provides lphybeast.spi.OperatorContributor with ma.lphybeast.MAOperatorContributor;
    provides lphybeast.spi.AlignmentHandler with ma.lphybeast.MAAlignmentHandler;
}
