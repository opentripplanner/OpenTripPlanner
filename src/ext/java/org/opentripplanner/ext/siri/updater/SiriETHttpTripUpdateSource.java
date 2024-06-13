package org.opentripplanner.ext.siri.updater;

import static org.opentripplanner.updater.trip.UpdateIncrementality.DIFFERENTIAL;
import static org.opentripplanner.updater.trip.UpdateIncrementality.FULL_DATASET;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import org.opentripplanner.framework.io.OtpHttpClientException;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.trip.UpdateIncrementality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.Siri;

public class SiriETHttpTripUpdateSource implements EstimatedTimetableSource {

  private static final Logger LOG = LoggerFactory.getLogger(SiriETHttpTripUpdateSource.class);

  /**
   * Feed id that is used to match trip ids in the TripUpdates
   */
  private final String feedId;

  private final String url;

  private final SiriLoader siriLoader;
  private final String requestorRef;

  /**
   * The incrementality of the last received collection of updates.
   */
  private UpdateIncrementality updateIncrementality = FULL_DATASET;
  private ZonedDateTime lastTimestamp = ZonedDateTime.now().minusMonths(1);

  public SiriETHttpTripUpdateSource(Parameters parameters) {
    this.feedId = parameters.feedId();
    this.url = parameters.url();

    this.requestorRef =
      parameters.requestorRef() == null || parameters.requestorRef().isEmpty()
        ? "otp-" + UUID.randomUUID()
        : parameters.requestorRef();

    this.siriLoader = createLoader(url, parameters);
  }

  @Override
  public Optional<Siri> getUpdates() {
    long t1 = System.currentTimeMillis();
    try {
      var siri = siriLoader.fetchETFeed(requestorRef);
      if (siri.map(Siri::getServiceDelivery).isEmpty()) {
        return Optional.empty();
      }

      var serviceDelivery = siri.get().getServiceDelivery();
      if (serviceDelivery.getResponseTimestamp().isBefore(lastTimestamp)) {
        LOG.info("Newer data has already been processed");
        return Optional.empty();
      }
      lastTimestamp = serviceDelivery.getResponseTimestamp();

      //All subsequent requests will return changes since last request
      updateIncrementality = DIFFERENTIAL;
      return siri;
    } catch (OtpHttpClientException e) {
      LOG.info("Failed after {} ms", (System.currentTimeMillis() - t1));
      LOG.warn("Could not get SIRI-ET data from {}", url, e);
    } catch (Exception e) {
      LOG.info("Failed after {} ms", (System.currentTimeMillis() - t1));
      LOG.warn("Failed to parse SIRI-ET feed from {}", url, e);
    }
    return Optional.empty();
  }

  @Override
  public UpdateIncrementality incrementalityOfLastUpdates() {
    return updateIncrementality;
  }

  @Override
  public String getFeedId() {
    return this.feedId;
  }

  public String toString() {
    return "SiriETHttpTripUpdateSource(" + url + ")";
  }

  private static SiriLoader createLoader(String url, Parameters parameters) {
    // Load real-time updates from a file.
    if (SiriFileLoader.matchesUrl(url)) {
      return new SiriFileLoader(url);
    }
    // Fallback to default loader
    else {
      return new SiriHttpLoader(
        url,
        parameters.timeout(),
        parameters.httpRequestHeaders(),
        parameters.previewInterval()
      );
    }
  }

  public interface Parameters {
    String url();

    String requestorRef();

    String feedId();

    Duration timeout();

    @Nullable
    Duration previewInterval();

    HttpHeaders httpRequestHeaders();
  }
}
