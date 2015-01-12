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

otp.namespace("otp.analyst");

/**
 * Scoring class.
 */
otp.analyst.Scoring = otp.Class({

    /**
     * Scoring constructor.
     */
    initialize : function() {
    },

    /**
     * Compute an accessibility score for a POI list.
     */
    score : function(timeGrid, population, wtFunc, beta) {
        var beta = beta || 1;
        var w = 0.0;
        for (var i = 0; i < population.size(); i++) {
            var poi = population.get(i);
            // Default POI weight is 1
            var poiW = poi.w || 1.0;
            var v = timeGrid.get(poi.location);
            if (v != null) {
                w += Math.pow(poiW * wtFunc(v.z), beta);
            }
        }
        return Math.pow(w, 1 / beta);
    },

    /**
     * Compute histogram
     */
    histogram : function(timeGrid, population, min, max, step) {
        var histo = [];
        for (var i = 0; i < Math.floor((max - min) / step); i++) {
            histo[i] = {
                    w: 0.0,
                    t: i * step + min
            }
        }
        for (var i = 0; i < population.size(); i++) {
            var poi = population.get(i);
            // Default POI weight is 1
            var poiW = poi.w || 1.0;
            var v = timeGrid.get(poi.location);
            if (v != null && v.z >= min && v.z <= max) {
                var index = Math.floor((v.z - min) / step);
                histo[index].w = histo[index].w + poiW * 1.0;
            }
        }
        return histo;
    }
});

/**
 * Accessibility function: step edge. w = 1 if t < t0
 */
otp.analyst.Scoring.stepEdge = function(t0) {
    return function(t) {
        return t < t0 ? 1.0 : 0.0;
    };
};

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
otp.analyst.Scoring.sigmoid = function(t0, t1) {
    var t1 = t1 || t0 / 4;
    return function(t) {
        return 1 / (1 + Math.exp((t - t0) / t1));
    };
};
