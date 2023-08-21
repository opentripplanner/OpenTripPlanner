package org.opentripplanner.graph_builder.module.stopconsolidation;

import static org.opentripplanner.transit.model.framework.FeedScopedId.parseId;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.TransitModel;

public class StopConsolidationModel {

  private static final IdPair TO_REPLACE = new IdPair(
    parseId("kcm:280"),
    parseId("commtrans:1079")
  );
  private final List<StopReplacement> replacements;

  public StopConsolidationModel(TransitModel transitModel) {
    this.replacements =
      Stream
        .of(TO_REPLACE)
        .map(r -> {
          var primaryStop = transitModel.getStopModel().getRegularStop(r.primary());
          Objects.requireNonNull(primaryStop, "No stop with id %s".formatted(r.secondary()));
          return new StopConsolidationModel.StopReplacement(primaryStop, r.secondary());
        })
        .toList();
  }

  public List<FeedScopedId> stopIdsToReplace() {
    return replacements.stream().map(StopReplacement::child).toList();
  }

  public List<StopReplacement> replacements() {
    return replacements;
  }

  public record IdPair(FeedScopedId primary, FeedScopedId secondary) {
    public IdPair {
      Objects.requireNonNull(primary);
      Objects.requireNonNull(secondary);
    }
  }

  public record StopReplacement(StopLocation primary, FeedScopedId child) {}
}
