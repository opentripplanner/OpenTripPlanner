package org.opentripplanner.routing.graph;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.stream.Collectors;
import org.apache.commons.lang3.NotImplementedException;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.TemporaryFreeEdge;
import org.opentripplanner.street.model.edge.TemporaryPartialStreetEdge;
import org.opentripplanner.street.model.vertex.Vertex;

class StreetSummaries {

  private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat(
    "0.######",
    new DecimalFormatSymbols(Locale.ROOT)
  );

  static String summarizeEdge(Edge e) {
    return switch (e) {
      case TemporaryPartialStreetEdge tpe -> String.format(
        "%s → %s %s ♿%s",
        summarizeVertex(e.getFromVertex()),
        summarizeVertex(e.getToVertex()),
        tpe.getPermission(),
        summarizeBoolean(tpe.isWheelchairAccessible())
      );
      case TemporaryFreeEdge tfe -> String.format(
        "%s → %s ALL",
        summarizeVertex(e.getFromVertex()),
        summarizeVertex(e.getToVertex())
      );
      default -> throw new NotImplementedException(
        "No summary for edge " + e.getClass().getSimpleName()
      );
    };
  }

  static String summarizeVertex(Vertex v) {
    var buf = new StringBuilder();

    var coord = String.format(
      "(%s,%s)".formatted(DECIMAL_FORMAT.format(v.getLat()), DECIMAL_FORMAT.format(v.getLon()))
    );
    buf.append(coord);

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

  private static String summarizeBoolean(boolean b) {
    if (b) {
      return "✅";
    } else {
      return "❌";
    }
  }
}
