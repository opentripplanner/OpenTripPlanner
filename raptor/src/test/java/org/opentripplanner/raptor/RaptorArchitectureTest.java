package org.opentripplanner.raptor;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._support.arch.ArchComponent;
import org.opentripplanner.raptor._support.arch.Module;
import org.opentripplanner.raptor._support.arch.Package;

public class RaptorArchitectureTest {

  private static final Package OTP_ROOT = Package.of("org.opentripplanner");
  private static final Package GNU_TROVE = Package.of("gnu.trove..");
  private static final Package OTP_UTILS = OTP_ROOT.subPackage("utils..");

  /* The Raptor module, all packages that other paths of OTP may use. */
  private static final Package RAPTOR = OTP_ROOT.subPackage("raptor");
  private static final Package RAPTOR_API = RAPTOR.subPackage("api..");
  private static final Package API = RAPTOR.subPackage("api");
  private static final Package API_MODEL = API.subPackage("model");
  private static final Package API_PATH = API.subPackage("path");
  private static final Package RAPTOR_UTIL = RAPTOR.subPackage("util");
  private static final Package RAPTOR_UTIL_PARETO_SET = RAPTOR_UTIL.subPackage("paretoset");
  private static final Package RAPTOR_UTIL_COMPOSITE = RAPTOR_UTIL.subPackage("composite");
  private static final Module RAPTOR_UTILS = Module.of(
    RAPTOR_UTIL,
    RAPTOR_UTIL_PARETO_SET,
    RAPTOR_UTIL_COMPOSITE
  );
  private static final Package RAPTOR_SPI = RAPTOR.subPackage("spi");
  private static final Package RAPTOR_PATH = RAPTOR.subPackage("path");
  private static final Package CONFIGURE = RAPTOR.subPackage("configure");
  private static final Package SERVICE = RAPTOR.subPackage("service");
  private static final Package RANGE_RAPTOR = RAPTOR.subPackage("rangeraptor");
  private static final Package RR_INTERNAL_API = RANGE_RAPTOR.subPackage("internalapi");
  private static final Package RR_TRANSIT = RANGE_RAPTOR.subPackage("transit");
  private static final Package RR_SUPPORT = RANGE_RAPTOR.subPackage("support");
  private static final Package RR_PATH = RANGE_RAPTOR.subPackage("path");
  private static final Package RR_PATH_CONFIGURE = RR_PATH.subPackage("configure");
  private static final Package RR_DEBUG = RANGE_RAPTOR.subPackage("debug");
  private static final Package RR_LIFECYCLE = RANGE_RAPTOR.subPackage("lifecycle");
  private static final Package RR_MULTI_CRITERIA = RANGE_RAPTOR.subPackage("multicriteria");
  private static final Package RR_MC_CONFIGURE = RR_MULTI_CRITERIA.subPackage("configure");
  private static final Package RR_STANDARD = RANGE_RAPTOR.subPackage("standard");
  private static final Package RR_STD_CONFIGURE = RR_STANDARD.subPackage("configure");
  private static final Package RR_CONTEXT = RANGE_RAPTOR.subPackage("context");

  /**
   * Packages used by standard-range-raptor and multi-criteria-range-raptor.
   */
  private static final Module RR_SHARED_PACKAGES = Module.of(
    OTP_UTILS,
    GNU_TROVE,
    RAPTOR_API,
    RAPTOR_SPI,
    RAPTOR_UTILS,
    RR_INTERNAL_API,
    RR_DEBUG,
    RR_LIFECYCLE,
    RR_TRANSIT,
    RR_SUPPORT,
    RR_PATH
  );

  @Test
  void enforcePackageDependenciesRaptorAPI() {
    API_MODEL.dependsOn(OTP_UTILS).verify();
    API_PATH.dependsOn(OTP_UTILS, API_MODEL).verify();
    var debug = API.subPackage("debug").dependsOn(OTP_UTILS).verify();
    var view = API.subPackage("view").dependsOn(OTP_UTILS, API_MODEL).verify();
    var request = API.subPackage("request")
      .dependsOn(OTP_UTILS, debug, API_MODEL, API_PATH, view)
      .verify();
    API.subPackage("response").dependsOn(OTP_UTILS, API_MODEL, API_PATH, request).verify();
  }

  @Test
  void enforcePackageDependenciesRaptorSPI() {
    RAPTOR_SPI.dependsOn(OTP_UTILS, API_MODEL, API_PATH).verify();
  }

  @Test
  void enforcePackageDependenciesUtil() {
    RAPTOR_UTIL.dependsOn(OTP_UTILS, RAPTOR_SPI).verify();
    RAPTOR_UTIL_PARETO_SET.dependsOn(RAPTOR_UTIL_COMPOSITE).verify();
    RAPTOR_UTIL_COMPOSITE.verify();
  }

  @Test
  void enforcePackageDependenciesRaptorPath() {
    RAPTOR_PATH.dependsOn(OTP_UTILS, API_PATH, API_MODEL, RAPTOR_SPI, RR_TRANSIT).verify();
  }

  @Test
  void enforcePackageDependenciesInRangeRaptorSharedPackages() {
    RR_INTERNAL_API.dependsOn(OTP_UTILS, RAPTOR_API, RAPTOR_SPI).verify();
    RR_DEBUG.dependsOn(RR_SHARED_PACKAGES).verify();
    RR_LIFECYCLE.dependsOn(RR_SHARED_PACKAGES).verify();
    RR_TRANSIT.dependsOn(RR_SHARED_PACKAGES, RR_DEBUG, RR_LIFECYCLE).verify();
    RR_CONTEXT.dependsOn(
      RR_SHARED_PACKAGES,
      RR_DEBUG,
      RR_LIFECYCLE,
      RR_SUPPORT,
      RR_TRANSIT
    ).verify();
    RR_PATH.dependsOn(RR_SHARED_PACKAGES, RR_DEBUG, RR_TRANSIT, RAPTOR_PATH).verify();
    RR_PATH_CONFIGURE.dependsOn(RR_SHARED_PACKAGES, RR_CONTEXT, RR_PATH).verify();
    RANGE_RAPTOR.dependsOn(RR_SHARED_PACKAGES, RR_INTERNAL_API, RR_LIFECYCLE, RR_TRANSIT).verify();
  }

  @Test
  void enforcePackageDependenciesInStandardRangeRaptorImplementation() {
    var stdInternalApi = RR_STANDARD.subPackage("internalapi")
      .dependsOn(RAPTOR_API, RAPTOR_SPI, RR_INTERNAL_API)
      .verify();
    var stdBestTimes = RR_STANDARD.subPackage("besttimes")
      .dependsOn(RR_SHARED_PACKAGES, stdInternalApi)
      .verify();
    var stdStopArrivals = RR_STANDARD.subPackage("stoparrivals")
      .dependsOn(RR_SHARED_PACKAGES, stdInternalApi)
      .verify();
    var stdStopArrivalsView = stdStopArrivals
      .subPackage("view")
      .dependsOn(RR_SHARED_PACKAGES, stdStopArrivals)
      .verify();
    var stdStopArrivalsPath = stdStopArrivals
      .subPackage("path")
      .dependsOn(RR_SHARED_PACKAGES, stdInternalApi, stdStopArrivalsView)
      .verify();
    var stdDebug = RR_STANDARD.subPackage("debug")
      .dependsOn(RR_SHARED_PACKAGES, stdInternalApi, stdStopArrivalsView)
      .verify();

    var RR_STANDARD_HEURISTIC = RR_STANDARD.subPackage("heuristics")
      .dependsOn(RR_SHARED_PACKAGES, stdInternalApi, stdBestTimes)
      .verify();

    RR_STANDARD.dependsOn(RR_SHARED_PACKAGES, stdInternalApi, stdBestTimes).verify();

    RR_STD_CONFIGURE.dependsOn(
      RR_SHARED_PACKAGES,
      RR_CONTEXT,
      RR_PATH_CONFIGURE,
      stdInternalApi,
      stdBestTimes,
      stdStopArrivals,
      stdStopArrivalsView,
      stdStopArrivalsPath,
      stdDebug,
      RR_STANDARD_HEURISTIC,
      RR_STANDARD
    ).verify();
  }

  @Test
  @Disabled
  void enforcePackageDependenciesInMultiCriteriaImplementation() {
    var mcArrivals = RR_MULTI_CRITERIA.subPackage("arrivals")
      .dependsOn(RR_SHARED_PACKAGES)
      .verify();
    var mcArrivalsC1 = mcArrivals
      .subPackage("c1")
      .dependsOn(mcArrivals, RR_SHARED_PACKAGES)
      .verify();
    var mcRide = RR_MULTI_CRITERIA.subPackage("ride")
      .dependsOn(mcArrivals, RR_SHARED_PACKAGES)
      .verify();
    var mcRideC1 = mcRide
      .subPackage("c1")
      .dependsOn(mcArrivals, mcRide, RR_SHARED_PACKAGES)
      .verify();
    var mcHeuristics = RR_MULTI_CRITERIA.subPackage("heuristic")
      .dependsOn(RR_SHARED_PACKAGES, mcArrivals)
      .verify();
    RR_MULTI_CRITERIA.dependsOn(RR_SHARED_PACKAGES, mcArrivals, mcRide, mcHeuristics).verify();

    RR_MC_CONFIGURE.dependsOn(
      RR_SHARED_PACKAGES,
      RR_CONTEXT,
      RR_PATH_CONFIGURE,
      mcHeuristics,
      mcArrivals,
      mcArrivalsC1,
      mcRideC1,
      RR_MULTI_CRITERIA
    ).verify();
  }

  @Test
  void enforcePackageDependenciesInRaptorService() {
    SERVICE.dependsOn(
      OTP_UTILS,
      RAPTOR_API,
      RAPTOR_SPI,
      RAPTOR_UTIL,
      CONFIGURE,
      RR_INTERNAL_API,
      RR_TRANSIT,
      RANGE_RAPTOR
    ).verify();
  }

  @Test
  void enforcePackageDependenciesInConfigure() {
    CONFIGURE.dependsOn(
      OTP_UTILS,
      RAPTOR_API,
      RAPTOR_SPI,
      RANGE_RAPTOR,
      RR_INTERNAL_API,
      RR_TRANSIT,
      RR_CONTEXT,
      RR_STD_CONFIGURE,
      RR_MC_CONFIGURE
    ).verify();
  }

  @Test
  void enforceNoCyclicDependencies() {
    slices()
      .matching(RAPTOR.packageIdentifierAllSubPackages())
      .should()
      .beFreeOfCycles()
      .check(ArchComponent.OTP_CLASSES);
  }
}
