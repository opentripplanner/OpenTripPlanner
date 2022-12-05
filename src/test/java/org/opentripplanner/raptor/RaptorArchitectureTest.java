package org.opentripplanner.raptor;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.opentripplanner.OtpArchitectureModules.FRAMEWORK_UTILS;
import static org.opentripplanner.OtpArchitectureModules.GNU_TROVE;
import static org.opentripplanner.OtpArchitectureModules.OTP_ROOT;
import static org.opentripplanner.OtpArchitectureModules.RAPTOR_API;

import org.junit.jupiter.api.Test;
import org.opentripplanner._support.arch.ArchComponent;
import org.opentripplanner._support.arch.Module;
import org.opentripplanner._support.arch.Package;

public class RaptorArchitectureTest {

  /* The Raptor module, all packages that other paths of OTP may use. */
  private static final Package RAPTOR = OTP_ROOT.subPackage("raptor");
  private static final Package RAPTOR_UTIL = RAPTOR.subPackage("util");
  private static final Package RAPTOR_UTIL_PARETO_SET = RAPTOR_UTIL.subPackage("paretoset");
  private static final Module RAPTOR_UTILS = Module.of(RAPTOR_UTIL, RAPTOR_UTIL_PARETO_SET);
  private static final Package RAPTOR_SPI = RAPTOR.subPackage("spi");
  private static final Package CONFIGURE = RAPTOR.subPackage("configure");
  private static final Package SERVICE = RAPTOR.subPackage("service");
  private static final Package RANGE_RAPTOR = RAPTOR.subPackage("rangeraptor");
  private static final Package RR_INTERNAL_API = RANGE_RAPTOR.subPackage("internalapi");
  private static final Package RR_TRANSIT = RANGE_RAPTOR.subPackage("transit");
  private static final Package RR_MULTI_CRITERIA = RANGE_RAPTOR.subPackage("multicriteria");
  private static final Package RR_MC_CONFIGURE = RR_MULTI_CRITERIA.subPackage("configure");
  private static final Package RR_STANDARD = RANGE_RAPTOR.subPackage("standard");
  private static final Package RR_STD_CONFIGURE = RR_STANDARD.subPackage("configure");
  private static final Package RR_CONTEXT = RANGE_RAPTOR.subPackage("context");

  @Test
  void enforcePackageDependenciesRaptorAPI() {
    var api = RAPTOR.subPackage("api");
    var debug = api.subPackage("debug").dependsOn(FRAMEWORK_UTILS).verify();
    var spi = RAPTOR.subPackage("spi").dependsOn(FRAMEWORK_UTILS).verify();
    var view = api.subPackage("view").dependsOn(FRAMEWORK_UTILS, spi).verify();
    var path = api.subPackage("path").dependsOn(FRAMEWORK_UTILS, spi).verify();
    var request = api
      .subPackage("request")
      .dependsOn(FRAMEWORK_UTILS, debug, spi, path, view)
      .verify();
    api.subPackage("response").dependsOn(FRAMEWORK_UTILS, request, path, spi).verify();
  }

  @Test
  void enforcePackageDependenciesUtil() {
    RAPTOR_UTIL.dependsOn(FRAMEWORK_UTILS, RAPTOR_SPI).verify();
    RAPTOR_UTIL_PARETO_SET.verify();
  }

  @Test
  void enforcePackageDependenciesInRaptorImplementation() {
    var rr = RANGE_RAPTOR;

    var internalApi = RR_INTERNAL_API.dependsOn(RAPTOR_API, RAPTOR_SPI).verify();

    // RangeRaptor common allowed dependencies
    var common = Module.of(
      FRAMEWORK_UTILS,
      GNU_TROVE,
      RAPTOR_API,
      RAPTOR_SPI,
      RAPTOR_UTILS,
      internalApi
    );

    var debug = rr.subPackage("debug").dependsOn(common).verify();
    var lifecycle = rr.subPackage("lifecycle").dependsOn(common).verify();
    RR_TRANSIT.dependsOn(common, debug, lifecycle).verify();
    RR_CONTEXT.subPackage("context").dependsOn(common, RR_TRANSIT);
    var path = rr.subPackage("path").dependsOn(common, debug, RR_TRANSIT).verify();
    var pathConfigure = path.subPackage("configure").dependsOn(common, RR_CONTEXT, path);
    RANGE_RAPTOR.dependsOn(common, internalApi, lifecycle, RR_TRANSIT).verify();

    // Common packages
    var rrCommon = Module.of(common, debug, lifecycle, RR_TRANSIT, path);

    // Standard Range Raptor Implementation
    var stdInternalApi = RR_STANDARD
      .subPackage("internalapi")
      .dependsOn(RAPTOR_API, RAPTOR_SPI)
      .verify();
    var stdBestTimes = RR_STANDARD
      .subPackage("besttimes")
      .dependsOn(rrCommon, stdInternalApi)
      .verify();
    var stdStopArrivals = RR_STANDARD
      .subPackage("stoparrivals")
      .dependsOn(rrCommon, stdInternalApi)
      .verify();
    var stdStopArrivalsView = stdStopArrivals
      .subPackage("view")
      .dependsOn(rrCommon, stdStopArrivals)
      .verify();
    var stdStopArrivalsPath = stdStopArrivals
      .subPackage("path")
      .dependsOn(rrCommon, stdInternalApi, stdStopArrivalsView)
      .verify();
    var stdDebug = RR_STANDARD
      .subPackage("debug")
      .dependsOn(rrCommon, stdInternalApi, stdStopArrivalsView)
      .verify();

    var RR_STANDARD_HEURISTIC = RR_STANDARD
      .subPackage("heuristics")
      .dependsOn(rrCommon, stdInternalApi, stdBestTimes)
      .verify();

    RR_STANDARD.dependsOn(rrCommon, stdInternalApi, stdBestTimes).verify();

    RR_STD_CONFIGURE
      .dependsOn(
        rrCommon,
        RR_CONTEXT,
        pathConfigure,
        stdInternalApi,
        stdBestTimes,
        stdStopArrivals,
        stdStopArrivalsView,
        stdStopArrivalsPath,
        stdDebug,
        RR_STANDARD_HEURISTIC,
        RR_STANDARD
      )
      .verify();

    // Multi-Criteria Range Raptor Implementation
    var mcArrivals = RR_MULTI_CRITERIA.subPackage("arrivals").dependsOn(common).verify();
    var mcHeuristics = RR_MULTI_CRITERIA
      .subPackage("heuristic")
      .dependsOn(rrCommon, mcArrivals)
      .verify();
    RR_MULTI_CRITERIA.dependsOn(rrCommon, mcArrivals, mcHeuristics).verify();

    RR_MC_CONFIGURE
      .dependsOn(rrCommon, RR_CONTEXT, pathConfigure, mcHeuristics, RR_MULTI_CRITERIA)
      .verify();
  }

  @Test
  void enforcePackageDependenciesInRaptorService() {
    SERVICE
      .dependsOn(
        FRAMEWORK_UTILS,
        RAPTOR_API,
        RAPTOR_SPI,
        RAPTOR_UTIL,
        CONFIGURE,
        RR_INTERNAL_API,
        RR_TRANSIT
      )
      .verify();
  }

  @Test
  void enforcePackageDependenciesInConfigure() {
    CONFIGURE
      .dependsOn(
        RAPTOR_API,
        RAPTOR_SPI,
        RANGE_RAPTOR,
        RR_INTERNAL_API,
        RR_TRANSIT,
        RR_CONTEXT,
        RR_STD_CONFIGURE,
        RR_MC_CONFIGURE
      )
      .verify();
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
