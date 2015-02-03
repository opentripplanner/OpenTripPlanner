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

otp.namespace("otp.core");

otp.core.GeocoderBuiltin = otp.Class({

    url: 'otp/routers/default/geocode',

    initialize : function(url, addressParam) {
        // Do nothing, the proper address and query param are already known.
    },

    geocode : function(address, callback) {
        // The built in geocoder returns results in the form expected by the client:
        // A JSON array of objects containing lat, lng, and description fields.
        $.getJSON(this.url, {query: address}, function(response) {
            callback.call(this, response);
        });
    }

});
