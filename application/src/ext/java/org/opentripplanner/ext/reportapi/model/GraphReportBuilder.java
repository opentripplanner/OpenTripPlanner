package org.opentripplanner.ext.reportapi.model;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transit.service.TransitService;

public class GraphReportBuilder {

  public static GraphStats build(TransitService transitService, Graph graph) {
    var constrainedTransfers = transitService.getConstrainedTransferService().listAll();

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
      } else {
        return "unknown";
      }
    });

    var stopCounts = countValues(
      transitService.listStopLocations(),
      GraphReportBuilder::firstLetterToLowerCase
    );

    var edgeTypes = countValues(graph.listEdges(), GraphReportBuilder::firstLetterToLowerCase);
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

  private static <T> TypeStats countValues(Iterable<T> input, Function<T, String> classify) {
    Map<String, Integer> result = new HashMap<>();
    int total = 0;
    for (T item : input) {
      var classification = classify.apply(item);
      var count = result.getOrDefault(classification, 0);
      result.put(classification, ++count);
      ++total;
    }
    return new TypeStats(total, result);
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
