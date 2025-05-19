package org.opentripplanner.updater.trip.siri.updater.lite;

import static org.opentripplanner.updater.trip.UpdateIncrementality.FULL_DATASET;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import org.opentripplanner.framework.io.OtpHttpClientException;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.support.siri.SiriLoader;
import org.opentripplanner.updater.trip.UpdateIncrementality;
import org.opentripplanner.updater.trip.siri.updater.EstimatedTimetableSource;
import org.opentripplanner.utils.tostring.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri21.Siri;

/**
 * SIRI Lite periodically downloads all messages as a single GET request.
 */
public class SiriETLiteHttpTripUpdateSource implements EstimatedTimetableSource {

  private static final Logger LOG = LoggerFactory.getLogger(SiriETLiteHttpTripUpdateSource.class);
  /**
   * The framework code requires a requestor ref but in SIRI Lite this is not used.
   */
  private static final String DUMMY_REQUESTOR_REF = "OpenTripPlanner";

  private final Parameters parameters;

  private final SiriLoader siriLoader;

  public SiriETLiteHttpTripUpdateSource(Parameters parameters, SiriLoader siriLoader) {
    this.parameters = parameters;
    this.siriLoader = siriLoader;
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

  public String toString() {
    return ToStringBuilder.of(this.getClass())
      .addStr("feedId", parameters.feedId())
      .addStr("uri", parameters.uri().toString())
      .toString();
  }

  public interface Parameters {
    URI uri();

    String feedId();

    Duration timeout();

    HttpHeaders httpRequestHeaders();
  }
}
