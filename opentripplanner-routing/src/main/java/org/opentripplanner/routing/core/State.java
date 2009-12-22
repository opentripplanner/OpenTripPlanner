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

package org.opentripplanner.routing.core;

import java.util.Date;

import org.onebusaway.gtfs.model.AgencyAndId;

public class State {

    private long _time;
    private int curPattern = -1;
    public AgencyAndId tripId = null;
    
    public State() {
        this(System.currentTimeMillis());
    }

    public State(long time) {
        _time = time;
    }    

    public State(long time, int pattern, AgencyAndId tripId) {
        _time = time;
        curPattern = pattern;
        this.tripId = tripId;
    }

    public long getTime() {
        return _time;
    }

    public void incrementTimeInSeconds(int numOfSeconds) {
        _time += numOfSeconds * 1000;
    }

    public State clone() {
        State ret = new State(_time, curPattern, tripId);
        return ret;
    }

    public String toString() {
        return "<State " + new Date(_time) + "," + curPattern + ">";
    }

    public void setPattern(int curPattern) {
        this.curPattern = curPattern;
    }

    public int getPattern() {
        return curPattern;
    }

}