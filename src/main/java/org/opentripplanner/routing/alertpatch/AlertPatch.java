package org.opentripplanner.routing.alertpatch;

import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * This adds a note to all boardings of a given route or stop (optionally, in a given direction)
 */
public class AlertPatch implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(AlertPatch.class);

    private static final long serialVersionUID = 20140319L;

    private String id;

    private Alert alert;

    private List<TimePeriod> timePeriods = new ArrayList<>();

    private FeedScopedId agency;

    private FeedScopedId operatorId;

    private FeedScopedId route;

    private FeedScopedId trip;

    private FeedScopedId stop;

    /**
     * The headsign of the alert
     */
    private String direction;

    /**
     * The id of the feed this patch is intended for.
     */
    private String feedId;

    /**
     * Direction id of the GTFS trips this alert concerns, set to -1 if no direction.
     */
    private int directionId = -1;

    /**
     * Used to limit when Alert is applicable
     */
    private Set<StopCondition> stopConditions = new HashSet<>();

    /**
     * The provider's internal ID for this alert
     */
    private String situationNumber;

    public Alert getAlert() {
        return alert;
    }

    public boolean displayDuring(State state) {
        return displayDuring(state.getStartTimeSeconds(), state.getTimeSeconds());
    }

    public boolean displayDuring(long startTimeSeconds, long endTimeSeconds) {
        for (TimePeriod timePeriod : timePeriods) {
            if (endTimeSeconds >= timePeriod.startTime) {
                if (startTimeSeconds < timePeriod.endTime) {
                    return true;
                }
            }
        }
        return false;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void apply(Graph graph) {
        Agency agency = this.agency != null ? graph.index.getAgencyForId(this.agency) : null;
        Route route = this.route != null ? graph.index.getRouteForId(this.route) : null;
        Stop stop = this.stop != null ? graph.index.getStopForId(this.stop) : null;
        Trip trip = this.trip != null ? graph.index.getTripForId().get(this.trip) : null;

        if (route != null || trip != null || agency != null) {
            Collection<TripPattern> tripPatterns = null;

            if (trip != null) {
                tripPatterns = new LinkedList<>();
                TripPattern tripPattern = graph.index.getPatternForTrip().get(trip);
                if (tripPattern != null) {
                    tripPatterns.add(tripPattern);
                }
            } else if (route != null) {
               tripPatterns = graph.index.getPatternsForRoute().get(route);
            } else {
                // Find patterns for the feed.
                tripPatterns = graph.index.getPatternsForFeedId().get(feedId);
            }

            if (tripPatterns != null) {
                for (TripPattern tripPattern : tripPatterns) {
                    if (direction != null && !direction.equals(tripPattern.getDirection())) {
                        continue;
                    }
                    if (directionId != -1 && directionId == tripPattern.directionId) {
                        continue;
                    }
                    for (int i = 0; i < tripPattern.stopPattern.stops.length; i++) {
                        if (stop == null || stop.equals(tripPattern.stopPattern.stops[i])) {
                            LOG.warn("Cannot add Alert patch to Board/Alight edges - transit edges do not exist anymore under Raptor.");
                        }
                    }
                }
            }
        } else if (stop != null) {
            LOG.warn("Cannot add alert to Stop - PreBoard and PreAlight edges no longer exist.");
        }
    }

    public void remove(Graph graph) {
        Agency agency = this.agency != null ? graph.index.getAgencyForId(this.agency) : null;
        Route route = this.route != null ? graph.index.getRouteForId(this.route) : null;
        Stop stop = this.stop != null ? graph.index.getStopForId(this.stop) : null;
        Trip trip = this.trip != null ? graph.index.getTripForId().get(this.trip) : null;

        if (route != null || trip != null || agency != null) {
            Collection<TripPattern> tripPatterns = null;

            if(trip != null) {
                tripPatterns = new LinkedList<TripPattern>();
                TripPattern tripPattern = graph.index.getPatternForTrip().get(trip);
                if(tripPattern != null) {
                    tripPatterns.add(tripPattern);
                }
            } else if (route != null) {
               tripPatterns = graph.index.getPatternsForRoute().get(route);
            } else {
                // Find patterns for the feed.
                tripPatterns = graph.index.getPatternsForFeedId().get(feedId);
            }

            if (tripPatterns != null) {
                for (TripPattern tripPattern : tripPatterns) {
                    if (direction != null && !direction.equals(tripPattern.getDirection())) {
                        continue;
                    }
                    if (directionId != -1 && directionId != tripPattern.directionId) {
                        continue;
                    }
                    for (int i = 0; i < tripPattern.stopPattern.stops.length; i++) {
                        if (stop == null || stop.equals(tripPattern.stopPattern.stops[i])) {
                            LOG.warn("Cannot remove Alert patch from Board/Alight edges - transit edges do not exist anymore under Raptor.");
                        }
                    }
                }
            }
        } else if (stop != null) {
            LOG.warn("Cannot remove alert from Stop - PreBoard and PreAlight edges no longer exist.");
        }
    }

    public void setAlert(Alert alert) {
        this.alert = alert;
    }

    private void writeObject(ObjectOutputStream os) throws IOException {
        if (timePeriods instanceof ArrayList<?>) {
            ((ArrayList<TimePeriod>) timePeriods).trimToSize();
        }
        os.defaultWriteObject();
    }

    public void setTimePeriods(List<TimePeriod> periods) {
        timePeriods = periods;
    }

    public FeedScopedId getAgency() {
        return agency;
    }

    public FeedScopedId getRoute() {
        return route;
    }

    public FeedScopedId getTrip() {
        return trip;
    }

    public FeedScopedId getStop() {
        return stop;
    }

    public void setAgency(FeedScopedId agency) {
        this.agency = agency;
    }

    public void setOperatorId(FeedScopedId operatorId) {
        this.operatorId = operatorId;
    }

    public void setRoute(FeedScopedId route) {
        this.route = route;
    }

    public void setTrip(FeedScopedId trip) {
        this.trip = trip;
    }

    public void setDirection(String direction) {
        if (direction != null && direction.equals("")) {
            direction = null;
        }
        this.direction = direction;
    }

    public void setDirectionId(int direction) {
        this.directionId = direction;
    }

    public String getDirection() {
        return direction;
    }

    public int getDirectionId() {
        return directionId;
    }

    public void setStop(FeedScopedId stop) {
        this.stop = stop;
    }

    public String getFeedId() {
        return feedId;
    }

    public void setFeedId(String feedId) {
        this.feedId = feedId;
    }

    public boolean hasTrip() {
        return trip != null;
    }

    public Set<StopCondition> getStopConditions() {
        return stopConditions;
    }

    public void setSituationNumber(String situationNumber) {
        this.situationNumber = situationNumber;
    }

    public String getSituationNumber() {
        return situationNumber;
    }

    // TODO - Alerts should not be added to the internal model if they are the same; This check should be done
    //      - when importing the Alerts into the system. Then the Alert can use the System identity for
    //      - hachCode and equals.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlertPatch that = (AlertPatch) o;
        return Objects.equals(feedId, that.feedId) &&
                Objects.equals(agency, that.agency) &&
                Objects.equals(id, that.id) &&
                Objects.equals(route, that.route) &&
                Objects.equals(trip, that.trip) &&
                Objects.equals(stop, that.stop) &&
                directionId == that.directionId &&
                Objects.equals(direction, that.direction) &&
                Objects.equals(timePeriods, that.timePeriods) &&
                Objects.equals(alert, that.alert);
    }

    @Override
    public int hashCode() {
        return Objects.hash(directionId, feedId, agency, id, route, trip, stop, direction, timePeriods, alert);
    }
}
