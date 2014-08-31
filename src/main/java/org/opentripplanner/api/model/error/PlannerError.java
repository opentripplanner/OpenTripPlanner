/* This program is free software: you can redistribute it and/or
   modify it under the terms of the GNU Lesser General Public License
   as published by the Free Software Foundation, either version 3 of
   the License, or (at your option) any later version.
   
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   
   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package org.opentripplanner.api.model.error;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opentripplanner.api.common.Message;
import org.opentripplanner.api.common.LocationNotAccessible;
import org.opentripplanner.routing.error.GraphNotFoundException;
import org.opentripplanner.routing.error.PathNotFoundException;
import org.opentripplanner.routing.error.TransitTimesException;
import org.opentripplanner.routing.error.TrivialPathException;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This API response element represents an error in trip planning. */
public class PlannerError {

    private static final Logger LOG = LoggerFactory.getLogger(PlannerError.class);
    private static Map<Class<? extends Exception>, Message> messages;
    static {
        messages = new HashMap<Class<? extends Exception>, Message> ();
        messages.put(VertexNotFoundException.class,  Message.OUTSIDE_BOUNDS);
        messages.put(PathNotFoundException.class,    Message.PATH_NOT_FOUND);
        messages.put(LocationNotAccessible.class,    Message.LOCATION_NOT_ACCESSIBLE);
        messages.put(TransitTimesException.class,    Message.NO_TRANSIT_TIMES);
        messages.put(TrivialPathException.class,     Message.TOO_CLOSE);
        messages.put(GraphNotFoundException.class,   Message.GRAPH_UNAVAILABLE);
        messages.put(IllegalArgumentException.class, Message.BOGUS_PARAMETER);
    }
    
    public int    id;
    public String msg;
    public Message message;
    private List<String> missing = null;
    private boolean noPath = false;

    /** An error where no path has been found, but no points are missing */
    public PlannerError() {
        noPath = true;
    }

    public PlannerError(Exception e) {
        this();
        message = messages.get(e.getClass());
        if (message == null) {
            LOG.error("exception planning trip: ", e);
            message = Message.SYSTEM_ERROR;
        }
        this.setMsg(message);
        if (e instanceof VertexNotFoundException)
            this.setMissing(((VertexNotFoundException)e).getMissing());
    }

    
    public PlannerError(boolean np) {
        noPath = np;
    }

    public PlannerError(Message msg) {
        setMsg(msg);
    }

    public PlannerError(List<String> missing) {
        this.setMissing(missing);
    }

    public PlannerError(int id, String msg) {
        this.id  = id;
        this.msg = msg;
    }

    public void setMsg(Message msg) {
        this.msg = msg.get();
        this.id  = msg.getId();
    }

    /**
     * @param missing the list of point names which cannot be found (from, to, intermediate.n)
     */
    public void setMissing(List<String> missing) {
        this.missing = missing;
    }

    /**
     * @return the list of point names which cannot be found (from, to, intermediate.n)
     */
    public List<String> getMissing() {
        return missing;
    }

    /**
     * @param noPath whether no path has been found
     */
    public void setNoPath(boolean noPath) {
        this.noPath = noPath;
    }

    /**
     * @return whether no path has been found
     */
    public boolean getNoPath() {
        return noPath;
    }
    
    public static boolean isPlanningError(Class<?> clazz) {
        return messages.containsKey(clazz);
    }
}
