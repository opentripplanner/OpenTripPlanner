package org.opentripplanner.inspector.vector;

import edu.colorado.cires.cmg.mvt.VectorTile;
import edu.colorado.cires.cmg.mvt.adapt.jts.JtsAdapter;
import edu.colorado.cires.cmg.mvt.adapt.jts.TileGeomResult;
import edu.colorado.cires.cmg.mvt.build.MvtLayerBuild;
import edu.colorado.cires.cmg.mvt.build.MvtLayerParams;
import edu.colorado.cires.cmg.mvt.build.MvtLayerProps;
import java.util.List;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.opentripplanner.apis.support.mapping.PropertyMapper;
import org.opentripplanner.framework.geometry.GeometryUtils;

/**
 * Common functionality for creating a vector tile from a data source able to supply a set of JTS
 * geometries with userData of type {@link T} for an {@link Envelope}.
 */
public abstract class LayerBuilder<T> {

  private static final GeometryFactory GEOMETRY_FACTORY = GeometryUtils.getGeometryFactory();
  private final MvtLayerProps layerProps = new MvtLayerProps();
  private final VectorTile.Tile.Layer.Builder layerBuilder;
  private final PropertyMapper<T> mapper;
  private final double expansionFactor;

  public LayerBuilder(PropertyMapper<T> mapper, String layerName, double expansionFactor) {
    this.mapper = mapper;
    this.layerBuilder = MvtLayerBuild.newLayerBuilder(layerName, MvtLayerParams.DEFAULT);
    this.expansionFactor = expansionFactor;
  }

  /**
   * Get a list of geometries in this layer inside the query envelope. The geometries should include
   * an object of type T as their userData.
   */
  protected abstract List<Geometry> getGeometries(Envelope query);

  final VectorTile.Tile.Layer build(Envelope envelope) {
    Envelope query = new Envelope(envelope);
    query.expandBy(envelope.getWidth() * expansionFactor, envelope.getHeight() * expansionFactor);

    TileGeomResult tileGeom = JtsAdapter.createTileGeom(
      getGeometries(query),
      envelope,
      query,
      GEOMETRY_FACTORY,
      MvtLayerParams.DEFAULT,
      g -> true
    );

    List<VectorTile.Tile.Feature> features = JtsAdapter.toFeatures(
      tileGeom.mvtGeoms,
      layerProps,
      this.mapper
    );
    layerBuilder.addAllFeatures(features);

    MvtLayerBuild.writeProps(layerBuilder, layerProps);
    return layerBuilder.build();
  }
}
