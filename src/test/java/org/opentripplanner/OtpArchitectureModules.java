package org.opentripplanner;

import org.opentripplanner._support.arch.Module;
import org.opentripplanner._support.arch.Package;

public interface OtpArchitectureModules {
  Package OTP_ROOT = Package.of("org.opentripplanner");
  Package UTIL = OTP_ROOT.subPackage("util");
  /**
   * This is a bag of TRUE util classes - no dependencies to other OTP classes of frameworks.
   * The {@link #UTIL} packages needs cleanup, it contains model, framework and API classes.
   * The strategy is therefore to move the true util classes into sub packages, and then later
   * to move the reminding classes to the places they belong.
   */
  Module UTILS = Module.of(UTIL.subPackage("lang"), UTIL.subPackage("time"));

  Package TRANSIT = OTP_ROOT.subPackage("transit");
  Package TRANSIT_MODEL = TRANSIT.subPackage("model");
}
