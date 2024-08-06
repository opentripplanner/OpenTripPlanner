package org.opentripplanner.inspector.vector.edge;

import static org.opentripplanner.framework.lang.DoubleUtils.roundTo2Decimals;
import static org.opentripplanner.inspector.vector.KeyValue.kv;

import com.google.common.collect.Lists;
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
    var baseProps = List.of(kv("class", input.getClass().getSimpleName()));
    List<KeyValue> properties =
      switch (input) {
        case StreetEdge e -> mapStreetEdge(e);
        case EscalatorEdge e -> List.of(kv("distance", e.getDistanceMeters()));
        default -> List.of();
      };
    return ListUtils.combine(baseProps, properties);
  }

  private static List<KeyValue> mapStreetEdge(StreetEdge se) {
    var props = Lists.newArrayList(
      kv("permission", se.getPermission().toString()),
      kv("bicycleSafetyFactor", roundTo2Decimals(se.getBicycleSafetyFactor()))
    );
    if (se.hasBogusName()) {
      props.addFirst(kv("name", "%s (generated)".formatted(se.getName().toString())));
    } else {
      props.addFirst(kv("name", se.getName().toString()));
    }
    return props;
  }
}
