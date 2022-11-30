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
import org.opentripplanner.api.mapping.PropertyMapper;
import org.opentripplanner.ext.vectortiles.VectorTilesResource.LayerParameters;
import org.opentripplanner.framework.geometry.GeometryUtils;

public abstract class LayerBuilder<T> {

  private static final GeometryFactory GEOMETRY_FACTORY = GeometryUtils.getGeometryFactory();
  private final MvtLayerProps layerProps = new MvtLayerProps();
  private final VectorTile.Tile.Layer.Builder layerBuilder;
  private final PropertyMapper<T> mapper;

  public LayerBuilder(String layerName, PropertyMapper<T> mapper) {
    this.mapper = mapper;
    this.layerBuilder = MvtLayerBuild.newLayerBuilder(layerName, MvtLayerParams.DEFAULT);
  }

  /**
   * Get a list of geometries in this layer inside the query envelope. The geometries should include
   * an object of type T as their userData.
   */
  protected abstract List<Geometry> getGeometries(Envelope query);

  public VectorTile.Tile.Layer build(Envelope envelope, LayerParameters params) {
    Envelope query = new Envelope(envelope);
    query.expandBy(
      envelope.getWidth() * params.expansionFactor(),
      envelope.getHeight() * params.expansionFactor()
    );

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
