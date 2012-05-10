package org.opentripplanner.api.ws.analyst;

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
import org.opentripplanner.analyst.parameter.MIMEImageFormat;
import org.opentripplanner.analyst.parameter.Style;
import org.opentripplanner.analyst.parameter.StyleList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/legend.{format}")
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