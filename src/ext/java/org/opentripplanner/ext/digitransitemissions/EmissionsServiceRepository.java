package org.opentripplanner.ext.digitransitemissions;

import java.io.Serializable;
import java.util.Optional;
import javax.annotation.Nonnull;

public interface EmissionsServiceRepository extends Serializable {
  Optional<DigitransitEmissionsService> retrieveEmissionsService();

  void saveEmissionsService(@Nonnull DigitransitEmissionsService emissionsService);
}
