open module lphy.beast {
    requires beast.base;
    requires beast.pkgmgmt;
    requires beast.labs;
    requires beast.classic;
    requires lphy.base;
    requires info.picocli;
    requires com.google.common;
    requires beagle;
    requires java.xml;

    exports lphybeast;
    exports lphybeast.spi;
    exports lphybeast.tobeast;
    exports lphybeast.tobeast.values;
    exports lphybeast.tobeast.generators;
    exports lphybeast.tobeast.loggers;
    exports lphybeast.tobeast.operators;

    uses lphybeast.spi.LPhyBEASTExt;
    uses lphybeast.spi.MCMCStrategy;
    uses lphybeast.spi.ValueHandler;
    uses lphybeast.spi.TreeLikelihoodStrategy;
    uses lphybeast.spi.OperatorContributor;
    uses lphybeast.spi.AlignmentHandler;

    provides lphybeast.spi.LPhyBEASTExt with lphybeast.spi.LPhyBEASTExtImpl;
    provides lphybeast.spi.MCMCStrategy with lphybeast.spi.DefaultMCMCStrategy;
    provides lphybeast.spi.TreeLikelihoodStrategy with lphybeast.spi.DefaultTreeLikelihoodStrategy;
    provides lphybeast.spi.AlignmentHandler with lphybeast.spi.DefaultAlignmentHandler;
}
