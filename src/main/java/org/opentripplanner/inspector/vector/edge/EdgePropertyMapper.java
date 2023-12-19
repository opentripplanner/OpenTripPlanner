package org.opentripplanner.inspector.vector.edge;

import java.util.Collection;
import java.util.List;
import org.opentripplanner.api.mapping.PropertyMapper;
import org.opentripplanner.inspector.vector.KeyValue;
import org.opentripplanner.street.model.edge.Edge;

/**
 * A {@link PropertyMapper} for the {@link EdgeLayerBuilder} for the OTP debug client.
 */
public class EdgePropertyMapper extends PropertyMapper<Edge> {

  @Override
  protected Collection<KeyValue> map(Edge edge) {
    return List.of(new KeyValue("java-class", edge.getClass().getSimpleName()));
  }
}
