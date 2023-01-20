package org.opentripplanner.ext.siri.updater;

import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.xml.stream.XMLStreamException;
import org.opentripplanner.ext.siri.SiriHttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.Siri;

public class SiriVMHttpTripUpdateSource implements VehicleMonitoringSource {

  private static final Logger LOG = LoggerFactory.getLogger(SiriVMHttpTripUpdateSource.class);

  private static final long RETRY_INTERVAL_MILLIS = 5000;
  private static final Map<String, String> requestHeaders = new HashMap<>();
  /**
   * Feed id that is used to match trip ids in the TripUpdates
   */
  private final String feedId;
  private final String url;
  private final String originalRequestorRef;
  /**
   * True iff the last list with updates represent all updates that are active right now, i.e. all
   * previous updates should be disregarded
   */
  private boolean fullDataset = true;
  private ZonedDateTime lastTimestamp = ZonedDateTime.now().minusMonths(1);
  private String requestorRef;
  private int timeout;
  private int retryCount = 0;

  public SiriVMHttpTripUpdateSource(Parameters parameters) {
    this.url = parameters.getUrl();
    this.requestorRef = parameters.getRequestorRef();
    if (requestorRef == null || requestorRef.isEmpty()) {
      requestorRef = "otp-" + UUID.randomUUID();
    }

    originalRequestorRef = this.requestorRef;

    this.feedId = parameters.getFeedId();

    int timeoutSec = parameters.getTimeoutSec();
    if (timeoutSec > 0) {
      this.timeout = 1000 * timeoutSec;
    }

    requestHeaders.put("ET-Client-Name", SiriHttpUtils.getUniqueETClientName("-VM"));
  }

  @Override
  public Siri getUpdates() {
    long t1 = System.currentTimeMillis();
    long creating = 0;
    long fetching = 0;
    long unmarshalling = 0;

    fullDataset = false;
    try {
      String vmServiceRequest = SiriHelper.createVMServiceRequestAsXml(requestorRef);
      creating = System.currentTimeMillis() - t1;
      t1 = System.currentTimeMillis();

      InputStream is = SiriHttpUtils.postData(url, vmServiceRequest, timeout, requestHeaders);
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

        return siri;
      }
    } catch (IOException | JAXBException | XMLStreamException e) {
      LOG.info("Failed after {} ms", (System.currentTimeMillis() - t1));
      LOG.warn("Failed to parse SIRI-VM feed from {}", url, e);

      final long sleepTime = RETRY_INTERVAL_MILLIS + RETRY_INTERVAL_MILLIS * retryCount;

      retryCount++;

      LOG.info("Caught timeout - retry no. {} after {} millis", retryCount, sleepTime);

      try {
        Thread.sleep(sleepTime);
      } catch (InterruptedException ex) {
        // Ignore
      }

      // Creating new requestorRef so all data is refreshed
      requestorRef = originalRequestorRef + "-retry-" + retryCount;

      return getUpdates();
    } finally {
      LOG.info(
        "Updating VM [{}]: Create req: {}, Fetching data: {}, Unmarshalling: {}",
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
    return "SiriVMHttpTripUpdateSource(" + url + ")";
  }

  interface Parameters {
    String getUrl();

    String getRequestorRef();

    String getFeedId();

    int getTimeoutSec();
  }
}
