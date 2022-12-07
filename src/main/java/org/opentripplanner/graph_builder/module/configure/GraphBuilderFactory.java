package org.opentripplanner.graph_builder.module.configure;

import dagger.BindsInstance;
import dagger.Component;
import java.time.ZoneId;
import java.util.List;
import javax.annotation.Nullable;
import javax.inject.Singleton;
import org.opentripplanner.ext.dataoverlay.EdgeUpdaterModule;
import org.opentripplanner.ext.flex.FlexLocationsToStreetEdgesMapper;
import org.opentripplanner.ext.transferanalyzer.DirectTransferAnalyzer;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.graph_builder.GraphBuilderDataSources;
import org.opentripplanner.graph_builder.issue.report.DataImportIssuesToHTML;
import org.opentripplanner.graph_builder.module.DirectTransferGenerator;
import org.opentripplanner.graph_builder.module.GraphCoherencyCheckerModule;
import org.opentripplanner.graph_builder.module.OsmBoardingLocationsModule;
import org.opentripplanner.graph_builder.module.PruneNoThruIslands;
import org.opentripplanner.graph_builder.module.StreetLinkerModule;
import org.opentripplanner.graph_builder.module.TimeZoneAdjusterModule;
import org.opentripplanner.graph_builder.module.TripPatternNamer;
import org.opentripplanner.graph_builder.module.geometry.CalculateWorldEnvelopeModule;
import org.opentripplanner.graph_builder.module.map.BusRouteStreetMatcher;
import org.opentripplanner.graph_builder.module.ned.ElevationModule;
import org.opentripplanner.graph_builder.module.osm.OpenStreetMapModule;
import org.opentripplanner.gtfs.graphbuilder.GtfsModule;
import org.opentripplanner.netex.NetexModule;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.worldenvelope.service.WorldEnvelopeModel;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.transit.service.TransitModel;

@Singleton
@Component(modules = { GraphBuilderModules.class })
public interface GraphBuilderFactory {
  //DataImportIssueStore issueStore();
  GraphBuilder graphBuilder();
  OpenStreetMapModule openStreetMapModule();
  GtfsModule gtfsModule();
  NetexModule netexModule();
  TimeZoneAdjusterModule timeZoneAdjusterModule();
  TripPatternNamer tripPatternNamer();
  BusRouteStreetMatcher busRouteStreetMatcher();
  OsmBoardingLocationsModule osmBoardingLocationsModule();
  StreetLinkerModule streetLinkerModule();
  PruneNoThruIslands pruneNoThruIslands();
  List<ElevationModule> elevationModules();
  FlexLocationsToStreetEdgesMapper flexLocationsToStreetEdgesMapper();
  DirectTransferGenerator directTransferGenerator();
  DirectTransferAnalyzer directTransferAnalyzer();
  GraphCoherencyCheckerModule graphCoherencyCheckerModule();
  EdgeUpdaterModule dataOverlayFactory();
  DataImportIssuesToHTML dataImportIssuesToHTML();
  CalculateWorldEnvelopeModule calculateWorldEnvelopeModule();

  @Component.Builder
  interface Builder {
    @BindsInstance
    Builder config(BuildConfig config);

    @BindsInstance
    Builder graph(Graph graph);

    @BindsInstance
    Builder transitModel(TransitModel transitModel);

    @BindsInstance
    Builder worldEnvelopeModel(WorldEnvelopeModel worldEnvelopeModel);

    @BindsInstance
    Builder dataSources(GraphBuilderDataSources graphBuilderDataSources);

    @BindsInstance
    Builder timeZoneId(@Nullable ZoneId zoneId);

    GraphBuilderFactory build();
  }
}
