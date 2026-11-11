/**
 * Core domain primitives shared by all OTP modules: feed scoped ids, cost, distance, i18n strings,
 * accessibility and the transaction model.
 */
module org.opentripplanner.core.model {
  requires jakarta.inject;
  requires org.opentripplanner.utils;
  requires org.slf4j;
  requires static org.jspecify;

  exports org.opentripplanner.core.model.accessibility;
  exports org.opentripplanner.core.model.basic;
  exports org.opentripplanner.core.model.deduplicator;
  exports org.opentripplanner.core.model.di;
  exports org.opentripplanner.core.model.doc;
  exports org.opentripplanner.core.model.event;
  exports org.opentripplanner.core.model.i18n;
  exports org.opentripplanner.core.model.id;
  exports org.opentripplanner.core.model.resources;
  exports org.opentripplanner.core.model.time;
  exports org.opentripplanner.core.model.transaction;
}
