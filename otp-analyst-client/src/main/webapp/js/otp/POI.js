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
 * POI list class.
 */

/**
 * Factory method.
 */
OTPA.poiList = function() {
    return new OTPA.POIList();
}

/**
 * POIList constructor.
 */
OTPA.POIList = function() {
    this.poiList = [];
};

/**
 * Load POIs from JSON data.
 */
OTPA.POIList.prototype.loadFromJson = function(jsonUrl) {
    var thisPl = this;
    $.ajax({
        url : jsonUrl,
        success : function(result) {
            if (typeof result === 'string')
                result = $.parseJSON(result);
            for (var i = 0; i < result.length; i++) {
                thisPl.poiList.push(result[i]);
            }
        },
        async : false
    });
};

/**
 * Add a POI programmatically.
 */
OTPA.POIList.prototype.addPOI = function(poi) {
    this.poiList.push(poi);
    return poi;
};

/**
 * TODO How to properly implement iterators in JS?
 * 
 * @return The number of POI in the list.
 */
OTPA.POIList.prototype.size = function(poi) {
    return this.poiList.length;
};

/**
 * @return The nth POI in the list.
 */
OTPA.POIList.prototype.get = function(index) {
    return this.poiList[index];
};
