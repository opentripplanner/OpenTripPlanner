package org.opentripplanner.ext.reportapi.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.opentripplanner.standalone.api.OtpServerRequestContext;

public class GraphReportBuilder {

  public static GraphStats build(OtpServerRequestContext context) {
    var transitService = context.transitService();
    var graph = context.graph();
    var constrainedTransfers = transitService.getTransferService().listAll();

    var constrainedTransferCounts = countValues(constrainedTransfers, transfer -> {
      var transferConstraint = transfer.getTransferConstraint();
      if (transferConstraint.isMinTransferTimeSet()) {
        return "minTransferTime";
      } else if (transferConstraint.isStaySeated()) {
        return "staySeated";
      } else if (transferConstraint.isGuaranteed()) {
        return "guaranteed";
      } else if (transferConstraint.isNotAllowed()) {
        return "notAllowed";
      } else return "unknown";
    });

    var stopCounts = countValues(
      transitService.listStopLocations(),
      GraphReportBuilder::firstLetterToLowerCase
    );

    var edgeTypes = countValues(graph.getEdges(), GraphReportBuilder::firstLetterToLowerCase);
    var vertexTypes = countValues(graph.getVertices(), GraphReportBuilder::firstLetterToLowerCase);

    return new GraphStats(
      new StreetStats(edgeTypes, vertexTypes),
      new TransitStats(
        stopCounts,
        transitService.listTrips().size(),
        transitService.listTripPatterns().size(),
        transitService.listRoutes().size(),
        constrainedTransferCounts
      )
    );
  }

  private static String firstLetterToLowerCase(Object instance) {
    var className = instance.getClass().getSimpleName();
    return Character.toLowerCase(className.charAt(0)) + className.substring(1);
  }

  private static <T> TypeStats countValues(Collection<T> input, Function<T, String> classify) {
    Map<String, Integer> result = new HashMap<>();
    input.forEach(item -> {
      var classification = classify.apply(item);
      var count = result.getOrDefault(classification, 0);
      result.put(classification, ++count);
    });

    return new TypeStats(input.size(), result);
  }

  public record GraphStats(StreetStats street, TransitStats transit) {}

  record StreetStats(TypeStats edges, TypeStats vertices) {}

  record TransitStats(
    TypeStats stops,
    int trips,
    int tripPatterns,
    int routes,
    TypeStats constrainedTransfers
  ) {}

  record TypeStats(int total, Map<String, Integer> types) {}
}
