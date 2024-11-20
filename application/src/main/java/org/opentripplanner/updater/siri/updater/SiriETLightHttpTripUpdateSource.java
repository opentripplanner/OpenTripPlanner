package org.opentripplanner.updater.siri.updater;

import static org.opentripplanner.updater.trip.UpdateIncrementality.FULL_DATASET;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import org.opentripplanner.framework.io.OtpHttpClientException;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.trip.UpdateIncrementality;
import org.opentripplanner.utils.tostring.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.Siri;

public class SiriETLightHttpTripUpdateSource implements EstimatedTimetableSource {

  private static final Logger LOG = LoggerFactory.getLogger(SiriETLightHttpTripUpdateSource.class);
  private static final String DUMMY_REQUESTOR_REF = "OTP";

  private final Parameters parameters;

  private final SiriLoader siriLoader;

  public SiriETLightHttpTripUpdateSource(Parameters parameters) {
    this.parameters = parameters;

    this.siriLoader = createLoader(parameters);
  }

  @Override
  public Optional<Siri> getUpdates() {
    try {
      var siri = siriLoader.fetchETFeed(DUMMY_REQUESTOR_REF);
      if (siri.map(Siri::getServiceDelivery).isEmpty()) {
        return Optional.empty();
      }
      return siri;
    } catch (OtpHttpClientException e) {
      LOG.warn("Could not get SIRI-ET data from {}", parameters.uri(), e);
    } catch (Exception e) {
      LOG.warn("Failed to parse SIRI-ET feed from {}", parameters.uri(), e);
    }
    return Optional.empty();
  }

  @Override
  public UpdateIncrementality incrementalityOfLastUpdates() {
    return FULL_DATASET;
  }

  @Override
  public String getFeedId() {
    return this.parameters.feedId();
  }

  public String toString() {
    return ToStringBuilder
      .of(this.getClass())
      .addStr("feedId", parameters.feedId())
      .addStr("uri", parameters.toString())
      .toString();
  }

  private static SiriLoader createLoader(Parameters parameters) {
    // Load real-time updates from a file.
    if (SiriFileLoader.matchesUrl(parameters.uri().toString())) {
      return new SiriFileLoader(parameters.uri().toString());
    }
    // Fallback to default loader
    else {
      return new SiriLightHttpLoader(
        parameters.uri(),
        parameters.timeout(),
        parameters.httpRequestHeaders()
      );
    }
  }

  public interface Parameters {
    URI uri();

    String feedId();

    Duration timeout();

    HttpHeaders httpRequestHeaders();
  }
}
