package org.opentripplanner.ext.vectortiles;

import com.wdtinc.mapbox_vector_tile.VectorTile;
import com.wdtinc.mapbox_vector_tile.adapt.jts.IUserDataConverter;
import com.wdtinc.mapbox_vector_tile.adapt.jts.UserDataKeyValueMapConverter;
import com.wdtinc.mapbox_vector_tile.build.MvtLayerProps;
import java.util.Collection;

/**
 * This class is used for adding data for each object in the vector layer from the userData in the
 * geometry.
 *
 * @param <T> is type of userData in the geometry.
 * @see UserDataKeyValueMapConverter
 */
public abstract class PropertyMapper<T> implements IUserDataConverter {

  public void addTags(
    Object userData,
    MvtLayerProps layerProps,
    VectorTile.Tile.Feature.Builder featureBuilder
  ) {
    if (userData != null) {
      for (var e : map((T) userData)) {
        if (e.key() != null && e.value() != null) {
          final int valueIndex = layerProps.addValue(e.value());

          if (valueIndex >= 0) {
            featureBuilder.addTags(layerProps.addKey(e.key()));
            featureBuilder.addTags(valueIndex);
          }
        }
      }
    }
  }

  /**
   * The return type is to allow null values.
   */
  protected abstract Collection<KeyValue> map(T input);
}
