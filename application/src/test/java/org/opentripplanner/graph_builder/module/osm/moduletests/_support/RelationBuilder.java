package org.opentripplanner.graph_builder.module.osm.moduletests._support;

import org.opentripplanner.osm.model.OsmMemberType;
import org.opentripplanner.osm.model.OsmRelation;
import org.opentripplanner.osm.model.OsmRelationMember;

public class RelationBuilder {

  private final OsmRelation relation = new OsmRelation();

  public static RelationBuilder ofMultiPolygon() {
    var builder = new RelationBuilder();
    builder.relation.addTag("type", "multipolygon");
    builder.relation.addTag("highway", "pedestrian");
    return builder;
  }

  public static RelationBuilder ofTurnRestriction(String restrictionType) {
    var builder = new RelationBuilder();
    builder.relation.addTag("type", "restriction");
    builder.relation.addTag("restriction", restrictionType);
    return builder;
  }

  public RelationBuilder withWayMember(long id, String role) {
    return withMember(id, role, OsmMemberType.WAY);
  }

  public RelationBuilder withNodeMember(long id, String role) {
    return withMember(id, role, OsmMemberType.NODE);
  }

  public OsmRelation build() {
    return relation;
  }

  private RelationBuilder withMember(long id, String role, OsmMemberType osmMemberType) {
    var member = new OsmRelationMember();
    member.setRole(role);
    member.setType(osmMemberType);
    member.setRef(id);
    relation.addMember(member);
    return this;
  }
}
