package org.opentripplanner.netex.validation;

import org.entur.netex2.model.CarPoolingService;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.netex.issues.ObjectNotFound;

/**
 * A car pooling service must refer to the authority or operator running it.
 */
class CarPoolingServiceOrganisationNotFound
  extends AbstractHMapValidationRule<String, CarPoolingService>
{

  @Override
  public Status validate(CarPoolingService service) {
    String ref = organisationRef(service);
    boolean found =
      index.getAuthoritiesById().containsKey(ref) || index.getOperatorsById().containsKey(ref);
    return found ? Status.OK : Status.DISCARD;
  }

  @Override
  public DataImportIssue logMessage(String key, CarPoolingService service) {
    return new ObjectNotFound(
      "CarPoolingService",
      service.getId(),
      "Organisation",
      organisationRef(service)
    );
  }

  @Nullable
  private static String organisationRef(CarPoolingService service) {
    var ref = service.getOrganisationRef();
    return ref == null ? null : ref.getValue().getRef();
  }
}
