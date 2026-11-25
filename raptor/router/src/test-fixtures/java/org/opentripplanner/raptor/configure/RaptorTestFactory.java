package org.opentripplanner.raptor.configure;

import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor.RaptorServiceFactory;
import org.opentripplanner.raptor.api.request.RaptorEnvironment;
import org.opentripplanner.raptor.api.request.RaptorTuningParameters;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

public class RaptorTestFactory {

  public static <T extends RaptorTripSchedule> RaptorService<T> raptorService() {
    return new RaptorServiceFactory<T>(
      new RaptorTuningParameters() {},
      new RaptorEnvironment() {}
    ).createService();
  }

  /** Internal config, for tests of Raptor internals that are not reachable through the service. */
  public static <T extends RaptorTripSchedule> RaptorConfig<T> configForTest() {
    return new RaptorConfig<>(new RaptorTuningParameters() {}, new RaptorEnvironment() {});
  }
}
