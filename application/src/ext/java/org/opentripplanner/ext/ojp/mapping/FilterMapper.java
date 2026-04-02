package org.opentripplanner.ext.ojp.mapping;

import static java.lang.Boolean.TRUE;

import de.vdv.ojp20.LineDirectionFilterStructure;
import de.vdv.ojp20.OJPStopEventRequestStructure;
import de.vdv.ojp20.OJPTripRequestStructure;
import de.vdv.ojp20.OperatorFilterStructure;
import de.vdv.ojp20.StopEventParamStructure;
import de.vdv.ojp20.TripParamStructure;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.opentripplanner.api.model.transit.FeedScopedIdMapper;
import org.opentripplanner.core.model.id.FeedScopedId;

class FilterMapper {

  private final FeedScopedIdMapper idMapper;

  FilterMapper(FeedScopedIdMapper idMapper) {
    this.idMapper = idMapper;
  }

  List<FeedScopedId> includedAgencies(OJPTripRequestStructure tr) {
    return List.copyOf(filterAgencies(tr, o -> !isExclude(o.isExclude())));
  }

  Set<FeedScopedId> includedAgencies(OJPStopEventRequestStructure ser) {
    return filterAgencies(ser, o -> !isExclude(o.isExclude()));
  }

  List<FeedScopedId> excludedAgencies(OJPTripRequestStructure tr) {
    return List.copyOf(filterAgencies(tr, f -> isExclude(f.isExclude())));
  }

  Set<FeedScopedId> excludedAgencies(OJPStopEventRequestStructure ser) {
    return filterAgencies(ser, f -> isExclude(f.isExclude()));
  }

  Set<FeedScopedId> includedRoutes(OJPStopEventRequestStructure ser) {
    return filterLines(ser, o -> !isExclude(o.isExclude()));
  }

  List<FeedScopedId> includedRoutes(OJPTripRequestStructure ser) {
    return List.copyOf(filterLines(ser, o -> !isExclude(o.isExclude())));
  }

  Set<FeedScopedId> excludedRoutes(OJPStopEventRequestStructure ser) {
    return filterLines(ser, f -> isExclude(f.isExclude()));
  }

  List<FeedScopedId> excludedRoutes(OJPTripRequestStructure tr) {
    return List.copyOf(filterLines(tr, f -> isExclude(f.isExclude())));
  }

  // private methods

  private Set<FeedScopedId> filterAgencies(
    OJPTripRequestStructure tr,
    Predicate<OperatorFilterStructure> predicate
  ) {
    var filter = params(tr).map(t -> t.getOperatorFilter());
    return filterAgencies(filter, predicate);
  }

  private Set<FeedScopedId> filterAgencies(
    OJPStopEventRequestStructure ser,
    Predicate<OperatorFilterStructure> predicate
  ) {
    var filter = params(ser).map(p -> p.getOperatorFilter());
    return filterAgencies(filter, predicate);
  }

  private Set<FeedScopedId> filterLines(
    OJPStopEventRequestStructure ser,
    Predicate<LineDirectionFilterStructure> predicate
  ) {
    var filter = params(ser).map(p -> p.getLineFilter());
    return filterLines(filter, predicate);
  }

  private Set<FeedScopedId> filterLines(
    OJPTripRequestStructure ser,
    Predicate<LineDirectionFilterStructure> predicate
  ) {
    var filter = params(ser).map(p -> p.getLineFilter());
    return filterLines(filter, predicate);
  }

  private Set<FeedScopedId> filterLines(
    Optional<LineDirectionFilterStructure> lineDirectionFilterStructure,
    Predicate<LineDirectionFilterStructure> predicate
  ) {
    return lineDirectionFilterStructure
      .filter(predicate)
      .map(o -> o.getLine())
      .stream()
      .flatMap(r -> r.stream().map(l -> l.getLineRef().getValue()))
      .map(idMapper::parse)
      .collect(Collectors.toSet());
  }

  private Set<FeedScopedId> filterAgencies(
    Optional<OperatorFilterStructure> operatorFilterStructure,
    Predicate<OperatorFilterStructure> predicate
  ) {
    return operatorFilterStructure
      .filter(predicate)
      .map(o -> o.getOperatorRef())
      .stream()
      .flatMap(r -> r.stream().map(ref -> ref.getValue()))
      .map(idMapper::parse)
      .collect(Collectors.toSet());
  }

  private static Optional<TripParamStructure> params(OJPTripRequestStructure tr) {
    return Optional.ofNullable(tr.getParams());
  }

  private static Optional<StopEventParamStructure> params(OJPStopEventRequestStructure ser) {
    return Optional.ofNullable(ser.getParams());
  }

  private static boolean isExclude(Boolean b) {
    return b == null || TRUE.equals(b);
  }
}
