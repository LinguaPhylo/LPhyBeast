open module lphy.beast.bdtree {
    requires lphy.beast;
    requires beast.base;
    requires bdtree;
    requires lphy.base;

    exports bdtree.lphybeast.spi;
    exports bdtree.lphybeast.tobeast.generators;

    provides lphybeast.spi.LPhyBEASTMapping with bdtree.lphybeast.spi.BDTreeLBImpl;
}
