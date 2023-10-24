package org.opentripplanner.ext.traveltime;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.geojson.MultiPolygon;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.ext.traveltime.geometry.DelaunayIsolineBuilder;
import org.opentripplanner.ext.traveltime.geometry.ZMetric;
import org.opentripplanner.ext.traveltime.geometry.ZSampleGrid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IsochroneRenderer {

  private static final Logger LOG = LoggerFactory.getLogger(IsochroneRenderer.class);
  private static final SimpleFeatureType contourSchema = makeContourSchema();

  static List<IsochroneData> renderIsochrones(
    ZSampleGrid<WTWD> sampleGrid,
    TravelTimeRequest traveltimeRequest
  ) {
    long t0 = System.currentTimeMillis();
    ZMetric<WTWD> zMetric = new IsolineMetric();
    DelaunayIsolineBuilder<WTWD> isolineBuilder = new DelaunayIsolineBuilder<>(
      sampleGrid.delaunayTriangulate(),
      zMetric
    );
    isolineBuilder.setDebug(traveltimeRequest.includeDebugGeometry);

    List<IsochroneData> isochrones = new ArrayList<>();
    for (Duration cutoff : traveltimeRequest.cutoffs) {
      long cutoffSec = cutoff.toSeconds();
      WTWD z0 = new WTWD();
      z0.w = 1.0;
      z0.wTime = cutoffSec;
      z0.d = traveltimeRequest.offRoadDistanceMeters;
      Geometry geometry = isolineBuilder.computeIsoline(z0);
      Geometry debugGeometry = null;
      if (traveltimeRequest.includeDebugGeometry) {
        debugGeometry = isolineBuilder.getDebugGeometry();
      }

      isochrones.add(new IsochroneData(cutoffSec, geometry, debugGeometry));
    }

    long t1 = System.currentTimeMillis();
    LOG.info("Computed {} isochrones in {}msec", isochrones.size(), (int) (t1 - t0));

    return isochrones;
  }

  /**
   * Create a geotools feature collection from a list of isochrones in the OTPA internal format.
   * Once in a FeatureCollection, they can for example be exported as GeoJSON.
   */
  static SimpleFeatureCollection makeContourFeatures(List<IsochroneData> isochrones) {
    DefaultFeatureCollection featureCollection = new DefaultFeatureCollection(null, contourSchema);
    SimpleFeatureBuilder fbuilder = new SimpleFeatureBuilder(contourSchema);
    for (IsochroneData isochrone : isochrones) {
      fbuilder.add(isochrone.geometry());
      fbuilder.add(isochrone.cutoffSec());
      featureCollection.add(fbuilder.buildFeature(null));
    }
    return featureCollection;
  }

  private static SimpleFeatureType makeContourSchema() {
    /* Create the output feature schema. */
    SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
    typeBuilder.setName("contours");
    typeBuilder.setCRS(DefaultGeographicCRS.WGS84);
    typeBuilder.setDefaultGeometry("the_geom");
    // Do not use "geom" or "geometry" below, it seems to broke shapefile generation
    typeBuilder.add("the_geom", MultiPolygon.class);
    typeBuilder.add("time", Long.class);
    return typeBuilder.buildFeatureType();
  }
}
