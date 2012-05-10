package org.opentripplanner.analyst.request;

import java.awt.image.BufferedImage;
import java.io.OutputStream;

import javax.imageio.ImageIO;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opentripplanner.analyst.core.Tile;
import org.opentripplanner.analyst.parameter.MIMEImageFormat;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Renderer {

    private static final Logger LOG = LoggerFactory.getLogger(Renderer.class);

    @Autowired
    private TileCache tileCache;

    @Autowired
    private SPTCache sptCache;

    public Response getResponse (TileRequest tileRequest, 
            RoutingRequest sptRequestA, RoutingRequest sptRequestB, 
            RenderRequest renderRequest) throws Exception {

        Tile tile = tileCache.get(tileRequest);
        ShortestPathTree sptA = sptCache.get(sptRequestA);
        ShortestPathTree sptB = sptCache.get(sptRequestB);
        
        BufferedImage image;
        switch (renderRequest.layer) {
        case DIFFERENCE :
            image = tile.linearCombination(1, sptA, -1, sptB, 128, renderRequest);
            break;
        case HAGERSTRAND :
            long elapsed = Math.abs(sptRequestB.dateTime - sptRequestA.dateTime);
            image = tile.linearCombination(-1, sptA, -1, sptB, elapsed/60, renderRequest);
            break;
        case TRAVELTIME :
        default :
            image = tile.generateImage(sptA, renderRequest);
        }
        
        // geotiff kludge
        if (renderRequest.format.toString().equals("image/geotiff")) {
            GridCoverage2D gc = tile.getGridCoverage2D(image);
            return generateStreamingGeotiffResponse(gc);
        } else {
            return generateStreamingImageResponse(image, renderRequest.format);
        }
    }
        
    public static Response generateStreamingImageResponse(
            final BufferedImage image, final MIMEImageFormat format) {
        
        if (image == null) {
            LOG.warn("response image is null");
        }
            
        StreamingOutput streamingOutput = new StreamingOutput() {
            public void write(OutputStream outStream) {
                try {
                    long t0 = System.currentTimeMillis();
                    ImageIO.write(image, format.type, outStream);
                    long t1 = System.currentTimeMillis();
                    LOG.debug("wrote image in {}msec", (int)(t1-t0));
                } catch (Exception e) {
                    LOG.error("exception while preparing image : {}", e.getMessage());
                    throw new WebApplicationException(e);
                }
            }
       };

       CacheControl cc = new CacheControl();
       cc.setMaxAge(3600);
       cc.setNoCache(false);
       return Response.ok(streamingOutput)
                       .type(format.toString())
                       .cacheControl(cc)
                       .build();
    }
    
    
    private static Response generateStreamingGeotiffResponse(final GridCoverage2D coverage) {
        
        StreamingOutput streamingOutput = new StreamingOutput() {
            public void write(OutputStream outStream) {
                try {
                    long t0 = System.currentTimeMillis();
                    GeoTiffWriteParams wp = new GeoTiffWriteParams();
                    wp.setCompressionMode(GeoTiffWriteParams.MODE_EXPLICIT);
                    wp.setCompressionType("LZW");
                    ParameterValueGroup params = new GeoTiffFormat().getWriteParameters();
                    params.parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString()).setValue(wp);
                    new GeoTiffWriter(outStream).write(coverage, (GeneralParameterValue[]) params.values().toArray(new GeneralParameterValue[1]));
                    new GeoTiffWriter(outStream).write(coverage, null);
                    long t1 = System.currentTimeMillis();
                    LOG.debug("wrote geotiff in {}msec", t1-t0);
                } catch (Exception e) {
                    LOG.error("exception while preparing geotiff : {}", e.getMessage());
                    throw new WebApplicationException(e);
                }
            }
       };

       CacheControl cc = new CacheControl();
       cc.setMaxAge(3600);
       cc.setNoCache(false);
       return Response.ok(streamingOutput)
                       .type("image/geotiff")
                       .cacheControl(cc)
                       .build();
    }

}
