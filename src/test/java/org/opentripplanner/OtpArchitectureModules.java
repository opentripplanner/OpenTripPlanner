package org.opentripplanner;

import org.opentripplanner._support.arch.Module;
import org.opentripplanner._support.arch.Package;

public interface OtpArchitectureModules {
  /* Third party libs*/

  Package GNU_TROVE = Package.of("gnu.trove.(*)..");
  Package JACKSON_ANNOTATIONS = Package.of("com.fasterxml.jackson.annotation");
  Package JTS_GEOM = Package.of("org.locationtech.jts.(*)..");
  Package GOOGLE_COLLECTIONS = Package.of("com.google.common.collect");

  /* OTP Modules */

  Package OTP_ROOT = Package.of("org.opentripplanner");
  Package FRAMEWORK = OTP_ROOT.subPackage("framework");
  Package UTIL = OTP_ROOT.subPackage("util");

  Module GEO_UTILS = Module.of(JTS_GEOM, FRAMEWORK.subPackage("geometry"));

  Package RAPTOR_ADAPTER = OTP_ROOT
    .subPackage("routing")
    .subPackage("algorithm")
    .subPackage("raptoradapter");
  Package RAPTOR_ADAPTER_API = RAPTOR_ADAPTER.subPackage("api");

  /**
   * This is a bag of TRUE util classes - no dependencies to other OTP classes or frameworks
   * (except true utilities like slf4j).
   */
  Module UTILS = Module.of(
    FRAMEWORK.subPackage("lang"),
    FRAMEWORK.subPackage("logging"),
    FRAMEWORK.subPackage("text"),
    FRAMEWORK.subPackage("time"),
    FRAMEWORK.subPackage("tostring")
  );

  Package TRANSIT = OTP_ROOT.subPackage("transit");
  Package TRANSIT_MODEL = TRANSIT.subPackage("model");

  /* The Raptor module */
  Package RAPTOR_ROOT = OTP_ROOT.subPackage("raptor");
  Package RAPTOR_API = RAPTOR_ROOT.subPackage("api.(*)..");
}
