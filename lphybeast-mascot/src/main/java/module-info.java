open module lphy.beast.mascot {
    requires lphy.beast;
    requires beast.base;
    requires mascot;
    requires lphy.base;

    exports mascot.lphybeast.spi;
    exports mascot.lphybeast.tobeast.generators;
    exports mascot.lphybeast.tobeast.loggers;

    provides lphybeast.spi.LPhyBEASTMapping with mascot.lphybeast.spi.MascotLBImpl;
}
