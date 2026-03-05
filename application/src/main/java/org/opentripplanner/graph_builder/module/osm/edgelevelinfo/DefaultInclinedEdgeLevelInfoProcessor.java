package org.opentripplanner.graph_builder.module.osm.edgelevelinfo;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.ContradictoryLevelAndInclineInfoForWay;
import org.opentripplanner.graph_builder.issues.CouldNotApplyMultiLevelInfoToWay;
import org.opentripplanner.graph_builder.module.osm.OsmDatabase;
import org.opentripplanner.osm.model.OsmLevel;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.service.streetdetails.StreetDetailsRepository;
import org.opentripplanner.service.streetdetails.model.InclinedEdgeLevelInfo;
import org.opentripplanner.service.streetdetails.model.Level;
import org.opentripplanner.service.streetdetails.model.VertexLevelInfo;
import org.opentripplanner.street.model.edge.Edge;

public class DefaultInclinedEdgeLevelInfoProcessor implements InclinedEdgeLevelInfoProcessor {

  private final DataImportIssueStore issueStore;
  private final StreetDetailsRepository streetDetailsRepository;
  private final OsmDatabase osmdb;

  public DefaultInclinedEdgeLevelInfoProcessor(
    DataImportIssueStore issueStore,
    StreetDetailsRepository streetDetailsRepository,
    OsmDatabase osmdb
  ) {
    this.issueStore = issueStore;
    this.streetDetailsRepository = streetDetailsRepository;
    this.osmdb = osmdb;
  }

  @Override
  public Optional<InclinedEdgeLevelInfo> findInclinedEdgeLevelInfo(OsmWay way) {
    var nodeRefs = way.getNodeRefs();
    long firstNodeRef = nodeRefs.get(0);
    long lastNodeRef = nodeRefs.get(nodeRefs.size() - 1);

    InclinedEdgeLevelInfo levelInfo = findLevelInfo(way, firstNodeRef, lastNodeRef);
    InclinedEdgeLevelInfo inclineInfo = findInclineInfo(way, firstNodeRef, lastNodeRef);

    if (
      levelInfo != null &&
      inclineInfo != null &&
      levelInfo.lowerVertexInfo().osmNodeId() != inclineInfo.lowerVertexInfo().osmNodeId()
    ) {
      issueStore.add(
        new ContradictoryLevelAndInclineInfoForWay(
          way,
          osmdb.getNode(firstNodeRef).getCoordinate(),
          osmdb.getNode(lastNodeRef).getCoordinate()
        )
      );
      // Default to level info in case of contradictory information. Ideally this should be from
      // the tag that is more reliable.
      return Optional.of(levelInfo);
    } else if (levelInfo != null) {
      return Optional.of(levelInfo);
    } else if (inclineInfo != null) {
      return Optional.of(inclineInfo);
    }
    return Optional.empty();
  }

  private InclinedEdgeLevelInfo findLevelInfo(OsmWay way, long firstNodeRef, long lastNodeRef) {
    List<OsmLevel> levels = osmdb.getLevelsForEntity(way);
    // This check also filters out the default level because the list size is 1 when the default
    // level is used.
    if (levels.size() == 2) {
      OsmLevel firstVertexOsmLevel = levels.get(0);
      OsmLevel lastVertexOsmLevel = levels.get(1);
      if (firstVertexOsmLevel.level() < lastVertexOsmLevel.level()) {
        return new InclinedEdgeLevelInfo(
          new VertexLevelInfo(
            new Level(firstVertexOsmLevel.level(), firstVertexOsmLevel.name()),
            firstNodeRef
          ),
          new VertexLevelInfo(
            new Level(lastVertexOsmLevel.level(), lastVertexOsmLevel.name()),
            lastNodeRef
          )
        );
      } else if (firstVertexOsmLevel.level() > lastVertexOsmLevel.level()) {
        return new InclinedEdgeLevelInfo(
          new VertexLevelInfo(
            new Level(lastVertexOsmLevel.level(), lastVertexOsmLevel.name()),
            lastNodeRef
          ),
          new VertexLevelInfo(
            new Level(firstVertexOsmLevel.level(), firstVertexOsmLevel.name()),
            firstNodeRef
          )
        );
      }
    }
    return null;
  }

  private InclinedEdgeLevelInfo findInclineInfo(OsmWay way, long firstNodeRef, long lastNodeRef) {
    if (way.isInclineUp()) {
      return new InclinedEdgeLevelInfo(
        new VertexLevelInfo(null, firstNodeRef),
        new VertexLevelInfo(null, lastNodeRef)
      );
    } else if (way.isInclineDown()) {
      return new InclinedEdgeLevelInfo(
        new VertexLevelInfo(null, lastNodeRef),
        new VertexLevelInfo(null, firstNodeRef)
      );
    }
    return null;
  }

  @Override
  public void storeLevelInfoForEdge(
    @Nullable Edge forwardEdge,
    @Nullable Edge backwardEdge,
    InclinedEdgeLevelInfo inclinedEdgeLevelInfo,
    OsmWay way
  ) {
    Edge edge = forwardEdge != null ? forwardEdge : backwardEdge;
    if (edge != null && inclinedEdgeLevelInfo.canBeAppliedToEdge(edge)) {
      if (forwardEdge != null) {
        streetDetailsRepository.addInclinedEdgeLevelInfo(forwardEdge, inclinedEdgeLevelInfo);
      }
      if (backwardEdge != null) {
        streetDetailsRepository.addInclinedEdgeLevelInfo(backwardEdge, inclinedEdgeLevelInfo);
      }
    } else {
      var nodeRefs = way.getNodeRefs();
      long firstNodeRef = nodeRefs.get(0);
      long lastNodeRef = nodeRefs.get(nodeRefs.size() - 1);
      issueStore.add(
        new CouldNotApplyMultiLevelInfoToWay(
          way,
          osmdb.getNode(firstNodeRef).getCoordinate(),
          osmdb.getNode(lastNodeRef).getCoordinate(),
          way.getNodeRefs().size()
        )
      );
    }
  }
}
