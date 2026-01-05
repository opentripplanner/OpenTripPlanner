package org.opentripplanner.ext.fares.service.gtfs.v2;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareTransferRule;

public class GtfsFaresV2ServiceBuilder {

  private List<FareLegRule> legRules = List.of();
  private List<FareTransferRule> fareTransferRules = List.of();
  private Multimap<FeedScopedId, FeedScopedId> stopAreas = ImmutableMultimap.of();
  private Multimap<FeedScopedId, LocalDate> serviceDatesForServiceId = HashMultimap.create();

  public GtfsFaresV2ServiceBuilder withLegRules(List<FareLegRule> legRules) {
    this.legRules = legRules;
    return this;
  }

  public GtfsFaresV2ServiceBuilder withLegRules(FareLegRule... legRules) {
    return withLegRules(Arrays.stream(legRules).toList());
  }

  public GtfsFaresV2ServiceBuilder withTransferRules(List<FareTransferRule> fareTransferRules) {
    this.fareTransferRules = fareTransferRules;
    return this;
  }

  public GtfsFaresV2ServiceBuilder withStopAreas(Multimap<FeedScopedId, FeedScopedId> stopAreas) {
    this.stopAreas = stopAreas;
    return this;
  }

  public GtfsFaresV2ServiceBuilder withServiceIds(
    Multimap<FeedScopedId, LocalDate> serviceDatesForServiceId
  ) {
    this.serviceDatesForServiceId = serviceDatesForServiceId;
    return this;
  }

  public GtfsFaresV2ServiceBuilder addServiceId(FeedScopedId serviceId, LocalDate... dates) {
    this.serviceDatesForServiceId.putAll(serviceId, List.of(dates));
    return this;
  }

  public GtfsFaresV2Service build() {
    return new GtfsFaresV2Service(legRules, fareTransferRules, stopAreas, serviceDatesForServiceId);
  }
}
