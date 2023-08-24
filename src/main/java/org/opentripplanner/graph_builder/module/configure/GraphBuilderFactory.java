package org.opentripplanner.graph_builder.module.configure;

import dagger.BindsInstance;
import dagger.Component;
import jakarta.inject.Singleton;
import java.time.ZoneId;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.ext.dataoverlay.EdgeUpdaterModule;
import org.opentripplanner.ext.flex.AreaStopsToVerticesMapper;
import org.opentripplanner.ext.stopconsolidation.StopConsolidationModule;
import org.opentripplanner.ext.transferanalyzer.DirectTransferAnalyzer;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.graph_builder.GraphBuilderDataSources;
import org.opentripplanner.graph_builder.issue.report.DataImportIssueReporter;
import org.opentripplanner.graph_builder.module.DirectTransferGenerator;
import org.opentripplanner.graph_builder.module.GraphCoherencyCheckerModule;
import org.opentripplanner.graph_builder.module.OsmBoardingLocationsModule;
import org.opentripplanner.graph_builder.module.StreetLinkerModule;
import org.opentripplanner.graph_builder.module.TimeZoneAdjusterModule;
import org.opentripplanner.graph_builder.module.TripPatternNamer;
import org.opentripplanner.graph_builder.module.geometry.CalculateWorldEnvelopeModule;
import org.opentripplanner.graph_builder.module.islandpruning.PruneIslands;
import org.opentripplanner.graph_builder.module.map.BusRouteStreetMatcher;
import org.opentripplanner.graph_builder.module.ned.ElevationModule;
import org.opentripplanner.graph_builder.module.osm.OsmModule;
import org.opentripplanner.gtfs.graphbuilder.GtfsModule;
import org.opentripplanner.netex.NetexModule;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.worldenvelope.WorldEnvelopeRepository;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.transit.service.TransitModel;

@Singleton
@Component(modules = { GraphBuilderModules.class })
public interface GraphBuilderFactory {
  //DataImportIssueStore issueStore();
  GraphBuilder graphBuilder();
  OsmModule osmModule();
  GtfsModule gtfsModule();
  NetexModule netexModule();
  TimeZoneAdjusterModule timeZoneAdjusterModule();
  TripPatternNamer tripPatternNamer();
  BusRouteStreetMatcher busRouteStreetMatcher();
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
  StopConsolidationModule stopConsolidator();

  @Component.Builder
  interface Builder {
    @BindsInstance
    Builder config(BuildConfig config);

    @BindsInstance
    Builder graph(Graph graph);

    @BindsInstance
    Builder transitModel(TransitModel transitModel);

    @BindsInstance
    Builder worldEnvelopeRepository(WorldEnvelopeRepository worldEnvelopeRepository);

    @BindsInstance
    Builder dataSources(GraphBuilderDataSources graphBuilderDataSources);

    @BindsInstance
    Builder timeZoneId(@Nullable ZoneId zoneId);

    GraphBuilderFactory build();
  }
}
