/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.graph_builder.impl.osm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.GraphBuilderUtils;
import org.opentripplanner.graph_builder.model.osm.*;
import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.graph_builder.services.StreetUtils;
import org.opentripplanner.graph_builder.services.TurnRestriction;
import org.opentripplanner.graph_builder.services.TurnRestrictionType;
import org.opentripplanner.graph_builder.services.osm.OpenStreetMapContentHandler;
import org.opentripplanner.graph_builder.services.osm.OpenStreetMapProvider;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.EndpointVertex;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.impl.DistanceLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

/**
 * Builds a street graph from OpenStreetMap data.
 * 
 */
public class OpenStreetMapGraphBuilderImpl implements GraphBuilder {

    private static Logger _log = LoggerFactory.getLogger(OpenStreetMapGraphBuilderImpl.class);

    private List<OpenStreetMapProvider> _providers = new ArrayList<OpenStreetMapProvider>();

    private Map<Object, Object> _uniques = new HashMap<Object, Object>();

    private Map<String, KeyValuePermission> _tagPermissions = new LinkedHashMap<String, KeyValuePermission>();
    private Map<List<OSMKeyValue>, String>  _creativeNaming = new LinkedHashMap<List<OSMKeyValue>, String>();

    private class OSMKeyValue {
        public String key;
        public String value;
        public boolean wildcard;

        public OSMKeyValue(String key, String value, boolean wildcard) {
            this.key        = key;
            this.value      = value;
            this.wildcard   = wildcard;
        }

        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null || !(obj instanceof OSMKeyValue))
                return false;

            OSMKeyValue other = (OSMKeyValue) obj;

            if(this.wildcard != other.wildcard)
                return false;

            if(this.wildcard == true && this.key.equals(other.key))
                return true;

            return this.key.equals(other.key) && this.value.equals(other.value);
        }

        public int hashCode() {
            if(wildcard) {
                return key.hashCode();
            }
            return key.hashCode() ^ value.hashCode();
        }
    };

    private HashMap<P2<String>, P2<Double>> safetyFeatures = new HashMap<P2<String>, P2<Double>>();

    private HashSet<P2<String>> _slopeOverrideTags = new HashSet<P2<String>>();

    private class KeyValuePermission {
        public String key;

        public String value;

        public StreetTraversalPermission permission;

        public KeyValuePermission(String key, String value, StreetTraversalPermission permission) {
            this.key = key;
            this.value = value;
            this.permission = permission;
        }
    };

    /**
     * The source for OSM map data
     */
    public void setProvider(OpenStreetMapProvider provider) {
        _providers.add(provider);
    }

    /**
     * Multiple sources for OSM map data
     */
    public void setProviders(List<OpenStreetMapProvider> providers) {
        _providers.addAll(providers);
    }

    /**
     * The set of traversal permissions for a given set of tags.
     * 
     * @param provider
     */
    public void setDefaultAccessPermissions(LinkedHashMap<String, StreetTraversalPermission> mappy) {
        for (String tag : mappy.keySet()) {
            int ch_eq = tag.indexOf("=");

            if (ch_eq < 0) {
                _tagPermissions.put(tag, new KeyValuePermission(null, null, mappy.get(tag)));
            } else {
                String key = tag.substring(0, ch_eq), value = tag.substring(ch_eq + 1);

                _tagPermissions.put(tag, new KeyValuePermission(key, value, mappy.get(tag)));
            }
        }
        if (!_tagPermissions.containsKey("__default__")) {
            _log.warn("No default permissions for osm tags...");
        }
    }
    
    /**
     * Set the traversal permissions from a {@link StreetTraversalPermissionsSource} source.
     * 
     * @param source the permissions source
     */
    public void setDefaultAccessPermissionsSource(StreetTraversalPermissionsSource source) {
        LinkedHashMap<String, StreetTraversalPermission> permisions = new LinkedHashMap<String,StreetTraversalPermission>(source.getPermissions());
        setDefaultAccessPermissions(permisions);
    }

    /**
     * Streets where the slope is assumed to be flat because the underlying topographic
     * data cannot be trusted
     * 
     * @param features a list of osm attributes in the form key=value
     */
    public void setSlopeOverride(List<String> features) {
        for (String tag : features) {
            int ch_eq = tag.indexOf("=");

            if (ch_eq >= 0) {
                String key = tag.substring(0, ch_eq), value = tag.substring(ch_eq + 1);
                _slopeOverrideTags.add(new P2<String>(key, value));
            } 
        }
    }

    public void setCreativeNaming(LinkedHashMap<String, String> mappy) {
        for(String taglist : mappy.keySet()) {
            List<OSMKeyValue> vals = new ArrayList<OSMKeyValue>();

            for(String tag : taglist.split(";")) {
                int ch_eq = tag.indexOf("=");

                if(ch_eq < 0) {
                    _log.warn("Missing equal sign: " + taglist + " >> " + tag);
                } else {
                    String key   = tag.substring(0, ch_eq),
                           value = tag.substring(ch_eq + 1);

                    if(value.equals("")) {
                        vals.add(new OSMKeyValue(key, null, true));
                    } else {
                        vals.add(new OSMKeyValue(key, value, false));
                    }
                }
            }
            _creativeNaming.put(vals, mappy.get(taglist));
        }
    }

    @Override
    public void buildGraph(Graph graph) {
        Handler handler = new Handler();
        for (OpenStreetMapProvider provider : _providers) {
            _log.debug("gathering osm from provider: " + provider);
            provider.readOSM(handler);
        }
        _log.debug("building osm street graph");
        handler.buildGraph(graph);
    }

    @SuppressWarnings("unchecked")
    private <T> T unique(T value) {
        Object v = _uniques.get(value);
        if (v == null) {
            _uniques.put(value, value);
            v = value;
        }
        return (T) v;
    }

    /**
     * Sets processing of bicycle safety features from OSM tags. Takes a map from key,value pairs to
     * forwards,backwards multipliers. In Spring XML, this looks like:
     * 
     * <property name="safetyFeatures"> 
     *   <map> 
     *     <entry key="opposite_lane=cycleway" value="1,0.1" />
     *     <entry key="this_lane=cycleway" value="0.1,1" />
     *    </map>
     *  </property>
     *
     * Entries are multiplied 
     *
     * @param features
     */
    public void setSafetyFeatures(Map<String, String> features) {
        safetyFeatures = new HashMap<P2<String>, P2<Double>>();
        for (Map.Entry<String, String> entry : features.entrySet()) {
            String[] kv = entry.getKey().split("=");
            String[] strings = entry.getValue().split(",");
            P2<Double> values = new P2<Double>(Double.parseDouble(strings[0]), Double
                    .parseDouble(strings[1]));
            safetyFeatures.put(new P2<String>(kv), values);
        }
    }

    private class Handler implements OpenStreetMapContentHandler {

        private Map<Long, OSMNode> _nodes = new HashMap<Long, OSMNode>();

        private Map<Long, OSMWay> _ways = new HashMap<Long, OSMWay>();

        private Map<Long, OSMRelation> _relations = new HashMap<Long, OSMRelation>();

        private Set<Long> _nodesWithNeighbors = new HashSet<Long>();

		private Map<Long, List<TurnRestrictionTag>> turnRestrictionsByFromWay = new HashMap<Long, List<TurnRestrictionTag>>();
		private Map<Long, List<TurnRestrictionTag>> turnRestrictionsByToWay = new HashMap<Long, List<TurnRestrictionTag>>();
		
		private Map<TurnRestrictionTag, TurnRestriction> turnRestrictionsByTag = new HashMap<TurnRestrictionTag, TurnRestriction>();

        public void buildGraph(Graph graph) {
            // Remove all simple islands
            _nodes.keySet().retainAll(_nodesWithNeighbors);

            long wayIndex = 0;

            createUsefulNames();

            // figure out which nodes that are actually intersections
            Set<Long> possibleIntersectionNodes = new HashSet<Long>();
            Set<Long> intersectionNodes = new HashSet<Long>();
            for (OSMWay way : _ways.values()) {
                List<Long> nodes = way.getNodeRefs();
                for (long node : nodes) {
                    if (possibleIntersectionNodes.contains(node)) {
                        intersectionNodes.add(node);
                    } else {
                        possibleIntersectionNodes.add(node);
                    }
                }
            }
            GeometryFactory geometryFactory = new GeometryFactory();
            
            /* build an ordinary graph, which we will convert to an edge-based graph */
            ArrayList<Vertex> endpoints = new ArrayList<Vertex>();

            for (OSMWay way : _ways.values()) {

                if (wayIndex % 1000 == 0)
                    _log.debug("ways=" + wayIndex + "/" + _ways.size());
                wayIndex++;
                StreetTraversalPermission permissions = getPermissionsForEntity(way);
                if (permissions == StreetTraversalPermission.NONE)
                    continue;

                List<Long> nodes = way.getNodeRefs();

                Vertex startEndpoint = null, endEndpoint = null;

                ArrayList<Coordinate> segmentCoordinates = new ArrayList<Coordinate>();

                /*
                 * Traverse through all the nodes of this edge. For nodes which are not shared with
                 * any other edge, do not create endpoints -- just accumulate them for geometry. For
                 * nodes which are shared, create endpoints and StreetVertex instances.
                 */

                Long startNode = null;
                OSMNode osmStartNode = null;
                for (int i = 0; i < nodes.size() - 1; i++) {
                    Long endNode = nodes.get(i + 1);
                    if (osmStartNode == null) {
                        startNode = nodes.get(i);
                        osmStartNode = _nodes.get(startNode);
                    }
                    OSMNode osmEndNode = _nodes.get(endNode);

                    if (osmStartNode == null || osmEndNode == null)
                        continue;

                    LineString geometry;

                    /*
                     * skip vertices that are not intersections, except that we use them for
                     * geometry
                     */
                    if (segmentCoordinates.size() == 0) {
                        segmentCoordinates.add(getCoordinate(osmStartNode));
                    }

                    if (intersectionNodes.contains(endNode) || i == nodes.size() - 2) {
                        segmentCoordinates.add(getCoordinate(osmEndNode));
                        geometry = geometryFactory.createLineString(segmentCoordinates
                                .toArray(new Coordinate[0]));
                        segmentCoordinates.clear();
                    } else {
                        segmentCoordinates.add(getCoordinate(osmEndNode));
                        continue;
                    }

                    /* generate endpoints */
                    if (startEndpoint == null) {
                        //first iteration on this way
                        String label = "osm node " + osmStartNode.getId();

                        startEndpoint = graph.getVertex(label);
                        if (startEndpoint == null) {
                            Coordinate coordinate = getCoordinate(osmStartNode);
                            startEndpoint = new EndpointVertex(label, coordinate.x, coordinate.y,
                                    label);
                            graph.addVertex(startEndpoint);
                            endpoints.add(startEndpoint);
                        }
                    } else {
                        startEndpoint = endEndpoint;
                    }

                    String label = "osm node " + osmEndNode.getId();
                    endEndpoint = graph.getVertex(label);
                    if (endEndpoint == null) {
                        Coordinate coordinate = getCoordinate(osmEndNode);
                        endEndpoint = new EndpointVertex(label, coordinate.x, coordinate.y, label);
                        graph.addVertex(endEndpoint);
                        endpoints.add(endEndpoint);
                    }

                    P2<PlainStreetEdge> streets = getEdgesForStreet(startEndpoint, endEndpoint,
                            way, i, permissions, geometry);
                    PlainStreetEdge street = streets.getFirst();

                    if (street != null) {
                        graph.addEdge(street);
                    }

                    PlainStreetEdge backStreet = streets.getSecond();
                    if (backStreet != null) {
                        graph.addEdge(backStreet);
                    }


                    /* Check if there are turn restrictions starting on this segment */
                    List<TurnRestrictionTag> restrictionTags = turnRestrictionsByFromWay.get(way.getId());
                    if (restrictionTags != null) {
                    	for (TurnRestrictionTag tag : restrictionTags) {
                    		if (tag.via == startNode) {
                    			TurnRestriction restriction = turnRestrictionsByTag.get(tag);
                    			restriction.from = backStreet;
                    		} else if (tag.via == endNode) {
                    			TurnRestriction restriction = turnRestrictionsByTag.get(tag);
                    			restriction.from = street;
                    		}
                    	}
                    }
                    
                    restrictionTags = turnRestrictionsByToWay.get(way);
                    if (restrictionTags != null) {
                    	for (TurnRestrictionTag tag : restrictionTags) {
                    		if (tag.via == startNode) { 
                    			TurnRestriction restriction = turnRestrictionsByTag.get(tag);
                    			restriction.to = street;
                    		} else if (tag.via == endNode) {
                    			TurnRestriction restriction = turnRestrictionsByTag.get(tag);
                    			restriction.to = backStreet;
                    		}
                    	}
                    }
                    startNode = endNode;
                    osmStartNode = _nodes.get(startNode);
                }
            }
            
            /* unify turn restrictions */
            Map<Edge, TurnRestriction> turnRestrictions = new HashMap<Edge, TurnRestriction>();
            for (TurnRestriction restriction : turnRestrictionsByTag.values()) {
            	turnRestrictions.put(restriction.from, restriction);
            }
            
            StreetUtils.pruneFloatingIslands(graph);
			StreetUtils.makeEdgeBased(graph, endpoints, turnRestrictions);
            
        }

        private Coordinate getCoordinate(OSMNode osmNode) {
            return new Coordinate(osmNode.getLon(), osmNode.getLat());
        }

        public void addNode(OSMNode node) {
            if(!_nodesWithNeighbors.contains(node.getId()))
                return;

            if (_nodes.containsKey(node.getId()))
                return;

            _nodes.put(node.getId(), node);

            if (_nodes.size() % 10000 == 0)
                _log.debug("nodes=" + _nodes.size());
        }

        public void addWay(OSMWay way) {
            if (_ways.containsKey(way.getId()))
                return;

            _ways.put(way.getId(), way);

            if (_ways.size() % 1000 == 0)
                _log.debug("ways=" + _ways.size());
        }

        public void addRelation(OSMRelation relation) {
            if (_relations.containsKey(relation.getId()))
                return;

            /* Currently only type=route;route=road relations are handled */
            if (   !(relation.isTag("type", "restriction" ))             		
            	&& !(relation.isTag("type", "route"       ) && relation.isTag("route", "road"))
                && !(relation.isTag("type", "multipolygon") && relation.hasTag("highway"))) {
                return;
            }

            _relations.put(relation.getId(), relation);

            if (_relations.size() % 100 == 0)
                _log.debug("relations=" + _relations.size());

        }

        public void secondPhase() {
            int count = _ways.values().size();

            processRelations();

            for(Iterator<OSMWay> it = _ways.values().iterator(); it.hasNext(); ) {
                OSMWay way = it.next();
                if (!(way.hasTag("highway") || way.isTag("railway", "platform"))) {
                    it.remove();
                } else if (way.isTag("highway", "conveyer") || way.isTag("highway", "proposed")) {
                    it.remove();
                } else {
                    // Since the way is kept, update nodes-with-neighbots
                    List<Long> nodes = way.getNodeRefs();
                    if (nodes.size() > 1) {
                        _nodesWithNeighbors.addAll(nodes);
                    }
                }
            }

            _log.debug("purged " + (count - _ways.values().size() ) + " ways out of " + count);
        }
        
        /** Copies useful metadata from relations to the relavant ways/nodes.
         */
        private void processRelations() {
            _log.debug("Processing relations...");

            for(OSMRelation relation : _relations.values()) {
            	if (relation.isTag("type", "restriction" )) {
            		processRestriction(relation);
            	} else {
            		processRoad(relation);
            	}
            }
        }

		/** A temporary holder for turn restrictions while we have only way/node ids but not yet edge objects */
		class TurnRestrictionTag {
			@SuppressWarnings("unused")
			private long to;
			@SuppressWarnings("unused")
			private long from;
			private long via;
			private TurnRestrictionType type;

			TurnRestrictionTag(long from, long to, long via, TurnRestrictionType type) {
				this.from = from;
				this.to = to;
				this.via = via;
				this.type = type;
			}
		}

        /**
         * Handle turn restrictions
         * @param relation
         */
		private void processRestriction(OSMRelation relation) {
			long from = -1, to = -1, via = -1;
			for (OSMRelationMember member : relation.getMembers()) {
				String role = member.getRole();
				if (role.equals("from")) {
					from = member.getRef();
				} else if (role.equals("to")) {
					to = member.getRef();
				} else if (role.equals("via")) {
					via = member.getRef();
				}
			}
			if (from == -1 || to == -1 || via == -1) {
				_log.debug("Bad restriction " + relation.getId());
				return;
			}
			
			TurnRestrictionTag tag;
			if (relation.isTag("restriction", "no_right_turn")) {
				tag = new TurnRestrictionTag(from, to, via, TurnRestrictionType.NO_TURN);
			} else if (relation.isTag("restriction", "no_left_turn")) {
				tag = new TurnRestrictionTag(from, to, via, TurnRestrictionType.NO_TURN);
			} else if (relation.isTag("restriction", "no_straight_on")) {
				tag = new TurnRestrictionTag(from, to, via, TurnRestrictionType.NO_TURN);
			} else if (relation.isTag("restriction", "only_straight_on")) {
				tag = new TurnRestrictionTag(from, to, via, TurnRestrictionType.ONLY_TURN);
			} else if (relation.isTag("restriction", "only_right_turn")) {
				tag = new TurnRestrictionTag(from, to, via, TurnRestrictionType.ONLY_TURN);
			} else if (relation.isTag("restriction", "only_left_turn")) {
				tag = new TurnRestrictionTag(from, to, via, TurnRestrictionType.ONLY_TURN);
			} else {
				_log.debug("unknown restriction type " + relation.getTag("restriction"));
				return;
			}
			TurnRestriction restriction = new TurnRestriction();
			restriction.type = tag.type;
			turnRestrictionsByTag.put(tag, restriction);
			
			GraphBuilderUtils.addToMapList(turnRestrictionsByFromWay, from, tag);
			GraphBuilderUtils.addToMapList(turnRestrictionsByToWay, to, tag);
			
		}
		
		private void processRoad(OSMRelation relation) {
			for( OSMRelationMember member : relation.getMembers()) {
			    if("way".equals(member.getType()) && _ways.containsKey(member.getRef())) {
			        OSMWay way = _ways.get(member.getRef());
			        if(way != null) {
			            if(relation.hasTag("name")) {
			                if(way.hasTag("otp:route_name")) {
			                	way.addTag("otp:route_name", addUniqueName(way.getTag("otp:route_name"), relation.getTag("name")));
			                } else {
			                    way.addTag(new OSMTag("otp:route_name", relation.getTag("name")));
			                }
			            }
			            if(relation.hasTag("ref")) {
			                if(way.hasTag("otp:route_ref")) {
			                    way.addTag("otp:route_ref", addUniqueName(way.getTag("otp:route_ref"), relation.getTag("ref")));
			                } else {
			                    way.addTag(new OSMTag("otp:route_ref", relation.getTag("ref")));
			                }
			            }
			            if(relation.hasTag("highway") && relation.isTag("type", "multipolygon") && !way.hasTag("highway")) {
			                way.addTag("highway", relation.getTag("highway"));
			            }
			        }
			    }
			}
		}

        private String addUniqueName(String routes, String name) {
        	String[] names = routes.split(", ");
        	for (String existing : names) {
        		if (existing.equals(name)) {
        			return routes;
        		}
        	}
			return routes + ", " + name;
		}

        private void createUsefulNames() {
            Map<String, Set<OSMWay>> key_map = new HashMap<String, Set<OSMWay>>();
            Map<OSMKeyValue, Set<OSMWay>> keyvalue_map = new HashMap<OSMKeyValue, Set<OSMWay>>();
            Set<OSMWay> processed_ways = new HashSet<OSMWay>();
            Pattern p = Pattern.compile("\\{(.+?)\\}");
            Matcher m = p.matcher("");

            _log.debug("Generating creative names...");

            for(OSMWay way : _ways.values()) {
                Map<String, String> tags = way.getTags();

                /* If a way already has a name, then trying to give it another one
                 * doesn't make alot of sense... */
                if(tags.containsKey("name")) {
                    continue;
                }

                for(String key : tags.keySet()) {
                    OSMKeyValue kv = new OSMKeyValue(key, tags.get(key), false);
                    Set<OSMWay> keyvalue_map_set = keyvalue_map.get(kv);
                    Set<OSMWay> key_map_set = key_map.get(key);

                    if(keyvalue_map_set == null) {
                        keyvalue_map_set = new HashSet<OSMWay>();
                        keyvalue_map.put(kv, keyvalue_map_set);
                    }
                    keyvalue_map_set.add(way);

                    if(key_map_set == null) {
                        key_map_set = new HashSet<OSMWay>();
                        key_map.put(key, key_map_set);
                    }
                    key_map_set.add(way);
                }
            }

            for(List<OSMKeyValue> lkv : _creativeNaming.keySet()) {
                Set<OSMWay> hope   = null;
                Map<String, Matcher> replace = new HashMap<String, Matcher>();
                String format = _creativeNaming.get(lkv);

                for(OSMKeyValue kv : lkv) {
                    if(hope == null) {
                        hope = new HashSet<OSMWay>();
                        if(kv.wildcard) {
                            if(key_map.containsKey(kv.key)) {
                                hope.addAll(key_map.get(kv.key));
                            }
                        } else {
                            if(keyvalue_map.containsKey(kv)) {
                                hope.addAll(keyvalue_map.get(kv));
                            }
                        }
                        hope.removeAll(processed_ways);
                    } else {
                        if(kv.wildcard) {
                            if(key_map.containsKey(kv.key)) {
                                hope.retainAll(key_map.get(kv.key));
                            } else {
                                hope.clear();
                            }
                        } else {
                            if(keyvalue_map.containsKey(kv)) {
                                hope.retainAll(keyvalue_map.get(kv));
                            } else {
                                hope.clear();
                            }
                        }
                    }
                }

                m.reset(format);
                while(m.find()) {
                    replace.put(m.group(1), Pattern.compile("\\{" + m.group(1) + "\\}").matcher(""));
                }

                for(OSMWay way : hope) {
                    String gen_name = format;
                    for(String key : replace.keySet()) {
                        Matcher nm = replace.get(key);
                        nm.reset(gen_name);
                        gen_name = nm.replaceAll(way.getTag(key));
                    }

                    way.addTag(new OSMTag("otp:gen_name", gen_name));
                    processed_ways.add(way);
                    _log.debug("generated name: " + way + " >> " + gen_name);
                }
            }
        }

        /**
         * Handle oneway streets, cycleways, and whatnot. See
         * http://wiki.openstreetmap.org/wiki/Bicycle for various scenarios, along with
         * http://wiki.openstreetmap.org/wiki/OSM_tags_for_routing#Oneway.
         * 
         * @param end
         * @param start
         */
        private P2<PlainStreetEdge> getEdgesForStreet(Vertex start, Vertex end, OSMWay way,
                long startNode, StreetTraversalPermission permissions, LineString geometry) {
            // get geometry length in meters, irritatingly.
            Coordinate[] coordinates = geometry.getCoordinates();
            double d = 0;
            for (int i = 1; i < coordinates.length; ++i) {
                d += DistanceLibrary.distance(coordinates[i - 1], coordinates[i]);
            }

            LineString backGeometry = (LineString) geometry.reverse();

            Map<String, String> tags = way.getTags();

            if (permissions == StreetTraversalPermission.NONE)
                return new P2<PlainStreetEdge>(null, null);

            PlainStreetEdge street = null, backStreet = null;

            /*
             * Three basic cases, 1) bidirectional for everyone, 2) unidirectional for cars only, 3)
             * bidirectional for pedestrians only.
             */

            if (way.isTagTrue("oneway") && way.isTagFalse(tags.get("oneway:bicycle"))
                            || "opposite_lane".equals(tags.get("cycleway")) || "opposite"
                            .equals(tags.get("cycleway"))) { // 2.
                street = getEdgeForStreet(start, end, way, startNode, d, permissions, geometry,
                        false);
                if (permissions.remove(StreetTraversalPermission.CAR) != StreetTraversalPermission.NONE)
                    backStreet = getEdgeForStreet(end, start, way, startNode, d, permissions
                            .remove(StreetTraversalPermission.CAR), backGeometry, true);
            } else if (way.isTagTrue("oneway")
                    || "roundabout".equals(tags.get("junction"))) { // 3
                street = getEdgeForStreet(start, end, way, startNode, d, permissions, geometry,
                        false);
                if (permissions.allows(StreetTraversalPermission.PEDESTRIAN))
                    backStreet = getEdgeForStreet(end, start, way, startNode, d,
                            StreetTraversalPermission.PEDESTRIAN, backGeometry, true);
            } else { // 1.
                street = getEdgeForStreet(start, end, way, startNode, d, permissions, geometry,
                        false);
                backStreet = getEdgeForStreet(end, start, way, startNode, d, permissions,
                        backGeometry, true);
            }
            
            /* mark edges that are on roundabouts */
            if ("roundabout".equals(tags.get("junction"))) {
                street.setRoundabout(true);
                if (backStreet != null) backStreet.setRoundabout(true);
            }

            /* set bicycle safety features according to configuration */

            for (Map.Entry<P2<String>, P2<Double>> feature : safetyFeatures.entrySet()) {
                String key = feature.getKey().getFirst();
                String value = feature.getKey().getSecond();
                if (value.equals(tags.get(key))) {
                    P2<Double> multipliers = feature.getValue();
                    if (street != null) {
                        street.setBicycleSafetyEffectiveLength(street.getBicycleSafetyEffectiveLength() * multipliers.getFirst());
                    }
                    if (backStreet != null) {
                        backStreet.setBicycleSafetyEffectiveLength(backStreet.getBicycleSafetyEffectiveLength() * multipliers.getSecond());
                    }
                }
            }
            return new P2<PlainStreetEdge>(street, backStreet);
        }

        private PlainStreetEdge getEdgeForStreet(Vertex start, Vertex end, OSMWay way,
                long startNode, double length, StreetTraversalPermission permissions,
                LineString geometry, boolean back) {

            String id = "way " + way.getId() + " from " + startNode;
            id = unique(id);

            String name = way.getAssumedName();
            if (name == null) {
                name = id;
            }
            PlainStreetEdge street = new PlainStreetEdge(start, end, geometry, name, length,
                    permissions, back);
            street.setId(id);

            if (!way.hasTag("name")) {
                street.setBogusName(true);
            }

            /* TODO: This should probably generalized somehow? */
            if (way.isTagFalse("wheelchair") || ("steps".equals(way.getTag("highway")) && !way.isTagTrue("wheelchair"))) {
                street.setWheelchairAccessible(false);
            }

            Map<String, String> tags = way.getTags();
            if(tags != null) {
                for (P2<String> kvp : _slopeOverrideTags) {
                    String key = kvp.getFirst();
                    String value = kvp.getSecond();
                    if (value.equals(tags.get(key))) {
                        street.setSlopeOverride(true);
                        break;
                    }
                }
            }

            return street;
        }

        private StreetTraversalPermission getPermissionsForEntity(OSMWithTags entity) {
            Map<String, String> tags = entity.getTags();
            StreetTraversalPermission def = null;
            StreetTraversalPermission permission = null;

            for (KeyValuePermission kvp : _tagPermissions.values()) {
                if (tags.containsKey(kvp.key) && kvp.value.equals(tags.get(kvp.key))) {
                    def = kvp.permission;
                    break;
                }
            }

            if (def == null) {
                if (_tagPermissions.containsKey("__default__")) {
                    String all_tags = null;
                    for (String key : tags.keySet()) {
                        String tag = key + "=" + tags.get(key);
                        if (all_tags == null) {
                            all_tags = tag;
                        } else {
                            all_tags += "; " + tag;
                        }
                    }
                    _log.debug("Used default permissions: " + all_tags);
                    def = _tagPermissions.get("__default__").permission;
                } else {
                    def = StreetTraversalPermission.ALL;
                }
            }

            String access = tags.get("access");
            String motorcar = tags.get("motorcar");
            String bicycle = tags.get("bicycle");
            String foot = tags.get("foot");

            /*
             * Only access=*, motorcar=*, bicycle=*, and foot=* is examined, since those are the
             * only modes supported by OTP (wheelchairs are not of concern here)
             * 
             * Only *=no, and *=private are checked for, all other values are presumed to be
             * permissive (=> This may not be perfect, but is closer to reality, since most people
             * don't follow the rules perfectly ;-)
             */
            if (access != null) {
                if ("no".equals(access) || "private".equals(access)) {
                    permission = StreetTraversalPermission.NONE;
                } else {
                    permission = def;
                }
            } else if (motorcar != null || bicycle != null || foot != null) {
                permission = def;
            }

            if (motorcar != null) {
                if ("no".equals(motorcar) || "private".equals(motorcar)) {
                    permission = permission.remove(StreetTraversalPermission.CAR);
                } else {
                    permission = permission.add(StreetTraversalPermission.CAR);
                }
            }

            if (bicycle != null) {
                if ("no".equals(bicycle) || "private".equals(bicycle)) {
                    permission = permission.remove(StreetTraversalPermission.BICYCLE);
                } else {
                    permission = permission.add(StreetTraversalPermission.BICYCLE);
                }
            }

            if (foot != null) {
                if ("no".equals(foot) || "private".equals(foot)) {
                    permission = permission.remove(StreetTraversalPermission.PEDESTRIAN);
                } else {
                    permission = permission.add(StreetTraversalPermission.PEDESTRIAN);
                }
            }

            if (permission == null)
                return def;

            return permission;
        }
    }
}
