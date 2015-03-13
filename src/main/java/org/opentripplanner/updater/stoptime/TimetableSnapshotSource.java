/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.updater.stoptime;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import jersey.repackaged.com.google.common.base.Preconditions;

import com.google.common.collect.Maps;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.edgetype.TimetableResolver;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship;

/**
 * This class should be used to create snapshots of lookup tables of realtime data. This is
 * necessary to provide planning threads a consistent constant view of a graph with realtime data at
 * a specific point in time.
 */
public class TimetableSnapshotSource {
    private static final Logger LOG = LoggerFactory.getLogger(TimetableSnapshotSource.class);

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
    private TimetableResolver snapshot = null;

    /** The working copy of the timetable resolver. Should not be visible to routing threads. */
    private TimetableResolver buffer = new TimetableResolver();

    /** Should expired realtime data be purged from the graph. */
    public boolean purgeExpiredData = true;

    protected ServiceDate lastPurgeDate = null;

    protected long lastSnapshotTime = -1;

    private final TimeZone timeZone;

    private GraphIndex graphIndex;

    public TimetableSnapshotSource(Graph graph) {
        timeZone = graph.getTimeZone();
        graphIndex = graph.index;
    }

    /**
     * @return an up-to-date snapshot mapping TripPatterns to Timetables. This snapshot and the
     *         timetable objects it references are guaranteed to never change, so the requesting
     *         thread is provided a consistent view of all TripTimes. The routing thread need only
     *         release its reference to the snapshot to release resources.
     */
    public TimetableResolver getTimetableSnapshot() {
        return getTimetableSnapshot(false);
    }

    protected synchronized TimetableResolver getTimetableSnapshot(boolean force) {
        long now = System.currentTimeMillis();
        if (force || now - lastSnapshotTime > maxSnapshotFrequency) {
            if (force || buffer.isDirty()) {
                LOG.debug("Committing {}", buffer.toString());
                snapshot = buffer.commit(force);
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
     * A GTFS-RT feed is always applied against a single static feed (indicated by feedId).
     * However, multi-feed support is not completed and we currently assume there is only one static feed when matching IDs.
     * 
     * @param graph graph to update (only needed for changing stop patterns) 
     * @param updates GTFS-RT TripUpdate's
     * @param feedId
     */
    public void applyTripUpdates(Graph graph, final List<TripUpdate> updates, final String feedId) {
        if (updates == null) {
            LOG.warn("updates is null");
            return;
        }

        LOG.debug("message contains {} trip updates", updates.size());
        int uIndex = 0;
        for (TripUpdate tripUpdate : updates) {
            if (!tripUpdate.hasTrip()) {
                LOG.warn("Missing TripDescriptor in gtfs-rt trip update: \n{}", tripUpdate);
                continue;
            }

            ServiceDate serviceDate = new ServiceDate();
            TripDescriptor tripDescriptor = tripUpdate.getTrip();

            if (tripDescriptor.hasStartDate()) {
                try {
                    serviceDate = ServiceDate.parseString(tripDescriptor.getStartDate());
                } catch (ParseException e) {
                    LOG.warn("Failed to parse startDate in gtfs-rt trip update: \n{}", tripUpdate);
                    continue;
                }
            }

            uIndex += 1;
            LOG.debug("trip update #{} ({} updates) :",
                    uIndex, tripUpdate.getStopTimeUpdateCount());
            LOG.trace("{}", tripUpdate);

            boolean applied = false;
            if (tripDescriptor.hasScheduleRelationship()) {
                switch(tripDescriptor.getScheduleRelationship()) {
                    case SCHEDULED:
                        applied = handleScheduledTrip(tripUpdate, feedId, serviceDate);
                        break;
                    case ADDED:
                        applied = validateAndHandleAddedTrip(graph, tripUpdate, feedId, serviceDate);
                        break;
                    case UNSCHEDULED:
                        applied = handleUnscheduledTrip(tripUpdate, feedId, serviceDate);
                        break;
                    case CANCELED:
                        applied = handleCanceledTrip(tripUpdate, feedId, serviceDate);
                        break;
                    case REPLACEMENT:
                        applied = handleReplacementTrip(tripUpdate, feedId, serviceDate);
                        break;
                }
            } else {
                // Default
                applied = handleScheduledTrip(tripUpdate, feedId, serviceDate);
            }

            if (applied) {
                appliedBlockCount++;
            } else {
                LOG.warn("Failed to apply TripUpdate.");
                LOG.trace(" Contents: {}", tripUpdate);
            }

            if (appliedBlockCount % logFrequency == 0) {
                LOG.info("Applied {} trip updates.", appliedBlockCount);
            }
        }
        LOG.debug("end of update message");

        // Make a snapshot after each message in anticipation of incoming requests
        // Purge data if necessary (and force new snapshot if anything was purged)
        if(purgeExpiredData) {
            boolean modified = purgeExpiredData();
            getTimetableSnapshot(modified);
        } else {
            getTimetableSnapshot();
        }
    }

    protected boolean handleScheduledTrip(TripUpdate tripUpdate, String feedId, ServiceDate serviceDate) {
        TripDescriptor tripDescriptor = tripUpdate.getTrip();
        // This does not include Agency ID or feed ID, trips are feed-unique and we currently assume a single static feed.
        String tripId = tripDescriptor.getTripId();
        TripPattern pattern = getPatternForTripId(tripId);

        if (pattern == null) {
            LOG.warn("No pattern found for tripId {}, skipping TripUpdate.", tripId);
            return false;
        }

        if (tripUpdate.getStopTimeUpdateCount() < 1) {
            LOG.warn("TripUpdate contains no updates, skipping.");
            return false;
        }

        // Apply update on the *scheduled* time table and set the updated trip times in the buffer
        TripTimes updatedTripTimes = pattern.scheduledTimetable.createUpdatedTripTimes(tripUpdate,
                timeZone, serviceDate);
        boolean success = buffer.update(pattern, updatedTripTimes, serviceDate); 
        return success;
    }

    /**
     * Validate and handle GTFS-RT TripUpdate message containing an ADDED trip.
     * 
     * @param graph graph to update 
     * @param tripUpdate GTFS-RT TripUpdate message
     * @param feedId
     * @param serviceDate
     * @return true iff successful
     */
    protected boolean validateAndHandleAddedTrip(final Graph graph, final TripUpdate tripUpdate,
            final String feedId, final ServiceDate serviceDate) {
        // Preconditions
        Preconditions.checkNotNull(graph);
        Preconditions.checkNotNull(tripUpdate);
        Preconditions.checkNotNull(serviceDate);
        
        //
        // Validate added trip
        //
        
        // Check whether trip id of ADDED trip is available
        TripDescriptor tripDescriptor = tripUpdate.getTrip();
        if (!tripDescriptor.hasTripId()) {
            LOG.warn("No trip id found for ADDED trip, skipping.");
            return false;
        }
        
        // Check whether trip id already exists in graph
        String tripId = tripDescriptor.getTripId();
        Trip trip = getTripForTripId(tripId);
        if (trip != null) {
            // TODO: should we support this?
            LOG.warn("Graph already contains trip id of ADDED trip, skipping.");
            return false;
        }
        
        // Check whether a start date exists
        if (!tripDescriptor.hasStartDate()) {
            // TODO: should we support this and just use the current day?
            LOG.warn("ADDED trip doesn't have a start date in TripDescriptor, skipping.");
            return false;
        }
        
        // Check whether at least two stop updates exist
        if (tripUpdate.getStopTimeUpdateCount() < 2) {
            LOG.warn("ADDED trip has less then two stops, skipping.");
            return false;
        }
        
        // Check whether all stop times are available and all stops exist
        Integer previousStopSequence = null;
        Long previousTime = null;
        List<StopTimeUpdate> stopTimeUpdates = tripUpdate.getStopTimeUpdateList();
        List<Stop> stops = new ArrayList<>(stopTimeUpdates.size()); 
        for (int index = 0; index < stopTimeUpdates.size(); ++index) {
            StopTimeUpdate stopTimeUpdate = stopTimeUpdates.get(index);
            
            // Check stop sequence
            if (stopTimeUpdate.hasStopSequence()) {
                Integer stopSequence = stopTimeUpdate.getStopSequence();
                
                // Check non-negative
                if (stopSequence < 0) {
                    LOG.warn("ADDED trip contains negative stop sequence, skipping.");
                    return false;
                }
                
                // Check whether sequence is increasing
                if (previousStopSequence != null && previousStopSequence > stopSequence) {
                    LOG.warn("ADDED trip contains decreasing stop sequence, skipping.");
                    return false;
                }
                previousStopSequence = stopSequence;
            } else {
                LOG.warn("ADDED trip misses some stop sequences, skipping.");
                return false;
            }
            
            // Find stops
            if (stopTimeUpdate.hasStopId()) {
                // Find stop
                Stop stop = getStopForStopId(stopTimeUpdate.getStopId());
                if (stop != null) {
                    // Remember stop
                    stops.add(stop);
                } else {
                    LOG.warn("Graph doesn't contain stop id \"{}\" of ADDED trip, skipping.",
                            stopTimeUpdate.getStopId());
                    return false;
                }
            } else {
                LOG.warn("ADDED trip misses some stop ids, skipping.");
                return false;
            }
            
            // Check arrival time
            if (stopTimeUpdate.hasArrival() && stopTimeUpdate.getArrival().hasTime()) {
                // Check for increasing time
                Long time = stopTimeUpdate.getArrival().getTime();
                if (previousTime != null && previousTime > time) {
                    LOG.warn("ADDED trip contains decreasing times, skipping.");
                    return false;
                }
                previousTime = time;
            } else {
                // Only first stop is allowed to miss arrival time
                // TODO: should we support only requiring an arrival time on the last stop and interpolate? 
                if (index > 0) {
                    LOG.warn("ADDED trip misses arrival time, skipping.");
                    return false;
                }
            }
            
            // Check departure time
            if (stopTimeUpdate.hasDeparture() && stopTimeUpdate.getDeparture().hasTime()) {
                // Check for increasing time
                Long time = stopTimeUpdate.getDeparture().getTime();
                if (previousTime != null && previousTime > time) {
                    LOG.warn("ADDED trip contains decreasing times, skipping.");
                    return false;
                }
                previousTime = time;
            } else {
                // Only last stop is allowed to miss departure time
                // TODO: should we support only requiring a departure time on the first stop and interpolate? 
                if (index < stopTimeUpdates.size() - 1) {
                    LOG.warn("ADDED trip misses departure time, skipping.");
                    return false;
                }
            }
            
            // Check schedule relationship
            if (stopTimeUpdate.hasScheduleRelationship() &&
                    stopTimeUpdate.getScheduleRelationship() != ScheduleRelationship.SCHEDULED) {
                LOG.warn("ADDED trip has invalid schedule relationship, skipping.");
                return false;
            }
        }
        
        //
        // Handle added trip
        //
        
        boolean success = handleAddedTrip(graph, tripUpdate, stops, feedId, serviceDate);
        return success;
    }

    /**
     * Handle GTFS-RT TripUpdate message containing an ADDED trip.
     * 
     * @param graph graph to update 
     * @param tripUpdate GTFS-RT TripUpdate message
     * @param stops the stops of each StopTimeUpdate in the TripUpdate message
     * @param feedId
     * @param serviceDate service date for added trip 
     * @return true iff successful
     */
    private boolean handleAddedTrip(final Graph graph, final TripUpdate tripUpdate, final List<Stop> stops,
            final String feedId, final ServiceDate serviceDate) {
        // Preconditions
        Preconditions.checkNotNull(stops);
        Preconditions.checkArgument(tripUpdate.getStopTimeUpdateCount() == stops.size(),
                "number of stop should match the number of stop time updates");
        
        // TODO: check whether trip id has been used for previously ADDED trip message
        
        //
        // Handle added trip
        //
        
        // Create new Route
        Route route = null; // TODO: create Route?
        
        // Create new Trip
        Trip trip = new Trip();
        // TODO: which Agency ID to use? Currently use feed id.
        trip.setId(new AgencyAndId(feedId, tripUpdate.getTrip().getTripId()));
        trip.setRoute(route);
        trip.setServiceId(null); // TODO: create ServiceId?
        
        // Calculate seconds since epoch on GTFS midnight (noon minus 12h) of service date 
        Calendar serviceCalendar = serviceDate.getAsCalendar(timeZone);
        final long midnightSecondsSinceEpoch = serviceCalendar.getTimeInMillis() / MILLIS_PER_SECOND;
        
        // Create StopTimes
        List<StopTime> stopTimes = new ArrayList<>(tripUpdate.getStopTimeUpdateCount());
        for (int index = 0; index < tripUpdate.getStopTimeUpdateCount(); ++index) {
            StopTimeUpdate stopTimeUpdate = tripUpdate.getStopTimeUpdate(index);
            Stop stop = stops.get(index);
            
            // Create stop time
            StopTime stopTime = new StopTime();
            stopTime.setTrip(trip);
            stopTime.setStop(stop);
            // Set arrival time
            if (stopTimeUpdate.hasArrival() && stopTimeUpdate.getArrival().hasTime()) {
                long arrivalTime = stopTimeUpdate.getArrival().getTime() - midnightSecondsSinceEpoch;
                if (arrivalTime < 0 || arrivalTime > MAX_ARRIVAL_DEPARTURE_TIME) {
                    LOG.warn("ADDED trip has invalid arrival time (compared to start date in "
                            + "TripDescriptor), skipping.");
                    return false;
                }
                stopTime.setArrivalTime((int) arrivalTime);
            }
            // Set departure time
            if (stopTimeUpdate.hasDeparture() && stopTimeUpdate.getDeparture().hasTime()) {
                long departureTime = stopTimeUpdate.getDeparture().getTime() - midnightSecondsSinceEpoch;
                if (departureTime < 0 || departureTime > MAX_ARRIVAL_DEPARTURE_TIME) {
                    LOG.warn("ADDED trip has invalid departure time (compared to start date in "
                            + "TripDescriptor), skipping.");
                    return false;
                }
                stopTime.setDepartureTime((int) departureTime);
            }
            stopTime.setTimepoint(1); // Exact time
            stopTime.setStopSequence(stopTimeUpdate.getStopSequence());
            // Set pickup type
            if (index == tripUpdate.getStopTimeUpdateCount() - 1) {
                stopTime.setPickupType(1); // No pickup available
            } else {
                stopTime.setPickupType(0); // Regularly scheduled pickup
            }
            // Set drop off type
            if (index == 0) {
                stopTime.setDropOffType(1); // No drop off available
            } else {
                stopTime.setDropOffType(0); // Regularly scheduled drop off
            }
            
            // Add stop time to list
            stopTimes.add(stopTime);
        }
        
        // TODO: filter/interpolate stop times like in GTFSPatternHopFactory?
        
        // Create StopPattern
        StopPattern stopPattern = new StopPattern(stopTimes);

        // Create TripPattern
        // TODO: make a cache for newly created trip patterns?
        TripPattern pattern = new TripPattern(trip.getRoute(), stopPattern);
        
        // Create vertices and edges for new TripPattern
        // TODO: purge these vertices and edges once in a while
        pattern.makePatternVerticesAndEdges(graph, graphIndex.stopVertexForStop);
        
        // Create new trip times
        TripTimes newTripTimes = new TripTimes(trip, stopTimes, graph.deduplicator);
        
        // Add new trip times to the buffer
        boolean success = buffer.update(pattern, newTripTimes, serviceDate); 
        return success;
    }

    protected boolean handleUnscheduledTrip(TripUpdate tripUpdate, String feedId, ServiceDate serviceDate) {
        // TODO: Handle unscheduled trip
        LOG.warn("Unscheduled trips are currently unsupported. Skipping TripUpdate.");
        return false;
    }

    protected boolean handleReplacementTrip(TripUpdate tripUpdate, String feedId, ServiceDate serviceDate) {
        // TODO: Handle replacement trip
        LOG.warn("Replacement trips are currently unsupported. Skipping TripUpdate.");
        return false;
    }

    protected boolean handleCanceledTrip(TripUpdate tripUpdate, String agencyId,
                                         ServiceDate serviceDate) {
        TripDescriptor tripDescriptor = tripUpdate.getTrip();
        String tripId = tripDescriptor.getTripId(); // This does not include Agency ID, trips are feed-unique.
        TripPattern pattern = getPatternForTripId(tripId);

        if (pattern == null) {
            LOG.warn("No pattern found for tripId {}, skipping TripUpdate.", tripId);
            return false;
        }

        // Apply update on the *scheduled* time table and set the updated trip times in the buffer
        TripTimes updatedTripTimes = pattern.scheduledTimetable.createUpdatedTripTimes(tripUpdate,
                timeZone, serviceDate);
        boolean success = buffer.update(pattern, updatedTripTimes, serviceDate); 
        return success;
    }

    protected boolean purgeExpiredData() {
        ServiceDate today = new ServiceDate();
        ServiceDate previously = today.previous().previous(); // Just to be safe...

        if(lastPurgeDate != null && lastPurgeDate.compareTo(previously) > 0) {
            return false;
        }

        LOG.debug("purging expired realtime data");
        // TODO: purge expired realtime data

        lastPurgeDate = previously;

        return buffer.purgeExpiredData(previously);
    }

    private TripPattern getPatternForTripId(String tripIdWithoutAgency) {
        Trip trip = getTripForTripId(tripIdWithoutAgency);
        TripPattern pattern = graphIndex.patternForTrip.get(trip);
        return pattern;
    }

    /**
     * Retrieve trip given a trip id without an agency
     * 
     * @param tripId trip id without the agency
     * @return trip or null if trip doesn't exist
     */
    private Trip getTripForTripId(String tripId) {
        /* Lazy-initialize a separate index that ignores agency IDs.
         * Stopgap measure assuming no cross-feed ID conflicts, until we get GTFS loader replaced. */
        if (graphIndex.tripForIdWithoutAgency == null) {
            Map<String, Trip> map = Maps.newHashMap();
            for (Trip trip : graphIndex.tripForId.values()) {
                Trip previousValue = map.put(trip.getId().getId(), trip);
                if (previousValue != null) {
                    LOG.warn("Duplicate trip id detected when agency is ignored for realtime updates: {}", tripId);
                }
            }
            graphIndex.tripForIdWithoutAgency = map;
        }
        Trip trip = graphIndex.tripForIdWithoutAgency.get(tripId);
        return trip;
    }

    /**
     * Retrieve stop given a stop id without an agency
     * 
     * @param stopId trip id without the agency
     * @return stop or null if stop doesn't exist
     */
    private Stop getStopForStopId(String stopId) {
        /* Lazy-initialize a separate index that ignores agency IDs.
         * Stopgap measure assuming no cross-feed ID conflicts, until we get GTFS loader replaced. */
        if (graphIndex.stopForIdWithoutAgency == null) {
            Map<String, Stop> map = Maps.newHashMap();
            for (Stop stop : graphIndex.stopForId.values()) {
                Stop previousValue = map.put(stop.getId().getId(), stop);
                if (previousValue != null) {
                    LOG.warn("Duplicate stop id detected when agency is ignored for realtime updates: {}", stopId);
                }
            }
            graphIndex.stopForIdWithoutAgency = map;
        }
        Stop stop = graphIndex.stopForIdWithoutAgency.get(stopId);
        return stop;
    }
    
}
