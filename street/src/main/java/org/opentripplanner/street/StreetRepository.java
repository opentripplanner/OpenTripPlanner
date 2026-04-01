package org.opentripplanner.street;

import java.io.Serializable;
import org.opentripplanner.street.model.StreetModelDetails;

/**
 *
 */
public interface StreetRepository extends Serializable {
  StreetModelDetails streetModelDetails();

  void setStreetModelDetails(StreetModelDetails streetModelDetails);
}
