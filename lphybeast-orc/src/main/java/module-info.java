open module lphy.beast.orc {
    requires lphy.beast;
    requires beast.base;
    requires orc;

    exports orc.lphybeast;

    provides lphybeast.spi.ClockOperatorContributor with orc.lphybeast.ORCClockOperatorContributor;
}
