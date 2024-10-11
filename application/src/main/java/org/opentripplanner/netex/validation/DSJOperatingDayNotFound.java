package org.opentripplanner.netex.validation;

import javax.annotation.Nullable;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.netex.issues.ObjectNotFound;
import org.rutebanken.netex.model.DatedServiceJourney;
import org.rutebanken.netex.model.OperatingDayRefStructure;

class DSJOperatingDayNotFound extends AbstractHMapValidationRule<String, DatedServiceJourney> {

  @Override
  public Status validate(DatedServiceJourney dsj) {
    var ref = getOperatingDayRef(dsj);
    var opDay = index.getOperatingDayById().lookup(ref);

    return opDay == null ? Status.DISCARD : Status.OK;
  }

  @Override
  public DataImportIssue logMessage(String dsjId, DatedServiceJourney dsj) {
    String ref = getOperatingDayRef(dsj);

    return new ObjectNotFound("DatedServiceJourney", dsj.getId(), "OperatingDayRef", ref);
  }

  @Nullable
  private String getOperatingDayRef(DatedServiceJourney dsj) {
    OperatingDayRefStructure ref = dsj.getOperatingDayRef();
    return ref == null ? null : ref.getRef();
  }
}
