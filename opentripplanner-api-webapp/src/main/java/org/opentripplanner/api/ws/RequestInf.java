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
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.MediaType;

import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;

public interface RequestInf {

    public static enum OptimizeType {
        TRANSFERS, QUICK, FLAT
    }

    public static String FROM = "fromPlace";
    public static String TO = "toPlace";
    public static String INTERMEDIATE_PLACES = "intermediate_places";
    public static String DATE = "date";
    public static String TIME = "time";

    public static String MAX_WALK_DISTANCE = "max_walk_distance";
    public static String OPTIMIZE = "optimize";
    public static String MODE = "mode";
    public static String NUMBER_ITINERARIES = "numItineraries";
    public static String OUTPUT_FORMAT = "outputFormat";

    public static String ARRIVE_BY = "arriveBy";
    public static String WALK_SPEED = "walk_speed";

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
    public Double getWalk();

    /**
     * @param walk
     *            the walk to set
     */
    public void setWalk(Double walk);

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
    public Set<OptimizeType> getOptimize();

    /**
     * @param optimize
     *            the optimize to set
     */
    public void addOptimize(OptimizeType opt);

    /** */
    public void addOptimize(List<OptimizeType> oList);

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
     * @return whether the trip is an arriveBy trip (true) or a departAfter trip (false)
     */
    public boolean isArriveBy();

    /**
     * Sets the trip to an arriveBy trip (true) or a departAfter trip (false)
     */
    public void setArriveBy(boolean arriveBy);

    /**
     * @return the outputFormat
     */
    public MediaType getOutputFormat();

    /**
     * @param outputFormat
     *            the outputFormat to set
     */
    public void setOutputFormat(MediaType outputFormat);

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

}