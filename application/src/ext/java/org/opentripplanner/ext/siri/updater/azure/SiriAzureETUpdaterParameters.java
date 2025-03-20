package org.opentripplanner.ext.siri.updater.azure;

import com.azure.core.amqp.implementation.ConnectionStringProperties;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.apache.hc.core5.net.URIBuilder;
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
    // This url is only used for metrics
    if (getFullyQualifiedNamespace() != null) {
      return getFullyQualifiedNamespace();
    }
    // fallback to service bus url
    if (getServiceBusUrl() != null) {
      try {
        // Don't include the preshared key
        return new ConnectionStringProperties(getServiceBusUrl()).getEndpoint().toString();
      } catch (IllegalArgumentException ignore) {}
    }
    return "unknown";
  }

  @Override
  public Optional<URI> buildDataInitializationUrl() throws URISyntaxException {
    var url = getDataInitializationUrl();
    if (url == null) {
      return Optional.empty();
    }
    return Optional.of(
      new URIBuilder(url)
        .addParameter("fromDateTime", fromDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE))
        .build()
    );
  }
}
