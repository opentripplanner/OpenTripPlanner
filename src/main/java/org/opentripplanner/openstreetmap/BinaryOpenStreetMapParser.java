package org.opentripplanner.openstreetmap;

import de.topobyte.osm4j.core.access.OsmHandler;
import de.topobyte.osm4j.core.access.OsmInputException;
import de.topobyte.osm4j.core.model.iface.EntityType;
import de.topobyte.osm4j.core.model.iface.OsmBounds;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmRelation;
import de.topobyte.osm4j.core.model.iface.OsmRelationMember;
import de.topobyte.osm4j.core.model.iface.OsmTag;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.pbf.seq.PbfReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.opentripplanner.graph_builder.module.osm.OSMDatabase;
import org.opentripplanner.openstreetmap.model.OSMNode;
import org.opentripplanner.openstreetmap.model.OSMNodeRef;
import org.opentripplanner.openstreetmap.model.OSMRelation;
import org.opentripplanner.openstreetmap.model.OSMRelationMember;
import org.opentripplanner.openstreetmap.model.OSMTag;
import org.opentripplanner.openstreetmap.model.OSMWay;

public class BinaryOpenStreetMapParser implements OsmHandler {

  OSMDatabase osmdb;
  private boolean parseWays = true;
  private boolean parseRelations = true;
  private boolean parseNodes = true;
  private InputStream inputStream;

  public BinaryOpenStreetMapParser(OSMDatabase osmdb) {
    this.osmdb = osmdb;
  }

  public void process(FileInputStream inputStream) throws OsmInputException {
    this.inputStream = inputStream;
    PbfReader reader = new PbfReader(inputStream, true);
    reader.setHandler(this);
    reader.read();
  }

  /**
   * Should relations be parsed
   */
  public void setParseWays(boolean parseWays) {
    this.parseWays = parseWays;
  }

  /**
   * Should relations be parsed
   */
  public void setParseRelations(boolean parseRelations) {
    this.parseRelations = parseRelations;
  }

  /**
   * Should nodes be parsed
   */
  public void setParseNodes(boolean parseNodes) {
    this.parseNodes = parseNodes;
  }

  @Override
  public void handle(OsmBounds osmBounds) throws IOException {

  }

  @Override
  public void handle(OsmNode osmNode) throws IOException {
    if (!parseNodes) {
      return;
    }
    OSMNode tmp = new OSMNode();
    tmp.setId(osmNode.getId());
    tmp.lat = osmNode.getLatitude();
    tmp.lon = osmNode.getLongitude();
    for (int i = 0; i < osmNode.getNumberOfTags(); i++) {
      OSMTag tag = new OSMTag();
      OsmTag osmTag = osmNode.getTag(i);
      tag.setK(osmTag.getKey());
      tag.setV(osmTag.getValue());
      tmp.addTag(tag);
    }

    osmdb.addNode(tmp);
  }

  @Override
  public void handle(OsmWay osmWay) throws IOException {
    if (!parseWays) {
      return;
    }

    OSMWay tmp = new OSMWay();
    tmp.setId(osmWay.getId());

    for (int index = 0; index < osmWay.getNumberOfTags(); index++) {
      OSMTag tag = new OSMTag();
      OsmTag osmTag = osmWay.getTag(index);

      tag.setK(osmTag.getKey());
      tag.setV(osmTag.getValue());
      tmp.addTag(tag);
    }

    for (int index = 0; index < osmWay.getNumberOfNodes(); index++) {
      OSMNodeRef nodeRef = new OSMNodeRef();
      nodeRef.setRef(osmWay.getNodeId(index));
      tmp.addNodeRef(nodeRef);
    }
    osmdb.addWay(tmp);
  }

  @Override
  public void handle(OsmRelation osmRelation) throws IOException {
    if (!parseRelations) {
      return;
    }
    OSMRelation tmp = new OSMRelation();
    tmp.setId(osmRelation.getId());

    for (int i = 0; i < osmRelation.getNumberOfTags(); i++) {
      OSMTag tag = new OSMTag();
      OsmTag osmTag = osmRelation.getTag(i);
      tag.setK(osmTag.getKey());
      tag.setV(osmTag.getValue());
      tmp.addTag(tag);
    }

    for (int i = 0; i < osmRelation.getNumberOfMembers(); i++) {
      OsmRelationMember m = osmRelation.getMember(i);
      OSMRelationMember member = new OSMRelationMember();
      setRelationMemberTypeString(member, m.getType());
      member.setRole(m.getRole());
      member.setRef(m.getId());
    }
  }

  public void complete() {
    // Jump in circles
    if (inputStream != null) {
      IOUtils.closeQuietly(inputStream);
    }
  }

  private void setRelationMemberTypeString(OSMRelationMember m, EntityType type) {
    switch (type) {
      case Way:
        m.setType("way");
        break;
      case Relation:
        m.setType("relation");
        break;
      case Node:
        m.setType("node");
        break;
      default:
        assert false;
    }
  }
}
