package org.opentripplanner.updater.vehicle_positions;

import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import java.util.List;
import java.util.Optional;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.opentripplanner.updater.WriteToGraphCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Add vehicle positions to OTP patterns via a realtime source *
 * <pre>
 * rt.type = vehicle-positions
 * rt.frequencySec = 60
 * rt.sourceType = gtfs-http
 * rt.url = http://host.tld/path
 * rt.feedId = TA
 * </pre>
 */
public class PollingVehiclePositionUpdater extends PollingGraphUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(PollingVehiclePositionUpdater.class);
  /**
   * Update streamer
   */
  private final VehiclePositionSource vehiclePositionSource;
  private final String feedId;
  /**
   * Parent update manager. Is used to execute graph writer runnables.
   */
  private WriteToGraphCallback saveResultOnGraph;
  private VehiclePositionPatternMatcher vehiclePositionPatternMatcher;

  public PollingVehiclePositionUpdater(VehiclePositionsUpdaterParameters params) {
    super(params);
    var p = (VehiclePositionsUpdaterParameters) params;
    vehiclePositionSource = new GtfsRealtimeHttpVehiclePositionSource(p.url());

    LOG.info(
      "Creating vehicle position updater running every {} seconds : {}",
      pollingPeriodSeconds,
      vehiclePositionSource
    );
    feedId = params.feedId();
  }

  @Override
  public void setGraphUpdaterManager(WriteToGraphCallback saveResultOnGraph) {
    this.saveResultOnGraph = saveResultOnGraph;
  }

  @Override
  public void setup(Graph graph) {
    var index = graph.index;
    vehiclePositionPatternMatcher =
      new VehiclePositionPatternMatcher(
        feedId,
        tripId -> index.getTripForId().get(tripId),
        trip -> graph.index.getPatternForTrip().get(trip),
        (trip, date) -> getPatternIncludingRealtime(graph, trip, date),
        graph.getVehiclePositionService(),
        graph.getTimeZone().toZoneId()
      );
  }

  @Override
  public void teardown() {}

  /**
   * Repeatedly makes blocking calls to an UpdateStreamer to retrieve new stop time updates, and
   * applies those updates to the graph.
   */
  @Override
  public void runPolling() {
    // Get update lists from update source
    List<VehiclePosition> updates = vehiclePositionSource.getPositions();

    if (updates != null) {
      // Handle updating trip positions via graph writer runnable
      var runnable = new VehiclePositionUpdaterRunnable(updates, vehiclePositionPatternMatcher);
      saveResultOnGraph.execute(runnable);
    }
  }

  public String toString() {
    String s = (vehiclePositionSource == null) ? "NONE" : vehiclePositionSource.toString();
    return "Streaming vehicle position updater with update source = " + s;
  }

  private static TripPattern getPatternIncludingRealtime(Graph graph, Trip trip, ServiceDate sd) {
    return Optional
      .ofNullable(graph.getTimetableSnapshot())
      .map(snapshot -> snapshot.getLastAddedTripPattern(trip.getId(), sd))
      .orElseGet(() -> graph.index.getPatternForTrip().get(trip));
  }
}
