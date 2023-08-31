package org.opentripplanner.ext.digitransitemissions;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.Serializable;
import java.util.Optional;
import javax.annotation.Nonnull;

@Singleton
public class DefaultEmissionsServiceRepository implements EmissionsServiceRepository, Serializable {

  private volatile DigitransitEmissionsService emissionsService = null;

  @Inject
  public DefaultEmissionsServiceRepository() {}

  @Override
  public Optional<DigitransitEmissionsService> retrieveEmissionsService() {
    return Optional.ofNullable(emissionsService);
  }

  @Override
  public void saveEmissionsService(@Nonnull DigitransitEmissionsService emissionsService) {
    this.emissionsService = emissionsService;
  }
}
