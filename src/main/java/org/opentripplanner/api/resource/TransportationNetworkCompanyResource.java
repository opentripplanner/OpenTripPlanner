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

package org.opentripplanner.api.resource;

import org.opentripplanner.api.model.TransportationNetworkCompanyResponse;
import org.opentripplanner.routing.transportation_network_company.TransportationNetworkCompanyService;
import org.opentripplanner.standalone.OTPServer;
import org.opentripplanner.standalone.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import static org.opentripplanner.api.param.QueryParameter.checkRangeInclusive;
import static org.opentripplanner.api.resource.ServerInfo.Q;

@Path("/routers/{routerId}/transportation_network_company")
public class TransportationNetworkCompanyResource {

    private static final Logger LOG = LoggerFactory.getLogger(TransportationNetworkCompanyResource.class);

    @Context
    OTPServer otpServer;

    @GET @Path("/eta_estimate")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML + Q, MediaType.TEXT_XML + Q })
    public TransportationNetworkCompanyResponse getEtaEstimates(
        @QueryParam("from") String from,
        @QueryParam("companies") String companies,
        @PathParam("routerId") String routerId
    ) {
        LOG.info("Received eta_estimate request");

        TransportationNetworkCompanyResponse response = new TransportationNetworkCompanyResponse();

        if (companies == null) {
            response.setError(
                new UnsupportedOperationException("companies paramater is required")
            );
            return response;
        }

        if (from == null) {
            response.setError(
                new UnsupportedOperationException("from paramater is required")
            );
            return response;
        }

        String[] fields = from.split(",");
        double latitude, longitude;
        try {
            latitude = Double.parseDouble(fields[0]);
            longitude = Double.parseDouble(fields[1]);
        } catch (Exception e) {
            response.setError(new Exception("Invalid format of from parameter.  Should be `lat,lon`."));
            return response;
        }

        try {
            checkRangeInclusive(latitude,  -90,  90);
            checkRangeInclusive(longitude, -180, 180);
        } catch (Exception e) {
            response.setError(e);
            return response;
        }

        Router router = otpServer.getRouter(routerId);
        if (router == null) {
            response.setError(
                new UnsupportedOperationException("Unable to find router with id: " + routerId)
            );
            return response;
        }

        TransportationNetworkCompanyService service = router.graph.getService(TransportationNetworkCompanyService.class);
        if (service == null) {
            response.setError(
                new UnsupportedOperationException("Unconfigured Transportaiton Network Company service for router with id: " + routerId)
            );
            return response;
        }

        try {
            response.setEstimates(service.getArrivalTimes(latitude, longitude, companies));
        } catch (Exception e) {
            response.setError(e);
        }
        return response;
    }

}
