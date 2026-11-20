package org.opentripplanner.updater.elevator.siri;

import java.net.URI;
import java.time.Duration;
import org.opentripplanner.framework.io.HttpHeaders;
import org.opentripplanner.updater.spi.PollingGraphUpdaterParameters;

/**
 * The parameters of the SIRI-FM elevator status updater.
 *
 * @param url           the URL of the SIRI-FM feed, polled with HTTP GET
 * @param frequency     how often the feed is polled
 * @param httpHeaders   the HTTP headers sent with each request
 * @param unknownStatus how an elevator with an unknown status is treated
 */
public record SiriFmElevatorUpdaterParameters(
  String configRef,
  URI url,
  Duration frequency,
  HttpHeaders httpHeaders,
  UnknownElevatorStatus unknownStatus
) implements PollingGraphUpdaterParameters {}
