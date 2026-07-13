package org.opentripplanner.street.graph.summary;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.stream.Collectors;
import org.apache.commons.lang3.NotImplementedException;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetTransitEntityLink;
import org.opentripplanner.street.model.edge.TemporaryFreeEdge;
import org.opentripplanner.street.model.edge.VehicleParkingEdge;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.VehicleParkingEntranceVertex;
import org.opentripplanner.street.model.vertex.Vertex;

/// Converts edges and vertices to human-readable strings, useful for test assertions.
class StreetSummarizer {

  private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat(
    "0.######",
    new DecimalFormatSymbols(Locale.ROOT)
  );

  static String summarizeEdge(Edge e) {
    return switch (e) {
      case StreetEdge tpe -> String.format(
        "%s → %s %s ♿%s",
        summarizeVertex(e.getFromVertex()),
        summarizeVertex(e.getToVertex()),
        tpe.getPermission(),
        summarizeBoolean(tpe.isWheelchairAccessible())
      );
      case TemporaryFreeEdge _ -> String.format(
        "%s → %s ALL",
        summarizeVertex(e.getFromVertex()),
        summarizeVertex(e.getToVertex())
      );
      case StreetTransitEntityLink link -> String.format(
        "%s linked to %s",
        summarizeVertex(link.getFromVertex()),
        summarizeVertex(link.getToVertex())
      );
      case VehicleParkingEdge p -> String.format(
        "Parking %s → %s",
        summarizeVertex(p.getFromVertex()),
        summarizeVertex(p.getToVertex())
      );
      default -> throw new NotImplementedException(
        "No summary for edge " + e.getClass().getSimpleName()
      );
    };
  }

  static String summarizeVertex(Vertex v) {
    var buf = new StringBuilder();

    var coord = String.format("(%s,%s)".formatted(format(v.getLat()), format(v.getLon())));
    buf.append(coord);
    if (v instanceof TransitStopVertex tsv) {
      buf.append("[%s]".formatted(tsv.getId()));
    }
    if (v instanceof VehicleParkingEntranceVertex pev) {
      buf.append("[%s]".formatted(pev.getLabel()));
    }

    if (!v.areaStops().isEmpty()) {
      var ids = v
        .areaStops()
        .stream()
        .map(FeedScopedId::toString)
        .sorted()
        .collect(Collectors.joining(","));
      buf.append("[areaStops=").append(ids).append("]");
    }

    return buf.toString();
  }

  private static String format(double value) {
    var s = DECIMAL_FORMAT.format(value);
    return "-0".equals(s) ? "0" : s;
  }

  private static String summarizeBoolean(boolean b) {
    if (b) {
      return "✅";
    } else {
      return "❌";
    }
  }
}
