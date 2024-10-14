package org.opentripplanner.netex.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;

public class InterchangePointMappingFailed implements DataImportIssue {

  private static final String MSG =
    "Interchange mapping failed. Detail: %s, " +
    "interchange: %s, point: %s, serviceJourney: %s, scheduledStopPoint: %s";

  private final String point;
  private final String interchangeId;
  private final String detail;
  private final String sjId;
  private final String sspId;

  public InterchangePointMappingFailed(
    String detail,
    String interchangeId,
    String point,
    String sjId,
    String sspId
  ) {
    this.point = point;
    this.interchangeId = interchangeId;
    this.detail = detail;
    this.sjId = sjId;
    this.sspId = sspId;
  }

  @Override
  public String getMessage() {
    return String.format(MSG, detail, interchangeId, point, sjId, sspId);
  }
}
