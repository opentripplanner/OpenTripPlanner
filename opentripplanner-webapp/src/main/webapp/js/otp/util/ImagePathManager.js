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

otp.namespace("otp.util");

/**
 * The imagePathManager takes care of returning the right path to an
 * image whether a custom icon is used for an agency or not.
 *
 * The application controller currently takes care of populating the
 * list of agencies using custom icons.
 *
 * Example usage:
 * otp.util.imagePathManager.imagePath({agencyId: 'MTA NYCT', route: 'N', mode: 'SUBWAY', imageType: 'marker'})
 */

otp.util.imagePathManager = (function() {

    // the agencies that custom paths will be generated for
    var useCustomIconsForAgencies = [];

    return {
        addCustomAgencies: function(newCustomAgencies)
        {
            var agencies = typeof newCustomAgencies === 'string' ? [newCustomAgencies] : newCustomAgencies;
            Ext.each(agencies, function(agency) { useCustomIconsForAgencies.push(agency); });
        },

        /**
         * Generate the appropriate image path given the options
         * here are the options used:
         *   - agencyId
         *   - mode
         *   - route (optional, but required if a custom path is used)
         *   - imageType (optional, but controls what kind of path is generated)
         * The case is a bit hacky, in that the upper case mode and route are used for the custom path
         * but the the lower case version of the mode is used for the generic case
         */
        imagePath: function(options)
        {
            if (typeof options.mode !== 'string') {
                throw "ImagePathManager imagePath: Mode not specified";
            }

            if (useCustomIconsForAgencies.indexOf(options.agencyId) !== -1) {
                // route must also be specified if we have a custom agencyId
                if (typeof options.route !== "string") {
                    throw "ImagePathManager imagePath: Route not specified for custom agencyId: " + options.agencyId;
                }
                
                var path = 'custom/' + options.agencyId + '/' + options.mode.toUpperCase() + '/' + options.route.toUpperCase();
                if (typeof options.imageType === 'string') {
                    path += '-' + options.imageType;
                }
                path += '.png';
                return path;
            } else {
                var path = 'images/';
                path += options.imageType === 'marker' ? 'map' : 'ui';
                path += '/trip/mode/' + options.mode.toLowerCase() + '.png';
                return path;
            }
        }
    };
})();
