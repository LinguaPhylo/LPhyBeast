open module lphy.beast.mc3 {
    requires lphy.beast;
    requires beast.base;
    requires beast.coupled.mcmc;

    exports mc3.lphybeast;
    exports mc3.lphybeast.spi;

    provides lphybeast.spi.MCMCStrategy with mc3.lphybeast.CoupledMCMCStrategy;
}
