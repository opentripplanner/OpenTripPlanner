package org.opentripplanner.ext.carpooling.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.ext.carpooling.CarpoolingParameters;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.CarpoolingService;
import org.opentripplanner.ext.carpooling.corridor.CorridorBuilder;
import org.opentripplanner.ext.carpooling.corridor.RoutableCarpoolTripResolver;
import org.opentripplanner.ext.carpooling.corridor.TransitStopIndex;
import org.opentripplanner.ext.carpooling.internal.DefaultCarpoolingRepository;
import org.opentripplanner.ext.carpooling.internal.DefaultCarpoolingService;
import org.opentripplanner.ext.carpooling.street.CarReachableVertexSnapper;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.routing.linking.internal.VertexCreationService;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.service.StreetLimitationParametersService;

@Module
public class CarpoolingModule {

  @Provides
  @Singleton
  @Nullable
  public CarpoolingRepository provideCarpoolingRepository(
    @Nullable CarpoolingParameters parameters
  ) {
    if (OTPFeature.CarPooling.isOff()) {
      return null;
    }
    return new DefaultCarpoolingRepository(parameters.expirySweepInterval());
  }

  @Provides
  @Singleton
  @Nullable
  public static CarReachableVertexSnapper provideCarReachableVertexSnapper(
    @Nullable CarpoolingParameters parameters
  ) {
    if (OTPFeature.CarPooling.isOff()) {
      return null;
    }
    return new CarReachableVertexSnapper(parameters.minCarEscapeMeters());
  }

  @Provides
  @Singleton
  @Nullable
  public static CorridorBuilder provideCorridorBuilder(
    @Nullable TransitStopIndex stopIndex,
    StreetLimitationParametersService streetLimitationParametersService,
    @Nullable CarpoolingParameters parameters
  ) {
    if (OTPFeature.CarPooling.isOff()) {
      return null;
    }
    return new CorridorBuilder(
      stopIndex,
      streetLimitationParametersService,
      parameters.maxTripDuration()
    );
  }

  @Provides
  @Singleton
  @Nullable
  public static RoutableCarpoolTripResolver provideRoutableCarpoolTripResolver(
    VertexCreationService vertexCreationService,
    @Nullable CarReachableVertexSnapper carReachableVertexSnapper,
    @Nullable CorridorBuilder corridorBuilder,
    @Nullable CarpoolingParameters parameters
  ) {
    if (OTPFeature.CarPooling.isOff()) {
      return null;
    }
    return new RoutableCarpoolTripResolver(
      vertexCreationService,
      carReachableVertexSnapper,
      corridorBuilder,
      parameters.maxRoutePointSnap()
    );
  }

  @Provides
  @Singleton
  @Nullable
  public static CarpoolingParameters provideCarpoolingParameters(RouterConfig routerConfig) {
    if (OTPFeature.CarPooling.isOff()) {
      return null;
    }
    return routerConfig.carpoolingParameters();
  }

  @Provides
  @Singleton
  @Nullable
  public static TransitStopIndex provideTransitStopIndex(
    Graph graph,
    @Nullable CarReachableVertexSnapper carReachableVertexSnapper,
    @Nullable CarpoolingParameters parameters
  ) {
    if (OTPFeature.CarPooling.isOff()) {
      return null;
    }
    return new TransitStopIndex(graph, carReachableVertexSnapper, parameters.maxStopWalk());
  }

  @Provides
  @Singleton
  @Nullable
  public static CarpoolingService provideCarpoolingService(
    @Nullable CarpoolingRepository repository,
    StreetLimitationParametersService streetLimitationParametersService,
    VertexCreationService vertexCreationService,
    @Nullable CarReachableVertexSnapper carReachableVertexSnapper,
    @Nullable TransitStopIndex stopIndex,
    @Nullable CarpoolingParameters parameters
  ) {
    if (OTPFeature.CarPooling.isOff()) {
      return null;
    }
    return new DefaultCarpoolingService(
      repository,
      streetLimitationParametersService,
      vertexCreationService,
      carReachableVertexSnapper,
      stopIndex,
      parameters
    );
  }
}
