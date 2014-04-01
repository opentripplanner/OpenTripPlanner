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

class Ray {

    private Angle bearing;

    private VLPoint base_point;

  public double distance(VLPoint point_temp)
  {
    return point_temp.distance(
                     point_temp.projection_onto(this) );
  }

    public Angle bearing() {
        return bearing;
    }

    public VLPoint base_point() {
        return base_point;
    }

  public Ray(VLPoint base_point_temp, VLPoint bearing_point)
  { 
      assert(  !( base_point_temp.equals(bearing_point) )  ); 

      base_point = base_point_temp;
      bearing = new Angle( bearing_point.y-base_point_temp.y,
                            bearing_point.x-base_point_temp.x );
    }
    public Ray(VLPoint base_point_temp, Angle bearing_temp) {
        base_point = base_point_temp; 
        bearing = bearing_temp; 
    }

    public boolean equals(Object  o)
  {
      if (!(o instanceof Ray)) {
          return false;
      }
      Ray ray2 = (Ray)  o;
      if( base_point().equals( ray2.base_point)
          && bearing().equals(ray2.bearing ))
      return true;
    else
      return false;
  }


  LineSegment intersection( LineSegment line_segment_temp,
                            double epsilon)
  {

    //First construct a LineSegment parallel with the Ray which is so
    //long, that its intersection with line_segment_temp will be
    //equal to the intersection of this with line_segment_temp.
    double R = base_point().distance( line_segment_temp) 
               + line_segment_temp.length();
    LineSegment seg_approx =
        new LineSegment(  base_point, base_point.plus(
                     new VLPoint( R*Math.cos(bearing.get()),
                                R*Math.sin(bearing.get()) ))  );
    LineSegment intersect_seg = line_segment_temp.intersection(
                                              seg_approx,
                                              epsilon);
    //Make sure point closer to ray_temp's base_point is listed first.
    if( intersect_seg.size() == 2
        && intersect_seg.first().distance(  base_point() ) >
        intersect_seg.second().distance( base_point() )  ){
      intersect_seg.reverse();
    }
    return intersect_seg;
  }


}