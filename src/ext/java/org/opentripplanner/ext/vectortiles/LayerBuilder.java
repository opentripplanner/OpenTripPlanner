package org.opentripplanner.ext.vectortiles;

import com.wdtinc.mapbox_vector_tile.VectorTile;
import com.wdtinc.mapbox_vector_tile.adapt.jts.JtsAdapter;
import com.wdtinc.mapbox_vector_tile.adapt.jts.TileGeomResult;
import com.wdtinc.mapbox_vector_tile.build.MvtLayerBuild;
import com.wdtinc.mapbox_vector_tile.build.MvtLayerParams;
import com.wdtinc.mapbox_vector_tile.build.MvtLayerProps;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.opentripplanner.common.geometry.GeometryUtils;

import java.util.List;

public abstract class LayerBuilder<T> {
  static private final GeometryFactory GEOMETRY_FACTORY = GeometryUtils.getGeometryFactory();
  private final MvtLayerProps layerProps = new MvtLayerProps();
  private final VectorTile.Tile.Layer.Builder layerBuilder;
  private final PropertyMapper<T> mapper;

  public LayerBuilder(String layerName, PropertyMapper<T> mapper) {
    this.mapper = mapper;
    this.layerBuilder = MvtLayerBuild.newLayerBuilder(layerName, MvtLayerParams.DEFAULT);
  }

  VectorTile.Tile.Layer build(Envelope envelope) {
    Envelope query = new Envelope(envelope);
    query.expandBy(
        envelope.getWidth() * getExpansionFactor(),
        envelope.getHeight() * getExpansionFactor());

    TileGeomResult tileGeom = JtsAdapter.createTileGeom(
        getGeometries(query),
        envelope,
        query,
        GEOMETRY_FACTORY,
        MvtLayerParams.DEFAULT,
        g -> true
    );

    List<VectorTile.Tile.Feature> features = JtsAdapter.toFeatures(tileGeom.mvtGeoms, layerProps, this.mapper);
    layerBuilder.addAllFeatures(features);

    MvtLayerBuild.writeProps(layerBuilder, layerProps);
    return layerBuilder.build();
  }

  /**
   * Get a list of geometries in this layer inside the query envelope. The geometries should include
   * an object of type T as their userData.
   */
  abstract protected List<Geometry> getGeometries(Envelope query);

  /**
   * How far outside its boundaries should the tile contain information. The value is a fraction of
   * the tile size.
   */
  abstract protected double getExpansionFactor();
}
