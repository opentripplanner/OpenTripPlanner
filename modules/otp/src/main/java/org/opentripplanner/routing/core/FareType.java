package org.opentripplanner.routing.core;

import java.io.Serializable;
import org.opentripplanner.model.fare.FareProduct;

/**
 * @deprecated Because it exists only for backwards compatibility, and you should use the Fares V2
 * type, namely {@link FareProduct}.
 */
@Deprecated
public enum FareType implements Serializable {
  regular,
  senior,
  youth,
  electronicRegular,
  electronicSenior,
  electronicSpecial,
  electronicYouth,
}
