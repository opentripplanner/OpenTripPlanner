package org.opentripplanner.graph_builder.module.configure;

import dagger.BindsInstance;
import dagger.Component;
import jakarta.inject.Singleton;
import java.time.ZoneId;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.ext.dataoverlay.EdgeUpdaterModule;
import org.opentripplanner.ext.emission.EmissionRepository;
import org.opentripplanner.ext.emission.configure.EmissionGraphBuilderModule;
import org.opentripplanner.ext.emission.internal.graphbuilder.EmissionGraphBuilder;
import org.opentripplanner.ext.flex.AreaStopsToVerticesMapper;
import org.opentripplanner.ext.stopconsolidation.StopConsolidationModule;
import org.opentripplanner.ext.stopconsolidation.StopConsolidationRepository;
import org.opentripplanner.ext.transferanalyzer.DirectTransferAnalyzer;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.graph_builder.GraphBuilderDataSources;
import org.opentripplanner.graph_builder.issue.report.DataImportIssueReporter;
import org.opentripplanner.graph_builder.module.DirectTransferGenerator;
import org.opentripplanner.graph_builder.module.GraphCoherencyCheckerModule;
import org.opentripplanner.graph_builder.module.OsmBoardingLocationsModule;
import org.opentripplanner.graph_builder.module.RouteToCentroidStationIdsValidator;
import org.opentripplanner.graph_builder.module.StreetLinkerModule;
import org.opentripplanner.graph_builder.module.TimeZoneAdjusterModule;
import org.opentripplanner.graph_builder.module.TripPatternNamer;
import org.opentripplanner.graph_builder.module.geometry.CalculateWorldEnvelopeModule;
import org.opentripplanner.graph_builder.module.islandpruning.PruneIslands;
import org.opentripplanner.graph_builder.module.ned.ElevationModule;
import org.opentripplanner.graph_builder.module.osm.OsmModule;
import org.opentripplanner.gtfs.graphbuilder.GtfsModule;
import org.opentripplanner.netex.NetexModule;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.osminfo.OsmInfoGraphBuildRepository;
import org.opentripplanner.service.osminfo.configure.OsmInfoGraphBuildServiceModule;
import org.opentripplanner.service.vehicleparking.VehicleParkingRepository;
import org.opentripplanner.service.worldenvelope.WorldEnvelopeRepository;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.street.model.StreetLimitationParameters;
import org.opentripplanner.transit.service.TimetableRepository;

@Singleton
@Component(
  modules = {
    GraphBuilderModules.class,
    OsmInfoGraphBuildServiceModule.class,
    EmissionGraphBuilderModule.class,
  }
)
public interface GraphBuilderFactory {
  AreaStopsToVerticesMapper areaStopsToVerticesMapper();
  CalculateWorldEnvelopeModule calculateWorldEnvelopeModule();
  DataImportIssueReporter dataImportIssueReporter();
  DirectTransferGenerator directTransferGenerator();
  DirectTransferAnalyzer directTransferAnalyzer();
  GraphCoherencyCheckerModule graphCoherencyCheckerModule();
  GraphBuilder graphBuilder();
  GtfsModule gtfsModule();
  List<ElevationModule> elevationModules();
  NetexModule netexModule();
  OsmBoardingLocationsModule osmBoardingLocationsModule();
  OsmModule osmModule();
  PruneIslands pruneIslands();
  StreetLinkerModule streetLinkerModule();
  TimeZoneAdjusterModule timeZoneAdjusterModule();
  TripPatternNamer tripPatternNamer();

  @Nullable
  EdgeUpdaterModule dataOverlayFactory();

  @Nullable
  EmissionGraphBuilder emissionGraphBuilder();

  @Nullable
  RouteToCentroidStationIdsValidator routeToCentroidStationIdValidator();

  @Nullable
  StopConsolidationModule stopConsolidationModule();

  @Component.Builder
  interface Builder {
    @BindsInstance
    Builder config(BuildConfig config);

    @BindsInstance
    Builder graph(Graph graph);

    @BindsInstance
    Builder timetableRepository(TimetableRepository timetableRepository);

    @BindsInstance
    Builder osmInfoGraphBuildRepository(OsmInfoGraphBuildRepository osmInfoGraphBuildRepository);

    @BindsInstance
    Builder worldEnvelopeRepository(WorldEnvelopeRepository worldEnvelopeRepository);

    @BindsInstance
    Builder stopConsolidationRepository(
      @Nullable StopConsolidationRepository stopConsolidationRepository
    );

    @BindsInstance
    Builder vehicleParkingRepository(VehicleParkingRepository parkingRepository);

    @BindsInstance
    Builder streetLimitationParameters(StreetLimitationParameters streetLimitationParameters);

    @BindsInstance
    Builder dataSources(GraphBuilderDataSources graphBuilderDataSources);

    @BindsInstance
    Builder timeZoneId(@Nullable ZoneId zoneId);

    GraphBuilderFactory build();

    @BindsInstance
    Builder emissionRepository(@Nullable EmissionRepository emissionRepository);
  }
}
