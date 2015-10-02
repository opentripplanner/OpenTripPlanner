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

import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.opentripplanner.api.common.RoutingResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

/**
 * Run an uploaded script. This is unsafe, enable it with care.
 * 
 * TODO Enable role-based permissions.
 * 
 * @author laurent
 */
// @RolesAllowed({ "SCRIPTING" })
@Path("/scripting")
public class ScriptResource extends RoutingResource {

    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(ScriptResource.class);

    @POST
    @Path("/run")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadFile(@FormDataParam("scriptfile") InputStream uploadedInputStream,
            @FormDataParam("scriptfile") FormDataContentDisposition fileDetail) {

        try {
            if (!otpServer.scriptingService.enableScriptingWebService)
                return Response.status(Status.FORBIDDEN)
                        .entity("Scripting web-service is desactivated for security reasons.\n")
                        .build();

            String scriptContent = IOUtils.toString(uploadedInputStream, Charsets.UTF_8.name());
            Object retval = otpServer.scriptingService.runScript(fileDetail.getFileName(),
                    scriptContent);
            return Response.ok().entity(retval).build();

        } catch (Throwable e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.toString())
                    .type(MediaType.TEXT_PLAIN).build();
        }
    }

}