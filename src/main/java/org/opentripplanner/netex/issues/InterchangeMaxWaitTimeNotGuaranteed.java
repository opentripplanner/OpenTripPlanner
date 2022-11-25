package org.opentripplanner.netex.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.rutebanken.netex.model.ServiceJourneyInterchange;

public class InterchangeMaxWaitTimeNotGuaranteed implements DataImportIssue {

  private static final String MSG =
    "Interchange max-wait-time ignored. Max-wait-time is only supported for guaranteed " +
    "interchanges. Interchange: %s";

  private final ServiceJourneyInterchange interchange;

  public InterchangeMaxWaitTimeNotGuaranteed(ServiceJourneyInterchange interchange) {
    this.interchange = interchange;
  }

  @Override
  public String getMessage() {
    return String.format(MSG, interchange);
  }
}
