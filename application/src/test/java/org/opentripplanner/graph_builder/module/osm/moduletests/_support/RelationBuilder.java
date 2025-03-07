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

  public RelationBuilder withWayMember(long id, String role) {
    var member = new OsmRelationMember();
    member.setRole(role);
    member.setType(OsmMemberType.WAY);
    member.setRef(id);
    relation.addMember(member);
    return this;
  }

  public OsmRelation build() {
    return relation;
  }
}
