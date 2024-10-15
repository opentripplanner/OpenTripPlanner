package org.opentripplanner.netex.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.model.transfer.ConstrainedTransfer;

public class InterchangeWithoutConstraint implements DataImportIssue {

  private static final String MSG =
    "Interchange dropped. The interchange have no effect on routing. Interchange: %s";

  private final ConstrainedTransfer transfer;

  public InterchangeWithoutConstraint(ConstrainedTransfer transfer) {
    this.transfer = transfer;
  }

  @Override
  public String getMessage() {
    return String.format(MSG, transfer);
  }
}
