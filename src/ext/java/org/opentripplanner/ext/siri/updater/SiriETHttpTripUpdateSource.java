package org.opentripplanner.ext.siri.updater;

import java.time.Duration;
import java.time.ZonedDateTime;
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

  private final SiriHttpLoader siriHttpLoader;
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

    requestorRef =
      parameters.requestorRef() == null || parameters.requestorRef().isEmpty()
        ? "otp-" + UUID.randomUUID()
        : parameters.requestorRef();

    siriHttpLoader =
      new SiriHttpLoader(
        url,
        parameters.timeout(),
        parameters.httpRequestHeaders(),
        parameters.previewInterval()
      );
  }

  @Override
  public Siri getUpdates() {
    long t1 = System.currentTimeMillis();
    try {
      Siri siri = siriHttpLoader.fetchETFeed(requestorRef);

      if (siri.getServiceDelivery().getResponseTimestamp().isBefore(lastTimestamp)) {
        LOG.info("Newer data has already been processed");
        return null;
      }
      lastTimestamp = siri.getServiceDelivery().getResponseTimestamp();

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
    return null;
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
