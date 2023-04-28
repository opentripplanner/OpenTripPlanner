package org.opentripplanner.netex.validation;

import java.util.function.Supplier;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.netex.index.NetexEntityIndex;
import org.opentripplanner.netex.index.hierarchy.AbstractHierarchicalMap;

/**
 * Validate input NeTEx entities, especially relations. The validation step is mainly there to
 * enable us to remove entities with “broken” references. This happens in real life, when the import
 * comes from more than one source (stops register & authority transit data) and these sources are
 * out of sync.
 * <p>
 * Make sure the order of the validation steps are inline with the data relations. For example, if A
 * reference B, any validation of the reference should be done AFTER validation steps on B. If B is
 * removed, then validating A -> B, should also remove A.
 */
public class Validator {

  private final NetexEntityIndex index;
  private final DataImportIssueStore issueStore;

  private Validator(NetexEntityIndex index, DataImportIssueStore issueStore) {
    this.index = index;
    this.issueStore = issueStore;
  }

  public static void validate(NetexEntityIndex index, DataImportIssueStore issueStore) {
    new Validator(index, issueStore).run();
  }

  private void run() {
    validate(index.quayIdByStopPointRef, new PassengerStopAssignmentQuayNotFound());
    validate(index.serviceJourneyById, new JourneyPatternNotFoundInSJ());
    validate(index.serviceJourneyById, new JourneyPatternSJMismatch());
    validate(index.serviceJourneyById, ServiceJourneyNonIncreasingPassingTime::new);
    validate(index.datedServiceJourneys, new DSJOperatingDayNotFound());
    validate(index.datedServiceJourneys, new DSJServiceJourneyNotFound());
  }

  /**
   * Validate a set of entities for a given stateless rule.
   */
  private <K, V> void validate(
    AbstractHierarchicalMap<K, V> map,
    AbstractHMapValidationRule<K, V> rule
  ) {
    rule.setup(index.readOnlyView());
    map.validate(rule, issueStore::add);
  }

  /**
   * Validate a set of entities for a given stateful rule.
   */
  private <K, V> void validate(
    AbstractHierarchicalMap<K, V> map,
    Supplier<AbstractHMapValidationRule<K, V>> ruleSupplier
  ) {
    map.validate(() -> ruleSupplier.get().setup(index.readOnlyView()), issueStore::add);
  }
}
