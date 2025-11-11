package org.opentripplanner.street.internal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.opentripplanner.street.StreetRepository;
import org.opentripplanner.street.model.StreetModelDetails;

@Singleton
public class DefaultStreetRepository implements StreetRepository {

  private StreetModelDetails streetModelDetails = StreetModelDetails.DEFAULT;

  @Inject
  public DefaultStreetRepository() {}

  @Override
  public StreetModelDetails streetModelDetails() {
    return streetModelDetails;
  }

  @Override
  public void setStreetModelDetails(StreetModelDetails streetModelDetails) {
    this.streetModelDetails = streetModelDetails;
  }
}
