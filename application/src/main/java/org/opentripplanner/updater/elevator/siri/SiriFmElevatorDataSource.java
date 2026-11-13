package org.opentripplanner.updater.elevator.siri;

import java.util.List;
import org.entur.siri21.util.SiriXml;
import org.opentripplanner.framework.io.HttpHeaders;
import org.opentripplanner.framework.io.OtpHttpClient;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.opentripplanner.service.elevatorstatus.model.ElevatorStatus;
import org.opentripplanner.updater.spi.DataSource;
import org.opentripplanner.utils.tostring.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetches the status of the elevators from a SIRI-FM 2.1 XML feed with HTTP GET. Each response is
 * a full snapshot of all the elevators of the feed. See {@link SiriFmElevatorMapper} for how the
 * facility conditions are mapped.
 */
public class SiriFmElevatorDataSource implements DataSource<ElevatorStatus> {

  private static final Logger LOG = LoggerFactory.getLogger(SiriFmElevatorDataSource.class);

  private final SiriFmElevatorUpdaterParameters parameters;
  private final HttpHeaders headers;
  private final OtpHttpClient httpClient;
  private final SiriFmElevatorMapper mapper;
  private List<ElevatorStatus> updates = List.of();

  public SiriFmElevatorDataSource(SiriFmElevatorUpdaterParameters parameters) {
    this.parameters = parameters;
    this.headers = HttpHeaders.of().acceptApplicationXML().add(parameters.httpHeaders()).build();
    this.httpClient = new OtpHttpClientFactory().create(LOG);
    this.mapper = new SiriFmElevatorMapper(parameters.unknownStatus());
  }

  @Override
  public boolean update() {
    updates = httpClient.getAndMap(parameters.url(), headers, response ->
      mapper.map(SiriXml.parseXml(response.body()))
    );
    return true;
  }

  @Override
  public List<ElevatorStatus> getUpdates() {
    return updates;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(SiriFmElevatorDataSource.class)
      .addStr("url", parameters.url().toString())
      .addEnum("unknownStatus", parameters.unknownStatus())
      .toString();
  }
}
