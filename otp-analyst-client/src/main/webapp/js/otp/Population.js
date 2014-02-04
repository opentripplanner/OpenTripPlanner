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
 * Population class. A population is a list of (weighted) locations.
 */

otp.analyst.Population = otp.Class({
    
    /**
     * Constructor. Create an empty population.
     */
    initialize : function() {
        this.data = [];
    },

    /**
     * Load population from JSON data.
     */
    loadFromJson : function(jsonUrl) {
        var thisPl = this;
        $.ajax({
            url : jsonUrl,
            success : function(result) {
                if (typeof result === 'string')
                    result = $.parseJSON(result);
                for (var i = 0; i < result.length; i++) {
                    thisPl.data.push(result[i]);
                }
            },
            async : false
        });
        return this;
    },

    /**
     * Add a location programmatically.
     */
    add : function(poi) {
        this.data.push(poi);
        return this;
    },

    /**
     * TODO How to properly implement iterators in JS?
     * 
     * @return The number of elements in the list.
     */
    size : function(poi) {
        return this.data.length;
    },

    /**
     * @return The nth element in the list.
     */
    get : function(index) {
        return this.data[index];
    }
});
