package org.opentripplanner;

import java.util.List;
import org.opentripplanner.ext.carhailing.service.CarHailingServiceParameters;

public interface ServicesParameters {
  List<CarHailingServiceParameters> carHailingServiceParameters();
}
