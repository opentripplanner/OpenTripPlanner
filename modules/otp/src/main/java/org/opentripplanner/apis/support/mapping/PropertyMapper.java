package org.opentripplanner.apis.support.mapping;

import edu.colorado.cires.cmg.mvt.VectorTile;
import edu.colorado.cires.cmg.mvt.adapt.jts.IUserDataConverter;
import edu.colorado.cires.cmg.mvt.adapt.jts.UserDataKeyValueMapConverter;
import edu.colorado.cires.cmg.mvt.build.MvtLayerProps;
import java.util.Collection;
import org.opentripplanner.inspector.vector.KeyValue;

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
