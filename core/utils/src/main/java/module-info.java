/**
 * Low-level utilities shared by all OTP modules. This module has no dependencies on other OTP
 * code.
 */
module org.opentripplanner.utils {
  requires static org.jspecify;

  exports org.opentripplanner.utils.collection;
  exports org.opentripplanner.utils.lang;
  exports org.opentripplanner.utils.logging;
  exports org.opentripplanner.utils.text;
  exports org.opentripplanner.utils.time;
  exports org.opentripplanner.utils.tostring;
}
