package org.opentripplanner.ext.stopconsolidation;

import static org.opentripplanner.transit.model.framework.FeedScopedId.parseId;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.TransitModel;

@Singleton
public class StopConsolidationModel {

  private static final IdPair TO_REPLACE = new IdPair(
    parseId("kcm:280"),
    parseId("commtrans:1079")
  );
  private final TransitModel transitModel;

  @Inject
  public StopConsolidationModel(TransitModel transitModel) {
    this.transitModel = transitModel;
  }

  public List<FeedScopedId> stopIdsToReplace() {
    return replacements().stream().map(StopReplacement::child).toList();
  }

  public List<StopReplacement> replacements() {
    return Stream
      .of(TO_REPLACE)
      .map(r -> {
        var primaryStop = transitModel.getStopModel().getRegularStop(r.primary());
        Objects.requireNonNull(primaryStop, "No stop with id %s".formatted(r.secondary()));
        return new StopConsolidationModel.StopReplacement(primaryStop, r.secondary());
      })
      .toList();
  }

  public boolean isConsolidatedStop(StopLocation stop) {
    return TO_REPLACE.secondary.equals(stop.getId());
  }

  public I18NString agencySpecificName(Agency agency, StopLocation stop) {
    if (TO_REPLACE.secondary().equals(stop.getId())) {
      return I18NString.of("CONSOLIDATED STOP for agency %s".formatted(agency.getName()));
    } else {
      return stop.getName();
    }
  }

  public record IdPair(FeedScopedId primary, FeedScopedId secondary) {
    public IdPair {
      Objects.requireNonNull(primary);
      Objects.requireNonNull(secondary);
    }
  }

  public record StopReplacement(StopLocation primary, FeedScopedId child) {}
}
