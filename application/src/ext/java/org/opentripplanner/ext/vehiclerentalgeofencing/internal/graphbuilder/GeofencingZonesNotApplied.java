package org.opentripplanner.ext.vehiclerentalgeofencing.internal.graphbuilder;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;

/**
 * A network's geofencing zones were read but could not be applied to the street graph, so the
 * network is routable without the restrictions its zones would impose.
 */
public record GeofencingZonesNotApplied(String network, String reason) implements DataImportIssue {
  @Override
  public String getMessage() {
    return "Geofencing zones for rental network '%s' were not applied: %s".formatted(
      network,
      reason
    );
  }
}
