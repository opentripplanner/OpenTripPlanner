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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opentripplanner.analyst.core.IsochroneData;
import org.opentripplanner.analyst.request.IsoChroneRequest;
import org.opentripplanner.api.common.RoutingResource;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.standalone.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;
import com.vividsolutions.jts.geom.MultiPolygon;

/**
 * Return isochrone geometry as a set of GeoJSON or zipped-shapefile multi-polygons.
 * 
 * Example of request:
 * 
 * <code>
 * http://localhost:8080/otp-rest-servlet/ws/isochrone?routerId=bordeaux&algorithm=accSampling&fromPlace=47.059,-0.880&date=2013/10/01&time=12:00:00&maxWalkDistance=1000&mode=WALK,TRANSIT&cutoffSec=1800&cutoffSec=3600
 * </code>
 * 
 * @author laurent
 */
@Path("/routers/{routerId}/isochrone")
public class LIsochrone extends RoutingResource {

    private static final Logger LOG = LoggerFactory.getLogger(LIsochrone.class);

    @QueryParam("cutoffSec")
    private List<Integer> cutoffSecList;

    @QueryParam("maxTimeSec")
    private Integer maxTimeSec;

    @QueryParam("debug")
    private Boolean debug;

    @QueryParam("precisionMeters")
    @DefaultValue("200")
    private Integer precisionMeters;

    @QueryParam("offRoadDistanceMeters")
    @DefaultValue("150")
    private Integer offRoadDistanceMeters;

    @QueryParam("coordinateOrigin")
    private String coordinateOrigin = null;

    private static final SimpleFeatureType contourSchema = makeContourSchema();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGeoJsonIsochrone() throws Exception {
        SimpleFeatureCollection contourFeatures = makeContourFeatures(computeIsochrone());
        StringWriter writer = new StringWriter();
        FeatureJSON fj = new FeatureJSON();
        fj.writeFeatureCollection(contourFeatures, writer);
        CacheControl cc = new CacheControl();
        cc.setMaxAge(3600);
        cc.setNoCache(false);
        return Response.ok().entity(writer.toString()).cacheControl(cc).build();
    }

    @GET
    @Produces("application/x-zip-compressed")
    public Response getZippedShapefileIsochrone(@QueryParam("shpName") String shpName,
            @QueryParam("stream") @DefaultValue("true") boolean stream) throws Exception {
        SimpleFeatureCollection contourFeatures = makeContourFeatures(computeIsochrone());
        /* Output the staged features to Shapefile */
        final File shapeDir = Files.createTempDir();
        File shapeFile = new File(shapeDir, shpName + ".shp");
        LOG.debug("writing out shapefile {}", shapeFile);
        ShapefileDataStore outStore = new ShapefileDataStore(shapeFile.toURI().toURL());
        outStore.createSchema(contourSchema);
        Transaction transaction = new DefaultTransaction("create");
        SimpleFeatureStore featureStore = (SimpleFeatureStore) outStore.getFeatureSource();
        featureStore.setTransaction(transaction);
        try {
            featureStore.addFeatures(contourFeatures);
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        } finally {
            transaction.close();
        }
        shapeDir.deleteOnExit(); // Note: the order is important
        for (File f : shapeDir.listFiles())
            f.deleteOnExit();
        /* Zip up the shapefile components */
        StreamingOutput output = new DirectoryZipper(shapeDir);
        if (stream) {
            return Response.ok().entity(output).build();
        } else {
            File zipFile = new File(shapeDir, shpName + ".zip");
            OutputStream fos = new FileOutputStream(zipFile);
            output.write(fos);
            zipFile.deleteOnExit();
            return Response.ok().entity(zipFile).build();
        }
    }

    /**
     * Create a geotools feature collection from a list of isochrones in the OTPA internal format.
     * Once in a FeatureCollection, they can for example be exported as GeoJSON.
     */
    public static SimpleFeatureCollection makeContourFeatures(List<IsochroneData> isochrones) {
        DefaultFeatureCollection featureCollection = new DefaultFeatureCollection(null,
                contourSchema);
        SimpleFeatureBuilder fbuilder = new SimpleFeatureBuilder(contourSchema);
        for (IsochroneData isochrone : isochrones) {
            fbuilder.add(isochrone.geometry);
            fbuilder.add(isochrone.cutoffSec);
            featureCollection.add(fbuilder.buildFeature(null));
        }
        return featureCollection;
    }

    /**
     * Generic method to compute isochrones. Parse the request, call the adequate builder, and
     * return a list of generic isochrone data.
     * 
     * @return
     * @throws Exception
     */
    public List<IsochroneData> computeIsochrone() throws Exception {

        if (debug == null)
            debug = false;
        if (precisionMeters < 10)
            throw new IllegalArgumentException("Too small precisionMeters: " + precisionMeters);
        if (offRoadDistanceMeters < 10)
            throw new IllegalArgumentException("Too small offRoadDistanceMeters: " + offRoadDistanceMeters);

        IsoChroneRequest isoChroneRequest = new IsoChroneRequest(cutoffSecList);
        isoChroneRequest.includeDebugGeometry = debug;
        isoChroneRequest.precisionMeters = precisionMeters;
        isoChroneRequest.offRoadDistanceMeters = offRoadDistanceMeters;
        if (coordinateOrigin != null)
            isoChroneRequest.coordinateOrigin = new GenericLocation(null, coordinateOrigin)
                    .getCoordinate();
        RoutingRequest sptRequest = buildRequest();

        if (maxTimeSec != null) {
            isoChroneRequest.maxTimeSec = maxTimeSec;
        } else {
            isoChroneRequest.maxTimeSec = isoChroneRequest.maxCutoffSec;
        }

        Router router = otpServer.getRouter(routerId);
        return router.isoChroneSPTRenderer.getIsochrones(isoChroneRequest, sptRequest);
    }

    static SimpleFeatureType makeContourSchema() {
        /* Create the output feature schema. */
        SimpleFeatureTypeBuilder tbuilder = new SimpleFeatureTypeBuilder();
        tbuilder.setName("contours");
        tbuilder.setCRS(DefaultGeographicCRS.WGS84);
        // Do not use "geom" or "geometry" below, it seems to broke shapefile generation
        tbuilder.add("the_geom", MultiPolygon.class);
        tbuilder.add("time", Integer.class); // TODO change to something more descriptive and lowercase
        return tbuilder.buildFeatureType();
    }

    // TODO Extract this to utility package?
    private static class DirectoryZipper implements StreamingOutput {
        private File directory;

        DirectoryZipper(File directory) {
            this.directory = directory;
        }

        @Override
        public void write(OutputStream outStream) throws IOException {
            ZipOutputStream zip = new ZipOutputStream(outStream);
            for (File f : directory.listFiles()) {
                zip.putNextEntry(new ZipEntry(f.getName()));
                Files.copy(f, zip);
                zip.closeEntry();
                zip.flush();
            }
            zip.close();
        }
    }

}