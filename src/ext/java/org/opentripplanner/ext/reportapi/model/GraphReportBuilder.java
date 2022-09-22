package org.opentripplanner.ext.reportapi.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opentripplanner.standalone.api.OtpServerRequestContext;

public class GraphReportBuilder {

  public static GraphStats build(OtpServerRequestContext context) {
    var transitService = context.transitService();
    var graph = context.graph();
    var constrainedTransfers = transitService.getTransferService().listAll();

    var constrainedTransfersByType = constrainedTransfers
      .stream()
      .collect(
        Collectors.groupingBy(transfer -> {
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
        })
      );

    var stopsByType = transitService
      .listStopLocations()
      .stream()
      .collect(
        Collectors.groupingBy(stop -> {
          var className = stop.getClass().getSimpleName();
          // lower case first letter
          return Character.toLowerCase(className.charAt(0)) + className.substring(1);
        })
      );

    Map<String, Integer> constrainedTransferCounts = countMapValues(constrainedTransfersByType);
    Map<String, Integer> stopCounts = countMapValues(stopsByType);

    return new GraphStats(
      new StreetStats(graph.countEdges(), graph.countVertices()),
      new TransitStats(
        stopCounts,
        transitService.getAllTripPatterns().size(),
        transitService.getAllRoutes().size(),
        new ConstrainedTransferStats(constrainedTransfers.size(), constrainedTransferCounts)
      )
    );
  }

  @Nonnull
  private static <T> Map<String, Integer> countMapValues(Map<String, List<T>> input) {
    Map<String, Integer> result = new HashMap<>();
    input.forEach((key, value) -> result.put(key, value.size()));
    return result;
  }

  record GraphStats(StreetStats street, TransitStats transit) {}

  record StreetStats(int edges, int vertices) {}

  record TransitStats(
    Map<String, Integer> stops,
    int patterns,
    int routes,
    ConstrainedTransferStats constrainedTransfers
  ) {}

  record ConstrainedTransferStats(int total, Map<String, Integer> types) {}
}
