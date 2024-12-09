package org.opentripplanner.graph_builder.issue.report;

import java.io.IOException;
import java.util.Collection;
import org.geojson.MultiPolygon;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.geojson.GeoJSONWriter;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Write the data issues into a GeoJSON file, which can be viewed on a map, or imported into GIS.
 */
public class GeoJsonWriter {

  private static final Logger LOG = LoggerFactory.getLogger(GeoJsonWriter.class);
  private final DataSource target;
  private final Collection<DataImportIssue> issues;

  GeoJsonWriter(CompositeDataSource reportDirectory, Bucket bucket) {
    this.target = reportDirectory.entry(bucket.key().key() + ".geojson");
    this.issues = bucket.issues();
  }

  /**
   * Write the issues to a GeoJSON file
   * @return true if a GeoJSON was written, false if not
   */
  boolean writeFile() {
    var filteredIssues = issues.stream().filter(issue -> issue.getGeometry() != null).toList();

    if (filteredIssues.isEmpty()) {
      return false;
    }

    var featureCollection = makeContourFeatures(issues);
    try (GeoJSONWriter geoJSONWriter = new GeoJSONWriter(target.asOutputStream())) {
      // Set slightly higher resolution than default. We output small details
      geoJSONWriter.setMaxDecimals(6);
      // Make easier to view the file
      geoJSONWriter.setPrettyPrinting(true);
      geoJSONWriter.writeFeatureCollection(featureCollection);
    } catch (IOException e) {
      LOG.error("Unable to write GeoJSON", e);
      return false;
    }
    return true;
  }

  /**
   * Create a geotools feature collection from a list of data import issues.
   * Once in a FeatureCollection, they can for example be exported as GeoJSON.
   */
  private static SimpleFeatureCollection makeContourFeatures(Collection<DataImportIssue> issues) {
    SimpleFeatureType schema = makeDataIssueSchema();
    DefaultFeatureCollection featureCollection = new DefaultFeatureCollection(null, schema);
    SimpleFeatureBuilder fbuilder = new SimpleFeatureBuilder(schema);
    for (DataImportIssue issue : issues) {
      fbuilder.add(issue.getGeometry());
      fbuilder.add(issue.getMessage());
      featureCollection.add(fbuilder.buildFeature(null));
    }
    return featureCollection;
  }

  private static SimpleFeatureType makeDataIssueSchema() {
    /* Create the output feature schema. */
    SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
    typeBuilder.setName("dataImportIssues");
    typeBuilder.setCRS(DefaultGeographicCRS.WGS84);
    typeBuilder.setDefaultGeometry("the_geom");
    // Do not use "geom" or "geometry" below, it seems to break shapefile generation
    typeBuilder.add("the_geom", MultiPolygon.class);
    typeBuilder.add("description", String.class);
    return typeBuilder.buildFeatureType();
  }
}
