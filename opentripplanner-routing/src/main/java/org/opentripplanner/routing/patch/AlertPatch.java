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

package org.opentripplanner.routing.patch;

import java.util.List;
import java.util.LinkedList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.TransitIndexService;
import org.opentripplanner.routing.transit_index.RouteSegment;
import org.opentripplanner.routing.transit_index.RouteVariant;

/**
 * This adds a note to all boardings of a given route or stop (optionally, in a given direction)
 * 
 * @author novalis
 * 
 */
@XmlRootElement(name = "AlertPatch")
public class AlertPatch extends AbstractPatch {
    private static final long serialVersionUID = -7947169269916558755L;

    private String agency;

    private AgencyAndId route;

    private AgencyAndId trip;

    private AgencyAndId stop;

    private String direction;

    private boolean cancelled = false;

    public AlertPatch() {
    }

    @Override
    public void apply(Graph graph) {
            TransitIndexService index = graph.getService(TransitIndexService.class);

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
                        if (segment.board != null) {
                            segment.board.addPatch(this);
                        }
                        if(segment.alight != null) {
                            segment.alight.addPatch(this);
                        }
                    }
                }
            }
        } else if (stop != null) {
            Edge edge = index.getPreboardEdge(stop);
            if(edge != null)
                edge.addPatch(this);

            edge = index.getPrealightEdge(stop);
            if(edge != null)
                edge.addPatch(this);
        }
    }

    @Override
    public void remove(Graph graph) {
            TransitIndexService index = graph.getService(TransitIndexService.class);

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
                        if (segment.board != null) {
                            segment.board.removePatch(this);
                        }
                        if(segment.alight != null) {
                            segment.alight.removePatch(this);
                        }
                    }
                }
            }
        } else if (stop != null) {
            Edge edge = index.getPreboardEdge(stop);
            if(edge != null)
                edge.removePatch(this);

            edge = index.getPrealightEdge(stop);
            if(edge != null)
                edge.removePatch(this);
        }
    }

    @Override
    public boolean filterTraverseResult(StateEditor result, boolean displayOnly) {
        result.addAlert(alert);
        return displayOnly || !isCancelled();
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

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public boolean isCancelled() {
        return cancelled;
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
        if(cancelled != other.cancelled) {
            return false;
        }
        return other.alert.equals(alert) && super.equals(other);
    }

    public int hashCode() {
        return ((direction == null ? 0 : direction.hashCode()) +
                (agency == null ? 0 : agency.hashCode()) +
                (trip == null ? 0 : trip.hashCode()) +
                (stop == null ? 0 : stop.hashCode()) +
                (route == null ? 0 : route.hashCode()) +
                (alert == null ? 0 : alert.hashCode())) *
                (cancelled ? 5 : 7);
    }
}
