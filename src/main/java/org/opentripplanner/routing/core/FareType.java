package org.opentripplanner.routing.core;

import java.io.Serializable;

/**
 * @deprecated Because it exists only for backwards compatibility, and you should use the Fares V2
 * type, namely {@link org.opentripplanner.model.fare.FareProduct}.
 */
@Deprecated
public enum FareType implements Serializable {
  regular,
  student,
  senior,
  tram,
  special,
  youth,
  electronicRegular,
  electronicSenior,
  electronicSpecial,
  electronicYouth,
}
