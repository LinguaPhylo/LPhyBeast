open module lphy.beast.sa {
    requires lphy.beast;
    requires beast.base;
    requires sa;
    requires lphy.base;

    exports sa.lphybeast.spi;
    exports sa.lphybeast.operators;
    exports sa.lphybeast.tobeast.generators;

    provides lphybeast.spi.LPhyBEASTMapping with sa.lphybeast.spi.SALBImpl;
}
