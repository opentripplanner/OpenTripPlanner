package org.opentripplanner.standalone.config;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.ext.examples.updater.ExampleGraphUpdater;
import org.opentripplanner.ext.siri.updater.SiriETUpdater;
import org.opentripplanner.ext.siri.updater.SiriSXUpdater;
import org.opentripplanner.ext.siri.updater.SiriVMUpdater;
import org.opentripplanner.updater.alerts.GtfsRealtimeAlertsUpdater;
import org.opentripplanner.updater.bike_rental.BikeRentalUpdater;
import org.opentripplanner.updater.stoptime.PollingStoptimeUpdater;
import org.opentripplanner.updater.stoptime.WebsocketGtfsRealtimeUpdater;
import org.opentripplanner.updater.street_notes.WFSNotePollingGraphUpdater;

/**
 * This class is an object representation of a single real-time updater in 'router-config.json'.
 * Each updater defines an inner interface with its required attributes.
 */
public class UpdaterConfig
    implements WebsocketGtfsRealtimeUpdater.WebSocketGtfsRealTimeUpdaterConfig,
    ExampleGraphUpdater.ExampleGraphUpdaterConfig,
    GtfsRealtimeAlertsUpdater.GtfsRealTimeAlertsUpdaterConfig,
    PollingStoptimeUpdater.PollingStopTimeUpdaterConfig,
    BikeRentalUpdater.BikeRentalUpdaterConfig,
    SiriETUpdater.SiriETUpdaterConfig,
    SiriVMUpdater.SiriVMUpdaterConfig,
    SiriSXUpdater.SiriSXUpdaterConfig,
    WFSNotePollingGraphUpdater.WFSNotePollingGraphUpdaterConfig {

  private final String url;
  private final int frequencySec;
  private final String feedId;
  private final String type;
  private final String requestorRef;
  private final int timeoutSec;
  private final int reconnectPeriodSec;
  private final String sourceType;
  private final String apiKey;
  private final String network;
  private final String networks;
  private final int earlyStartSec;
  private final boolean fuzzyTripMatching;
  private final int logFrequency;
  private final int maxSnapshotFrequencyMs;
  private final boolean purgeExpiredData;
  private final boolean blockReadinessUntilInitialized;
  private final String featureType;
  private final JsonNode source;

  public UpdaterConfig(NodeAdapter c) {
    this.url = c.asText("url", null);
    this.frequencySec = c.asInt("frequencySec", 60);
    this.feedId = c.asText("feedId", null);
    this.type = c.asText("type", null);
    this.requestorRef = c.asText("requestorRef", null);
    this.timeoutSec = c.asInt("timoutSec", 0);
    this.reconnectPeriodSec = c.asInt("reconnectPeriodSec", 0);
    this.sourceType = c.asText("sourceType", null);
    this.apiKey = c.asText("apiKey", null);
    this.network = c.asText("network", null);
    this.networks = c.asText("networks", "default");
    this.earlyStartSec = c.asInt("earlyStartSec", 0);
    this.fuzzyTripMatching = c.asBoolean("fuzzyTripMatching", false);
    this.logFrequency = c.asInt("logFrequency", -1);
    this.maxSnapshotFrequencyMs = c.asInt("maxSnapshotFrequencyMs", -1);
    this.purgeExpiredData = c.asBoolean("purgeExpiredData", true);
    this.blockReadinessUntilInitialized = c.asBoolean("blockReadinessUntilInitialized", false);
    this.featureType = c.asText("featureType", null);
    this.source = c.asRawNode();
  }

  public String getUrl() {
    return url;
  }

  public int getFrequencySec() {
    return frequencySec;
  }

  public String getFeedId() {
    return feedId;
  }

  public String getType() {
    return type;
  }

  public String getRequestorRef() {
    return requestorRef;
  }

  public int getTimeoutSec() {
    return timeoutSec;
  }

  public int getReconnectPeriodSec() {
    return reconnectPeriodSec;
  }

  public String getSourceType() {
    return sourceType;
  }

  public String getApiKey() {
    return apiKey;
  }

  public String getNetwork() {
    return network;
  }

  public String getNetworks() {
    return networks;
  }

  public int getEarlyStartSec() {
    return earlyStartSec;
  }

  public boolean fuzzyTripMatching() {
    return fuzzyTripMatching;
  }

  public int getLogFrequency() {
    return logFrequency;
  }

  public int getMaxSnapshotFrequencyMs() {
    return maxSnapshotFrequencyMs;
  }

  public boolean purgeExpiredData() {
    return purgeExpiredData;
  }

  public boolean blockReadinessUntilInitialized() {
    return blockReadinessUntilInitialized;
  }

  public String getFeatureType() { return featureType; }

  public JsonNode getSource() { return source; }
}
