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

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.MediaType;

import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.util.DateUtils;

/**
 *
 */
public class Request implements RequestInf {

    private String m_from;
    private String m_to;
    private Double m_walk = 0.50;
    private TraverseModeSet m_modes; // defaults in constructor
    private Set<OptimizeType> m_optimize; // default in constructor
    private boolean m_optimizeEmpty = true;
    private Date m_dateTime = new Date();
    private boolean m_arriveBy = false;
    private MediaType m_outputFormat = MediaType.APPLICATION_JSON_TYPE;
    private Integer m_numItineraries = 3;

    private final Hashtable<String, String> m_parameters = new Hashtable<String, String>();

    public Request() {
        m_modes = new TraverseModeSet("TRANSIT,WALK");

        m_optimize = new HashSet<OptimizeType>();
        Collections.addAll(m_optimize, OptimizeType.quick);
    }

    public Hashtable<String, String> getParameters() {
        return m_parameters;
    }

    /** add stuff to the inputs array */
    private void paramPush(String param, Object o) {
        if (o != null)
            m_parameters.put(param, o.toString());
    }

    /**
     * @return the from
     */
    public String getFrom() {
        return m_from;
    }

    /**
     * @param from
     *            the from to set
     */
    public void setFrom(String from) {
        paramPush(FROM, from);
        m_from = from;
    }

    /**
     * @return the to
     */
    public String getTo() {
        return m_to;
    }

    /**
     * @param to
     *            the to to set
     */
    public void setTo(String to) {
        paramPush(TO, to);
        m_to = to;
    }

    /**
     * @return the walk
     */
    public Double getWalk() {
        return m_walk;
    }

    /**
     * @param walk
     *            the walk to set
     */
    public void setWalk(Double walk) {
        paramPush(WALK, walk);
        m_walk = walk;
    }

    /**
     * @return the modes
     */
    public TraverseModeSet getModes() {
        return m_modes;
    }

    /** */
    public String getModesAsStr() {
        String retVal = null;
        for (TraverseMode m : m_modes.getModes()) {
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
        m_modes.setMode(mode, true);
        paramPush(MODE, m_modes);
    }

    /** */
    public void addMode(List<TraverseMode> mList) {
        for (TraverseMode m : mList) {
            addMode(m);
        }
        paramPush(MODE, m_modes);
    }

    /**
     * @return the optimize
     */
    public Set<OptimizeType> getOptimize() {
        return m_optimize;
    }

    /** */
    public String getOptimizeAsStr() {
        String retVal = null;
        for (OptimizeType o : m_optimize) {
            if (retVal == null)
                retVal = "";
            else
                retVal += ", ";
            retVal += o;
        }

        return retVal;
    }

    /**
     * @param optimize
     *            the optimize to set
     */
    public void addOptimize(OptimizeType opt) {
        if (m_optimizeEmpty) {
            m_optimizeEmpty = false;
            m_optimize = new HashSet<OptimizeType>();
        }
        m_optimize.add(opt);
        paramPush(OPTIMIZE, m_optimize);
    }

    /** */
    public void addOptimize(List<OptimizeType> oList) {
        if (oList != null && oList.size() > 0) {
            for (OptimizeType o : oList)
                addOptimize(o);
        }
    }

    /**
     * @return the dateTime
     */
    public Date getDateTime() {
        return m_dateTime;
    }

    /**
     * @param dateTime
     *            the dateTime to set
     */
    public void setDateTime(Date dateTime) {
        m_dateTime = dateTime;
    }

    /**
     * @param dateTime
     *            the dateTime to set
     */
    public void setDateTime(String date, String time) {
        paramPush(DATE, date);
        paramPush(TIME, time);
        m_dateTime = DateUtils.toDate(date, time);
    }

    /**
     * @return the departAfter
     */
    public boolean isArriveBy() {
        return m_arriveBy;
    }

    public void setArriveBy(boolean arriveBy) {
        paramPush(ARRIVE_BY, arriveBy);
        m_arriveBy = arriveBy;
    }

    /**
     * @return the outputFormat
     */
    public MediaType getOutputFormat() {
        return m_outputFormat;
    }

    /**
     * @param outputFormat
     *            the outputFormat to set
     */
    public void setOutputFormat(MediaType outputFormat) {
        paramPush(OUTPUT_FORMAT, outputFormat);
        m_outputFormat = outputFormat;
    }

    /**
     * @return the numItineraries
     */
    public Integer getNumItineraries() {
        return m_numItineraries;
    }

    /**
     * @param numItineraries
     *            the numItineraries to set
     */
    public void setNumItineraries(Integer numItineraries) {
        if (numItineraries < 1 || numItineraries > 10)
            numItineraries = 3;
        paramPush(NUMBER_ITINERARIES, numItineraries);
        m_numItineraries = numItineraries;
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
                + isArriveBy() + sep + getOptimizeAsStr() + sep + getModesAsStr() + sep
                + getNumItineraries() + sep + getOutputFormat();
    }

    public TraverseModeSet getModeSet() {
        return m_modes;
    }

    @Override
    public void removeMode(TraverseMode mode) {
        m_modes.setMode(mode, false);
        paramPush(MODE, m_modes);
    }

    public void setModes(TraverseModeSet modes) {
        m_modes = modes;
        paramPush(MODE, m_modes);
        
    }

}