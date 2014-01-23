/* This program is free software: you can redistribute it and/or
   modify it under the terms of the GNU Lesser General Public License
   as published by the Free Software Foundation, either version 3 of
   the License, or (at your option) any later version.
   
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   
   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

var OTPA = OTPA || {}; // namespace

/**
 * Accessibility function: step edge. w = 1 if t < t0
 */
OTPA.stepEdge = function(t0) {
    return function(t) {
        return t < t0 ? 1.0 : 0.0;
    };
}

/**
 * Accessibility function: smooth edge (sigmoid). A smooth function with: TODO:
 * Make sure w(t=0)=1 ?
 * 
 * <pre>
 * t            w
 * --------------------
 * t0 - 2.t1    0.880
 * t0 - t1      0.731
 * t0           0.5
 * t0 + t1      0.263
 * t0 + 2.t1    0.119
 * t0 + 4.t1    0.018
 * +inf         0
 * --------------------
 * </pre>
 */
OTPA.sigmoid = function(t0, t1) {
    var t1 = t1 || t0 / 4;
    return function(t) {
        return 1 / (1 + Math.exp((t - t0) / t1));
    }
}

/**
 * Scoring class.
 */

/**
 * Factory method
 */
OTPA.scoring = function() {
    return new OTPA.Scoring();
}

/**
 * Scoring constructor.
 */
OTPA.Scoring = function() {
};

/**
 * Compute an accessibility score for a POI list.
 */
OTPA.Scoring.prototype.score = function(timeGrid, poiList, wtFunc, beta) {
    var beta = beta || 1;
    var w = 0.0;
    for (var i = 0; i < poiList.size(); i++) {
        var poi = poiList.get(i);
        // Default POI weight is 1
        var poiW = poi.w || 1.0;
        var v = timeGrid.get(poi.location);
        if (v != null) {
            w += Math.pow(poiW * wtFunc(v.t), beta);
        }
    }
    return Math.pow(w, 1 / beta);
};
