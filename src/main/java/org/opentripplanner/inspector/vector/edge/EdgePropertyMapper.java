package org.opentripplanner.inspector.vector.edge;

import static org.opentripplanner.framework.lang.DoubleUtils.roundTo2Decimals;
import static org.opentripplanner.inspector.vector.KeyValue.kv;

import java.util.Collection;
import java.util.List;
import org.opentripplanner.apis.support.mapping.PropertyMapper;
import org.opentripplanner.framework.collection.ListUtils;
import org.opentripplanner.inspector.vector.KeyValue;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.EscalatorEdge;
import org.opentripplanner.street.model.edge.StreetEdge;

public class EdgePropertyMapper extends PropertyMapper<Edge> {

  @Override
  protected Collection<KeyValue> map(Edge input) {
    List<KeyValue> baseProps = List.of(kv("class", input.getClass().getSimpleName()));
    List<KeyValue> properties =
      switch (input) {
        case StreetEdge e -> List.of(
          kv("permission", e.getPermission().toString()),
          kv("bicycleSafetyFactor", roundTo2Decimals(e.getBicycleSafetyFactor()))
        );
        case EscalatorEdge e -> List.of(kv("distance", e.getDistanceMeters()));
        default -> List.of();
      };
    return ListUtils.combine(baseProps, properties);
  }
}
