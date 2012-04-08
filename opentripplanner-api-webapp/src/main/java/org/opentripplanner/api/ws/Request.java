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

package org.opentripplanner.api.ws;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import org.opentripplanner.common.model.NamedPlace;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A trip planning request. Some parameters may not be honored by the trip planner for some or all
 * itineraries. For example, maxWalkDistance may be relaxed if the alternative is to not provide a
 * route.
 */
public class Request {

    private static final Logger LOG = LoggerFactory.getLogger(Request.class);
    
    /** The complete list of incoming query parameters. */
    private final HashMap<String, String> parameters = new HashMap<String, String>();
    /** The router ID -- internal ID to switch between router implementation (or graphs) */
    private String routerId;
    /** The start location -- either a Vertex name or latitude, longitude in degrees */
    // TODO change this to Doubles and a Vertex
    private String from;
    /** The start location's user-visible name */
    private String fromName;
    /** The end location (see the from field for format). */
    private String to;
    /** The end location's user-visible name */
    private String toName;
    /** An unordered list of intermediate locations to be visited (see the from field for format). */
    private List<NamedPlace> intermediatePlaces;
    /** The maximum distance (in meters) the user is willing to walk. Defaults to 1/2 mile. */
    private Double maxWalkDistance = Double.MAX_VALUE;
    /** The set of TraverseModes that a user is willing to use. Defaults to WALK | TRANSIT. */
    private TraverseModeSet modes; // defaults in constructor
    /** The set of characteristics that the user wants to optimize for -- defaults to QUICK, or optimize for transit time. */
    private OptimizeType optimize = OptimizeType.QUICK;
    /** The date/time that the trip should depart (or arrive, for requests where arriveBy is true) */
    private Date dateTime = new Date();
    /** Whether the trip should depart at dateTime (false, the default), or arrive at dateTime. */
    private boolean arriveBy = false;
    /** Whether the trip must be wheelchair accessible. */
    private boolean wheelchair = false;
    /** The maximum number of possible itineraries to return. */
    private Integer numItineraries = 3;
    /** The maximum slope of streets for wheelchair trips. */
    private double maxSlope = -1;
    /** Whether the planner should return intermediate stops lists for transit legs. */
    private boolean showIntermediateStops = false;
    /** List of preferred routes. */
    private String[] preferredRoutes;
    /** List of unpreferred routes. */
    private String[] unpreferredRoutes;
    private Integer minTransferTime;
    private String[] bannedRoutes;
    private Integer transferPenalty;
    private double walkSpeed;
    private double triangleSafetyFactor;
    private double triangleSlopeFactor;
    private double triangleTimeFactor;
    private Integer maxTransfers;
    private boolean intermediatePlacesOrdered;
    
    public Request() {
        modes = new TraverseModeSet("TRANSIT,WALK");
        setIntermediatePlaces(new ArrayList<String>());
    }

    public HashMap<String, String> getParameters() {
        return parameters;
    }

    /**
     * @return router ID
     */
    public String getRouterId() {
        return routerId;
    }

    /**
     * @param routerId
     *            the router ID, used to switch between router instances.
     */
    public void setRouterId(String routerId) {
        this.routerId = routerId;
    }

    /**
     * @return the from
     */
    public String getFrom() {
        return from;
    }

    /**
     * @param from
     *            the from to set
     */
    // TODO factor out splitting code which appears in 3 places
    public void setFrom(String from) {
        if (from.contains("::")) {
            String[] parts = from.split("::");
            this.fromName = parts[0];
            this.from = parts[1];
        } else {
            this.from = from;
        }
    }

    /**
     * @return the to
     */
    public String getTo() {
        return to;
    }

    /**
     * @param to
     *            the to to set
     */
    public void setTo(String to) {
        if (to.contains("::")) {
            String[] parts = to.split("::");
            this.toName = parts[0];
            this.to = parts[1];
        } else {
            this.to = to;
        }
    }

    /**
     * @return the (soft) maximum walk distance
     */
    public Double getMaxWalkDistance() {
        return maxWalkDistance;
    }

    /**
     * @param walk
     *            the (soft) maximum walk distance to set
     */
    public void setMaxWalkDistance(Double walk) {
        this.maxWalkDistance = walk;
    }

    /**
     * @return the modes
     */
    public TraverseModeSet getModes() {
        return modes;
    }

    /** */
    // TODO move this into TraverseModeSet
    public String getModesAsStr() {
        String retVal = null;
        for (TraverseMode m : modes.getModes()) {
            if (retVal == null)
                retVal = "";
            else
                retVal += ", ";
            retVal += m;
        }

        return retVal;
    }

    /**
     * @param modes
     *            the modes to set
     */
    public void addMode(TraverseMode mode) {
        modes.setMode(mode, true);
    }

    /** */
    public void addMode(List<TraverseMode> mList) {
        for (TraverseMode m : mList) {
            addMode(m);
        }
    }

    /**
     * @return the optimization type
     */
    public OptimizeType getOptimize() {
        return optimize;
    }

    /**
     * @param optimize
     *            the optimize to set
     */
    public void setOptimize(OptimizeType opt) {
        optimize = opt;
    }

    /**
     * @return the dateTime
     */
    public Date getDateTime() {
        return dateTime;
    }

    /**
     * @param dateTime
     *            the dateTime to set
     */
    public void setDateTime(Date dateTime) {
        this.dateTime = new Date(dateTime.getTime());
    }

    /**
     * @param dateTime
     *            the dateTime to set
     */
    public void setDateTime(String date, String time) {
        dateTime = DateUtils.toDate(date, time);
        LOG.debug("JVM default timezone is {}", TimeZone.getDefault());
        LOG.debug("Request datetime parsed as {}", dateTime);
    }

    /**
     * @return the departAfter
     */
    public boolean isArriveBy() {
        return arriveBy;
    }

    public void setArriveBy(boolean arriveBy) {
        this.arriveBy = arriveBy;
    }

    /**
     * @return the numItineraries
     */
    public Integer getNumItineraries() {
        return numItineraries;
    }

    /**
     * @param numItineraries
     *            the numItineraries to set
     */
    public void setNumItineraries(Integer numItineraries) {
        if (numItineraries < 1 || numItineraries > 10)
            numItineraries = 3;
        this.numItineraries = numItineraries;
    }

    /** */
    public String toHtmlString() {
        // What?
        return toString("<br/>");
    }

    /** */
    public String toString() {
        // What?
        return toString(" ");
    }

    /** */
    public String toString(String sep) {
        return getFrom() + sep + getTo() + sep + getMaxWalkDistance() + sep + getDateTime() + sep
                + isArriveBy() + sep + getOptimize() + sep + getModesAsStr() + sep
                + getNumItineraries();
    }
    
    public TraverseModeSet getModeSet() {
        return modes;
    }

    public void removeMode(TraverseMode mode) {
        modes.setMode(mode, false);
    }

    public void setModes(TraverseModeSet modes) {
        this.modes = modes;

    }

    /** Set whether the trip must be wheelchair accessible */
    public void setWheelchair(boolean wheelchair) {
        this.wheelchair = wheelchair;
    }

    /** return whether the trip must be wheelchair accessible */
    public boolean getWheelchair() {
        return wheelchair;
    }

    /**
     * @param intermediatePlaces the intermediatePlaces to set
     */
    public void setIntermediatePlaces(List<String> intermediates) {
        this.intermediatePlaces = new ArrayList<NamedPlace>(intermediates.size());
        for (String place : intermediates) {
            String name = place;
            if (place.contains("::")) {
                String[] parts = place.split("::");
                name = parts[0];
                place = parts[1];
            }
            NamedPlace intermediate = new NamedPlace(name, place);
            intermediatePlaces.add(intermediate);
        }
    }

    /**
     * @return the intermediatePlaces
     */
    public List<NamedPlace> getIntermediatePlaces() {
        return intermediatePlaces;
    }

    /**
     * @return the maximum street slope for wheelchair trips
     */
    public double getMaxSlope() {
        return maxSlope;
    }
    
    /**
     * @param maxSlope the maximum street slope for wheelchair trpis
     */
    public void setMaxSlope(double maxSlope) {
        this.maxSlope = maxSlope;
    }
    
    /** 
     * @param showIntermediateStops
     *          whether the planner should return intermediate stop lists for transit legs 
     */
    public void setShowIntermediateStops(boolean showIntermediateStops) {
        this.showIntermediateStops = showIntermediateStops;
    }

    /** 
     * @return whether the planner should return intermediate stop lists for transit legs 
     */
    public boolean getShowIntermediateStops() {
        return showIntermediateStops;
    }

    public void setMinTransferTime(Integer minTransferTime) {
        this.minTransferTime = minTransferTime;
    }
    
    public Integer getMinTransferTime() {
        return minTransferTime;
    }
    
    public void setPreferredRoutes(String[] preferredRoutes) {
        this.preferredRoutes = preferredRoutes.clone();
    }

    public String[] getPreferredRoutes() {
        return preferredRoutes;
    }

    public void setUnpreferredRoutes(String[] unpreferredRoutes) {
        this.unpreferredRoutes = unpreferredRoutes.clone();
    }

    public String[] getUnpreferredRoutes() {
        return unpreferredRoutes;
    }

    public void setBannedRoutes(String[] bannedRoutes) {
        this.bannedRoutes = bannedRoutes.clone();
    }

    public String[] getBannedRoutes() {
        return bannedRoutes;
    }

    public void setTransferPenalty(Integer transferPenalty) {
        this.transferPenalty = transferPenalty;
    }

    public Integer getTransferPenalty() {
        return transferPenalty;
    }

    public void setWalkSpeed(double walkSpeed) {
        this.walkSpeed = walkSpeed;
    }

    public double getWalkSpeed() {
        return walkSpeed;
    }

    public void setTriangleSafetyFactor(double triangleSafetyFactor) {
        this.triangleSafetyFactor = triangleSafetyFactor;
    }

    public double getTriangleSafetyFactor() {
        return triangleSafetyFactor;
    }

    public void setTriangleSlopeFactor(double triangleSlopeFactor) {
        this.triangleSlopeFactor = triangleSlopeFactor;
    }

    public double getTriangleSlopeFactor() {
        return triangleSlopeFactor;
    }

    public void setTriangleTimeFactor(double triangleTimeFactor) {
        this.triangleTimeFactor = triangleTimeFactor;
    }

    public double getTriangleTimeFactor() {
        return triangleTimeFactor;
    }

    public void setMaxTransfers(Integer maxTransfers) {
        this.maxTransfers = maxTransfers;
    }

    public Integer getMaxTransfers() {
        return maxTransfers;
    }

    public void setIntermediatePlacesOrdered(boolean intermediatePlacesOrdered) {
        this.intermediatePlacesOrdered = intermediatePlacesOrdered;
    }

    public boolean isIntermediatePlacesOrdered() {
        return intermediatePlacesOrdered;
    }

    public String getFromName() {
        return fromName;
    }

    public String getToName() {
        return toName;
    }

    public NamedPlace getFromPlace() {
        return new NamedPlace(fromName, from);
    }

    public NamedPlace getToPlace() {
        return new NamedPlace(toName, to);
    }
    
    /**
     * Build a request from the parameters in a concrete subclass of SearchResource.
     */
//    public Request(SearchResource) {
//        
//    }
    
}