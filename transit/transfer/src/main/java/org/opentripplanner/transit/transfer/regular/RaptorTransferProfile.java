package org.opentripplanner.transit.transfer.regular;

import java.io.Serializable;

/**
 * A regular-transfer discovery profile. Wheelchair is its own mode here - not a flag layered on
 * top of WALK - so a wheelchair-only detour a plain-WALK search would dominance-prune away gets
 * its own search and survives.
 */
public enum RaptorTransferProfile implements Serializable {
  WALK,
  WHEELCHAIR,
  BICYCLE,
  CAR,
  SCOOTER,
}
