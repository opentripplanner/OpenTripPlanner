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

package org.opentripplanner.api.ws.analyst;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.geotools.geometry.Envelope2D;
import org.opentripplanner.analyst.core.SlippyTile;
import org.opentripplanner.analyst.request.RenderRequest;
import org.opentripplanner.analyst.request.Renderer;
import org.opentripplanner.analyst.request.TileRequest;
import org.opentripplanner.api.common.RoutingResource;
import org.opentripplanner.analyst.parameter.Layer;
import org.opentripplanner.analyst.parameter.LayerList;
import org.opentripplanner.analyst.parameter.MIMEImageFormat;
import org.opentripplanner.analyst.parameter.Style;
import org.opentripplanner.analyst.parameter.StyleList;
import org.opentripplanner.routing.core.RoutingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sun.jersey.api.core.InjectParam;
import com.sun.jersey.api.spring.Autowire;

// removed component, mixing spring and jersey annotations is bad?
@Path("/tile/{z}/{x}/{y}.png") 
@Autowire
public class TileService extends RoutingResource {
    
    private static final Logger LOG = LoggerFactory.getLogger(TileService.class);

    @InjectParam
    private Renderer renderer;

    @PathParam("x") int x; 
    @PathParam("y") int y;
    @PathParam("z") int z;
    
    @QueryParam("layers")  @DefaultValue("traveltime") LayerList layers; 
    @QueryParam("styles")  @DefaultValue("mask")       StyleList styles;
    @QueryParam("format")  @DefaultValue("image/png")  MIMEImageFormat format;

    @GET @Produces("image/*")
    public Response tileGet() throws Exception { 
        
        Envelope2D env = SlippyTile.tile2Envelope(x, y, z);
        TileRequest tileRequest = new TileRequest(env, 256, 256);
        RoutingRequest sptRequestA = buildRequest(0);
        RoutingRequest sptRequestB = buildRequest(1);

        Layer layer = layers.get(0);
        Style style = styles.get(0);
        RenderRequest renderRequest = new RenderRequest(format, layer, style, true, false);

        return renderer.getResponse(tileRequest, sptRequestA, sptRequestB, renderRequest);
    }

}