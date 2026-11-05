/** Framework support for OTP's write-domain transactions (repository registry and update manager). */
module org.opentripplanner.core.framework {
  requires jakarta.inject;
  requires transitive org.opentripplanner.core.model;
  requires org.slf4j;
  requires static org.jspecify;

  exports org.opentripplanner.core.framework.transaction.configure;
}
