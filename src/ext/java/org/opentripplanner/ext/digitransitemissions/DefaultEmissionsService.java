package org.opentripplanner.ext.digitransitemissions;

import jakarta.inject.Inject;
import java.io.Serializable;
import java.util.Optional;
import org.opentripplanner.model.plan.Itinerary;

public class DefaultEmissionsService implements Serializable, EmissionsService {

  private EmissionsServiceRepository repository = null;

  @Inject
  public DefaultEmissionsService(EmissionsServiceRepository repository) {
    this.repository = repository;
  }

  @Override
  public Double getEmissionsForItinerary(Itinerary itinerary) {
    Optional<DigitransitEmissionsService> service = repository.retrieveEmissionsService();
    if (service.isPresent()) {
      return service.get().getEmissionsForItinerary(itinerary);
    }
    return null;
  }
}
