package org.opentripplanner.ext.vehicleparking.sirifm;

import java.util.List;
import java.util.Map;
import org.opentripplanner.framework.io.OtpHttpClient;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.updater.spi.DataSource;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.vehicle_parking.AvailabiltyUpdate;
import org.rutebanken.siri20.util.SiriXml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SiriFmUpdater implements DataSource<AvailabiltyUpdate> {

  private static final Logger LOG = LoggerFactory.getLogger(SiriFmUpdater.class);
  private final SiriFmUpdaterParameters params;
  private final OtpHttpClient httpClient;
  private final Map<String, String> headers;
  private List<AvailabiltyUpdate> updates = List.of();

  public SiriFmUpdater(SiriFmUpdaterParameters parameters) {
    params = parameters;
    headers = HttpHeaders.of().acceptApplicationXML().add(parameters.httpHeaders()).build().asMap();
    httpClient = new OtpHttpClientFactory().create(LOG);
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(this.getClass())
      .addStr("url", this.params.url().toString())
      .toString();
  }

  @Override
  public boolean update() {
    LOG.error("RUNNING {}", this);

    updates =
      httpClient.getAndMap(
        params.url(),
        headers,
        resp -> {
          var siri = SiriXml.parseXml(resp);

          var conditions = siri
            .getServiceDelivery()
            .getFacilityMonitoringDeliveries()
            .stream()
            .flatMap(d -> d.getFacilityConditions().stream())
            .toList();

          conditions.forEach(c -> {
            LOG.error("{}", c.getFacilityRef().getValue());
          });

          return List.of();
        }
      );
    return true;
  }

  @Override
  public List<AvailabiltyUpdate> getUpdates() {
    return updates;
  }
}
