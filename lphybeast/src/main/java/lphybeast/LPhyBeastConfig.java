package lphybeast;

public class LPhyBeastConfig {

    final public boolean compressConstantSites;
    final public String alignmentId;
    final public boolean logAllAlignments;

    public LPhyBeastConfig(boolean compressConstantSites, String alignmentId, boolean logAllAlignments) {
        this.compressConstantSites = compressConstantSites;
        this.alignmentId = alignmentId;
        this.logAllAlignments = logAllAlignments;
    }

    public LPhyBeastConfig() {
        this(false, null, false);
    }
}
