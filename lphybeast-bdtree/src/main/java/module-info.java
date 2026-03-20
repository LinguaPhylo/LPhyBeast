open module lphy.beast.bdtree {
    requires lphy.beast;
    requires beast.base;
    requires beast.bdtree;
    requires lphy.base;

    exports bdtree.lphybeast.spi;
    exports bdtree.lphybeast.tobeast.generators;

    provides lphybeast.spi.LPhyBEASTExt with bdtree.lphybeast.spi.BDTreeLBImpl;
}
