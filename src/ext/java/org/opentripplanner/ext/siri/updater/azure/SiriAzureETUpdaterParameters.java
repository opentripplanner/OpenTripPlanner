package org.opentripplanner.ext.siri.updater.azure;

import java.time.LocalDate;

public class SiriAzureETUpdaterParameters extends SiriAzureUpdaterParameters {

  private LocalDate fromDateTime;

  public SiriAzureETUpdaterParameters() {
    super("siri-azure-et-updater");
  }

  public LocalDate getFromDateTime() {
    return fromDateTime;
  }

  public void setFromDateTime(LocalDate fromDateTime) {
    this.fromDateTime = fromDateTime;
  }
}
