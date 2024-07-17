package org.opentripplanner.ext.vehicleparking.sirifm;

import static uk.org.siri.siri21.CountingTypeEnumeration.PRESENT_COUNT;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import org.opentripplanner.framework.io.OtpHttpClient;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.updater.spi.DataSource;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.vehicle_parking.AvailabiltyUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri21.FacilityConditionStructure;
import uk.org.siri.siri21.Siri;

public class SiriFmUpdater implements DataSource<AvailabiltyUpdate> {

  private static final Logger LOG = LoggerFactory.getLogger(SiriFmUpdater.class);
  private final SiriFmUpdaterParameters params;
  private final OtpHttpClient httpClient;
  private final Map<String, String> headers;
  private List<AvailabiltyUpdate> updates = List.of();

  private static final JAXBContext jaxbContext;

  static {
    try {
      jaxbContext = JAXBContext.newInstance(Siri.class);
    } catch (JAXBException e) {
      throw new RuntimeException(e);
    }
  }

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
    updates =
      httpClient.getAndMap(
        params.url(),
        headers,
        resp -> {
          var siri = parseXml(resp);

          return Stream
            .ofNullable(siri.getServiceDelivery())
            .flatMap(sd -> sd.getFacilityMonitoringDeliveries().stream())
            .flatMap(d -> d.getFacilityConditions().stream())
            .filter(this::conformsToItalianProfile)
            .map(this::mapToUpdate)
            .toList();
        }
      );
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

  private Siri parseXml(InputStream stream) {
    try {
      var xmlif = XMLInputFactory.newInstance();
      var jaxbUnmarshaller = jaxbContext.createUnmarshaller();
      var streamReader = xmlif.createXMLStreamReader(stream);
      return (Siri) jaxbUnmarshaller.unmarshal(streamReader);
    } catch (JAXBException | XMLStreamException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<AvailabiltyUpdate> getUpdates() {
    return updates;
  }
}
