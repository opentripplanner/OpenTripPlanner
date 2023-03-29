package org.opentripplanner.standalone.config.routerconfig;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_3;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.function.Function;
import org.opentripplanner.ext.ridehailing.RideHailingServiceParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.routerconfig.services.UberConfig;

public class RideHailingServicesConfig {

  private final Multimap<Type, Object> configList = ArrayListMultimap.create();

  public RideHailingServicesConfig(NodeAdapter rootAdapter) {
    rootAdapter
      .of("rideHailingServices")
      .since(V2_3)
      .summary("Configuration for interfaces to external ride hailing services like Uber.")
      .asObjects(it -> {
        Type type = it
          .of("type")
          .since(V2_3)
          .summary("The type of the service.")
          .asEnum(Type.class);
        var config = type.parseConfig(it);
        configList.put(type, config);
        // We do not care what we return here
        return config;
      });
  }

  public List<RideHailingServiceParameters> rideHailingServiceParameters() {
    return configList
      .values()
      .stream()
      .filter(RideHailingServiceParameters.class::isInstance)
      .map(RideHailingServiceParameters.class::cast)
      .toList();
  }

  public enum Type {
    UBER_CAR_HAILING(UberConfig::create);

    private final Function<NodeAdapter, ?> factory;

    Type(Function<NodeAdapter, ?> factory) {
      this.factory = factory;
    }

    Object parseConfig(NodeAdapter nodeAdapter) {
      return factory.apply(nodeAdapter);
    }
  }
}
