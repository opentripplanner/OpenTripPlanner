package org.opentripplanner.ext.vehicleparking.sirifm;

import static uk.org.siri.siri21.CountingTypeEnumeration.PRESENT_COUNT;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.entur.siri21.util.SiriXml;
import org.opentripplanner.framework.io.OtpHttpClient;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.updater.spi.DataSource;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.vehicle_parking.AvailabiltyUpdate;
import org.opentripplanner.utils.tostring.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri21.FacilityConditionStructure;

/**
 * Parses SIRI 2.1 XML data into parking availability updates. The data needs to conform to the
 * Italian profile of SIRI-FM.
 */
public class SiriFmDataSource implements DataSource<AvailabiltyUpdate> {

  private static final Logger LOG = LoggerFactory.getLogger(SiriFmDataSource.class);
  private final SiriFmUpdaterParameters params;
  private final OtpHttpClient httpClient;
  private final Map<String, String> headers;
  private List<AvailabiltyUpdate> updates = List.of();

  public SiriFmDataSource(SiriFmUpdaterParameters parameters) {
    params = parameters;
    headers = HttpHeaders.of().acceptApplicationXML().add(parameters.httpHeaders()).build().asMap();
    httpClient = new OtpHttpClientFactory().create(LOG);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(this.getClass())
      .addStr("url", this.params.url().toString())
      .toString();
  }

  @Override
  public boolean update() {
    updates = httpClient.getAndMap(params.url(), headers, resp -> {
      var siri = SiriXml.parseXml(resp);

      return Stream.ofNullable(siri.getServiceDelivery())
        .flatMap(sd -> sd.getFacilityMonitoringDeliveries().stream())
        .flatMap(d -> d.getFacilityConditions().stream())
        .filter(this::conformsToItalianProfile)
        .map(this::mapToUpdate)
        .toList();
    });
    return true;
  }

  private AvailabiltyUpdate mapToUpdate(FacilityConditionStructure c) {
    var id = new FeedScopedId(params.feedId(), c.getFacilityRef().getValue());
    var available = c.getMonitoredCountings().getFirst().getCount().intValue();
    return new AvailabiltyUpdate(id, available);
  }

  /**
   * Checks if the {@link FacilityConditionStructure} contains all the necessary information that
   * are required by the Italian Siri-FM profile.
   */
  private boolean conformsToItalianProfile(FacilityConditionStructure c) {
    return (
      c.getFacilityRef() != null &&
      c.getFacilityRef().getValue() != null &&
      c.getMonitoredCountings().size() == 1 &&
      c.getMonitoredCountings().getFirst().getCountingType() == PRESENT_COUNT
    );
  }

  @Override
  public List<AvailabiltyUpdate> getUpdates() {
    return updates;
  }
}
