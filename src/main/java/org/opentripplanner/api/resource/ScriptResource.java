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