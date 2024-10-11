package org.opentripplanner;

import org.opentripplanner._support.arch.Module;
import org.opentripplanner._support.arch.Package;

public interface OtpArchitectureModules {
  /* Third party libs*/

  Package DAGGER = Package.of("dagger..");
  Package GEO_JSON = Package.of("org.geojson..");
  Package GEO_TOOLS = Package.of("org.geotools..");
  Package GNU_TROVE = Package.of("gnu.trove.(*)..");
  Package GOOGLE_COLLECTIONS = Package.of("com.google.common.collect");
  Package JACKSON_ANNOTATIONS = Package.of("com.fasterxml.jackson.annotation");
  Package JTS_GEOM = Package.of("org.locationtech.jts.(*)..");
  Package OPEN_GIS = Package.of("org.geotools.api..");

  /* OTP Modules */

  Package OTP_ROOT = Package.of("org.opentripplanner");
  Package DATASTORE = OTP_ROOT.subPackage("datastore");
  Package FRAMEWORK = OTP_ROOT.subPackage("framework");
  Module GEO_UTILS = Module.of(JTS_GEOM, FRAMEWORK.subPackage("geometry"));
  Package RAPTOR_ADAPTER = OTP_ROOT
    .subPackage("routing")
    .subPackage("algorithm")
    .subPackage("raptoradapter");
  Package RAPTOR_ADAPTER_API = RAPTOR_ADAPTER.subPackage("api");
  Package TRANSIT = OTP_ROOT.subPackage("transit");
  Package TRANSIT_MODEL = TRANSIT.subPackage("model");

  /* The Raptor module */
  Package RAPTOR_ROOT = OTP_ROOT.subPackage("raptor");
  Package RAPTOR_API = RAPTOR_ROOT.subPackage("api..");

  /**
   * This is a bag of TRUE util classes - no dependencies to other OTP classes or frameworks
   * (except true utilities like slf4j).
   */
  Module FRAMEWORK_UTILS = Module.of(
    FRAMEWORK.subPackage("application"),
    FRAMEWORK.subPackage("error"),
    FRAMEWORK.subPackage("i18n"),
    FRAMEWORK.subPackage("lang"),
    FRAMEWORK.subPackage("logging"),
    FRAMEWORK.subPackage("text"),
    FRAMEWORK.subPackage("time"),
    FRAMEWORK.subPackage("tostring"),
    FRAMEWORK.subPackage("concurrent"),
    FRAMEWORK.subPackage("doc")
  );
}
