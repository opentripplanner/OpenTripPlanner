package org.opentripplanner.graph_builder.module.configure;

import dagger.BindsInstance;
import dagger.Component;
import jakarta.inject.Singleton;
import java.time.ZoneId;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.ext.dataoverlay.EdgeUpdaterModule;
import org.opentripplanner.ext.emissions.EmissionsDataModel;
import org.opentripplanner.ext.emissions.EmissionsModule;
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
@Component(modules = { GraphBuilderModules.class, OsmInfoGraphBuildServiceModule.class })
public interface GraphBuilderFactory {
  //DataImportIssueStore issueStore();
  GraphBuilder graphBuilder();
  OsmModule osmModule();
  GtfsModule gtfsModule();
  EmissionsModule emissionsModule();
  NetexModule netexModule();
  TimeZoneAdjusterModule timeZoneAdjusterModule();
  TripPatternNamer tripPatternNamer();
  OsmBoardingLocationsModule osmBoardingLocationsModule();
  StreetLinkerModule streetLinkerModule();
  PruneIslands pruneIslands();
  List<ElevationModule> elevationModules();
  AreaStopsToVerticesMapper areaStopsToVerticesMapper();
  DirectTransferGenerator directTransferGenerator();
  DirectTransferAnalyzer directTransferAnalyzer();
  GraphCoherencyCheckerModule graphCoherencyCheckerModule();
  EdgeUpdaterModule dataOverlayFactory();
  DataImportIssueReporter dataImportIssueReporter();
  CalculateWorldEnvelopeModule calculateWorldEnvelopeModule();
  StreetLimitationParameters streetLimitationParameters();

  @Nullable
  RouteToCentroidStationIdsValidator routeToCentroidStationIdValidator();

  @Nullable
  StopConsolidationModule stopConsolidationModule();

  @Nullable
  StopConsolidationRepository stopConsolidationRepository();

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
    Builder emissionsDataModel(@Nullable EmissionsDataModel emissionsDataModel);
  }
}
