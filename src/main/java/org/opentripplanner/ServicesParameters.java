package org.opentripplanner;

import java.util.List;
import org.opentripplanner.ext.ridehailing.service.RideHailingServiceParameters;

public interface ServicesParameters {
  List<RideHailingServiceParameters> carHailingServiceParameters();
}
