package org.opentripplanner.osm.model;

public class RelationBuilder {

  private final OsmRelationBuilder builder = OsmRelation.of();

  public static RelationBuilder ofMultiPolygon() {
    var builder = new RelationBuilder();
    builder.withTag("type", "multipolygon");
    builder.withTag("highway", "pedestrian");
    return builder;
  }

  public static RelationBuilder ofTurnRestriction(String restrictionType) {
    return ofType("restriction").withTag("restriction", restrictionType);
  }

  public static RelationBuilder ofStopArea() {
    return ofType("public_transport").withTag("public_transport", "stop_area");
  }

  public static RelationBuilder ofType(String type) {
    var builder = new RelationBuilder();
    builder.builder.addTag("type", type);
    return builder;
  }

  public RelationBuilder withTag(String key, String value) {
    builder.addTag(key, value);
    return this;
  }

  public RelationBuilder withWayMember(long id, String role) {
    return withMember(id, role, OsmMemberType.WAY);
  }

  public RelationBuilder withNodeMember(long id) {
    return withMember(id, "", OsmMemberType.NODE);
  }

  public RelationBuilder withNodeMember(long id, String role) {
    return withMember(id, role, OsmMemberType.NODE);
  }

  public OsmRelation build() {
    return builder.build();
  }

  private RelationBuilder withMember(long id, String role, OsmMemberType osmMemberType) {
    var member = new OsmRelationMember();
    member.setRole(role);
    member.setType(osmMemberType);
    member.setRef(id);
    builder.addMember(member);
    return this;
  }
}
