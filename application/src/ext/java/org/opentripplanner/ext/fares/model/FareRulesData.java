package org.opentripplanner.ext.fares.model;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.core.model.id.FeedScopedId;

public final class FareRulesData {

  private final List<FareAttribute> fareAttributes;
  private final List<FareRule> fareRules;
  private final List<FareLegRule> fareLegRules;
  private final List<FareTransferRule> fareTransferRules;
  private final Multimap<FeedScopedId, FeedScopedId> stopAreas;
  private final SetMultimap<FeedScopedId, LocalDate> serviceIdsToServiceDates;

  public FareRulesData(
    List<FareAttribute> fareAttributes,
    List<FareRule> fareRules,
    List<FareLegRule> fareLegRules,
    List<FareTransferRule> fareTransferRules,
    Multimap<FeedScopedId, FeedScopedId> stopAreas,
    SetMultimap<FeedScopedId, LocalDate> serviceIdsToServiceDates
  ) {
    this.fareAttributes = fareAttributes;
    this.fareRules = fareRules;
    this.fareLegRules = fareLegRules;
    this.fareTransferRules = fareTransferRules;
    this.stopAreas = stopAreas;
    this.serviceIdsToServiceDates = serviceIdsToServiceDates;
  }

  public FareRulesData() {
    this(
      new ArrayList<>(),
      new ArrayList<>(),
      new ArrayList<>(),
      new ArrayList<>(),
      ArrayListMultimap.create(),
      HashMultimap.create()
    );
  }

  /**
   * Return a mapping of those service ids (to service dates) for all timeframes that are contained
   * in this instance.
   */
  public Multimap<FeedScopedId, LocalDate> timeframeServiceIds() {
    Multimap<FeedScopedId, LocalDate> ret = HashMultimap.create();
    fareLegRules
      .stream()
      .flatMap(r -> r.listTimeframeServiceIds().stream())
      .forEach(sid -> ret.putAll(sid, serviceIdsToServiceDates.get(sid)));
    return ret;
  }

  public void putServiceIds(FeedScopedId sId, List<LocalDate> serviceDatesForServiceId) {
    serviceIdsToServiceDates.putAll(sId, serviceDatesForServiceId);
  }

  public List<FareAttribute> fareAttributes() {
    return fareAttributes;
  }

  public List<FareRule> fareRules() {
    return fareRules;
  }

  public List<FareLegRule> fareLegRules() {
    return fareLegRules;
  }

  public List<FareTransferRule> fareTransferRules() {
    return fareTransferRules;
  }

  public Multimap<FeedScopedId, FeedScopedId> stopAreas() {
    return stopAreas;
  }
}
