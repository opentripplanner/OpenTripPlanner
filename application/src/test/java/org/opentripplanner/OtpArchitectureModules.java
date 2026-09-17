package org.opentripplanner;

import org.opentripplanner._support.arch.Module;
import org.opentripplanner._support.arch.Package;

public interface OtpArchitectureModules {
  /* Third party libs*/

  Package DAGGER = Package.of("dagger..");
  Package GNU_TROVE = Package.of("gnu.trove.(*)..");
  Package GOOGLE_COLLECTIONS = Package.of("com.google.common.collect");
  Package JACKSON_ANNOTATIONS = Package.of("com.fasterxml.jackson.annotation");
  Package JTS_GEOM = Package.of("org.locationtech.jts.(*)..");

  /* OTP Modules */

  Package OTP_ROOT = Package.of("org.opentripplanner");

  Package CORE = OTP_ROOT.subPackage("core");
  Package CORE_DOMAIN = CORE.subPackage("domain");
  Package CORE_DOMAIN_FRAMEWORK = CORE_DOMAIN.subPackage("framework");
  Package CORE_DOMAIN_FRAMEWORK_ALL = CORE_DOMAIN.subPackage("framework..");
  Package CORE_DOMAIN_MODEL = CORE_DOMAIN.subPackage("model");
  Package CORE_DOMAIN_MODEL_ALL = CORE_DOMAIN.subPackage("model..");
  Package UTILS_PACKAGE = OTP_ROOT.subPackage("utils");

  Package DATASTORE = OTP_ROOT.subPackage("datastore");
  Package FRAMEWORK = OTP_ROOT.subPackage("framework");
  Module GEO_UTILS = Module.of(JTS_GEOM, FRAMEWORK.subPackage("geometry"));
  Package RAPTOR_ADAPTER = OTP_ROOT.subPackage("routing")
    .subPackage("algorithm")
    .subPackage("raptoradapter");
  Package RAPTOR_ADAPTER_API = RAPTOR_ADAPTER.subPackage("api");
  Package TRANSIT = OTP_ROOT.subPackage("transit");
  Package TRANSIT_MODEL = TRANSIT.subPackage("model");
  Package GEOMETRY = OTP_ROOT.subPackage("street.geometry");

  /* The Raptor module */
  Package RAPTOR_ROOT = OTP_ROOT.subPackage("raptor");
  Package RAPTOR_SPI = RAPTOR_ROOT.subPackage("spi..");

  /**
   * This is a bag of TRUE util classes - no dependencies to other OTP classes or frameworks
   * (except true utilities like slf4j).
   */
  Module OTP_UTILS = Module.of(
    UTILS_PACKAGE.subPackage("collection"),
    UTILS_PACKAGE.subPackage("lang"),
    UTILS_PACKAGE.subPackage("logging"),
    UTILS_PACKAGE.subPackage("text"),
    UTILS_PACKAGE.subPackage("time"),
    UTILS_PACKAGE.subPackage("tostring")
  );

  Module FRAMEWORK_UTILS = Module.of(
    OTP_UTILS,
    CORE_DOMAIN_MODEL_ALL,
    CORE_DOMAIN_FRAMEWORK_ALL,
    FRAMEWORK.subPackage("application"),
    FRAMEWORK.subPackage("error"),
    FRAMEWORK.subPackage("i18n"),
    FRAMEWORK.subPackage("concurrent"),
    FRAMEWORK.subPackage("doc")
  );
}
