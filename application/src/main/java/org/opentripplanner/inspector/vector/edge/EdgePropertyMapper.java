package org.opentripplanner.inspector.vector.edge;

import static org.opentripplanner.inspector.vector.KeyValue.kv;
import static org.opentripplanner.utils.lang.DoubleUtils.roundTo2Decimals;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.apis.support.mapping.PropertyMapper;
import org.opentripplanner.inspector.vector.KeyValue;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.EscalatorEdge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.utils.collection.ListUtils;

public class EdgePropertyMapper extends PropertyMapper<Edge> {

  @Override
  protected Collection<KeyValue> map(Edge input) {
    var baseProps = List.of(kv("class", input.getClass().getSimpleName()));
    List<KeyValue> properties =
      switch (input) {
        case StreetEdge e -> mapStreetEdge(e);
        case EscalatorEdge e -> List.of(
          kv("distance", e.getDistanceMeters()),
          kv("duration", e.getDuration().map(d -> d.toString()).orElse(null))
        );
        default -> List.of();
      };
    return ListUtils.combine(baseProps, properties);
  }

  private static List<KeyValue> mapStreetEdge(StreetEdge se) {
    var props = Lists.newArrayList(
      kv("permission", streetPermissionAsString(se.getPermission())),
      kv("bicycleSafetyFactor", roundTo2Decimals(se.getBicycleSafetyFactor())),
      kv("walkSafetyFactor", roundTo2Decimals(se.getWalkSafetyFactor())),
      kv("noThruTraffic", noThruTrafficAsString(se)),
      kv("wheelchairAccessible", se.isWheelchairAccessible()),
      kv("maximumSlope", roundTo2Decimals(se.getMaxSlope()))
    );
    if (se.nameIsDerived()) {
      props.addFirst(kv("name", "%s (generated)".formatted(se.getName().toString())));
    } else {
      props.addFirst(kv("name", se.getName().toString()));
    }
    return props;
  }

  public static String streetPermissionAsString(StreetTraversalPermission permission) {
    return permission.name().replace("_AND_", " ");
  }

  private static String noThruTrafficAsString(StreetEdge se) {
    var noThruPermission = StreetTraversalPermission.NONE;
    if (se.isWalkNoThruTraffic()) {
      noThruPermission = noThruPermission.add(StreetTraversalPermission.PEDESTRIAN);
    }
    if (se.isBicycleNoThruTraffic()) {
      noThruPermission = noThruPermission.add(StreetTraversalPermission.BICYCLE);
    }
    if (se.isMotorVehicleNoThruTraffic()) {
      noThruPermission = noThruPermission.add(StreetTraversalPermission.CAR);
    }
    return streetPermissionAsString(noThruPermission);
  }
}
