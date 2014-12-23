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

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.geotools.geometry.Envelope2D;
import org.opentripplanner.analyst.core.GeometryIndex;
import org.opentripplanner.analyst.request.RenderRequest;
import org.opentripplanner.analyst.request.Renderer;
import org.opentripplanner.analyst.request.SPTRequest;
import org.opentripplanner.analyst.request.TileRequest;
import org.opentripplanner.api.parameter.CRSParameter;
import org.opentripplanner.api.parameter.IsoTimeParameter;
import org.opentripplanner.api.parameter.Layer;
import org.opentripplanner.api.parameter.MIMEImageFormat;
import org.opentripplanner.api.parameter.Style;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/routers/{routerId}/analyst/raster")
public class Raster {
    
    private static final Logger LOG = LoggerFactory.getLogger(Raster.class);

    @Context // FIXME inject Application context
    private GeometryIndex index;

    @Context // FIXME inject Application context
    private Renderer renderer;
    
    @GET @Produces("image/*")
    public Response getRaster(
           @QueryParam("x") Float x,  
           @QueryParam("y") Float y,  
           @QueryParam("width")  Integer width,  
           @QueryParam("height") Integer height,  
           @QueryParam("resolution") Double resolution,  
           @QueryParam("time") IsoTimeParameter time,
           @QueryParam("format") @DefaultValue("image/geotiff") MIMEImageFormat format,
           @QueryParam("crs") @DefaultValue("EPSG:4326") CRSParameter crs
           ) throws Exception {
        
        // BoundingBox is a subclass of Envelope, an Envelope2D constructor parameter
        Envelope2D bbox = new Envelope2D(index.getBoundingBox(crs.crs));
        if (resolution != null) {
            width  = (int) Math.ceil(bbox.width  / resolution);
            height = (int) Math.ceil(bbox.height / resolution);
        }
        
        TileRequest tileRequest = new TileRequest(bbox, width, height);
        SPTRequest sptRequest = new SPTRequest(x, y, time.cal);
        RenderRequest renderRequest = new RenderRequest(format, Layer.TRAVELTIME, Style.GRAY, false, false);

        return null; //renderer.getResponse(tileRequest, sptRequest, null, renderRequest);
        
    }

}