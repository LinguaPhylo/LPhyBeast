package mc3.lphybeast.spi;

import lphybeast.spi.MCMCStrategy;
import mc3.lphybeast.CoupledMCMCStrategy;

/**
 * Marker class for registering the CoupledMCMCStrategy via version.xml.
 * The actual strategy is discovered as a MCMCStrategy service.
 */
public class MC3LBImpl {
    // This class exists for documentation; the actual service is CoupledMCMCStrategy.
}
