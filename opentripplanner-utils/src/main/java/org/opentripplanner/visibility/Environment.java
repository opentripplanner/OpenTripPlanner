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

import java.util.ArrayList;
import java.util.List;

/**
 * \brief environment represented by simple polygonal outer boundary with simple polygonal holes
 * 
 * \remarks For methods to work correctly, the outer boundary vertices must be listed ccw and the
 * hole vertices cw
 */
public class Environment {

    Polygon outer_boundary;

    ArrayList<Polygon> holes = new ArrayList<Polygon>();

    ArrayList<pair<Integer, Integer>> flattened_index_key = new ArrayList<pair<Integer, Integer>>();

    public Environment(List<Polygon> polygons) {
        outer_boundary = polygons.get(0);
        for (int i = 1; i < polygons.size(); i++)
            holes.add(polygons.get(i));
        update_flattened_index_key();
    }

    public Environment(Polygon polygon_temp) {
        outer_boundary = polygon_temp;
        update_flattened_index_key();
    }

    Point kth_point(int k) {
        pair<Integer, Integer> ij = flattened_index_key.get(k);
        return get(ij.first()).get(ij.second());
    }

    int n() {
        int n_count = 0;
        n_count = outer_boundary.n();
        for (int i = 0; i < h(); i++)
            n_count += holes.get(i).n();
        return n_count;
    }

    int r() {
        int r_count = 0;
        r_count = outer_boundary.r();
        for (int i = 0; i < h(); i++) {
            Polygon polygon_temp = holes.get(i);
            r_count += polygon_temp.n() - polygon_temp.r();
        }
        return r_count;
    }

    int h() {
        return holes.size();
    }

    boolean is_in_standard_form() {
        if (outer_boundary.is_in_standard_form() == false || outer_boundary.area() < 0)
            return false;
        for (int i = 0; i < holes.size(); i++)
            if (holes.get(i).is_in_standard_form() == false || holes.get(i).area() > 0)
                return false;
        return true;
    }

    boolean is_valid(double epsilon) {
        if (n() <= 2)
            return false;

        // Check all Polygons are simple.
        if (!outer_boundary.is_simple(epsilon)) {
            /*
             * std::cerr << std::endl << "\x1b[31m" << "The outer boundary is not simple." <<
             * "\x1b[0m" << std::endl;
             */
            return false;
        }
        for (int i = 0; i < h(); i++)
            if (!holes.get(i).is_simple(epsilon)) {
                /*
                 * std::cerr << std::endl << "\x1b[31m" << "Hole " << i << " is not simple." <<
                 * "\x1b[0m" << std::endl;
                 */
                return false;
            }

        // Check none of the Polygons' boundaries intersect w/in epsilon.
        for (int i = 0; i < h(); i++)
            if (outer_boundary.boundary_distance(holes.get(i)) <= epsilon) {
                /*
                 * std::cerr << std::endl << "\x1b[31m" <<
                 * "The outer boundary intersects the boundary of hole " << i << "." << "\x1b[0m" <<
                 * std::endl;
                 */
                return false;
            }
        for (int i = 0; i < h(); i++)
            for (int j = i + 1; j < h(); j++)
                if (holes.get(i).boundary_distance(holes.get(j)) <= epsilon) {
                    /*
                     * std::cerr << std::endl << "\x1b[31m" << "The boundary of hole " << i <<
                     * " intersects the boundary of hole " << j << "." << "\x1b[0m" << std::endl;
                     */
                    return false;
                }

        // Check that the vertices of each hole are in the outside_boundary
        // and not in any other holes.
        // Loop over holes.
        for (int i = 0; i < h(); i++) {
            // Loop over vertices of a hole
            for (int j = 0; j < holes.get(i).n(); j++) {
                if (!holes.get(i).get(j).in(outer_boundary, epsilon)) {
                    /*
                     * std::cerr << std::endl << "\x1b[31m" << "Vertex " << j << " of hole " << i <<
                     * " is not within the outer boundary." << "\x1b[0m" << std::endl;
                     */
                    return false;
                }
                // Second loop over holes.
                for (int k = 0; k < h(); k++)
                    if (i != k && holes.get(i).get(j).in(holes.get(k), epsilon)) {
                        /*
                         * std::cerr << std::endl << "\x1b[31m" << "Vertex " << j << " of hole " <<
                         * i << " is in hole " << k << "." << "\x1b[0m" << std::endl;
                         */
                        return false;
                    }
            }
        }

        // Check outer_boundary is ccw and holes are cw.
        if (outer_boundary.area() <= 0) {
            /*
             * std::cerr << std::endl << "\x1b[31m" <<
             * "The outer boundary vertices are not listed ccw." << "\x1b[0m" << std::endl;
             */
            return false;
        }
        for (int i = 0; i < h(); i++)
            if (holes.get(i).area() >= 0) {
                /*
                 * std::cerr << std::endl << "\x1b[31m" << "The vertices of hole " << i <<
                 * " are not listed cw." << "\x1b[0m" << std::endl;
                 */
                return false;
            }

        return true;
    }

    double boundary_length() {
        // Precondition: nonempty Environment.
        assert (outer_boundary.n() > 0);

        double length_temp = outer_boundary.boundary_length();
        for (int i = 0; i < h(); i++)
            length_temp += holes.get(i).boundary_length();
        return length_temp;
    }

    double area() {
        double area_temp = outer_boundary.area();
        for (int i = 0; i < h(); i++)
            area_temp += holes.get(i).area();
        return area_temp;
    }

    ArrayList<Point> random_points(int count, double epsilon) {
        assert (area() > 0);

        BoundingBox bounding_box = bbox();
        ArrayList<Point> pts_in_environment = new ArrayList<Point>(count);
        Point pt_temp = new Point(
                Util.uniform_random_sample(bounding_box.x_min, bounding_box.x_max),
                Util.uniform_random_sample(bounding_box.y_min, bounding_box.y_max));
        while (pts_in_environment.size() < count) {
            while (!pt_temp.in(this, epsilon)) {
                pt_temp.set_x(Util.uniform_random_sample(bounding_box.x_min, bounding_box.x_max));
                pt_temp.set_y(Util.uniform_random_sample(bounding_box.y_min, bounding_box.y_max));
            }
            pts_in_environment.add(pt_temp);
            pt_temp.set_x(Util.uniform_random_sample(bounding_box.x_min, bounding_box.x_max));
            pt_temp.set_y(Util.uniform_random_sample(bounding_box.y_min, bounding_box.y_max));
        }
        return pts_in_environment;
    }

    /*
     * unneeded -DMT Polyline shortest_path( Point start, Point finish, Visibility_Graph
     * visibility_graph, double epsilon) { //true => data printed to terminal //false => silent
     * boolean PRINTING_DEBUG_DATA = false;
     * 
     * //For now, just find one shortest path, later change this to a //vector to find all shortest
     * paths (w/in epsilon). Polyline shortest_path_output; Visibility_Polygon
     * start_visibility_polygon(start, *this, epsilon);
     * 
     * //Trivial cases if( distance(start,finish) <= epsilon ){ shortest_path_output.add(start);
     * return shortest_path_output; } else if( finish.in(start_visibility_polygon, epsilon) ){
     * shortest_path_output.add(start); shortest_path_output.add(finish); return
     * shortest_path_output; }
     * 
     * Visibility_Polygon finish_visibility_polygon(finish, *this, epsilon);
     * 
     * //Connect start and finish Points to the visibility graph boolean *start_visible; //start row
     * of visibility graph boolean *finish_visible; //finish row of visibility graph start_visible =
     * new boolean[n()]; finish_visible = new boolean[n()]; for(int k=0; k<n(); k++){ if(
     * (*this)(k).in( start_visibility_polygon , epsilon ) ) start_visible[k] = true; else
     * start_visible[k] = false; if( (*this)(k).in( finish_visibility_polygon , epsilon ) )
     * finish_visible[k] = true; else finish_visible[k] = false; }
     * 
     * //Initialize search tree of visited nodes std::list<Shortest_Path_Node> T; //:WARNING: //If T
     * is a vector it is crucial to make T large enough that it //will not be resized. If T were
     * resized, any iterators pointing //to its contents would be invalidated, thus causing the
     * program //to fail. //T.reserve( n() + 3 );
     * 
     * //Initialize priority queue of unexpanded nodes std::set<Shortest_Path_Node> Q;
     * 
     * //ruct initial node Shortest_Path_Node current_node; //convention vertex_index == n() =>
     * corresponds to start Point //vertex_index == n() + 1 => corresponds to finish Point
     * current_node.vertex_index = n(); current_node.cost_to_come = 0;
     * current_node.estimated_cost_to_go = distance( start , finish ); //Put in T and on Q T.add(
     * current_node ); T.begin()->search_tree_location = T.begin();
     * current_node.search_tree_location = T.begin(); T.begin()->parent_search_tree_location =
     * T.begin(); current_node.parent_search_tree_location = T.begin(); Q.insert( current_node );
     * 
     * //Initialize temporary variables Shortest_Path_Node child; //children of current_node
     * ArrayList<Shortest_Path_Node> children; //flags boolean solution_found = false; boolean
     * child_already_visited = false; //-----------Begin Main Loop----------- while( !Q.empty() ){
     * 
     * //Pop top element off Q onto current_node current_node = *Q.begin(); Q.erase( Q.begin() );
     * 
     * if(PRINTING_DEBUG_DATA){ std::cout << std::endl <<"=============="
     * <<" current_node just poped off of Q " <<"==============" << std::endl; current_node.print();
     * std::cout << std::endl; }
     * 
     * //Check for goal state //(if current node corresponds to finish) if(
     * current_node.vertex_index == n() + 1 ){
     * 
     * if( PRINTING_DEBUG_DATA ){ std::cout <<"solution found!" << std::endl << std::endl; }
     * 
     * solution_found = true; break; }
     * 
     * //Expand current_node (compute children) children.clear();
     * 
     * if( PRINTING_DEBUG_DATA ){ std::cout << "-------------------------------------------" <<
     * std::endl << "Expanding Current Node (Computing Children)" << std::endl <<
     * "current size of search tree T = " << T.size() << std::endl <<
     * "-------------------------------------------" << std::endl; }
     * 
     * //if current_node corresponds to start if( current_node.vertex_index == n() ){ //loop over
     * environment vertices for(int i=0; i < n(); i++){ if( start_visible.get(i) ){
     * child.vertex_index = i; child.parent_search_tree_location =
     * current_node.search_tree_location; child.cost_to_come = distance( start , (*this)(i) );
     * child.estimated_cost_to_go = distance( (*this)(i) , finish ); children.add( child );
     * 
     * if( PRINTING_DEBUG_DATA ){ std::cout << std::endl << "computed child: " << std::endl;
     * child.print(); }
     * 
     * } } } //else current_node corresponds to a vertex of the environment else{ //check which
     * environment vertices are visible for(int i=0; i < n(); i++){ if( current_node.vertex_index !=
     * i ) if( visibility_graph( current_node.vertex_index , i ) ){ child.vertex_index = i;
     * child.parent_search_tree_location = current_node.search_tree_location; child.cost_to_come =
     * current_node.cost_to_come + distance( (*this)(current_node.vertex_index), (*this)(i) );
     * child.estimated_cost_to_go = distance( (*this)(i) , finish ); children.add( child );
     * 
     * if( PRINTING_DEBUG_DATA ){ std::cout << std::endl << "computed child: " << std::endl;
     * child.print(); }
     * 
     * } } //check if finish is visible if( finish_visible[ current_node.vertex_index ] ){
     * child.vertex_index = n() + 1; child.parent_search_tree_location =
     * current_node.search_tree_location; child.cost_to_come = current_node.cost_to_come + distance(
     * (*this)(current_node.vertex_index) , finish ); child.estimated_cost_to_go = 0; children.add(
     * child );
     * 
     * if( PRINTING_DEBUG_DATA ){ std::cout << std::endl << "computed child: " << std::endl;
     * child.print(); }
     * 
     * } }
     * 
     * if( PRINTING_DEBUG_DATA ){ std::cout << std::endl
     * <<"-----------------------------------------" << std::endl << "Processing " <<
     * children.size() << " children" << std::endl << "-----------------------------------------" <<
     * std::endl; }
     * 
     * //Process children for( ArrayList<Shortest_Path_Node>::iterator children_itr =
     * children.begin(); children_itr != children.end(); children_itr++ ){ child_already_visited =
     * false;
     * 
     * if( PRINTING_DEBUG_DATA ){ std::cout << std::endl << "current child being processed: " <<
     * std::endl; children_itr->print(); }
     * 
     * //Check if child state has already been visited //(by looking in search tree T) for(
     * std::list<Shortest_Path_Node>::iterator T_itr = T.begin(); T_itr != T.end(); T_itr++ ){ if(
     * children_itr->vertex_index == T_itr->vertex_index ){ children_itr->search_tree_location =
     * T_itr; child_already_visited = true; break; } }
     * 
     * if( !child_already_visited ){ //Add child to search tree T T.add( *children_itr );
     * (--T.end())->search_tree_location = --T.end(); children_itr->search_tree_location =
     * --T.end(); Q.insert( *children_itr ); } else if(
     * children_itr->search_tree_location->cost_to_come > children_itr->cost_to_come ){ //redirect
     * parent pointer in search tree children_itr->search_tree_location->parent_search_tree_location
     * = children_itr->parent_search_tree_location; //and update cost data
     * children_itr->search_tree_location->cost_to_come = children_itr->cost_to_come; //update Q
     * for(std::set<Shortest_Path_Node>::iterator Q_itr = Q.begin(); Q_itr!= Q.end(); Q_itr++){ if(
     * children_itr->vertex_index == Q_itr->vertex_index ){ Q.erase( Q_itr ); break; } } Q.insert(
     * *children_itr ); }
     * 
     * //If not already visited, insert into Q if( !child_already_visited ) Q.insert( *children_itr
     * );
     * 
     * if( PRINTING_DEBUG_DATA ){ std::cout << "child already visited? " << child_already_visited <<
     * std::endl; }
     * 
     * } } //-----------End Main Loop-----------
     * 
     * //Recover solution if( solution_found ){ shortest_path_output.add( finish );
     * std::list<Shortest_Path_Node>::iterator backtrace_itr =
     * current_node.parent_search_tree_location; Point waypoint;
     * 
     * if( PRINTING_DEBUG_DATA ){ std::cout << "----------------------------" << std::endl <<
     * "backtracing to find solution" << std::endl << "----------------------------" << std::endl;
     * 
     * }
     * 
     * while( true ){
     * 
     * if( PRINTING_DEBUG_DATA ){ std::cout << "backtrace node is " << std::endl;
     * backtrace_itr->print(); std::cout << std::endl; }
     * 
     * if( backtrace_itr->vertex_index < n() ) waypoint = (*this)( backtrace_itr->vertex_index );
     * else if( backtrace_itr->vertex_index == n() ) waypoint = start; //Add vertex if not redundant
     * if( shortest_path_output.size() > 0 && distance( shortest_path_output[
     * shortest_path_output.size() - 1 ], waypoint ) > epsilon ) shortest_path_output.add( waypoint
     * ); if( backtrace_itr->cost_to_come == 0 ) break; backtrace_itr =
     * backtrace_itr->parent_search_tree_location; } shortest_path_output.reverse(); }
     * 
     * //free memory delete [] start_visible; delete [] finish_visible;
     * 
     * //shortest_path_output.eliminate_redundant_vertices( epsilon ); //May not be desirable to
     * eliminate redundant vertices, because //those redundant vertices can make successive
     * waypoints along the //shortest path robustly visible (and thus easier for a robot to
     * //navigate)
     * 
     * return shortest_path_output; } Polyline shortest_path( Point& start, Point& finish, double
     * epsilon) { return shortest_path( start, finish, Visibility_Graph(*this, epsilon), epsilon );
     * }
     * 
     * 
     * void write_to_file( String& filename, int fios_precision_temp) { assert( fios_precision_temp
     * >= 1 );
     * 
     * std::ofstream fout( filename.c_str() ); //fout.open( filename.c_str() ); //Alternatives.
     * //fout << *this; fout.setf(std::ios::fixed); fout.setf(std::ios::showpoint);
     * fout.precision(fios_precision_temp); fout << "//Environment Model" << std::endl; fout <<
     * "//Outer Boundary" << std::endl << outer_boundary; for(int i=0; i<h(); i++) { fout <<
     * "//Hole" << std::endl << holes.get(i); } //fout << "//EOF marker"; fout.close(); }
     */
    BoundingBox bbox() {
        return outer_boundary.bbox();
    }

    Polygon get(int i) {
        if (i == 0) {
            return outer_boundary;
        } else {
            return holes.get(i - 1);
        }
    }

    public void enforce_standard_form() {
        if (outer_boundary.area() < 0)
            outer_boundary.reverse();
        outer_boundary.enforce_standard_form();
        for (int i = 0; i < h(); i++) {
            if (holes.get(i).area() > 0)
                holes.get(i).reverse();
            holes.get(i).enforce_standard_form();
        }
    }

    void eliminate_redundant_vertices(double epsilon) {
        outer_boundary.eliminate_redundant_vertices(epsilon);
        for (int i = 0; i < holes.size(); i++)
            holes.get(i).eliminate_redundant_vertices(epsilon);

        update_flattened_index_key();
    }

    void reverse_holes() {
        for (int i = 0; i < holes.size(); i++)
            holes.get(i).reverse();
    }

    void update_flattened_index_key() {
        flattened_index_key.clear();

        for (int i = 0; i <= h(); i++) {
            for (int j = 0; j < get(i).n(); j++) {
                pair<Integer, Integer> pair_temp = new pair<Integer, Integer>(i, j);
                flattened_index_key.add(pair_temp);
            }
        }
    }

    pair<Integer, Integer> one_to_two(int k) {
        pair<Integer, Integer> two = new pair<Integer, Integer>(0, 0);
        // Strategy: add up vertex count of each Polygon (outer boundary +
        // holes) until greater than k
        int current_polygon_index = 0;
        int vertex_count_up_to_current_polygon = get(0).n();
        int vertex_count_up_to_last_polygon = 0;

        while (k >= vertex_count_up_to_current_polygon && current_polygon_index < h()) {
            current_polygon_index++;
            two.first = two.first + 1;
            vertex_count_up_to_last_polygon = vertex_count_up_to_current_polygon;
            vertex_count_up_to_current_polygon += get(current_polygon_index).n();
        }
        two.second = k - vertex_count_up_to_last_polygon;

        return two;
    }

    public String toString() {
        String outs = "//Environment Model\n";
        outs += "//Outer Boundary\n" + get(0);
        for (int i = 1; i <= h(); i++) {
            outs += "//Hole\n " + get(i);
        }
        // outs << "//EOF marker";
        return outs;
    }

    double boundary_distance(Point point_temp) {
        return point_temp.boundary_distance(this);
    }

}