package org.opentripplanner.ext.siri.updater;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import org.opentripplanner.framework.io.OtpHttpClientException;
import org.opentripplanner.updater.spi.HttpHeaders;
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
   * True iff the last list with updates represent all updates that are active right now, i.e. all
   * previous updates should be disregarded
   */
  private boolean fullDataset = true;
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
      fullDataset = false;
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
  public boolean getFullDatasetValueOfLastUpdates() {
    return fullDataset;
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
