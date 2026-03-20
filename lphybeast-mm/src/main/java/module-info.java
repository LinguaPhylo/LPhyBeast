open module lphy.beast.mm {
    requires lphy.beast;
    requires beast.base;
    requires beast.morph.models;
    requires lphy.base;

    exports mm.lphybeast.spi;
    exports mm.lphybeast.tobeast.generators;

    provides lphybeast.spi.LPhyBEASTMapping with mm.lphybeast.spi.MMLBImpl;
}
