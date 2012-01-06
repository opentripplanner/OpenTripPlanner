package org.opentripplanner.api.ws.analysis;

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
     * @param key
     * @return
     */
    @Secured({ "ROLE_USER" })
    @GET
    @Path("/long")
    @Produces({ MediaType.APPLICATION_JSON })
    public Object getLong(
            @QueryParam("key") String key) {
        return store.getLong(key);
    }
    
    /** 
     * Turn on/off monitoring for a given key
     * @param key
     * @return
     */
    @Secured({ "ROLE_USER" })
    @POST
    @Path("/control")
    @Produces({ MediaType.APPLICATION_JSON })
    public Object control(
            @QueryParam("key") String key,
            @QueryParam("on") Boolean on) {
        store.setMonitoring(key, on);
        return "OK";
    }
    
}
