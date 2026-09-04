package org.opentripplanner.ext.vehiclerentalgeofencing.internal.graphbuilder;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;

/**
 * A network listed in the GBFS manifest could not be read, so its geofencing zones are absent from
 * the graph. The network is still routable, but without the restrictions its zones would impose.
 */
public record GeofencingFeedUnavailable(String network, String url, String reason) implements
  DataImportIssue {
  @Override
  public String getMessage() {
    return (
      "Geofencing zones for rental network '%s' were not applied: %s (%s)".formatted(
        network,
        reason,
        url
      )
    );
  }
}
