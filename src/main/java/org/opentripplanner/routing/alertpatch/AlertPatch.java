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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.transit_index.RouteSegment;
import org.opentripplanner.routing.transit_index.RouteVariant;
import org.opentripplanner.routing.transit_index.adapters.AgencyAndIdAdapter;

/**
 * This adds a note to all boardings of a given route or stop (optionally, in a given direction)
 *
 * @author novalis
 *
 */
@XmlRootElement(name = "AlertPatch")
public class AlertPatch implements Serializable {
    private static final long serialVersionUID = 20140319L;

    private String id;

    private Alert alert;

    private List<TimePeriod> timePeriods = new ArrayList<TimePeriod>();

    private String agency;

    private AgencyAndId route;

    private AgencyAndId trip;

    private AgencyAndId stop;

    private String direction;

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
/*
        if (route != null || trip != null || agency != null) {
            List<RouteVariant> variants;

            if(trip != null) {
                variants = new LinkedList<RouteVariant>();
                RouteVariant tripVariant = index.getVariantForTrip(trip);
                if(tripVariant != null) {
                    variants.add(index.getVariantForTrip(trip));
                }
            } else if (route != null) {
               variants = index.getVariantsForRoute(route);
            } else {
               variants = index.getVariantsForAgency(agency);
            }

            for (RouteVariant variant : variants) {
                if (direction != null && ! direction.equals(variant.getDirection())) {
                    continue;
                }
                for (RouteSegment segment : variant.getSegments()) {
                    if (stop == null || segment.stop.equals(stop)) {
                        graph.addAlertPatch(segment.board, this);
                        graph.addAlertPatch(segment.alight, this);
                    }
                }
            }
        } else if (stop != null) {
            Edge edge = index.getPreBoardEdge(stop);
            graph.addAlertPatch(edge, this);

            edge = index.getPreAlightEdge(stop);
            graph.addAlertPatch(edge, this);
        }
*/
    }

    public void remove(Graph graph) {
/*
        if (route != null || trip != null || agency != null) {
            List<RouteVariant> variants;

            if(trip != null) {
                variants = new LinkedList<RouteVariant>();
                RouteVariant tripVariant = index.getVariantForTrip(trip);
                if(tripVariant != null) {
                    variants.add(index.getVariantForTrip(trip));
                }
            } else if (route != null) {
               variants = index.getVariantsForRoute(route);
            } else {
               variants = index.getVariantsForAgency(agency);
            }

            for (RouteVariant variant : variants) {
                if (direction != null && !direction.equals(variant.getDirection())) {
                    continue;
                }
                for (RouteSegment segment : variant.getSegments()) {
                    if (stop == null || segment.stop.equals(stop)) {
                        graph.removeAlertPatch(segment.board, this);
                        graph.removeAlertPatch(segment.alight, this);
                    }
                }
            }
        } else if (stop != null) {
            Edge edge = index.getPreBoardEdge(stop);
            graph.removeAlertPatch(edge, this);

            edge = index.getPreAlightEdge(stop);
            graph.removeAlertPatch(edge, this);
        }
*/
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

    @XmlElement
    public String getDirection() {
        return direction;
    }

    public void setStop(AgencyAndId stop) {
        this.stop = stop;
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
        return true;
    }

    public int hashCode() {
        return ((direction == null ? 0 : direction.hashCode()) +
                (agency == null ? 0 : agency.hashCode()) +
                (trip == null ? 0 : trip.hashCode()) +
                (stop == null ? 0 : stop.hashCode()) +
                (route == null ? 0 : route.hashCode()) +
                (alert == null ? 0 : alert.hashCode()));
    }
}
