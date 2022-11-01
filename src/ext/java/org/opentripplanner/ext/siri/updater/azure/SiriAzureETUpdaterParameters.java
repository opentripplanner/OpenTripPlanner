package org.opentripplanner.ext.siri.updater.azure;

import java.time.LocalDate;
import org.opentripplanner.updater.trip.UrlUpdaterParameters;

public class SiriAzureETUpdaterParameters
  extends SiriAzureUpdaterParameters
  implements UrlUpdaterParameters {

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

  @Override
  public String getUrl() {
    return getDataInitializationUrl();
  }
}
