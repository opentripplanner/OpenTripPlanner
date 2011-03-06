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

import org.opentripplanner.routing.core.StateData.Editor;

public class State {

    private final long time;

    private final StateData data;

    public State() {
        this(System.currentTimeMillis());
    }

    public State(long time) {
        this(time, StateData.createDefault());
    }

    public State(long time, StateData data) {
        this.time = time;
        this.data = data;
    }

    /**
     * @return the time in milliseconds since the epoch.
     */
    public long getTime() {
        return time;
    }

    /**
     * @return all data associated with this state
     */
    public StateData getData() {
        return data;
    }

    public StateData.Editor edit() {
        Editor editor = data.edit();
        editor.setTime(time);
        return editor;
    }

    /****
     * State Transition Methods
     ****/

    /**
     * Create a new state whose time has been incremented the specified number of seconds and whose
     * data remains the same. The current state object is not modified.
     * 
     * @param numOfSeconds
     * @return the new state
     */
    public State incrementTimeInSeconds(int numOfSeconds) {
        long t = this.time + numOfSeconds * 1000;
        return new State(t, this.data);
    }

    /**
     * Create a new state with the specified time and the current state's data. The current state
     * object is not modified.
     * 
     * @param time
     * @return the new state
     */
    public State setTime(long time) {
        return new State(time, this.data);
    }

    /**
     * Create a new state whose data has been updated as specified and whose time remains the same.
     * The current state object is not modified.
     * 
     * @param updatedData
     * @return the new state.
     */
    public State setData(StateData updatedData) {
        return new State(this.time, updatedData);
    }

    public String toString() {
        return "<State " + new Date(time) + ">";
    }
}