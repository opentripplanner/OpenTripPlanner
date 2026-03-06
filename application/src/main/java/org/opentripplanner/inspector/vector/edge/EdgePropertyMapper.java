package org.opentripplanner.inspector.vector.edge;

import static org.opentripplanner.inspector.vector.KeyValue.kv;
import static org.opentripplanner.street.model.StreetTraversalPermission.BICYCLE;
import static org.opentripplanner.street.search.TraverseMode.WALK;
import static org.opentripplanner.utils.lang.DoubleUtils.roundTo2Decimals;

import com.google.common.collect.Lists;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.apis.support.mapping.PropertyMapper;
import org.opentripplanner.inspector.vector.KeyValue;
import org.opentripplanner.service.streetdetails.StreetDetailsService;
import org.opentripplanner.service.streetdetails.model.InclinedEdgeLevelInfo;
import org.opentripplanner.service.streetdetails.model.Level;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.ElevatorAlightEdge;
import org.opentripplanner.street.model.edge.ElevatorBoardEdge;
import org.opentripplanner.street.model.edge.ElevatorHopEdge;
import org.opentripplanner.street.model.edge.EscalatorEdge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.OsmVertex;
import org.opentripplanner.utils.collection.ListUtils;

public class EdgePropertyMapper extends PropertyMapper<Edge> {

  private final StreetDetailsService streetDetailsService;

  public EdgePropertyMapper(StreetDetailsService streetDetailsService) {
    this.streetDetailsService = streetDetailsService;
  }

  @Override
  protected Collection<KeyValue> map(Edge input) {
    var baseProps = List.of(kv("class", input.getClass().getSimpleName()));
    List<KeyValue> properties = switch (input) {
      case StreetEdge e -> mapStreetEdge(e);
      case EscalatorEdge e -> mapEscalatorEdge(e);
      case ElevatorHopEdge e -> List.of(
        kv("permission", e.getPermission()),
        kv("levels", e.getLevels()),
        kv("wheelchairAccessible", e.isWheelchairAccessible()),
        kv("travelTime", e.getTravelTime().map(Duration::toString).orElse(null)),
        kv("fromVertexLabel", e.getFromVertex().getLabel().toString()),
        kv("toVertexLabel", e.getToVertex().getLabel().toString())
      );
      case ElevatorBoardEdge e -> List.of(
        kv(
          "levelValue",
          streetDetailsService
            .findHorizontalEdgeLevelInfo(e)
            .map(l -> l.level())
            .orElse(null)
        ),
        kv(
          "levelName",
          streetDetailsService
            .findHorizontalEdgeLevelInfo(e)
            .map(l -> l.name())
            .orElse(null)
        ),
        kv("fromVertexLabel", e.getFromVertex().getLabel().toString()),
        kv("toVertexLabel", e.getToVertex().getLabel().toString())
      );
      case ElevatorAlightEdge e -> List.of(
        kv("fromVertexLabel", e.getFromVertex().getLabel().toString()),
        kv("toVertexLabel", e.getToVertex().getLabel().toString())
      );
      default -> List.of();
    };
    return ListUtils.combine(baseProps, properties);
  }

  private List<KeyValue> mapEscalatorEdge(EscalatorEdge ee) {
    var props = Lists.newArrayList(
      kv("distance", ee.getDistanceMeters()),
      kv("duration", ee.getDuration().map(Duration::toString).orElse(null)),
      kv("fromVertexLabel", ee.getFromVertex().getLabel().toString()),
      kv("toVertexLabel", ee.getToVertex().getLabel().toString())
    );
    var inclinedEdgeLevelInfoOptional = streetDetailsService.findInclinedEdgeLevelInfo(ee);
    if (inclinedEdgeLevelInfoOptional.isPresent()) {
      addLevelInfo(props, ee, inclinedEdgeLevelInfoOptional.get());
    }
    return props;
  }

  private List<KeyValue> mapStreetEdge(StreetEdge se) {
    var props = Lists.newArrayList(
      kv("permission", streetPermissionAsString(se.getPermission())),
      kv("noThruTraffic", noThruTrafficAsString(se)),
      kv("wheelchairAccessible", se.isWheelchairAccessible()),
      kv("maximumSlope", roundTo2Decimals(se.getMaxSlope())),
      kv("fromVertexLabel", se.getFromVertex().getLabel().toString()),
      kv("toVertexLabel", se.getToVertex().getLabel().toString())
    );
    if (se.getPermission().allows(BICYCLE)) {
      props.add(kv("bicycleSafetyFactor", roundTo2Decimals(se.getBicycleSafetyFactor())));
    }
    if (se.getPermission().allows(WALK)) {
      props.add(kv("walkSafetyFactor", roundTo2Decimals(se.getWalkSafetyFactor())));
    }
    if (se.nameIsDerived()) {
      props.addFirst(kv("name", "%s (generated)".formatted(se.getName().toString())));
    } else {
      props.addFirst(kv("name", se.getName().toString()));
    }
    if (se.isStairs()) {
      props.add(kv("isStairs", true));
      var inclinedEdgeLevelInfoOptional = streetDetailsService.findInclinedEdgeLevelInfo(se);
      if (inclinedEdgeLevelInfoOptional.isPresent()) {
        addLevelInfo(props, se, inclinedEdgeLevelInfoOptional.get());
      }
    }
    return props;
  }

  private void addLevelInfo(
    ArrayList<KeyValue> props,
    Edge edge,
    InclinedEdgeLevelInfo inclinedEdgeLevelInfo
  ) {
    String lowerVertexLabel = edge.getToVertex().getLabel().toString();
    String upperVertexLabel = edge.getFromVertex().getLabel().toString();
    if (
      edge.getFromVertex() instanceof OsmVertex fromVertex &&
      fromVertex.nodeId() == inclinedEdgeLevelInfo.lowerVertexInfo().osmNodeId()
    ) {
      lowerVertexLabel = edge.getFromVertex().getLabel().toString();
      upperVertexLabel = edge.getToVertex().getLabel().toString();
    }

    props.add(kv("lowerVertexLabel", lowerVertexLabel));
    Level lowerLevel = inclinedEdgeLevelInfo.lowerVertexInfo().level();
    if (lowerLevel != null) {
      props.add(kv("lowerLevelValue", lowerLevel.level()));
      props.add(kv("lowerLevelName", lowerLevel.name()));
    }

    props.add(kv("upperVertexLabel", upperVertexLabel));
    Level upperLevel = inclinedEdgeLevelInfo.upperVertexInfo().level();
    if (upperLevel != null) {
      props.add(kv("upperLevelValue", upperLevel.level()));
      props.add(kv("upperLevelName", upperLevel.name()));
    }
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
      noThruPermission = noThruPermission.add(BICYCLE);
    }
    if (se.isMotorVehicleNoThruTraffic()) {
      noThruPermission = noThruPermission.add(StreetTraversalPermission.CAR);
    }
    return streetPermissionAsString(noThruPermission);
  }
}
