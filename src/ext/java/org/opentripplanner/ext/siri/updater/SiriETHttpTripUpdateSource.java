package org.opentripplanner.ext.siri.updater;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.opentripplanner.ext.siri.SiriHttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.Siri;

public class SiriETHttpTripUpdateSource implements EstimatedTimetableSource {

  private static final Logger LOG = LoggerFactory.getLogger(SiriETHttpTripUpdateSource.class);
  private static final Map<String, String> requestHeaders = new HashMap<>();
  /**
   * Feed id that is used to match trip ids in the TripUpdates
   */
  private final String feedId;

  private final String url;

  private final String requestorRef;

  private final int timeout;
  private final int previewIntervalMillis;
  /**
   * True iff the last list with updates represent all updates that are active right now, i.e. all
   * previous updates should be disregarded
   */
  private boolean fullDataset = true;
  private ZonedDateTime lastTimestamp = ZonedDateTime.now().minusMonths(1);

  public SiriETHttpTripUpdateSource(Parameters parameters) {
    this.feedId = parameters.getFeedId();
    this.url = parameters.getUrl();
    this.requestorRef =
      parameters.getRequestorRef() == null || parameters.getRequestorRef().isEmpty()
        ? "otp-" + UUID.randomUUID()
        : parameters.getRequestorRef();
    this.timeout = parameters.getTimeoutSec() > 0 ? 1000 * parameters.getTimeoutSec() : -1;

    int min = parameters.getPreviewIntervalMinutes();
    this.previewIntervalMillis = min > 0 ? 1000 * 60 * min : -1;

    requestHeaders.put("ET-Client-Name", SiriHttpUtils.getUniqueETClientName("-ET"));
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
        previewIntervalMillis
      );
      creating = System.currentTimeMillis() - t1;
      t1 = System.currentTimeMillis();

      InputStream is = SiriHttpUtils.postData(url, etServiceRequest, timeout, requestHeaders);
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
      }
    } catch (IOException e) {
      LOG.info("Failed after {} ms", (System.currentTimeMillis() - t1));
      LOG.warn("Could not get SIRI-ET data from {}, caused by {}", url, e.getMessage());
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
    String getUrl();

    String getRequestorRef();

    String getFeedId();

    int getTimeoutSec();

    int getPreviewIntervalMinutes();
  }
}
