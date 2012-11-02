/**
 Ported by David Turner from Visilibity, by Karl J. Obermeyer
   
 
 This port undoubtedly introduced a number of bugs (and removed some features).
 
 Bug reports should be directed to the OpenTripPlanner project, unless they 
 can be reproduced in the original VisiLibity.
  
 This program is free software: you can redistribute it and/or modify it under
 the terms of the GNU Lesser General Public License as published by the
 Free Software Foundation, either version 3 of the License, or (at your
 option) any later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
 License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opentripplanner.visibility;

import java.util.PriorityQueue;
import java.util.HashSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VisibilityPolygon extends VLPolygon {
    private static Logger log = LoggerFactory.getLogger(VisibilityPolygon.class);
    VLPoint observer;

    public boolean is_spike(VLPoint observer, VLPoint point1, VLPoint point2, VLPoint point3, double epsilon) {

        return (
        // Make sure observer not colocated with any of the points.
        observer.distance(point1) > epsilon && observer.distance(point2) > epsilon
                && observer.distance(point3) > epsilon
                // Test whether there is a spike with point2 as the tip
                && ((observer.distance(point2) >= observer.distance(point1) && observer
                        .distance(point2) >= observer.distance(point3)) || (observer
                        .distance(point2) <= observer.distance(point1) && observer.distance(point2) <= observer
                        .distance(point3)))
        // && the pike is sufficiently sharp,
        && Math.max(point2.distance(new Ray(observer, point1)),
                point2.distance(new Ray(observer, point3))) <= epsilon);
        // Formerly used
        // Math.abs( Polygon(point1, point2, point3).area() ) < epsilon
    }

    public void chop_spikes_at_back(VLPoint observer, double epsilon) {
        // Eliminate "special case" vertices of the visibility polygon.
        // While the top three vertices form a spike.
        while (vertices.size() >= 3
                && is_spike(observer, vertices.get(vertices.size() - 3),
                        vertices.get(vertices.size() - 2), vertices.get(vertices.size() - 1),
                        epsilon)) {
            vertices.set(vertices.size() - 2, vertices.get(vertices.size() - 1));
            vertices.remove(vertices.size() - 1);
        }
    }

    void chop_spikes_at_wrap_around(VLPoint observer, double epsilon) {
        // Eliminate "special case" vertices of the visibility polygon at
        // wrap-around. While the there's a spike at the wrap-around,
        while (vertices.size() >= 3
                && is_spike(observer, vertices.get(vertices.size() - 2),
                        vertices.get(vertices.size() - 1), vertices.get(0), epsilon)) {
            // Chop off the tip of the spike.
            vertices.remove(vertices.size() - 1);
        }
    }

    void chop_spikes(VLPoint observer, double epsilon) {
        HashSet<VLPoint> spike_tips = new HashSet<VLPoint>();
        ArrayList<VLPoint> vertices_temp = new ArrayList<VLPoint>();
        // Middle point is potentially the tip of a spike
        for (int i = 0; i < vertices.size(); i++)
            if (get(i + 2).distance(new LineSegment(get(i), get(i + 1))) <= epsilon
                    || get(i).distance(new LineSegment(get(i + 1), get(i + 2))) <= epsilon)
                spike_tips.add(get(i + 1));

        for (int i = 0; i < vertices.size(); i++)
            if (!spike_tips.contains(vertices.get(i)))
                vertices_temp.add(vertices.get(i));
        vertices = vertices_temp;
    }

    public VisibilityPolygon(VLPoint observer, Environment environment_temp, double epsilon) {
        this.observer = observer;
        // Visibility polygon algorithm for environments with holes
        // Radial line (AKA angular plane) sweep technique.
        //
        // Based on algorithms described in
        //
        // [1] "Automated Camera Layout to Satisfy Task-Specific and
        // Floorplan-Specific Coverage Requirements" by Ugur Murat Erdem
        // && Stan Scarloff, April 15, 2004
        // available at BUCS Technical Report Archive:
        // http://www.cs.bu.edu/techreports/pdf/2004-015-camera-layout.pdf
        //
        // [2] "Art Gallery Theorems && Algorithms" by Joseph O'Rourke
        //
        // [3] "Visibility Algorithms in the Plane" by Ghosh
        //

        // We define a k-point is a point seen on the other side of a
        // visibility occluding corner. This name is appropriate because
        // the vertical line in the letter "k" is like a line-of-sight past
        // the corner of the "k".

        //
        // Preconditions:
        // (1) the Environment is epsilon-valid,
        // (2) the Point observer is actually in the Environment
        // environment_temp,
        // (3) the guard has been epsilon-snapped to the boundary, followed
        // by vertices of the environment (the order of the snapping
        // is important).
        //
        // :WARNING:
        // For efficiency, the assertions corresponding to these
        // preconditions have been excluded.
        //
        assert (environment_temp.is_valid(epsilon));
        assert (environment_temp.is_in_standard_form());
        assert (observer.in(environment_temp, epsilon));

        // true => data printed to terminal
        // false => silent

        // The visibility polygon cannot have more vertices than the environment.
        vertices.ensureCapacity(environment_temp.n());

        //
        // --------PREPROCESSING--------
        //

        // construct a POLAR EDGE LIST from environment_temp's outer
        // boundary and holes. During this construction, those edges are
        // split which either (1) cross the ray emanating from the observer
        // parallel to the x-axis (of world coords), or (2) contain the
        // observer in their relative interior (w/in epsilon). Also, edges
        // having first vertex bearing >= second vertex bearing are
        // eliminated because they cannot possibly contribute to the
        // visibility polygon.

        final Angle ANGLE_PI = new Angle(Math.PI);
        final Angle ANGLE_ZERO = new Angle(0.0);
        ArrayList<PolarEdge> elp = new ArrayList<PolarEdge>();
        PolarPoint ppoint1, ppoint2;
        PolarPoint split_bottom, split_top;
        double t;
        // If the observer is standing on the Enviroment boundary with its
        // back to the wall, these will be the bearings of the next vertex
        // to the right && to the left, respectively.
        Angle right_wall_bearing = new Angle(0.0);
        Angle left_wall_bearing = new Angle(0.0);
        for (int i = 0; i <= environment_temp.h(); i++) {
            VLPolygon polygon = environment_temp.get(i);

            for (int j = 0; j < polygon.n(); j++) {
                ppoint1 = new PolarPoint(observer, polygon.get(j));
                ppoint2 = new PolarPoint(observer, polygon.get(j + 1));
                log.debug("contemplating " +  ppoint1 + " and " +  ppoint1);

                // If the observer is in the relative interior of the edge.
                if (observer.in_relative_interior_of(new LineSegment(ppoint1, ppoint2), epsilon)) {
                    log.debug("in relative interior");

                    // Split the edge at the observer && add the resulting two
                    // edges to elp (the polar edge list).
                    split_bottom = new PolarPoint(observer, observer);
                    split_top = new PolarPoint(observer, observer);

                    if (ppoint2.bearing.equals(ANGLE_ZERO))
                        ppoint2.set_bearing_to_2pi();

                    left_wall_bearing = ppoint1.bearing.clone();
                    right_wall_bearing = ppoint2.bearing.clone();

                    elp.add(new PolarEdge(ppoint1, split_bottom));
                    elp.add(new PolarEdge(split_top, ppoint2));
                    continue;
                }

                // Else if the observer is on first vertex of edge.
                else if (observer.distance(ppoint1) <= epsilon) {
                    log.debug("on first vertex");

                    if (ppoint2.bearing.equals(ANGLE_ZERO)) {
                        ppoint2.set_bearing_to_2pi();
                    }
                    // Get right wall bearing.
                    right_wall_bearing = ppoint2.bearing.clone();
                    elp.add(new PolarEdge(new PolarPoint(observer, observer), ppoint2));
                    continue;
                }
                // Else if the observer is on second vertex of edge.
                else if (observer.distance(ppoint2) <= epsilon) {
                    log.debug("on second vertex");

                    // Get left wall bearing.
                    left_wall_bearing = ppoint1.bearing.clone();
                    elp.add(new PolarEdge(ppoint1, new PolarPoint(observer, observer)));
                    continue;
                }

                // Otherwise the observer is not on the edge.

                // If edge not horizontal (w/in epsilon).
                else if (Math.abs(ppoint1.y - ppoint2.y) > epsilon) {
                    log.debug("off edge");

                    // Possible source of numerical instability?
                    t = (observer.y - ppoint2.y) / (ppoint1.y - ppoint2.y);
                    // If edge crosses the ray emanating horizontal && right of
                    // the observer.
                    if (0 < t && t < 1 && observer.x < t * ppoint1.x + (1 - t) * ppoint2.x) {
                        log.debug("crosses ray");

                        // If first point is above, omit edge because it runs
                        // 'against the grain'.
                        if (ppoint1.y > observer.y)
                            continue;
                        // Otherwise split the edge, making sure angles are assigned
                        // correctly on each side of the split point.
                        split_bottom = new PolarPoint(observer, new VLPoint(t * ppoint1.x + (1 - t)
                                * ppoint2.x, observer.y));
                        split_top = new PolarPoint(observer, new VLPoint(t * ppoint1.x + (1 - t)
                                * ppoint2.x, observer.y));
                        split_top.set_bearing(ANGLE_ZERO);
                        split_bottom.set_bearing_to_2pi();
                        elp.add(new PolarEdge(ppoint1, split_bottom));
                        elp.add(new PolarEdge(split_top, ppoint2));
                        continue;
                    } else {
                        if (ppoint1.bearing.compareTo(ppoint2.bearing) >= 0
                                && ppoint2.bearing.equals(ANGLE_ZERO)
                                && ppoint1.bearing.compareTo(ANGLE_PI) > 0) {
                            ppoint2.set_bearing_to_2pi();
                        // Filter out edges which run 'against the grain'.
                        } else if ((ppoint1.bearing.equals(ANGLE_ZERO) && 
                                ppoint2.bearing.compareTo(ANGLE_PI) > 0)
                                || ppoint1.bearing.compareTo(ppoint2.bearing) >= 0) {
                            continue;
                        }
                    }
                    elp.add(new PolarEdge(ppoint1, ppoint2));
                    continue;
                }
                // If edge is horizontal (w/in epsilon).
                else {
                    log.debug("epsilon horizontal");
                    // Filter out edges which run 'against the grain'.
                    if (ppoint1.bearing.compareTo(ppoint2.bearing) >= 0)
                        continue;
                    elp.add(new PolarEdge(ppoint1, ppoint2));
                }
            }
        }

        // construct a SORTED LIST, q1, OF VERTICES represented by
        // PolarPointWithEdgeInfo objects. A
        // PolarPointWithEdgeInfo is a derived class of PolarPoint
        // which includes (1) a pointer to the corresponding edge
        // (represented as a PolarEdge) in the polar edge list elp, and
        // (2) a boolean(is_first) which is true iff that vertex is the
        // first Point of the respective edge (is_first == false => it's
        // second Point). q1 is sorted according to lex. order of polar
        // coordinates just as PolarPoints are, but with the additional
        // requirement that if two vertices have equal polar coordinates,
        // the vertex which is the first point of its respective edge is
        // considered greater. q1 will serve as an event point queue for
        // the radial sweep.
        ArrayList<PolarPointWithEdgeInfo> q1 = new ArrayList<PolarPointWithEdgeInfo>();
        PolarPointWithEdgeInfo ppoint_wei1 = new PolarPointWithEdgeInfo(), ppoint_wei2 = new PolarPointWithEdgeInfo();
        Iterator<PolarEdge> elp_iterator = elp.iterator();
        while (elp_iterator.hasNext()) {
            PolarEdge edge = elp_iterator.next();
            ppoint_wei1.set_polar_point(edge.first);
            ppoint_wei1.incident_edge = edge;
            ppoint_wei1.is_first = true;
            ppoint_wei2.set_polar_point(edge.second);
            ppoint_wei2.incident_edge = edge;
            ppoint_wei2.is_first = false;
            // If edge contains the observer, then adjust the bearing of
            // the PolarPoint containing the observer.
            if (observer.distance(ppoint_wei1) <= epsilon) {
                if (right_wall_bearing.compareTo(left_wall_bearing) > 0) {
                    ppoint_wei1.set_bearing(right_wall_bearing);
                    edge.first.set_bearing(right_wall_bearing);
                } else {
                    ppoint_wei1.set_bearing(ANGLE_ZERO);
                    edge.first.set_bearing(ANGLE_ZERO);
                }
            } else if (observer.distance(ppoint_wei2) <= epsilon) {
                if (right_wall_bearing.compareTo(left_wall_bearing) > 0) {
                    ppoint_wei2.set_bearing(right_wall_bearing);
                    edge.second.set_bearing(right_wall_bearing);
                } else {
                    ppoint_wei2.set_bearing_to_2pi();
                    edge.second.set_bearing_to_2pi();
                }
            }
            q1.add(ppoint_wei1.clone());
            q1.add(ppoint_wei2.clone());
        }

        // Put event point in correct order.
        // Collections.sort is a stable sort.
        Collections.sort(q1);
        for (PolarPointWithEdgeInfo q : q1) {
            log.debug("q: " + q);
        }
        //
        // -------PREPARE FOR MAIN LOOP-------
        //

        // current_vertex is used to hold the event point (from q1)
        // considered at iteration of the main loop.

        //
        PolarPointWithEdgeInfo current_vertex = new PolarPointWithEdgeInfo();
        // Note active_edge and e are not actually edges themselves, but
        // iterators pointing to edges. active_edge keeps track of the
        // current edge visible during the sweep. e is an auxiliary
        // variable used in calculation of k-points
        PolarEdge active_edge, e;
        // More aux vars for computing k-points.
        PolarPoint k = new PolarPoint();
        double k_range;
        LineSegment xing;

        // Priority queue of edges, where lower (first) priority indicates closer
        // range to observer along current ray (of ray sweep).
        IncidentEdgeCompare my_iec = new IncidentEdgeCompare(observer, current_vertex, epsilon);
        PriorityQueue<PolarEdge> q2 = new PriorityQueue<PolarEdge>(elp.size(), my_iec);

        // Initialize main loop.
        current_vertex.set(q1.remove(0));
        active_edge = current_vertex.incident_edge;

        // Insert e into q2 as long as it doesn't contain the
        // observer.
        if (observer.distance(active_edge.first) > epsilon
                && observer.distance(active_edge.second) > epsilon) {

            q2.add(active_edge);
        }

        vertices.add(new VLPoint(current_vertex));
        log.debug("adding: " + current_vertex + "\n--");

        // -------BEGIN MAIN LOOP-------//
        //
        // Perform radial sweep by sequentially considering each vertex
        // (event point) in q1.

        while (!q1.isEmpty()) {

            // Pop current_vertex from q1.
            current_vertex.set(q1.remove(0));
            log.debug("cv: " + current_vertex);

            // ---Handle Event Point---

            // TYPE 1: current_vertex is the _second_vertex_ of active_edge.
            if (current_vertex.incident_edge.equals(active_edge) && !current_vertex.is_first) {
                log.debug( "type 1");

                if (!q1.isEmpty()) {
                    // If the next vertex in q1 is contiguous.
                    if (current_vertex.distance(q1.get(0)) <= epsilon) {
                        continue;
                    }
                }

                // Push current_vertex onto visibility polygon
                vertices.add(new VLPoint(current_vertex));
                log.debug("adding: " + current_vertex);

                chop_spikes_at_back(observer, epsilon);

                while (!q2.isEmpty()) {
                    e = q2.peek();
                    log.debug("q2: " + e);
                    // If the current_vertex bearing has not passed, in the
                    // lex. order sense, the bearing of the second point of the
                    // edge at the front of q2.
                    if ((current_vertex.bearing.get() <= e.second.bearing.get())
                    // For robustness.
                            && new Ray(observer, current_vertex.bearing).distance(e.second) >= epsilon
                    /*
                     * was && std::min( distance(Ray(observer, current_vertex.bearing), e.second),
                     * distance(Ray(observer, e.second.bearing), current_vertex) ) >= epsilon
                     */
                    ) {
                        // Find intersection point k of ray (through
                        // current_vertex) with edge e.
                        xing = new Ray(observer, current_vertex.bearing).intersection(
                                new LineSegment(e.first, e.second), epsilon);

                        // assert( xing.size() > 0 );

                        if (xing.size() > 0) {
                            k = new PolarPoint(observer, xing.first());
                        } else { // Error contingency.
                            k = current_vertex;
                            e = current_vertex.incident_edge;
                        }

                        // Push k onto the visibility polygon.
                        vertices.add(new VLPoint(k));
                        log.debug("adding k1: " + k);
                        chop_spikes_at_back(observer, epsilon);
                        active_edge = e;
                        break;
                    }

                    q2.poll();
                }
            } // Close Type 1.

            // If current_vertex is the _first_vertex_ of its edge.
            if (current_vertex.is_first) {
                log.debug("is first");
                // Find intersection point k of ray (through current_vertex)
                // with active_edge.

                xing = new Ray(observer, current_vertex.bearing).intersection(new LineSegment(
                        active_edge.first, active_edge.second), epsilon);
                if (xing.size() == 0
                        || (active_edge.first.distance(observer) <= epsilon && active_edge.second.bearing
                                .compareTo(current_vertex.bearing) <= 0)
                        || active_edge.second.compareTo(current_vertex) < 0) {

                    k_range = Double.POSITIVE_INFINITY;
                } else {
                    k = new PolarPoint(observer, xing.first());
                    k_range = k.range;
                }

                // Incident edge of current_vertex.
                e = current_vertex.incident_edge;

                // Insert e into q2 as long as it doesn't contain the
                // observer.
                if (observer.distance(e.first) > epsilon && observer.distance(e.second) > epsilon) {

                    q2.add(e);
                }

                // TYPE 2: current_vertex is (1) a first vertex of some edge
                // other than active_edge, && (2) that edge should not become
                // the next active_edge. This happens, e.g., if that edge is
                // (rangewise) in back along the current bearing.
                if (k_range < current_vertex.range) {
                    // this is empty, in the original too -DMT
                    log.debug("type 2");
                } // Close Type 2.

                // TYPE 3: current_vertex is (1) the first vertex of some edge
                // other than active_edge, && (2) that edge should become the
                // next active_edge. This happens, e.g., if that edge is
                // (rangewise) in front along the current bearing.
                if (k_range >= current_vertex.range) {
                    // Push k onto the visibility polygon unless effectively
                    // contiguous with current_vertex.
                    log.debug("type 3");
                    if (xing.size() > 0 && k_range != Double.POSITIVE_INFINITY
                            && k.distance(current_vertex) > epsilon
                            && active_edge.first.distance(observer) > epsilon) {

                        // Push k-point onto the visibility polygon.
                        vertices.add(new VLPoint(k));
                        log.debug("adding k2: " + k);
                        chop_spikes_at_back(observer, epsilon);
                    }

                    // Push current_vertex onto the visibility polygon.
                    vertices.add(new VLPoint(current_vertex));
                    log.debug("adding: " + current_vertex);
                    chop_spikes_at_back(observer, epsilon);
                    // Set active_edge to edge of current_vertex.
                    active_edge = e;

                } // Close Type 3.
            }

        } //
          //
          // -------END MAIN LOOP-------//

        // The VisibilityPolygon should have a minimal representation
        chop_spikes_at_wrap_around(observer, epsilon);
        eliminate_redundant_vertices(epsilon);
        chop_spikes(observer, epsilon);
        enforce_standard_form();

    }

    VisibilityPolygon(VLPoint observer, VLPolygon polygon_temp, double epsilon) {
        this(observer, new Environment(polygon_temp), epsilon);
    }

}