package org.opentripplanner;

import java.util.List;
import org.opentripplanner.ext.carhailing.service.uber.UberServiceParameters;

public interface ServicesParameters {
  List<UberServiceParameters> uberServiceParameters();
}
