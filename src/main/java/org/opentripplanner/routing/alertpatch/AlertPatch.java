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

package org.opentripplanner.routing.alertpatch;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.*;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.google.common.collect.Multimap;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.api.adapters.AgencyAndIdAdapter;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.PreAlightEdge;
import org.opentripplanner.routing.edgetype.PreBoardEdge;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This adds a note to all boardings of a given route or stop (optionally, in a given direction)
 *
 * @author novalis
 *
 */
@XmlRootElement(name = "AlertPatch")
public class AlertPatch implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(AlertPatch.class);

    private static final long serialVersionUID = 20140319L;

    private String id;

    private Alert alert;

    private List<TimePeriod> timePeriods = new ArrayList<TimePeriod>();

    private String agency;

    private AgencyAndId route;

    private AgencyAndId trip;

    private AgencyAndId stop;

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

    @XmlElement
    public Alert getAlert() {
        return alert;
    }

    public boolean displayDuring(State state) {
        for (TimePeriod timePeriod : timePeriods) {
            if (state.getTimeSeconds() >= timePeriod.startTime) {
                if (state.getStartTimeSeconds() < timePeriod.endTime) {
                    return true;
                }
            }
        }
        return false;
    }

    @XmlElement
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void apply(Graph graph) {
        Agency agency = null;
        if (feedId != null) {
            Map<String, Agency> agencies = graph.index.agenciesForFeedId.get(feedId);
            agency = this.agency != null ? agencies.get(this.agency) : null;
        }
        Route route = this.route != null ? graph.index.routeForId.get(this.route) : null;
        Stop stop = this.stop != null ? graph.index.stopForId.get(this.stop) : null;
        Trip trip = this.trip != null ? graph.index.tripForId.get(this.trip) : null;

        if (route != null || trip != null || agency != null) {
            Collection<TripPattern> tripPatterns = null;

            if (trip != null) {
                tripPatterns = new LinkedList<>();
                TripPattern tripPattern = graph.index.patternForTrip.get(trip);
                if (tripPattern != null) {
                    tripPatterns.add(tripPattern);
                }
            } else if (route != null) {
               tripPatterns = graph.index.patternsForRoute.get(route);
            } else {
                // Find patterns for the feed.
                tripPatterns = graph.index.patternsForFeedId.get(feedId);
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
                            graph.addAlertPatch(tripPattern.boardEdges[i], this);
                            graph.addAlertPatch(tripPattern.alightEdges[i], this);
                        }
                    }
                }
            }
        } else if (stop != null) {
            TransitStop transitStop = graph.index.stopVertexForStop.get(stop);

            for (Edge edge : transitStop.getOutgoing()) {
                if (edge instanceof PreBoardEdge) {
                    graph.addAlertPatch(edge, this);
                    break;
                }
            }

            for (Edge edge : transitStop.getIncoming()) {
                if (edge instanceof PreAlightEdge) {
                    graph.addAlertPatch(edge, this);
                    break;
                }
            }
        }
    }

    public void remove(Graph graph) {
        Agency agency = null;
        if (feedId != null) {
            Map<String, Agency> agencies = graph.index.agenciesForFeedId.get(feedId);
            agency = this.agency != null ? agencies.get(this.agency) : null;
        }
        Route route = this.route != null ? graph.index.routeForId.get(this.route) : null;
        Stop stop = this.stop != null ? graph.index.stopForId.get(this.stop) : null;
        Trip trip = this.trip != null ? graph.index.tripForId.get(this.trip) : null;

        if (route != null || trip != null || agency != null) {
            Collection<TripPattern> tripPatterns = null;

            if(trip != null) {
                tripPatterns = new LinkedList<TripPattern>();
                TripPattern tripPattern = graph.index.patternForTrip.get(trip);
                if(tripPattern != null) {
                    tripPatterns.add(tripPattern);
                }
            } else if (route != null) {
               tripPatterns = graph.index.patternsForRoute.get(route);
            } else {
                // Find patterns for the feed.
                tripPatterns = graph.index.patternsForFeedId.get(feedId);
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
                            graph.removeAlertPatch(tripPattern.boardEdges[i], this);
                            graph.removeAlertPatch(tripPattern.alightEdges[i], this);
                        }
                    }
                }
            }
        } else if (stop != null) {
            TransitStop transitStop = graph.index.stopVertexForStop.get(stop);

            for (Edge edge : transitStop.getOutgoing()) {
                if (edge instanceof PreBoardEdge) {
                    graph.removeAlertPatch(edge, this);
                    break;
                }
            }

            for (Edge edge : transitStop.getIncoming()) {
                if (edge instanceof PreAlightEdge) {
                    graph.removeAlertPatch(edge, this);
                    break;
                }
            }
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

    public String getAgency() {
        return agency;
    }

    @XmlJavaTypeAdapter(AgencyAndIdAdapter.class)
    public AgencyAndId getRoute() {
        return route;
    }

    @XmlJavaTypeAdapter(AgencyAndIdAdapter.class)
    public AgencyAndId getTrip() {
        return trip;
    }

    @XmlJavaTypeAdapter(AgencyAndIdAdapter.class)
    public AgencyAndId getStop() {
        return stop;
    }

    public void setAgencyId(String agency) {
        this.agency = agency;
    }

    public void setRoute(AgencyAndId route) {
        this.route = route;
    }

    public void setTrip(AgencyAndId trip) {
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

    @XmlElement
    public String getDirection() {
        return direction;
    }

    @XmlElement
    public int getDirectionId() {
        return directionId;
    }

    public void setStop(AgencyAndId stop) {
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

    public boolean equals(Object o) {
        if (!(o instanceof AlertPatch)) {
            return false;
        }
        AlertPatch other = (AlertPatch) o;
        if (direction == null) {
            if (other.direction != null) {
                return false;
            }
        } else {
            if (!direction.equals(other.direction)) {
                return false;
            }
        }
        if (directionId != other.directionId) {
            return false;
        }
        if (agency == null) {
            if (other.agency != null) {
                return false;
            }
        } else {
            if (!agency.equals(other.agency)) {
                return false;
            }
        }
        if (trip == null) {
            if (other.trip != null) {
                return false;
            }
        } else {
            if (!trip.equals(other.trip)) {
                return false;
            }
        }
        if (stop == null) {
            if (other.stop != null) {
                return false;
            }
        } else {
            if (!stop.equals(other.stop)) {
                return false;
            }
        }
        if (route == null) {
            if (other.route != null) {
                return false;
            }
        } else {
            if (!route.equals(other.route)) {
                return false;
            }
        }
        if (alert == null) {
            if (other.alert != null) {
                return false;
            }
        } else {
            if (!alert.equals(other.alert)) {
                return false;
            }
        }
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else {
            if (!id.equals(other.id)) {
                return false;
            }
        }
        if (timePeriods == null) {
            if (other.timePeriods != null) {
                return false;
            }
        } else {
            if (!timePeriods.equals(other.timePeriods)) {
                return false;
            }
        }
        if (feedId == null) {
            if (other.feedId != null) {
                return false;
            }
        } else {
            if (!feedId.equals(other.feedId)) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        return ((direction == null ? 0 : direction.hashCode()) +
                directionId +
                (agency == null ? 0 : agency.hashCode()) +
                (trip == null ? 0 : trip.hashCode()) +
                (stop == null ? 0 : stop.hashCode()) +
                (route == null ? 0 : route.hashCode()) +
                (alert == null ? 0 : alert.hashCode()) +
                (feedId == null ? 0 : feedId.hashCode()));
    }
}
