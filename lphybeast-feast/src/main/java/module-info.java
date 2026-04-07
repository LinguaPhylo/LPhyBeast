open module lphy.beast.feast {
    requires lphy.beast;
    requires feast;
    requires beast.base;
    requires beast.labs;
    requires lphy.base;

    exports feast.lphybeast;
    exports feast.lphybeast.spi;
    exports feast.lphybeast.tobeast.generators;

    provides lphybeast.spi.LPhyBEASTMapping with feast.lphybeast.spi.FeastLBImpl;
    provides lphybeast.spi.ValueHandler with feast.lphybeast.FeastValueHandler;
}
