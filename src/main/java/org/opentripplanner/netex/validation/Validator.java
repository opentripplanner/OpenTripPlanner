package org.opentripplanner.netex.validation;

import org.opentripplanner.netex.index.NetexEntityIndex;
import org.opentripplanner.netex.index.hierarchy.AbstractHierarchicalMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Validate input data, especially relations.
 */
public class Validator {
  private static final Logger LOG = LoggerFactory.getLogger(Validator.class);

  private final NetexEntityIndex index;

  private Validator(NetexEntityIndex index) {
    this.index = index;
  }

  public static void validate(NetexEntityIndex index) {
    new Validator(index).run();
  }

  private void run() {
    validate(index.quayIdByStopPointRef, new PassengerStopAssignmentQuayNotFound());
    validate(index.serviceJourneyById, new JourneyPatternNotFoundInSJ());
    validate(index.serviceJourneyById, new JourneyPatternSJMismatch());
  }

  /**
   * Validate a set of entities for a given rule.
   */
  private <K,V> void validate(AbstractHierarchicalMap<K,V> map, AbstractHMapValidationRule<K,V> rule) {
    rule.setup(index.readOnlyView());

    // Do not use the lambda here, as the code ref (Class+line nr) will be odd
    //noinspection Convert2MethodRef
    map.validate(rule, m -> LOG.warn(m));
  }
}
