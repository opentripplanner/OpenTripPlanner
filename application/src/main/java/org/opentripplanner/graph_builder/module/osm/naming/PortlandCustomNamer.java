package org.opentripplanner.graph_builder.module.osm.naming;

import java.util.HashSet;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.graph_builder.module.osm.OsmDatabase;
import org.opentripplanner.graph_builder.module.osm.StreetEdgePair;
import org.opentripplanner.graph_builder.services.osm.EdgeNamer;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.street.model.edge.StreetEdge;

/**
 * These rules were developed in consultation with Grant Humphries, PJ Houser, and Mele Sax-Barnett.
 * They describe which sidewalks and paths in the Portland area should be specially designated in
 * the narrative.
 *
 * @author novalis
 */
public class PortlandCustomNamer implements EdgeNamer {

  public static String[] STREET_SUFFIXES = {
    "Avenue",
    "Street",
    "Drive",
    "Court",
    "Highway",
    "Lane",
    "Way",
    "Place",
    "Road",
    "Boulevard",
    "Alley",
  };

  public static String[] PATH_WORDS = {
    "Trail",
    "Trails",
    "Greenway",
    "Esplanade",
    "Spur",
    "Loop",
  };

  private final HashSet<StreetEdge> nameByOrigin = new HashSet<>();

  private final HashSet<StreetEdge> nameByDestination = new HashSet<>();

  @Override
  public I18NString name(OsmEntity entity) {
    var defaultName = entity.getAssumedName();
    if (!entity.hasTag("name")) {
      // this is already a generated name, so there's no need to add any
      // additional data
      return defaultName;
    }
    if (entity.isTag("footway", "sidewalk") || entity.isTag("path", "sidewalk")) {
      if (isStreet(defaultName.toString())) {
        return NonLocalizedString.ofNullable(sidewalk(defaultName.toString()));
      }
    }
    String highway = entity.getTag("highway");
    if ("footway".equals(highway) || "path".equals(highway) || "cycleway".equals(highway)) {
      if (!isObviouslyPath(defaultName.toString())) {
        return NonLocalizedString.ofNullable(path(defaultName.toString()));
      }
    }
    if ("pedestrian".equals(highway)) {
      return NonLocalizedString.ofNullable(pedestrianStreet(defaultName.toString()));
    }
    return defaultName;
  }

  @Override
  public void recordEdges(OsmWay way, StreetEdgePair edgePair, OsmDatabase osmdb) {
    final boolean isHighwayLink = isHighwayLink(way);
    final boolean isLowerLink = isLowerLink(way);
    edgePair
      .asIterable()
      .forEach(edge -> {
        if (!edge.nameIsDerived()) {
          return; // this edge already has a real name so there is nothing to do
        }
        if (isHighwayLink) {
          if (edge.isBack()) {
            nameByDestination.add(edge);
          } else {
            nameByOrigin.add(edge);
          }
        } else if (isLowerLink) {
          if (edge.isBack()) {
            nameByOrigin.add(edge);
          } else {
            nameByDestination.add(edge);
          }
        }
      });
  }

  @Override
  public void finalizeNames() {
    for (StreetEdge e : nameByOrigin) {
      nameAccordingToOrigin(e, 15);
    }
    for (StreetEdge e : nameByDestination) {
      nameAccordingToDestination(e, 15);
    }
  }

  private boolean isStreet(String defaultName) {
    for (String suffix : STREET_SUFFIXES) {
      if (defaultName.endsWith(suffix)) {
        return true;
      }
    }
    return false;
  }

  private boolean isObviouslyPath(String defaultName) {
    for (String word : PATH_WORDS) {
      if (defaultName.contains(word)) {
        return true;
      }
    }
    return false;
  }

  private String path(String name) {
    if (!name.toLowerCase().contains("path")) {
      name = name + " (path)".intern();
    }
    return name;
  }

  private String pedestrianStreet(String name) {
    if (!name.toLowerCase().contains("pedestrian street")) {
      name = name + " (pedestrian street)".intern();
    }
    return name;
  }

  private String sidewalk(String name) {
    if (!name.toLowerCase().contains("sidewalk")) {
      name = name + " (sidewalk)".intern();
    }
    return name;
  }

  private static String nameAccordingToDestination(StreetEdge e, int maxDepth) {
    if (maxDepth == 0) {
      return null;
    }
    for (StreetEdge out : e.getToVertex().getOutgoingStreetEdges()) {
      if (out.nameIsDerived()) {
        String name = nameAccordingToDestination(out, maxDepth - 1);
        if (name == null) {
          continue;
        }
        e.setName(new NonLocalizedString(name));
        return name;
      } else {
        String name = out.getDefaultName();
        e.setName(new NonLocalizedString(name));
        return name;
      }
    }
    return null;
  }

  private static String nameAccordingToOrigin(StreetEdge e, int maxDepth) {
    if (maxDepth == 0) {
      return null;
    }
    for (StreetEdge in : e.getFromVertex().getIncomingStreetEdges()) {
      if (in.nameIsDerived()) {
        String name = nameAccordingToOrigin(in, maxDepth - 1);
        if (name == null) {
          continue;
        }
        e.setName(new NonLocalizedString(name));
        return name;
      } else {
        String name = in.getDefaultName();
        e.setName(new NonLocalizedString(name));
        return name;
      }
    }
    return null;
  }

  private static boolean isHighwayLink(OsmWay way) {
    String highway = way.getTag("highway");
    return "motorway_link".equals(highway) || "trunk_link".equals(highway);
  }

  private static boolean isLowerLink(OsmWay way) {
    String highway = way.getTag("highway");
    return (
      "secondary_link".equals(highway) ||
      "primary_link".equals(highway) ||
      "tertiary_link".equals(highway)
    );
  }
}
