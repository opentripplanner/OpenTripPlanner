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

/**
 * otp.core.GeocoderBag is an alternative to the otp.core.GeocoderBuiltin geocoder for usage inside the Netherlands
 * 
 * It will add a geocoder that can make requests to a BAG geocoder instance or any similar API
 *
 * BAG is a Dutch acronym for Basic Address Data and is a Netherlands-specific geo database.
 * On top of this dataset (in combination with other data-sets ) an open source geocoder has been developed
 * 
 * More information about the geocoder can be found http://blog.plannerstack.org/shop/bag42-vm/ or at https://github.com/calendar42/bag42/
 * 
 * USAGE: Replace or add the geocoder config inside config.geocoders in config.js with:
 *
 * {
 *     name: 'BAG geocoder',
 *     className: 'otp.core.GeocoderBag',
 *     url: 'http://example.bagurl.org/api/v0/geocode/json',
 *     addressParam: 'address'
 * }
 *
 * NOTE: the UI can handle multiple geocoders, it offers a dropdown in that case
 *
 */

otp.core.GeocoderBag = otp.Class({

    initialize : function(url, addressParam) {
        this.url = url;
        this.addressParam = addressParam;
    },

    geocode : function(address, callback) {
        var params = {};
        var this_ = this;

        // Make sure to add the star at the end of the query to return more free-matching addresses
        params[this.addressParam] = address+"*";

        $.getJSON(this.url, params)
            .done( function (data) {
                // Success: transform the data to a JSON array of objects containing lat, lng, and description fields as the client expects
                data = data.results.map(function (r) {
                    return {
                        "description": r.formatted_address,
                        "lat": r.geometry.location.lat,
                        "lng": r.geometry.location.lng,
                    };
                });
                callback.call(this, data);
            })
            .fail( function (err) {
                alert("Something went wrong retrieving the geocoder results from: " + this_.url + " for: " + address);
            });
    }
});
