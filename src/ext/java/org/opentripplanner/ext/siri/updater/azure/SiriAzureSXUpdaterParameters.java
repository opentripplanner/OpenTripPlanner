package org.opentripplanner.ext.siri.updater.azure;

import java.time.LocalDate;

public class SiriAzureSXUpdaterParameters extends SiriAzureUpdaterParameters {

  private LocalDate fromDateTime;
  private LocalDate toDateTime;

  public SiriAzureSXUpdaterParameters() {
    super("siri-azure-sx-updater");
  }

  public LocalDate getFromDateTime() {
    return fromDateTime;
  }

  public void setFromDateTime(LocalDate fromDateTime) {
    this.fromDateTime = fromDateTime;
  }

  public LocalDate getToDateTime() {
    return toDateTime;
  }

  public void setToDateTime(LocalDate toDateTime) {
    this.toDateTime = toDateTime;
  }
}
