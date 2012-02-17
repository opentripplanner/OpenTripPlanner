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

package org.opentripplanner.api.ws.analysis;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlRootElement;

import org.opentripplanner.util.monitoring.MonitoringStore;
import org.opentripplanner.util.monitoring.MonitoringStoreFactory;
import org.springframework.security.access.annotation.Secured;

import com.sun.jersey.api.spring.Autowire;
import com.sun.jersey.spi.resource.Singleton;

/**
 * Monitor the state of the system, and control monitoring (to turn expensive things on/off)
 * 
 * @author novalis
 * 
 */
@Path("/monitoring")
@XmlRootElement
@Autowire
@Singleton
public class Monitoring {
    static MonitoringStore store = MonitoringStoreFactory.getStore();

    /**
     * Get a Long from the monitoring store
     * 
     * @param key
     * @return
     */
    @Secured({ "ROLE_USER" })
    @GET
    @Path("/long")
    @Produces({ MediaType.APPLICATION_JSON })
    public Object getLong(@QueryParam("key") String key) {
        return store.getLong(key);
    }

    /**
     * Turn on/off monitoring for a given key
     * 
     * @param key
     * @return
     */
    @Secured({ "ROLE_USER" })
    @POST
    @Path("/monitoring")
    @Produces({ MediaType.APPLICATION_JSON })
    public Object control(@FormParam("key") String key, @FormParam("on") boolean on) {
        System.out.println("setting " + key + " to " + on);
        store.setMonitoring(key, on);
        return "OK";
    }

    /**
     * Turn on/off monitoring for a given key
     * 
     * @param key
     * @return
     */
    @Secured({ "ROLE_USER" })
    @GET
    @Path("/monitoring")
    @Produces({ MediaType.APPLICATION_JSON })
    public Object control(@QueryParam("key") String key) {
        return store.isMonitoring(key);
    }

}
