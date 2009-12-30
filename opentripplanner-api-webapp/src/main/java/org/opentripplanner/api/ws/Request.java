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

import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.util.DateUtils;

/**
 * A trip planning request. Some parameters may not be honored by the trip planner for some or all
 * itineraries. For example, maxWalkDistance may be relaxed if the alternative is to not provide a
 * route.
 */
public class Request implements RequestInf {

    /**
     * The start location -- either a Vertex name or latitude, longitude in degrees
     */
    private String from;
    /**
     * The end location (see the from field for format).
     */
    private String to;
    /**
     * An unordered list of intermediate locations to be visited (see the from field for format).
     * Presently unused.
     */
    private List<String> intermediatePlaces;
    /**
     * The maximum distance (in meters) the user is willing to walk. Defaults to 1/2 mile.
     */
    private Double maxWalkDistance = 804.0; // half a mile in meters
    /**
     * The set of TraverseModes that a user is willing to use. Defaults to WALK | TRANSIT.
     */
    private TraverseModeSet modes; // defaults in constructor
    /**
     * The set of characteristics that the user wants to optimize for -- defaults to QUICK, or
     * optimize for transit time.
     */
    private OptimizeType optimize = OptimizeType.QUICK;
    
    /**
     * The date/time that the trip should depart (or arrive, for requests where arriveBy is true)
     */
    private Date dateTime = new Date();
    /**
     * Whether the trip should depart at dateTime (false, the default), or arrive at dateTime.
     */
    private boolean arriveBy = false;
    /**
     * Whether the trip must be wheelchair accessible.
     */
    private boolean wheelchair = false;
  
    /**
     * The maximum number of possible itineraries to return.
     */
    private Integer numItineraries = 3;

    /** 
     * The complete list of parameters.
     */
    private final HashMap<String, String> parameters = new HashMap<String, String>();

    public Request() {
        modes = new TraverseModeSet("TRANSIT,WALK");
        intermediatePlaces = new ArrayList<String>();
    }

    public HashMap<String, String> getParameters() {
        return parameters;
    }

    /** add stuff to the inputs array */
    private void paramPush(String param, Object o) {
        if (o != null)
            parameters.put(param, o.toString());
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
    public void setFrom(String from) {
        paramPush(FROM, from);
        this.from = from;
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
        paramPush(TO, to);
        this.to = to;
    }

    /**
     * @return the walk
     */
    public Double getWalk() {
        return maxWalkDistance;
    }

    /**
     * @param walk
     *            the walk to set
     */
    public void setWalk(Double walk) {
        paramPush(MAX_WALK_DISTANCE, walk);
        this.maxWalkDistance = walk;
    }

    /**
     * @return the modes
     */
    public TraverseModeSet getModes() {
        return modes;
    }

    /** */
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
        paramPush(MODE, modes);
    }

    /** */
    public void addMode(List<TraverseMode> mList) {
        for (TraverseMode m : mList) {
            addMode(m);
        }
        paramPush(MODE, modes);
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
        paramPush(OPTIMIZE, optimize);
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
        this.dateTime = dateTime;
    }

    /**
     * @param dateTime
     *            the dateTime to set
     */
    public void setDateTime(String date, String time) {
        paramPush(DATE, date);
        paramPush(TIME, time);
        dateTime = DateUtils.toDate(date, time);
    }

    /**
     * @return the departAfter
     */
    public boolean isArriveBy() {
        return arriveBy;
    }

    public void setArriveBy(boolean arriveBy) {
        paramPush(ARRIVE_BY, arriveBy);
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
        paramPush(NUMBER_ITINERARIES, numItineraries);
        this.numItineraries = numItineraries;
    }

    /** */
    public String toHtmlString() {
        return toString("<br/>");
    }

    /** */
    public String toString() {
        return toString(" ");
    }

    /** */
    public String toString(String sep) {
        return getFrom() + sep + getTo() + sep + getWalk() + sep + getDateTime() + sep
                + isArriveBy() + sep + getOptimize() + sep + getModesAsStr() + sep
                + getNumItineraries();
    }

    public TraverseModeSet getModeSet() {
        return modes;
    }

    @Override
    public void removeMode(TraverseMode mode) {
        modes.setMode(mode, false);
        paramPush(MODE, modes);
    }

    public void setModes(TraverseModeSet modes) {
        this.modes = modes;
        paramPush(MODE, modes);

    }

    /** Set whether the trip must be wheelchair accessible */
    public void setWheelchair(boolean wheelchair) {
        this.wheelchair = wheelchair;
        paramPush(WHEELCHAIR, wheelchair);
    }

    /** return whether the trip must be wheelchair accessible */
    public boolean getWheelchair() {
        return wheelchair;
    }
}