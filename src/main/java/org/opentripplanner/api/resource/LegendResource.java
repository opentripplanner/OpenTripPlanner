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

import java.awt.image.BufferedImage;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.opentripplanner.analyst.core.Tile;
import org.opentripplanner.analyst.request.Renderer;
import org.opentripplanner.api.parameter.MIMEImageFormat;
import org.opentripplanner.api.parameter.Style;
import org.opentripplanner.api.parameter.StyleList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/analyst/legend.{format}")
public class LegendResource {
    
    private static final Logger LOG = LoggerFactory.getLogger(LegendResource.class);

    @PathParam("format")  String format; 
    @QueryParam("width")  @DefaultValue("300") int width; 
    @QueryParam("height") @DefaultValue("150") int height;
    @QueryParam("styles")  @DefaultValue("color30") StyleList styles;

    @GET @Produces("image/*")
    public Response tileGet() throws Exception { 
    	if (format.equals("jpg"))
    		format = "jpeg";
        MIMEImageFormat mimeFormat = new MIMEImageFormat("image/" + format);
        Style style = styles.get(0);
        BufferedImage image = Tile.getLegend(style, width, height);
        return Renderer.generateStreamingImageResponse(image, mimeFormat);
    }

}