open module lphy.beast.flc {
    requires lphy.beast;
    requires beast.base;
    requires flc;
    requires lphy.base;

    exports flc.lphybeast.spi;
    exports flc.lphybeast.tobeast.generators;

    provides lphybeast.spi.LPhyBEASTMapping with flc.lphybeast.spi.FLCLBImpl;
}
