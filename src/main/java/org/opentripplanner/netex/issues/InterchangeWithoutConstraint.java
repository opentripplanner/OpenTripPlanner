package org.opentripplanner.netex.issues;

import org.opentripplanner.graph_builder.DataImportIssue;
import org.opentripplanner.model.transfer.Transfer;

public class InterchangeWithoutConstraint implements DataImportIssue {
    private static final String MSG
            = "Interchange dropped. The interchange have no effect on routing. Interchange: %s";

    private final Transfer transfer;

    public InterchangeWithoutConstraint(Transfer transfer) {
        this.transfer = transfer;
    }

    @Override
    public String getMessage() {
        return String.format(MSG, transfer);
    }
}
