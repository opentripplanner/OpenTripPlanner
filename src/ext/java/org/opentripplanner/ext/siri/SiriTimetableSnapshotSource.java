package org.opentripplanner.ext.siri;

import com.google.common.base.Preconditions;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.TimetableSnapshotProvider;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor.transit.mappers.TransitLayerUpdater;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.trippattern.RealTimeState;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.ArrivalBoardingActivityEnumeration;
import uk.org.siri.siri20.DepartureBoardingActivityEnumeration;
import uk.org.siri.siri20.EstimatedCall;
import uk.org.siri.siri20.EstimatedTimetableDeliveryStructure;
import uk.org.siri.siri20.EstimatedVehicleJourney;
import uk.org.siri.siri20.EstimatedVersionFrameStructure;
import uk.org.siri.siri20.NaturalLanguageStringStructure;
import uk.org.siri.siri20.RecordedCall;
import uk.org.siri.siri20.VehicleActivityCancellationStructure;
import uk.org.siri.siri20.VehicleActivityStructure;
import uk.org.siri.siri20.VehicleModesEnumeration;
import uk.org.siri.siri20.VehicleMonitoringDeliveryStructure;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.locks.ReentrantLock;

import static org.opentripplanner.ext.siri.TimetableHelper.createModifiedStopTimes;
import static org.opentripplanner.ext.siri.TimetableHelper.createModifiedStops;
import static org.opentripplanner.ext.siri.TimetableHelper.createUpdatedTripTimes;
import static org.opentripplanner.model.StopPattern.PICKDROP_NONE;
import static org.opentripplanner.model.StopPattern.PICKDROP_SCHEDULED;

/**
 * This class should be used to create snapshots of lookup tables of realtime data. This is
 * necessary to provide planning threads a consistent constant view of a graph with realtime data at
 * a specific point in time.
 */
public class SiriTimetableSnapshotSource implements TimetableSnapshotProvider {
    private static final Logger LOG = LoggerFactory.getLogger(SiriTimetableSnapshotSource.class);

    /**
     * Number of milliseconds per second
     */
    private static final int MILLIS_PER_SECOND = 1000;

    /**
     * Maximum time in seconds since midnight for arrivals and departures
     */
    private static final long MAX_ARRIVAL_DEPARTURE_TIME = 48 * 60 * 60;

    public int logFrequency = 2000;

    private int appliedBlockCount = 0;

    /**
     * If a timetable snapshot is requested less than this number of milliseconds after the previous
     * snapshot, just return the same one. Throttles the potentially resource-consuming task of
     * duplicating a TripPattern -> Timetable map and indexing the new Timetables.
     */
    public int maxSnapshotFrequency = 1000; // msec

    /**
     * The last committed snapshot that was handed off to a routing thread. This snapshot may be
     * given to more than one routing thread if the maximum snapshot frequency is exceeded.
     */
    private volatile TimetableSnapshot snapshot = null;

    /**
     * The working copy of the timetable snapshot. Should not be visible to routing threads. Should
     * only be modified by a thread that holds a lock on {@link #bufferLock}. All public methods that
     * might modify this buffer will correctly acquire the lock.
     */
    private final TimetableSnapshot buffer = new TimetableSnapshot();

    /**
     * Lock to indicate that buffer is in use
     */
    private final ReentrantLock bufferLock = new ReentrantLock(true);

    /**
     * A synchronized cache of trip patterns that are added to the graph due to GTFS-realtime messages.
     */
    private final SiriTripPatternCache tripPatternCache = new SiriTripPatternCache();

    /** Should expired realtime data be purged from the graph. */
    public boolean purgeExpiredData = true;

    protected ServiceDate lastPurgeDate = null;

    protected long lastSnapshotTime = -1;

    private final TimeZone timeZone;

    private final RoutingService routingService;

    public SiriFuzzyTripMatcher siriFuzzyTripMatcher;

    private TransitLayer realtimeTransitLayer;

    private TransitLayerUpdater transitLayerUpdater;

    public SiriTimetableSnapshotSource(final Graph graph) {
        timeZone = graph.getTimeZone();
        routingService = new RoutingService(graph);
        realtimeTransitLayer = graph.getRealtimeTransitLayer();
        transitLayerUpdater = graph.transitLayerUpdater;

        siriFuzzyTripMatcher = new SiriFuzzyTripMatcher(routingService);
    }

    /**
     * @return an up-to-date snapshot mapping TripPatterns to Timetables. This snapshot and the
     *         timetable objects it references are guaranteed to never change, so the requesting
     *         thread is provided a consistent view of all TripTimes. The routing thread need only
     *         release its reference to the snapshot to release resources.
     */
    public TimetableSnapshot getTimetableSnapshot() {
        TimetableSnapshot snapshotToReturn;

        // Try to get a lock on the buffer
        if (bufferLock.tryLock()) {
            // Make a new snapshot if necessary
            try {
                snapshotToReturn = getTimetableSnapshot(false);
            } finally {
                bufferLock.unlock();
            }
        } else {
            // No lock could be obtained because there is either a snapshot commit busy or updates
            // are applied at this moment, just return the current snapshot
            snapshotToReturn = snapshot;
        }

        return snapshotToReturn;
    }

    private TimetableSnapshot getTimetableSnapshot(final boolean force) {
        final long now = System.currentTimeMillis();
        if (force || now - lastSnapshotTime > maxSnapshotFrequency) {
            if (force || buffer.isDirty()) {
                LOG.debug("Committing {}", buffer.toString());
                snapshot = buffer.commit(transitLayerUpdater, force);
            } else {
                LOG.debug("Buffer was unchanged, keeping old snapshot.");
            }
            lastSnapshotTime = System.currentTimeMillis();
        } else {
            LOG.debug("Snapshot frequency exceeded. Reusing snapshot {}", snapshot);
        }
        return snapshot;
    }


    /**
     * Method to apply a trip update list to the most recent version of the timetable snapshot.
     *
     *
     * @param graph graph to update (needed for adding/changing stop patterns)
     * @param fullDataset true iff the list with updates represent all updates that are active right
     *        now, i.e. all previous updates should be disregarded
     * @param updates SIRI VehicleMonitoringDeliveries that should be applied atomically
     */
    public void applyVehicleMonitoring(final Graph graph, final String feedId, final boolean fullDataset, final List<VehicleMonitoringDeliveryStructure> updates) {
        if (updates == null) {
            LOG.warn("updates is null");
            return;
        }

        // Acquire lock on buffer
        bufferLock.lock();

        try {
            if (fullDataset) {
                // Remove all updates from the buffer
                buffer.clear(feedId);
            }

            for (VehicleMonitoringDeliveryStructure vmDelivery : updates) {

                ServiceDate serviceDate = new ServiceDate();

                List<VehicleActivityStructure> activities = vmDelivery.getVehicleActivities();
                if (activities != null) {
                    //Handle activities
                    LOG.info("Handling {} VM-activities.", activities.size());
                    int handledCounter = 0;
                    int skippedCounter = 0;
                    for (VehicleActivityStructure activity : activities) {
                        boolean handled = handleModifiedTrip(graph, feedId, activity, serviceDate);
                        if (handled) {
                            handledCounter++;
                        } else {
                            skippedCounter++;
                        }
                    }
                    LOG.info("Applied {} VM-activities, skipped {}.", handledCounter, skippedCounter);
                }
                List<VehicleActivityCancellationStructure> cancellations = vmDelivery.getVehicleActivityCancellations();
                if (cancellations != null && !cancellations.isEmpty()) {
                    //Handle cancellations
                    LOG.info("TODO: Handle {} cancellations.", cancellations.size());
                }

                List<NaturalLanguageStringStructure> notes = vmDelivery.getVehicleActivityNotes();
                if (notes != null && !notes.isEmpty()) {
                    //Handle notes
                    LOG.info("TODO: Handle {} notes.", notes.size());
                }

            }

            // Make a snapshot after each message in anticipation of incoming requests
            // Purge data if necessary (and force new snapshot if anything was purged)
            // Make sure that the public (locking) getTimetableSnapshot function is not called.
            if (purgeExpiredData) {
                final boolean modified = purgeExpiredData();
                getTimetableSnapshot(modified);
            } else {
                getTimetableSnapshot(false);
            }
        } finally {
            // Always release lock
            bufferLock.unlock();
            if (keepLogging) {
                LOG.info("Reducing SIRI-VM logging until restart");
                keepLogging = false;
            }
        }
    }

    /**
     * Method to apply a trip update list to the most recent version of the timetable snapshot.
     *
     *  @param graph graph to update (needed for adding/changing stop patterns)
     * @param fullDataset true iff the list with updates represent all updates that are active right
     *        now, i.e. all previous updates should be disregarded
     * @param updates SIRI VehicleMonitoringDeliveries that should be applied atomically
     */
    public void applyEstimatedTimetable(final Graph graph, final String feedId, final boolean fullDataset, final List<EstimatedTimetableDeliveryStructure> updates) {
        if (updates == null) {
            LOG.warn("updates is null");
            return;
        }

        // Acquire lock on buffer
        bufferLock.lock();

        try {
            if (fullDataset) {
                // Remove all updates from the buffer
                buffer.clear(feedId);
            }

            for (EstimatedTimetableDeliveryStructure etDelivery : updates) {

                List<EstimatedVersionFrameStructure> estimatedJourneyVersions = etDelivery.getEstimatedJourneyVersionFrames();
                if (estimatedJourneyVersions != null) {
                    //Handle deliveries
                    for (EstimatedVersionFrameStructure estimatedJourneyVersion : estimatedJourneyVersions) {
                        List<EstimatedVehicleJourney> journeys = estimatedJourneyVersion.getEstimatedVehicleJourneies();
                        LOG.info("Handling {} EstimatedVehicleJourneys.", journeys.size());
                        int handledCounter = 0;
                        int skippedCounter = 0;
                        int addedCounter = 0;
                        int notMonitoredCounter = 0;
                        for (EstimatedVehicleJourney journey : journeys) {
                            if (journey.isExtraJourney() != null && journey.isExtraJourney()) {
                                // Added trip
                                try {
                                    if (handleAddedTrip(graph, feedId, journey)) {
                                        addedCounter++;
                                    } else {
                                        skippedCounter++;
                                    }
                                } catch (Throwable t) {
                                    // Since this is work in progress - catch everything to continue processing updates
                                    LOG.warn("Adding ExtraJourney with id='{}' failed, caused by '{}'.", journey.getEstimatedVehicleJourneyCode(), t.getMessage());
                                    skippedCounter++;
                                }
                            } else {
                                // Updated trip
                                if (handleModifiedTrip(graph, feedId, journey)) {
                                    handledCounter++;
                                } else {
                                    if (journey.isMonitored() != null && !journey.isMonitored()) {
                                        notMonitoredCounter++;
                                    } else {
                                        skippedCounter++;
                                    }
                                }
                            }
                        }
                        LOG.info("Processed EstimatedVehicleJourneys: updated {}, added {}, skipped {}, not monitored {}.", handledCounter, addedCounter, skippedCounter, notMonitoredCounter);
                    }
                }
            }

            LOG.debug("message contains {} trip updates", updates.size());
            int uIndex = 0;
            LOG.debug("end of update message");

            // Make a snapshot after each message in anticipation of incoming requests
            // Purge data if necessary (and force new snapshot if anything was purged)
            // Make sure that the public (locking) getTimetableSnapshot function is not called.
            if (purgeExpiredData) {
                final boolean modified = purgeExpiredData();
                getTimetableSnapshot(modified);
            } else {
                getTimetableSnapshot(false);
            }
        } finally {
            // Always release lock
            bufferLock.unlock();
        }
    }

    /**
     * Returns any new TripPatterns added by real time information for a given stop.
     *
     * @param stop the stop
     * @return list of TripPatterns created by real time sources for the stop.
     */
    public List<TripPattern> getAddedTripPatternsForStop(Stop stop) {
        return tripPatternCache.getAddedTripPatternsForStop(stop);
    }

    private static boolean keepLogging = true;

    private boolean handleModifiedTrip(Graph graph, String feedId, VehicleActivityStructure activity, ServiceDate serviceDate) {
        if (activity.getValidUntilTime().isBefore(ZonedDateTime.now())) {
            //Activity has expired
            return false;
        }


        if (activity.getMonitoredVehicleJourney() == null ||
                activity.getMonitoredVehicleJourney().getVehicleRef() == null ||
                activity.getMonitoredVehicleJourney().getLineRef() == null) {
            //No vehicle reference or line reference
            return false;
        }

        Boolean isMonitored = activity.getMonitoredVehicleJourney().isMonitored();
        if (isMonitored != null && !isMonitored) {
            //Vehicle is reported as NOT monitored
            return false;
        }

        Set<Trip> trips = siriFuzzyTripMatcher.match(activity);

        if (trips == null || trips.isEmpty()) {
            if (keepLogging) {
                String lineRef = (activity.getMonitoredVehicleJourney().getLineRef() != null ? activity.getMonitoredVehicleJourney().getLineRef().getValue():null);
                String vehicleRef = (activity.getMonitoredVehicleJourney().getVehicleRef() != null ? activity.getMonitoredVehicleJourney().getVehicleRef().getValue():null);
                String tripId =  (activity.getMonitoredVehicleJourney().getCourseOfJourneyRef() != null ? activity.getMonitoredVehicleJourney().getCourseOfJourneyRef().getValue():null);
                LOG.debug("No trip found for [isMonitored={}, lineRef={}, vehicleRef={}, tripId={}], skipping VehicleActivity.", isMonitored, lineRef, vehicleRef, tripId);
            }

            return false;
        }

        //Find the trip that best corresponds to MonitoredVehicleJourney
        Trip trip = getTripForJourney(trips, activity.getMonitoredVehicleJourney());

        if (trip == null) {
            return false;
        }

        final Set<TripPattern> patterns = getPatternsForTrip(trips, activity.getMonitoredVehicleJourney());

        if (patterns == null) {
            return false;
        }
        boolean success = false;
        for (TripPattern pattern : patterns) {
            if (handleTripPatternUpdate(graph, pattern, activity, trip, serviceDate)) {
                success = true;
            }
        }

        if (!success) {
            LOG.info("Pattern not updated for trip " + trip.getId());
        }
        return success;
    }

    private boolean handleTripPatternUpdate(Graph graph, TripPattern pattern, VehicleActivityStructure activity, Trip trip, ServiceDate serviceDate) {

        // Apply update on the *scheduled* time table and set the updated trip times in the buffer
        Timetable currentTimetable = getCurrentTimetable(pattern, serviceDate);
        final TripTimes updatedTripTimes = createUpdatedTripTimes(currentTimetable, graph, activity, timeZone, trip.getId());
        if (updatedTripTimes == null) {
            return false;
        }

        final boolean success = buffer.update(pattern, updatedTripTimes, serviceDate);

        return success;
    }

    /**
     * Get the latest timetable for TripPattern for a given service date.
     *
     * Snapshot timetable is used as source if initialised, trip patterns scheduled timetable if not.
     *
     */
    private Timetable getCurrentTimetable(TripPattern tripPattern, ServiceDate serviceDate) {
        TimetableSnapshot timetableSnapshot=getTimetableSnapshot();
        if (timetableSnapshot!=null) {
            return getTimetableSnapshot().resolve(tripPattern, serviceDate);
        }
        return tripPattern.scheduledTimetable;
    }

    private boolean handleAddedTrip(Graph graph, String feedId,  EstimatedVehicleJourney estimatedVehicleJourney) {

        // Verifying values required in SIRI Profile

        // Added ServiceJourneyId
        String newServiceJourneyRef = estimatedVehicleJourney.getEstimatedVehicleJourneyCode();
        Preconditions.checkNotNull(newServiceJourneyRef, "EstimatedVehicleJourneyCode is required");

        // Replaced/duplicated ServiceJourneyId
//        VehicleJourneyRef existingServiceJourneyRef = estimatedVehicleJourney.getVehicleJourneyRef();
//        Preconditions.checkNotNull(existingServiceJourneyRef, "VehicleJourneyRef is required");

        // LineRef of added trip
        Preconditions.checkNotNull(estimatedVehicleJourney.getLineRef(), "LineRef is required");
        String lineRef = estimatedVehicleJourney.getLineRef().getValue();

        //OperatorRef of added trip
        Preconditions.checkNotNull(estimatedVehicleJourney.getOperatorRef(), "OperatorRef is required");
        String operatorRef = estimatedVehicleJourney.getOperatorRef().getValue();

        //Required in SIRI, but currently not in use by OTP
//        Preconditions.checkNotNull(estimatedVehicleJourney.getRouteRef(), "RouteRef is required");
//        String routeRef = estimatedVehicleJourney.getRouteRef().getValue();

//        Preconditions.checkNotNull(estimatedVehicleJourney.getGroupOfLinesRef(), "GroupOfLinesRef is required");
//        String groupOfLines = estimatedVehicleJourney.getGroupOfLinesRef().getValue();

//        Preconditions.checkNotNull(estimatedVehicleJourney.getExternalLineRef(), "ExternalLineRef is required");
        String externalLineRef = estimatedVehicleJourney.getExternalLineRef().getValue();

        // TODO - SIRI: Where is the Operator?
//        Operator operator = graphIndex.operatorForId.get(new FeedScopedId(feedId, operatorRef));
//        Preconditions.checkNotNull(operator, "Operator " + operatorRef + " is unknown");

        FeedScopedId tripId = new FeedScopedId(feedId, newServiceJourneyRef);
        FeedScopedId serviceId = new FeedScopedId(feedId, newServiceJourneyRef);

        Route replacedRoute = null;
        if (externalLineRef != null) {
            replacedRoute = graph.index.getRouteForId(new FeedScopedId(feedId, externalLineRef));
        }

        FeedScopedId routeId = new FeedScopedId(feedId, lineRef);
        Route route = graph.index.getRouteForId(routeId);

        if (route == null) { // Route is unknown - create new
            route = new Route();
            route.setId(routeId);
            route.setType(getRouteType(estimatedVehicleJourney.getVehicleModes()));
//            route.setOperator(operator);

            // TODO - SIRI: Is there a better way to find authority/Agency?
            // Finding first Route with same Operator, and using same Authority
            Agency agency = graph.index.getAllRoutes().stream()
//                    .filter(route1 -> route1 != null &&
//                            route1.getOperator() != null &&
//                            route1.getOperator().equals(operator))
                    .findFirst().get().getAgency();
            route.setAgency(agency);

            if (estimatedVehicleJourney.getPublishedLineNames() != null && !estimatedVehicleJourney.getPublishedLineNames().isEmpty()) {
                route.setShortName("" + estimatedVehicleJourney.getPublishedLineNames().get(0).getValue());
            }
            LOG.info("Adding route {} to graph.", routeId);
            graph.index.addRoutes(route);
        }

        Trip trip = new Trip();
        trip.setId(tripId);
        trip.setRoute(route);

        // TODO - SIRI: Set transport-submode based on replaced- and replacement-route
        if (replacedRoute != null) {
            if (replacedRoute.getType() >= 100 && replacedRoute.getType() < 200) { // Replaced-route is RAIL
                if (route.getType() == 100) {
                    // Replacement-route is also RAIL
//                    trip.setTransportSubmode(TransmodelTransportSubmode.REPLACEMENT_RAIL_SERVICE);
                } else if (route.getType() == 700) {
                    // Replacement-route is BUS
//                    trip.setTransportSubmode(TransmodelTransportSubmode.RAIL_REPLACEMENT_BUS);
                }
            }
        }

        trip.setServiceId(serviceId);

        // TODO - SIRI: PublishedLineName not defined in SIRI-profile
        if (estimatedVehicleJourney.getPublishedLineNames() != null && !estimatedVehicleJourney.getPublishedLineNames().isEmpty()) {
            trip.setRouteShortName("" + estimatedVehicleJourney.getPublishedLineNames().get(0).getValue());
        }

//        trip.setTripOperator(operator);

        // TODO - SIRI: Populate these?
        trip.setShapeId(null);          // Replacement-trip has different shape
//        trip.setTripPrivateCode(null);
//        trip.setTripPublicCode(null);
        trip.setBlockId(null);
        trip.setTripShortName(null);
        trip.setTripHeadsign(null);
//        trip.setKeyValues(null);

        List<Stop> addedStops = new ArrayList<>();
        List<StopTime> aimedStopTimes = new ArrayList<>();

        List<EstimatedCall> estimatedCalls = estimatedVehicleJourney.getEstimatedCalls().getEstimatedCalls();
        for (int i = 0; i < estimatedCalls.size(); i++) {
            EstimatedCall estimatedCall = estimatedCalls.get(i);

            Stop stop = getStopForStopId(feedId,estimatedCall.getStopPointRef().getValue());

            StopTime stopTime = new StopTime();
            stopTime.setStop(stop);
            stopTime.setStopSequence(i);
            stopTime.setTrip(trip);

            ZonedDateTime aimedArrivalTime = estimatedCall.getAimedArrivalTime();
            ZonedDateTime aimedDepartureTime = estimatedCall.getAimedDepartureTime();

            if (aimedArrivalTime != null) {
                stopTime.setArrivalTime(calculateSecondsSinceMidnight(aimedArrivalTime));
            }
            if (aimedDepartureTime != null) {
                stopTime.setDepartureTime(calculateSecondsSinceMidnight(aimedDepartureTime));
            }

            if (estimatedCall.getArrivalBoardingActivity() == ArrivalBoardingActivityEnumeration.ALIGHTING) {
                stopTime.setDropOffType(PICKDROP_SCHEDULED);
            } else {
                stopTime.setDropOffType(PICKDROP_NONE);
            }

            if (estimatedCall.getDepartureBoardingActivity() == DepartureBoardingActivityEnumeration.BOARDING) {
                stopTime.setPickupType(PICKDROP_SCHEDULED);
            } else {
                stopTime.setPickupType(PICKDROP_NONE);
            }

            if (estimatedCall.getDestinationDisplaies() != null && !estimatedCall.getDestinationDisplaies().isEmpty()) {
                NaturalLanguageStringStructure destinationDisplay = estimatedCall.getDestinationDisplaies().get(0);
                stopTime.setStopHeadsign(destinationDisplay.getValue());
            }

            if (i == 0) {
                // Fake arrival on first stop
                stopTime.setArrivalTime(stopTime.getDepartureTime());
            } else if (i == (estimatedCalls.size() - 1)) {
                // Fake departure from last stop
                stopTime.setDepartureTime(stopTime.getArrivalTime());
            }

            addedStops.add(stop);
            aimedStopTimes.add(stopTime);
        }

        StopPattern stopPattern = new StopPattern(aimedStopTimes);
        TripPattern pattern = new TripPattern(trip.getRoute(), stopPattern);

        TripTimes tripTimes = new TripTimes(trip, aimedStopTimes, graph.deduplicator);

        boolean isJourneyPredictionInaccurate = (estimatedVehicleJourney.isPredictionInaccurate() != null && estimatedVehicleJourney.isPredictionInaccurate());

        // If added trip is updated with realtime - loop through and add delays
        for (int i = 0; i < estimatedCalls.size(); i++) {
            EstimatedCall estimatedCall = estimatedCalls.get(i);
            ZonedDateTime expectedArrival = estimatedCall.getExpectedArrivalTime();
            ZonedDateTime expectedDeparture = estimatedCall.getExpectedDepartureTime();

            int aimedArrivalTime = aimedStopTimes.get(i).getArrivalTime();
            int aimedDepartureTime = aimedStopTimes.get(i).getDepartureTime();

            if (expectedArrival != null) {
                int expectedArrivalTime = calculateSecondsSinceMidnight(expectedArrival);
                tripTimes.updateArrivalDelay(i, expectedArrivalTime - aimedArrivalTime);
            }
            if (expectedDeparture != null) {
                int expectedDepartureTime = calculateSecondsSinceMidnight(expectedDeparture);
                tripTimes.updateDepartureDelay(i, expectedDepartureTime - aimedDepartureTime);
            }

            if (estimatedCall.isCancellation() != null) {
                tripTimes.setCancelledStop(i,  estimatedCall.isCancellation());
            }

            boolean isCallPredictionInaccurate = estimatedCall.isPredictionInaccurate() != null && estimatedCall.isPredictionInaccurate();
            tripTimes.setPredictionInaccurate(i, (isJourneyPredictionInaccurate | isCallPredictionInaccurate));

            if (i == 0) {
                // Fake arrival on first stop
                tripTimes.updateArrivalTime(i,tripTimes.getDepartureTime(i));
            } else if (i == (estimatedCalls.size() - 1)) {
                // Fake departure from last stop
                tripTimes.updateDepartureTime(i,tripTimes.getArrivalTime(i));
            }
        }

        // Adding trip to index necessary to include values in graphql-queries
        // TODO - SIRI: should more data be added to index?
        graph.index.getTripForId().put(tripId, trip);
        graph.index.getPatternForTrip().put(trip, pattern);

        if (estimatedVehicleJourney.isCancellation() != null && estimatedVehicleJourney.isCancellation()) {
            tripTimes.cancel();
        } else {
            tripTimes.setRealTimeState(RealTimeState.ADDED);
        }


        if (!graph.getServiceCodes().containsKey(serviceId)) {
            graph.getServiceCodes().put(serviceId, graph.getServiceCodes().size());
        }
        tripTimes.serviceCode = graph.getServiceCodes().get(serviceId);

        pattern.add(tripTimes);

        Preconditions.checkState(tripTimes.timesIncreasing(), "Non-increasing triptimes for added trip");

        ServiceDate serviceDate = getServiceDateForEstimatedVehicleJourney(estimatedVehicleJourney);

        if (graph.getCalendarService().getServiceDatesForServiceId(serviceId) == null ||
                graph.getCalendarService().getServiceDatesForServiceId(serviceId).isEmpty()) {
            LOG.info("Adding serviceId {} to CalendarService", serviceId);
            // TODO - SIRI: Need to add the ExtraJourney as a Trip - alerts may be attached to it
//           graph.getCalendarService().addServiceIdAndServiceDates(serviceId, Arrays.asList(serviceDate));
        }


        return addTripToGraphAndBuffer(feedId, graph, trip, aimedStopTimes, addedStops, tripTimes, serviceDate);
    }

    /*
     * Resolves TransportMode from SIRI VehicleMode
     */
    private int getRouteType(List<VehicleModesEnumeration> vehicleModes) {
        if (vehicleModes != null && !vehicleModes.isEmpty()) {
            VehicleModesEnumeration vehicleModesEnumeration = vehicleModes.get(0);
            switch (vehicleModesEnumeration) {
                case RAIL:
                    return 100;
                case COACH:
                    return 200;
                case BUS:
                    return 700;
                case METRO:
                    return 701;
                case TRAM:
                    return 900;
                case FERRY:
                    return 1000;
                case AIR:
                    return 1100;
            }
        }
        return 700;
    }

    private boolean handleModifiedTrip(Graph graph, String feedId, EstimatedVehicleJourney estimatedVehicleJourney) {

        //Check if EstimatedVehicleJourney is reported as NOT monitored
        if (estimatedVehicleJourney.isMonitored() != null && !estimatedVehicleJourney.isMonitored()) {
            //Ignore the notMonitored-flag if the journey is NOT monitored because it has been cancelled
            if (estimatedVehicleJourney.isCancellation() != null && !estimatedVehicleJourney.isCancellation()) {
                return false;
            }
        }

        //Values used in logging
        String operatorRef = (estimatedVehicleJourney.getOperatorRef() != null ? estimatedVehicleJourney.getOperatorRef().getValue() : null);
        String vehicleModes = "" + estimatedVehicleJourney.getVehicleModes();
        String lineRef = estimatedVehicleJourney.getLineRef().getValue();
        String vehicleRef = (estimatedVehicleJourney.getVehicleRef() != null ? estimatedVehicleJourney.getVehicleRef().getValue() : null);

        ServiceDate serviceDate = getServiceDateForEstimatedVehicleJourney(estimatedVehicleJourney);

        if (serviceDate == null) {
            return false;
        }

        Set<TripTimes> times = new HashSet<>();
        Set<TripPattern> patterns = new HashSet<>();

        Trip tripMatchedByServiceJourneyId = siriFuzzyTripMatcher.findTripByDatedVehicleJourneyRef(estimatedVehicleJourney);

        if (tripMatchedByServiceJourneyId != null) {
            /*
              Found exact match
             */
            TripPattern exactPattern = routingService.getPatternForTrip().get(tripMatchedByServiceJourneyId);

            if (exactPattern != null) {
                Timetable currentTimetable = getCurrentTimetable(exactPattern, serviceDate);
                TripTimes exactUpdatedTripTimes = createUpdatedTripTimes(graph, currentTimetable, estimatedVehicleJourney, timeZone, tripMatchedByServiceJourneyId.getId());
                if (exactUpdatedTripTimes != null) {
                    times.add(exactUpdatedTripTimes);
                    patterns.add(exactPattern);
                } else {
                    LOG.info("Failed to update TripTimes for trip found by exact match {}", tripMatchedByServiceJourneyId.getId());
                    return false;
                }
            }
        } else {
            /*
                No exact match found - search for trips based on arrival-times/stop-patterns
             */
            Set<Trip> trips = siriFuzzyTripMatcher.match(estimatedVehicleJourney);

            if (trips == null || trips.isEmpty()) {
                LOG.debug("No trips found for EstimatedVehicleJourney. [operator={}, vehicleModes={}, lineRef={}, vehicleRef={}]", operatorRef, vehicleModes, lineRef, vehicleRef);
                return false;
            }

            //Find the trips that best corresponds to EstimatedVehicleJourney
            Set<Trip> matchingTrips = getTripForJourney(trips, estimatedVehicleJourney);

            if (matchingTrips == null || matchingTrips.isEmpty()) {
                LOG.debug("Found no matching trip for SIRI ET (serviceDate, departureTime). [operator={}, vehicleModes={}, lineRef={}, vehicleJourneyRef={}]", operatorRef, vehicleModes, lineRef, vehicleRef);
                return false;
            }

            for (Trip matchingTrip : matchingTrips) {
                TripPattern pattern = getPatternForTrip(matchingTrip, estimatedVehicleJourney);
                if (pattern != null) {
                    Timetable currentTimetable = getCurrentTimetable(pattern, serviceDate);
                    TripTimes updatedTripTimes = createUpdatedTripTimes(graph, currentTimetable, estimatedVehicleJourney, timeZone, matchingTrip.getId());
                    if (updatedTripTimes != null) {
                        patterns.add(pattern);
                        times.add(updatedTripTimes);
                    }
                }
            }
        }

        if (patterns.isEmpty()) {
            LOG.debug("Found no matching pattern for SIRI ET (firstStopId, lastStopId, numberOfStops). [operator={}, vehicleModes={}, lineRef={}, vehicleRef={}]", operatorRef, vehicleModes, lineRef, vehicleRef);
            return false;
        }

        if (times.isEmpty()) {
            return false;
        }

        boolean result = false;
        for (TripTimes tripTimes : times) {
            Trip trip = tripTimes.trip;
            for (TripPattern pattern : patterns) {
                if (tripTimes.getNumStops() == pattern.stopPattern.stops.length) {
                    if (!tripTimes.isCanceled()) {
                        /*
                          UPDATED and MODIFIED tripTimes should be handled the same way to always allow latest realtime-update
                          to replace previous update regardless of realtimestate
                         */

                        cancelScheduledTrip(feedId, trip.getId().getId(), serviceDate);

                        // Check whether trip id has been used for previously ADDED/MODIFIED trip message and cancel
                        // previously created trip
                        cancelPreviouslyAddedTrip(feedId, trip.getId().getId(), serviceDate);

                        // Calculate modified stop-pattern
                        Timetable currentTimetable = getCurrentTimetable(pattern, serviceDate);
                        List<Stop> modifiedStops = createModifiedStops(currentTimetable, estimatedVehicleJourney,
                            routingService
                        );
                        List<StopTime> modifiedStopTimes = createModifiedStopTimes(currentTimetable, tripTimes, estimatedVehicleJourney, trip,
                            routingService
                        );

                        if (modifiedStops != null && modifiedStops.isEmpty()) {
                            tripTimes.cancel();
                        } else {
                            // Add new trip
                            result = result | addTripToGraphAndBuffer(feedId, graph, trip, modifiedStopTimes, modifiedStops, tripTimes, serviceDate);
                        }
                    } else {
                        result = result | buffer.update(pattern, tripTimes, serviceDate);
                    }

                    LOG.debug("Applied realtime data for trip {}", trip.getId().getId());
                } else {
                    LOG.debug("Ignoring update since number of stops do not match");
                }
            }
        }

        return result;
    }

    private ServiceDate getServiceDateForEstimatedVehicleJourney(EstimatedVehicleJourney estimatedVehicleJourney) {
        ZonedDateTime date;
        if (estimatedVehicleJourney.getRecordedCalls() != null && !estimatedVehicleJourney.getRecordedCalls().getRecordedCalls().isEmpty()){
            date = estimatedVehicleJourney.getRecordedCalls().getRecordedCalls().get(0).getAimedDepartureTime();
        } else {
            EstimatedCall firstCall = estimatedVehicleJourney.getEstimatedCalls().getEstimatedCalls().get(0);
            date = firstCall.getAimedDepartureTime();
        }

        if (date == null) {
            return null;
        }


        return new ServiceDate(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }

    private int calculateSecondsSinceMidnight(ZonedDateTime dateTime) {
        return dateTime.toLocalTime().toSecondOfDay();
    }


    /**
     * Add a (new) trip to the graph and the buffer
     *
     * @return true if successful
     */
    private boolean addTripToGraphAndBuffer(final String feedId, final Graph graph, final Trip trip,
                                            final List<StopTime> stopTimes, final List<Stop> stops, TripTimes updatedTripTimes,
                                            final ServiceDate serviceDate) {

        // Preconditions
        Preconditions.checkNotNull(stops);
        Preconditions.checkArgument(stopTimes.size() == stops.size(),
                "number of stop should match the number of stop time updates");

        // Create StopPattern
        final StopPattern stopPattern = new StopPattern(stopTimes);

        // Get cached trip pattern or create one if it doesn't exist yet
        final TripPattern pattern = tripPatternCache.getOrCreateTripPattern(stopPattern, trip, graph, serviceDate);

        // Add service code to bitset of pattern if needed (using copy on write)
        final int serviceCode = graph.getServiceCodes().get(trip.getServiceId());
        if (!pattern.getServices().get(serviceCode)) {
            final BitSet services = (BitSet) pattern.getServices().clone();
            services.set(serviceCode);
            pattern.setServices(services);
        }


        /*
         * Update pattern with triptimes so get correct dwell times and lower bound on running times.
         * New patterns only affects a single trip, previously added tripTimes is no longer valid, and is therefore removed
         */
        pattern.scheduledTimetable.tripTimes.clear();
        pattern.scheduledTimetable.addTripTimes(updatedTripTimes);
        pattern.scheduledTimetable.finish();

        // Remove trip times to avoid real time trip times being visible for ignoreRealtimeInformation queries
        pattern.scheduledTimetable.tripTimes.clear();

        // Add to buffer as-is to include it in the 'lastAddedTripPattern'
        buffer.update(pattern, updatedTripTimes, serviceDate);

        //TODO - SIRI: Add pattern to index?

        // Add new trip times to the buffer
        final boolean success = buffer.update(pattern, updatedTripTimes, serviceDate);
        return success;
    }

    /**
     * Cancel scheduled trip in buffer given trip id (without agency id) on service date
     *
     * @param tripId trip id without agency id
     * @param serviceDate service date
     * @return true if scheduled trip was cancelled
     */
    private boolean cancelScheduledTrip(String feedId, String tripId, final ServiceDate serviceDate) {
        boolean success = false;
        
        final TripPattern pattern = getPatternForTripId(feedId, tripId);

        if (pattern != null) {
            // Cancel scheduled trip times for this trip in this pattern
            final Timetable timetable = pattern.scheduledTimetable;
            final int tripIndex = timetable.getTripIndex(tripId);
            if (tripIndex == -1) {
                LOG.warn("Could not cancel scheduled trip {}", tripId);
            } else {
                final TripTimes newTripTimes = new TripTimes(timetable.getTripTimes(tripIndex));
                newTripTimes.cancel();
                buffer.update(pattern, newTripTimes, serviceDate);
                success = true;
            }
        }

        return success;
    }

    /**
     * Cancel previously added trip from buffer if there is a previously added trip with given trip
     * id (without agency id) on service date
     *
     * @param feedId feed id the trip id belongs to
     * @param tripId trip id without agency id
     * @param serviceDate service date
     * @return true if a previously added trip was cancelled
     */
    private boolean cancelPreviouslyAddedTrip(final String feedId, final String tripId, final ServiceDate serviceDate) {
        boolean success = false;

        final TripPattern pattern = buffer.getLastAddedTripPattern(new FeedScopedId(feedId, tripId), serviceDate);
        if (pattern != null) {
            // Cancel trip times for this trip in this pattern
            final Timetable timetable = buffer.resolve(pattern, serviceDate);
            final int tripIndex = timetable.getTripIndex(tripId);
            if (tripIndex == -1) {
                LOG.warn("Could not cancel previously added trip {}", tripId);
            } else {
                final TripTimes newTripTimes = new TripTimes(timetable.getTripTimes(tripIndex));
                newTripTimes.cancel();
                buffer.update(pattern, newTripTimes, serviceDate);
//                buffer.removeLastAddedTripPattern(feedId, tripId, serviceDate);
                success = true;
            }
        }

        return success;
    }

    private boolean purgeExpiredData() {
        final ServiceDate today = new ServiceDate();
        final ServiceDate previously = today.previous().previous(); // Just to be safe...

        if(lastPurgeDate != null && lastPurgeDate.compareTo(previously) > 0) {
            return false;
        }

        LOG.debug("purging expired realtime data");

        lastPurgeDate = previously;

        return buffer.purgeExpiredData(previously);
    }

    /**
     * Retrieve a trip pattern given a feed id and trid id.
     *
     * @param feedId feed id for the trip id
     * @param tripId trip id without agency
     * @return trip pattern or null if no trip pattern was found
     */
    private TripPattern getPatternForTripId(String feedId, String tripId) {
        Trip trip = routingService.getTripForId().get(new FeedScopedId(feedId, tripId));
        return routingService.getPatternForTrip().get(trip);
    }

    private Set<TripPattern> getPatternsForTrip(Set<Trip> matches, VehicleActivityStructure.MonitoredVehicleJourney monitoredVehicleJourney) {

        if (monitoredVehicleJourney.getOriginRef() == null) {
            return null;
        }

        ZonedDateTime date = monitoredVehicleJourney.getOriginAimedDepartureTime();
        if (date == null) {
            //If no date is set - assume Realtime-data is reported for 'today'.
            date = ZonedDateTime.now();
        }
        ServiceDate realTimeReportedServiceDate = new ServiceDate(date.getYear(), date.getMonthValue(), date.getDayOfMonth());

        Set<TripPattern> patterns = new HashSet<>();
        for (Iterator<Trip> iterator = matches.iterator(); iterator.hasNext(); ) {
            Trip currentTrip = iterator.next();
            TripPattern tripPattern = routingService.getPatternForTrip().get(currentTrip);
            Set<ServiceDate> serviceDates = routingService.getCalendarService().getServiceDatesForServiceId(currentTrip.getServiceId());

            if (!serviceDates.contains(realTimeReportedServiceDate)) {
                // Current trip has no service on the date of the 'MonitoredVehicleJourney'
                continue;
            }

            Stop firstStop = tripPattern.getStop(0);
            Stop lastStop = tripPattern.getStop(tripPattern.getStops().size() - 1);

            String siriOriginRef = monitoredVehicleJourney.getOriginRef().getValue();

            if (monitoredVehicleJourney.getDestinationRef() != null) {
                String siriDestinationRef = monitoredVehicleJourney.getDestinationRef().getValue();

                boolean firstStopIsMatch = firstStop.getId().getId().equals(siriOriginRef);
                boolean lastStopIsMatch = lastStop.getId().getId().equals(siriDestinationRef);

                if (!firstStopIsMatch && firstStop.isPartOfStation()) {
                    Stop otherFirstStop = routingService.getStopForId(
                            new FeedScopedId(firstStop.getId().getFeedId(), siriOriginRef)
                    );
                    firstStopIsMatch = firstStop.isPartOfSameStationAs(otherFirstStop);
                }

                if (!lastStopIsMatch && lastStop.isPartOfStation()) {
                    Stop otherLastStop = routingService.getStopForId(
                            new FeedScopedId(lastStop.getId().getFeedId(), siriDestinationRef)
                    );
                    lastStopIsMatch = lastStop.isPartOfSameStationAs(otherLastStop);
                }

                if (firstStopIsMatch & lastStopIsMatch) {
                    // Origin and destination matches
                    TripPattern lastAddedTripPattern = buffer.getLastAddedTripPattern(currentTrip.getId(), realTimeReportedServiceDate);
                    if (lastAddedTripPattern != null) {
                        patterns.add(lastAddedTripPattern);
                    } else {
                        patterns.add(tripPattern);
                    }
                }
            } else {
                //Match origin only - since destination is not defined
                if (firstStop.getId().getId().equals(siriOriginRef)) {
                    tripPattern.scheduledTimetable.tripTimes.get(0).getDepartureTime(0); // TODO does this line do anything?
                    patterns.add(tripPattern);
                }
            }


        }
        return patterns;
    }

    private Set<TripPattern> getPatternForTrip(Set<Trip> trips, EstimatedVehicleJourney journey) {
        Set<TripPattern> patterns = new HashSet<>();
        for (Trip trip : trips) {
            TripPattern pattern = getPatternForTrip(trip, journey);
            if (pattern != null) {
                patterns.add(pattern);
            }
        }
        return patterns;
    }
    private TripPattern getPatternForTrip(Trip trip, EstimatedVehicleJourney journey) {

        Set<ServiceDate> serviceDates = routingService.getCalendarService().getServiceDatesForServiceId(trip.getServiceId());

        List<RecordedCall> recordedCalls = (journey.getRecordedCalls() != null ? journey.getRecordedCalls().getRecordedCalls():new ArrayList<>());
        List<EstimatedCall> estimatedCalls = journey.getEstimatedCalls().getEstimatedCalls();

            String journeyFirstStopId;
            ServiceDate journeyDate;
            if (recordedCalls != null && !recordedCalls.isEmpty()) {
                RecordedCall recordedCall = recordedCalls.get(0);
                journeyFirstStopId = recordedCall.getStopPointRef().getValue();
                journeyDate = new ServiceDate(Date.from(recordedCall.getAimedDepartureTime().toInstant()));
            } else if (estimatedCalls != null && !estimatedCalls.isEmpty()) {
                EstimatedCall estimatedCall = estimatedCalls.get(0);
                journeyFirstStopId = estimatedCall.getStopPointRef().getValue();
                journeyDate = new ServiceDate(Date.from(estimatedCall.getAimedDepartureTime().toInstant()));
            } else {
                return null;
            }

            String journeyLastStopId = estimatedCalls.get(estimatedCalls.size() - 1).getStopPointRef().getValue();


            TripPattern lastAddedTripPattern = null;
            if (getTimetableSnapshot() != null) {
                lastAddedTripPattern  = getTimetableSnapshot().getLastAddedTripPattern(trip.getId(), journeyDate);
            }

            TripPattern tripPattern;
            if (lastAddedTripPattern != null) {
                tripPattern = lastAddedTripPattern;
            } else {
                tripPattern = routingService.getPatternForTrip().get(trip);
            }


            Stop firstStop = tripPattern.getStop(0);
            Stop lastStop = tripPattern.getStop(tripPattern.getStops().size() - 1);

            if (serviceDates.contains(journeyDate)) {
                boolean firstStopIsMatch = firstStop.getId().getId().equals(journeyFirstStopId);
                boolean lastStopIsMatch = lastStop.getId().getId().equals(journeyLastStopId);

                if (!firstStopIsMatch && firstStop.isPartOfStation()) {
                    Stop otherFirstStop = routingService
                        .getStopForId(
                                new FeedScopedId(firstStop.getId().getFeedId(), journeyFirstStopId)
                        );
                    firstStopIsMatch = firstStop.isPartOfSameStationAs(otherFirstStop);
                }

                if (!lastStopIsMatch && lastStop.isPartOfStation()) {
                    Stop otherLastStop = routingService
                        .getStopForId(
                                new FeedScopedId(lastStop.getId().getFeedId(), journeyLastStopId)
                        );
                    lastStopIsMatch = lastStop.isPartOfSameStationAs(otherLastStop);
                }

                if (firstStopIsMatch & lastStopIsMatch) {
                    // Found matches
                    return tripPattern;
                }

                return null;
            }

        return null;
    }

    /**
     * Finds the correct trip based on OTP-ServiceDate and SIRI-DepartureTime
     * @param trips
     * @param monitoredVehicleJourney
     * @return
     */
    private Trip getTripForJourney(Set<Trip> trips, VehicleActivityStructure.MonitoredVehicleJourney monitoredVehicleJourney) {
        ZonedDateTime date = monitoredVehicleJourney.getOriginAimedDepartureTime();
        if (date == null) {
            //If no date is set - assume Realtime-data is reported for 'today'.
            date = ZonedDateTime.now();
        }
        ServiceDate serviceDate = new ServiceDate(date.getYear(), date.getMonthValue(), date.getDayOfMonth());

        List<Trip> results = new ArrayList<>();
        for (Iterator<Trip> iterator = trips.iterator(); iterator.hasNext(); ) {

            Trip trip = iterator.next();
            Set<ServiceDate> serviceDatesForServiceId = routingService.getCalendarService().getServiceDatesForServiceId(trip.getServiceId());

            for (Iterator<ServiceDate> serviceDateIterator = serviceDatesForServiceId.iterator(); serviceDateIterator.hasNext(); ) {
                ServiceDate next = serviceDateIterator.next();
                if (next.equals(serviceDate)) {
                    results.add(trip);
                }
            }
        }

        if (results.size() == 1) {
            return results.get(0);
        } else if (results.size() > 1) {
            // Multiple possible matches - check if lineRef/routeId matches
            if (monitoredVehicleJourney.getLineRef() != null && monitoredVehicleJourney.getLineRef().getValue() != null) {
                String lineRef = monitoredVehicleJourney.getLineRef().getValue();
                for (Trip trip : results) {
                    if (lineRef.equals(trip.getRoute().getId().getId())) {
                        // Return first trip where the lineRef matches routeId
                        return trip;
                    }
                }
            }

            // Line does not match any routeId - return first result.
            return results.get(0);
        }

        return null;
    }

    /**
     * Finds the correct trip based on OTP-ServiceDate and SIRI-DepartureTime
     * @param trips
     * @param journey
     * @return
     */
    private Set<Trip> getTripForJourney(Set<Trip> trips, EstimatedVehicleJourney journey) {


        List<RecordedCall> recordedCalls = (journey.getRecordedCalls() != null ? journey.getRecordedCalls().getRecordedCalls():new ArrayList<>());
        List<EstimatedCall> estimatedCalls = journey.getEstimatedCalls().getEstimatedCalls();

        ZonedDateTime date;
        int stopNumber = 1;
        String firstStopId;
        if (recordedCalls != null && !recordedCalls.isEmpty()) {
            RecordedCall recordedCall = recordedCalls.get(0);
            date = recordedCall.getAimedDepartureTime();
            firstStopId = recordedCall.getStopPointRef().getValue();
        } else if (estimatedCalls != null && !estimatedCalls.isEmpty()) {
            EstimatedCall estimatedCall = estimatedCalls.get(0);
            if (estimatedCall.getOrder() != null) {
                stopNumber = estimatedCall.getOrder().intValue();
            } else if (estimatedCall.getVisitNumber() != null) {
                stopNumber = estimatedCall.getVisitNumber().intValue();
            }
            firstStopId = estimatedCall.getStopPointRef().getValue();
            date = estimatedCall.getAimedDepartureTime();
        } else {
            return null;
        }

        if (date == null) {
            //If no date is set - assume Realtime-data is reported for 'today'.
            date = ZonedDateTime.now();
        }
        ServiceDate serviceDate = new ServiceDate(date.getYear(), date.getMonthValue(), date.getDayOfMonth());

        int departureInSecondsSinceMidnight = calculateSecondsSinceMidnight(date);
        Set<Trip> result = new HashSet<>();
        for (Iterator<Trip> iterator = trips.iterator(); iterator.hasNext(); ) {

            Trip trip = iterator.next();
            Set<ServiceDate> serviceDatesForServiceId = routingService.getCalendarService().getServiceDatesForServiceId(trip.getServiceId());
            if (serviceDatesForServiceId.contains(serviceDate)) {

                TripPattern pattern = routingService.getPatternForTrip().get(trip);

                if (stopNumber < pattern.stopPattern.stops.length) {
                    boolean firstReportedStopIsFound = false;
                    Stop stop = pattern.stopPattern.stops[stopNumber-1];
                    if (firstStopId.equals(stop.getId().getId())) {
                       firstReportedStopIsFound = true;
                    } else {
                        String agencyId = stop.getId().getFeedId();
                        if (stop.isPartOfStation()) {
                            Stop alternativeStop = routingService
                                .getStopForId(new FeedScopedId(agencyId, firstStopId));
                            if (stop.isPartOfSameStationAs(alternativeStop)) {
                                firstReportedStopIsFound = true;
                            }
                        }
                    }
                    if (firstReportedStopIsFound) {
                        for (TripTimes times : getCurrentTimetable(pattern, serviceDate).tripTimes) {
                            if (times.getScheduledDepartureTime(stopNumber - 1) == departureInSecondsSinceMidnight) {
                                if (routingService.getCalendarService().getServiceDatesForServiceId(times.trip.getServiceId()).contains(serviceDate)) {
                                    result.add(times.trip);
                                }
                            }
                        }
                    }
                }
            }
        }

        if (result.size() >= 1) {
            return result;
        } else {
            return null;
        }
    }

    /**
     * Retrieve route given a route id without an agency
     *
     * @param feedId feed id for the route id
     * @param routeId route id without the agency
     * @return route or null if route can't be found in graph index
     */
    private Route getRouteForRouteId(String feedId, String routeId) {
        return routingService.getRouteForId(new FeedScopedId(feedId, routeId));
    }

    /**
     * Retrieve trip given a trip id without an agency
     *
     * @param feedId feed id for the trip id
     * @param tripId trip id without the agency
     * @return trip or null if trip can't be found in graph index
     */
    private Trip getTripForTripId(String feedId, String tripId) {
        Trip trip = routingService.getTripForId().get(new FeedScopedId(feedId, tripId));
        return trip;
    }

    /**
     * Retrieve stop given a feed id and stop id.
     *
     * @param feedId feed id for the stop id
     * @param stopId trip id without the agency
     * @return stop or null if stop doesn't exist
     */
    private Stop getStopForStopId(String feedId, String stopId) {
        return routingService.getStopForId(new FeedScopedId(feedId, stopId));
    }
}
