open module lphy.beast.ssm {
    requires lphy.beast;
    requires beast.base;
    requires substmodel;
    requires lphy.base;

    exports ssm.lphybeast.spi;
    exports ssm.lphybeast.tobeast.generators;

    provides lphybeast.spi.LPhyBEASTMapping with ssm.lphybeast.spi.SSMLBImpl;
}
