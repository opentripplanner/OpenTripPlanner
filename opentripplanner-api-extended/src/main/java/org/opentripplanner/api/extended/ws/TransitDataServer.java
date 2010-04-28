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

package org.opentripplanner.api.extended.ws;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.api.extended.ws.model.WmsInfo;
import org.springframework.beans.factory.annotation.Autowired;

import com.sun.jersey.api.spring.Autowire;

@Path("/")
@Autowire
public class TransitDataServer {

    private TransitServerGtfs transitServerGtfs;

    @Autowired
    public void setTransitServerGtfs(TransitServerGtfs transitServerGtfs) {
        this.transitServerGtfs = transitServerGtfs;
    }
    
    @GET
    @Path("/")
    @Produces("text/html")
    public String getIndex() {
        return "<html><body><h2>The system works</h2></body></html>";
    }   

    @GET
    @Path("wms")
    @Produces( { MediaType.APPLICATION_JSON })
    public WmsInfo getWmsInfo(@Context UriInfo ui) {
        try {
            String baseAddress = this.transitServerGtfs.getGeoserverBaseUri();
            UriBuilder uriBuilder = null;
            try {
                uriBuilder = UriBuilder.fromUri(baseAddress);
                MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
                for (Entry<String, List<String>> entrySet : queryParams.entrySet()) {
                    String key = entrySet.getKey();
                    List<String> vals = entrySet.getValue();
                    // we only expect the first anyway
                    String val = vals.get(0);
                    if (vals.size() > 1) {
                        System.out.println("*** got more than one value for: " + key + " - using first: " + val);
                    }
                    uriBuilder.queryParam(key, val);
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                throw new WebApplicationException(400);
            }
            URI uri = uriBuilder.build();
            String urlString = uri.toString();
            URL url = new URL(urlString);
            URLConnection conn = url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            // parse the geoserver response for stops and routes
            Pattern pattern = Pattern.compile("(stops|routes).(\\S+)");
            String line = null;
            List<String> stopIds = new ArrayList<String>();
            List<String> routeIds = new ArrayList<String>();
            while ((line = reader.readLine()) != null) {
                Matcher m = pattern.matcher(line);
                while (m.find()) {
                    String matchType = m.group(1);
                    if (matchType.equals("stops")) {
                        stopIds.add(m.group(2));
                    } else if (matchType.equals("routes")) {
                        routeIds.add(m.group(2));
                    }
                }
            }
            reader.close();

            // if we have any stops, then that's the type of result we return
            if (stopIds.size() > 0) {
                // if we've found any stop ids then we we use the first one
                String stopId = stopIds.get(0);
                return new WmsInfo(transitServerGtfs, new AgencyAndId("MTA NYCT", stopId));
            } else if (routeIds.size() > 0) {
                // we have only route ids back
                // first we have to convert the ids to have the agency and id on them too
                List<String> routeIdsWithAgencyId = new ArrayList<String>();
                for (String routeId : routeIds) {
                    routeIdsWithAgencyId.add("MTA NYCT " + routeId);
                }
                return new WmsInfo(transitServerGtfs, routeIdsWithAgencyId);
            }            
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // returns an empty response
        return new WmsInfo();
    }    
}
