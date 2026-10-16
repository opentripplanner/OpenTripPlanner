package org.opentripplanner.transit.service;

import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.organization.Operator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Indexed access to the agencies and operators of the {@link TransitRepository}. The scheduled
 * routes, trips and patterns are indexed by
 * {@link org.opentripplanner.transit.repository.ScheduledTimetableData}.
 * <p>
 * For performance reasons these indexes are not part of the serialized state of the graph.
 * They are rebuilt at runtime after graph deserialization.
 */
class TransitRepositoryIndex {

  private static final Logger LOG = LoggerFactory.getLogger(TransitRepositoryIndex.class);

  private final Map<FeedScopedId, Agency> agencyForId = new HashMap<>();
  private final Map<FeedScopedId, Operator> operatorForId = new HashMap<>();

  TransitRepositoryIndex(TransitRepository transitRepository) {
    LOG.info("Timetable repository index init...");

    for (Agency agency : transitRepository.getAgencies()) {
      this.agencyForId.put(agency.getId(), agency);
    }

    for (Operator operator : transitRepository.getOperators()) {
      this.operatorForId.put(operator.getId(), operator);
    }

    LOG.info("Timetable repository index init complete.");
  }

  Agency getAgencyForId(FeedScopedId id) {
    return agencyForId.get(id);
  }

  Operator getOperatorForId(FeedScopedId operatorId) {
    return operatorForId.get(operatorId);
  }
}
