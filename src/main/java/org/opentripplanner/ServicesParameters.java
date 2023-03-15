package org.opentripplanner;

import java.util.List;
import org.opentripplanner.ext.ridehailing.service.CarHailingServiceParameters;

public interface ServicesParameters {
  List<CarHailingServiceParameters> carHailingServiceParameters();
}
