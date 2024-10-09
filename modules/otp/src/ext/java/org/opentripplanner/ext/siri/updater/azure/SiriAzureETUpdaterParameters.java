package org.opentripplanner.ext.siri.updater.azure;

import com.azure.core.amqp.implementation.ConnectionStringProperties;
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
  public String url() {
    var url = getServiceBusUrl();
    try {
      return new ConnectionStringProperties(url).getEndpoint().toString();
    } catch (IllegalArgumentException e) {
      return url;
    }
  }
}
