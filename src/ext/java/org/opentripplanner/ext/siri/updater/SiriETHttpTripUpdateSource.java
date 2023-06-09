package org.opentripplanner.ext.siri.updater;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.UUID;
import javax.annotation.Nullable;
import org.opentripplanner.ext.siri.SiriHttpUtils;
import org.opentripplanner.framework.time.DurationUtils;
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

  private final String requestorRef;

  private final int timeoutMillis;

  @Nullable
  private final Duration previewInterval;

  private final HttpHeaders requestHeaders;

  /**
   * True iff the last list with updates represent all updates that are active right now, i.e. all
   * previous updates should be disregarded
   */
  private boolean fullDataset = true;
  private ZonedDateTime lastTimestamp = ZonedDateTime.now().minusMonths(1);

  public SiriETHttpTripUpdateSource(Parameters parameters) {
    this.feedId = parameters.feedId();
    this.url = parameters.url();
    this.requestHeaders = parameters.httpRequestHeaders();

    this.requestorRef =
      parameters.requestorRef() == null || parameters.requestorRef().isEmpty()
        ? "otp-" + UUID.randomUUID()
        : parameters.requestorRef();

    // TODO: This is not consistent, in SiriSXUpdater the default timeout is 0, here it is -1, but
    //       the same framework is used. Also, this is unnecessary; Refactor and ensure the timeout
    //       is non-null and initialized with a good default value.
    this.timeoutMillis = DurationUtils.toIntMilliseconds(parameters.timeout(), -1);
    this.previewInterval = parameters.previewInterval();
  }

  @Override
  public Siri getUpdates() {
    long t1 = System.currentTimeMillis();
    long creating = 0;
    long fetching = 0;
    long unmarshalling = 0;
    try {
      String etServiceRequest = SiriHelper.createETServiceRequestAsXml(
        requestorRef,
        previewInterval
      );
      creating = System.currentTimeMillis() - t1;
      t1 = System.currentTimeMillis();

      InputStream is = SiriHttpUtils.postData(
        url,
        etServiceRequest,
        timeoutMillis,
        requestHeaders.asMap()
      );
      if (is != null) {
        // Decode message
        fetching = System.currentTimeMillis() - t1;
        t1 = System.currentTimeMillis();

        Siri siri = SiriHelper.unmarshal(is);
        unmarshalling = System.currentTimeMillis() - t1;

        if (siri.getServiceDelivery().getResponseTimestamp().isBefore(lastTimestamp)) {
          LOG.info("Newer data has already been processed");
          return null;
        }
        lastTimestamp = siri.getServiceDelivery().getResponseTimestamp();

        //All subsequent requests will return changes since last request
        fullDataset = false;
        return siri;
      } else {
        LOG.error("Could not fetch SIRI-ET data from {}", url);
      }
    } catch (IOException e) {
      LOG.info("Failed after {} ms", (System.currentTimeMillis() - t1));
      LOG.warn("Could not get SIRI-ET data from {}", url, e);
    } catch (Exception e) {
      LOG.info("Failed after {} ms", (System.currentTimeMillis() - t1));
      LOG.warn("Failed to parse SIRI-ET feed from {}", url, e);
    } finally {
      LOG.info(
        "Updating ET [{}]: Create req: {}, Fetching data: {}, Unmarshalling: {}",
        requestorRef,
        creating,
        fetching,
        unmarshalling
      );
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
