package org.opentripplanner.updater.trip;

import static org.asynchttpclient.Dsl.asyncHttpClient;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.ws.WebSocket;
import org.asynchttpclient.ws.WebSocketListener;
import org.asynchttpclient.ws.WebSocketUpgradeHandler;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.updater.GraphUpdater;
import org.opentripplanner.updater.GtfsRealtimeFuzzyTripMatcher;
import org.opentripplanner.updater.WriteToGraphCallback;
import org.opentripplanner.updater.trip.metrics.TripUpdateMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class starts an HTTP client which opens a websocket connection to a GTFS-RT data source. A
 * callback is registered which handles incoming GTFS-RT messages as they stream in by placing a
 * GTFS-RT decoder Runnable task in the single-threaded executor for handling.
 *
 * <pre>
 * websocket.type = websocket-gtfs-rt-updater
 * websocket.defaultAgencyId = agency
 * websocket.url = ws://localhost:8088/tripUpdates
 * </pre>
 */
public class WebsocketGtfsRealtimeUpdater implements GraphUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(WebsocketGtfsRealtimeUpdater.class);

  /**
   * Number of seconds to wait before checking again whether we are still connected
   */
  private static final int CHECK_CONNECTION_PERIOD_SEC = 1;

  /**
   * Url of the websocket server
   */
  private final String url;

  /**
   * The ID for the static feed to which these TripUpdates are applied
   */
  private final String feedId;

  /**
   * The number of seconds to wait before reconnecting after a failed connection.
   */
  private final int reconnectPeriodSec;

  private final String configRef;

  private final BackwardsDelayPropagationType backwardsDelayPropagationType;

  private final TimetableSnapshotSource snapshotSource;

  /**
   * Parent update manager. Is used to execute graph writer runnables.
   */
  private WriteToGraphCallback saveResultOnGraph;

  private GtfsRealtimeFuzzyTripMatcher fuzzyTripMatcher;

  private final Consumer<UpdateResult> recordMetrics;

  public WebsocketGtfsRealtimeUpdater(
    WebsocketGtfsRealtimeUpdaterParameters parameters,
    TimetableSnapshotSource snapshotSource,
    TransitModel transitModel
  ) {
    this.configRef = parameters.configRef();
    this.url = parameters.getUrl();
    this.feedId = parameters.getFeedId();
    this.reconnectPeriodSec = parameters.getReconnectPeriodSec();
    this.backwardsDelayPropagationType = parameters.getBackwardsDelayPropagationType();
    this.snapshotSource = snapshotSource;
    this.fuzzyTripMatcher =
      new GtfsRealtimeFuzzyTripMatcher(new DefaultTransitService(transitModel));
    this.recordMetrics = TripUpdateMetrics.streaming(parameters);
  }

  @Override
  public void setGraphUpdaterManager(WriteToGraphCallback saveResultOnGraph) {
    this.saveResultOnGraph = saveResultOnGraph;
  }

  @Override
  public void run() throws InterruptedException, IOException {
    while (true) {
      AsyncHttpClient client = asyncHttpClient();
      WebSocketListener listener = new Listener();
      WebSocketUpgradeHandler handler = new WebSocketUpgradeHandler.Builder()
        .addWebSocketListener(listener)
        .build();
      WebSocket socket = null;
      boolean connectionSuccessful = true;
      // Try to create a websocket connection
      try {
        socket = client.prepareGet(url).execute(handler).get();
        LOG.info("Successfully connected to {}.", url);
      } catch (ExecutionException e) {
        LOG.error("Could not connect to {}: {}", url, e.getCause().getMessage());
        connectionSuccessful = false;
      } catch (Exception e) {
        LOG.error("Unknown exception when trying to connect to {}.", url, e);
        connectionSuccessful = false;
      }

      // If connection was unsuccessful, wait some time before trying again
      if (!connectionSuccessful) {
        Thread.sleep(reconnectPeriodSec * 1000);
      }

      // Keep checking whether connection is still open
      while (true) {
        if (socket == null || !socket.isOpen()) {
          // The connection is closed somehow, try to reconnect
          if (connectionSuccessful) {
            LOG.warn("Connection to {} was lost. Trying to reconnect...", url);
          }
          break;
        }
        Thread.sleep(CHECK_CONNECTION_PERIOD_SEC * 1000);
      }

      client.close();
    }
  }

  @Override
  public String getConfigRef() {
    return configRef;
  }

  /**
   * Auxiliary class to handle incoming messages via the websocket connection
   */
  private class Listener implements WebSocketListener {

    @Override
    public void onOpen(WebSocket websocket) {}

    @Override
    public void onClose(WebSocket websocket, int code, String reason) {}

    @Override
    public void onError(Throwable t) {}

    @Override
    public void onBinaryFrame(byte[] message, boolean finalFragment, int rsv) {
      FeedMessage feedMessage;
      List<FeedEntity> feedEntityList;
      List<TripUpdate> updates = null;
      boolean fullDataset = true;
      try {
        // Decode message
        feedMessage = FeedMessage.PARSER.parseFrom(message);
        feedEntityList = feedMessage.getEntityList();

        // Change fullDataset value if this is an incremental update
        if (
          feedMessage.hasHeader() &&
          feedMessage.getHeader().hasIncrementality() &&
          feedMessage
            .getHeader()
            .getIncrementality()
            .equals(GtfsRealtime.FeedHeader.Incrementality.DIFFERENTIAL)
        ) {
          fullDataset = false;
        }

        // Create List of TripUpdates
        updates = new ArrayList<>(feedEntityList.size());
        for (FeedEntity feedEntity : feedEntityList) {
          if (feedEntity.hasTripUpdate()) {
            updates.add(feedEntity.getTripUpdate());
          }
        }
      } catch (InvalidProtocolBufferException e) {
        LOG.error("Could not decode gtfs-rt message:", e);
      }

      if (updates != null) {
        // Handle trip updates via graph writer runnable
        TripUpdateGraphWriterRunnable runnable = new TripUpdateGraphWriterRunnable(
          snapshotSource,
          fuzzyTripMatcher,
          backwardsDelayPropagationType,
          fullDataset,
          updates,
          feedId,
          recordMetrics
        );
        saveResultOnGraph.execute(runnable);
      }
    }
  }
}
