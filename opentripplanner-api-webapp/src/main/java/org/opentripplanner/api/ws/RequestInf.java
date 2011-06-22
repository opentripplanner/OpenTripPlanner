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

import java.util.Date;

import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;

public interface RequestInf {

    public static String ROUTER_ID = "routerId";
	public static String FROM = "fromPlace";
	public static String TO = "toPlace";
	public static String INTERMEDIATE_PLACES = "intermediatePlaces";
	public static String DATE = "date";
	public static String TIME = "time";

	public static String MAX_WALK_DISTANCE = "maxWalkDistance";
	public static String OPTIMIZE = "optimize";
	public static String MODE = "mode";
	public static String NUMBER_ITINERARIES = "numItineraries";
	public static String SHOW_INTERMEDIATE_STOPS = "showIntermediateStops";

	public static String PREFERRED_ROUTES = "preferredRoutes";
	public static String UNPREFERRED_ROUTES = "unpreferredRoutes";
	public static String BANNED_ROUTES = "bannedRoutes";
	
	public static String ARRIVE_BY = "arriveBy";
	public static String WALK_SPEED = "walkSpeed";
	public static String WHEELCHAIR = "wheelchair";
	public static String MIN_TRANSFER_TIME = "minTransferTime";

	/**
	 * @return the from
	 */
	public String getFrom();

	/**
	 * @param from
	 *            the from to set
	 */
	public void setFrom(String from);

	/**
	 * @return the to
	 */
	public String getTo();

	/**
	 * @param to
	 *            the to to set
	 */
	public void setTo(String to);

	/**
	 * @return the walk
	 */
	public Double getMaxWalkDistance();

	/**
	 * @param walk
	 *            the walk to set
	 */
	public void setMaxWalkDistance(Double walk);

	/**
	 * @return the modes
	 */
	public TraverseModeSet getModes();

	/**
	 * @param modes
	 *            the modes to set
	 */
	public void addMode(TraverseMode mode);

	/** */
	public void setModes(TraverseModeSet mode);

	/**
	 * @return the optimize
	 */
	public OptimizeType getOptimize();

	/**
	 * @param optimize
	 *            the optimize to set
	 */
	public void setOptimize(OptimizeType opt);

	/**
	 * @return the dateTime
	 */
	public Date getDateTime();

	/**
	 * @param dateTime
	 *            the dateTime to set
	 */
	public void setDateTime(Date dateTime);

	/**
	 * @param dateTime
	 *            the dateTime to set
	 */
	public void setDateTime(String date, String time);

	/**
	 * @return whether the trip is an arriveBy trip (true) or a departAfter trip
	 *         (false)
	 */
	public boolean isArriveBy();

	/**
	 * Sets the trip to an arriveBy trip (true) or a departAfter trip (false)
	 */
	public void setArriveBy(boolean arriveBy);

	/**
	 * @return the numItineraries
	 */
	public Integer getNumItineraries();

	/**
	 * @param numItineraries
	 *            the numItineraries to set
	 */
	public void setNumItineraries(Integer numItineraries);

	public void removeMode(TraverseMode car);

	/**
	 * @param showIntermediateStops
	 *            whether the planner should return intermediate stop lists for
	 *            transit legs
	 */
	public void setShowIntermediateStops(boolean showIntermediateStops);

	/**
	 * @return whether the planner should return intermediate stop lists for
	 *         transit legs
	 */
	public boolean getShowIntermediateStops();

}