package org.opentripplanner.ext.siri.updater.azure;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.apache.hc.core5.net.URIBuilder;

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

  @Override
  public Optional<URI> buildDataInitializationUrl() throws URISyntaxException {
    var url = getDataInitializationUrl();
    if (url == null) {
      return Optional.empty();
    }

    return Optional.of(
      new URIBuilder(url)
        .addParameter("publishFromDateTime", fromDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE))
        .addParameter("publishToDateTime", toDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE))
        .build()
    );
  }
}
