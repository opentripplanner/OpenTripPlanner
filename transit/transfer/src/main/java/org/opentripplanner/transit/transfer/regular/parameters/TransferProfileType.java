package org.opentripplanner.transit.transfer.regular.parameters;

import java.io.Serializable;

/**
 * A regular-transfer discovery profile. Wheelchair is its own mode here - not a flag layered on
 * top of WALK - so a wheelchair-only detour a plain-WALK search would dominance-prune away gets
 * its own search and survives.
 */
public enum TransferProfileType implements Serializable {
  WALK,
}
