package org.opentripplanner.ext.vectortiles;

import com.wdtinc.mapbox_vector_tile.VectorTile;
import com.wdtinc.mapbox_vector_tile.adapt.jts.IUserDataConverter;
import com.wdtinc.mapbox_vector_tile.build.MvtLayerProps;
import org.opentripplanner.common.model.T2;

import java.util.Collection;

public abstract class PropertyMapper<T> implements IUserDataConverter {
  protected abstract Collection<T2<String, Object>> map(T input);

  public void addTags(
      Object userData,
      MvtLayerProps layerProps,
      VectorTile.Tile.Feature.Builder featureBuilder
  ) {
    if(userData != null) {
      for (T2<String, Object> e : map((T) userData)) {
        final String key = e.first;
        final Object value = e.second;

        if(key != null && value != null) {
          final int valueIndex = layerProps.addValue(value);

          if(valueIndex >= 0) {
            featureBuilder.addTags(layerProps.addKey(key));
            featureBuilder.addTags(valueIndex);
          }
        }
      }

    }
  }
}
