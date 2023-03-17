package org.opentripplanner;

import java.util.List;
import org.opentripplanner.ext.ridehailing.RideHailingServiceParameters;

public interface ServicesParameters {
  List<RideHailingServiceParameters> carHailingServiceParameters();
}
