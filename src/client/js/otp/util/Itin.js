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
 * Utility routines for OTP itinerary-based operations
 */

otp.util.Itin = {

    /**
     * Extracts the "place" from an OTP "name::place" string, where "place" is
     * a latitude,longitude string or a vertex ID.
     *
     * @param {string} locationStr an OTP GenericLocation string
     * @return {string} the "place" component of an OTP location string
     */

    getLocationPlace : function(locationStr) {
        return locationStr.indexOf("::") != -1 ?
            locationStr.split("::")[1] : locationStr;
    },


    /**
     * Extracts the "name" from an OTP "name::place" string, if present
     *
     * @param {string} locationStr an OTP GenericLocation string
     * @return {string} the "name" component of an OTP location string, null if not present
     */

    getLocationName : function(locationStr) {
        return locationStr.indexOf("::") != -1 ?
            locationStr.split("::")[0] : null;
    },

    /**
     * Extracts the "lat,lng" from an OTP "name::lat,lng" or just "lat,lng"  if "name::" not present
     *
     * @param {string} locationStr an OTP GenericLocation string
     * @return {{lat,lng}} the "lat" and "lng" component of an OTP location string "[name::]lat,lng". returns null if not present
     */
    getLocationLatLng : function(locationStr) {
        var location = locationStr.indexOf("::") != -1 ?
            locationStr.split("::")[1] : locationStr;
        if (locationStr.indexOf(",") == -1) return null;

        var [lat,lng]=location.split(",");
        return {lat:lat,lng:lng};
    },

    /**
     * Extracts the unqualified mode from an OTP "mode_qualifier" string
     *
     * @param {string} qualifiedModeStr an OTP QualifiedMode string
     * @return {string} the (unqualified) mode component of an OTP qualified mode string
     */

    getUnqualifiedMode : function(qualifiedModeStr) {
        return qualifiedModeStr.split("_")[0];
    },

    /**
     * Extracts the qualifier from an OTP "mode_qualifier" string, if present
     *
     * @param {string} qualifiedModeStr an OTP QualifiedMode string
     * @return {string} the qualifier component of an OTP qualified mode string, null if not present
     */

    getModeQualifier : function(qualifiedModeStr) {
        return qualifiedModeStr.indexOf("_") != -1 ?
            qualifiedModeStr.split("_")[1] : null;
    },

    isTransit : function(mode) {
        return mode === "TRANSIT"
            || mode === "RAIL"
            || mode === "COACH"
            || mode === "SUBWAY"
            || mode === "BUS"
            || mode === "TRAM"
            || mode === "FERRY"
            || mode === "AIRPLANE"
            || mode === "CABLE_CAR"
            || mode === "GONDOLA"
            || mode === "FUNICULAR"
            || mode === "TROLLEYBUS"
            || mode === "MONORAIL"
            || mode === "CARPOOL"
            || mode === "TAXI";
    },

    includesTransit : function(mode) {
        var modeArr = mode.split(",");
        for(var i = 0; i < modeArr.length; i++) {
            if(this.isTransit(modeArr[i])) return true;
        }
        return false;
    },

    includesWalk : function(mode) {
        var modeArr = mode.split(",");
        for(var i = 0; i < modeArr.length; i++) {
            if(modeArr[i] === "WALK") return true;
        }
        return false;
    },

    includesAnyBicycle : function(mode) {
        var modeArr = mode.split(",");
        for(var i = 0; i < modeArr.length; i++) {
            if(this.getUnqualifiedMode(modeArr[i]) === "BICYCLE") return true;
        }
        return false;
    },

    includesAnyCar : function(mode) {
        var modeArr = mode.split(",");
        for(var i = 0; i < modeArr.length; i++) {
            if(this.getUnqualifiedMode(modeArr[i]) === "CAR") return true;
        }
        return false;
    },

    absoluteDirectionStrings : {
        // note: keep these lower case (and uppercase via template / code if needed)
        //TRANSLATORS: Start on [street name] heading [Absolute direction] used in travel plan generation
        'NORTH': _tr('north'),
        'NORTHEAST': _tr('northeast'),
        'EAST': _tr('east'),
        'SOUTHEAST': _tr('southeast'),
        'SOUTH': _tr('south'),
        'SOUTHWEST': _tr('southwest'),
        'WEST': _tr('west'),
        'NORTHWEST': _tr('northwest'),
    },

    /**
     * Returns localized absolute direction string
     *
     * @param {string} dir a absolute direction string from a server
     * @return {string} localized absolute direction string
     */

    getLocalizedAbsoluteDirectionString : function(dir) {
        if (dir in this.absoluteDirectionStrings) return this.absoluteDirectionStrings[dir];
        // This is used if dir isn't found in directionStrings
        // This shouldn't happen
        return dir.toLowerCase();
    },

    relativeDirectionStrings : {
        // note: keep these lower case (and uppercase via template / code if needed)
        //TRANSLATORS: depart at street name/corner of x y etc. (First instruction in
        //itinerary)
        'DEPART': pgettext("itinerary", "depart"),
        //TRANSLATORS: [Relative direction (Hard/Slightly Left/Right...)] to continue
        //on /on to [streetname]
        'HARD_LEFT': _tr("hard left"),
        'LEFT': _tr("left"),
        'SLIGHTLY_LEFT': _tr("slight left"),
        'CONTINUE': _tr("continue"),
        'SLIGHTLY_RIGHT': _tr("slight right"),
        'RIGHT': _tr("right"),
        'HARD_RIGHT': _tr("hard right"),
        // rather than just being a direction, this should be
        // full-fledged to take just the exit name at the end
        'ELEVATOR': _tr("elevator"),
        'UTURN_LEFT': _tr("U-turn left"),
        'UTURN_RIGHT': _tr("U-turn right")
    },


    /**
     * Returns localized relative direction string
     *
     * @param {string} dir a relative direction string from a server
     * @return {string} localized direction string
     */

    getLocalizedRelativeDirectionString : function(dir) {
        if (dir in this.relativeDirectionStrings) return this.relativeDirectionStrings[dir];
        // This is used if dir isn't found in directionStrings
        // This shouldn't happen
        return dir.toLowerCase().replace('_',' ').replace('ly','');
    },

    distanceString : function(m) {
        return otp.util.Geo.distanceString(m);
    },

    durationString : function (startTime, endTime) {
      return otp.util.Time.secsToHrMin( (endTime - startTime)/1000.0 );
    },

    modeStrings : {
        //TRANSLATORS: Walk distance to place (itinerary header)
        'WALK': _tr('Walk'),
        //TRANSLATORS: Cycle distance to place (itinerary header)
        'BICYCLE': _tr('Cycle'),
        //TRANSLATORS: Car distance to place (itinerary header)
        'CAR': _tr('Car'),
        //TRANSLATORS: Bus: (route number) Start station to end station (itinerary header)
        'BUS' : _tr('Bus'),
        'SUBWAY' : _tr('Subway'),
        //TRANSLATORS: Used for intercity or long-distance travel.
        'RAIL' : _tr('Train'),
        'FERRY' : _tr('Ferry'),
        //TRANSLATORS: Tram, Streetcar, Light rail. Any light rail or street
        //level system within a metropolitan area.
        'TRAM' : _tr('Light Rail'),
        //TRANSLATORS: Used for street-level cable cars where the cable runs
        //beneath the car.
        'CABLE_CAR': _tr('Cable Car'),
        //TRANSLATORS: Any rail system designed for steep inclines.
        'FUNICULAR': _tr('Funicular'),
        //TRANSLATORS: Gondola, Suspended cable car. Typically used for aerial
        //cable cars where the car is suspended from the cable.
        'GONDOLA' : _tr('Aerial Tram'),
        'AIRPLANE' : _tr('Airplane'),
    },

    modeString : function(mode) {
        if(mode in this.modeStrings) return this.modeStrings[mode];
        return mode;
    },

    vertexTypeStrings : {
        //TRANSLATORS: WALK/CYCLE distance to [Bicycle rental station]
        'BIKESHARE_EMPTY': _tr('Vehicle rental station'),
        //TRANSLATORS: WALK/CYCLE distance to [Bicycle rental] {name}
        'BIKESHARE': _tr('Vehicle rental'),
        'TRANSIT': ""
    },

    /**
     * Returns localized place name
     *
     * based on vertexType and place name
     *
     * Currently only bike sharing is supported
     * @param {string} place
     * @return {string} localized place name
     */
    getName : function(place) {
        if ('vertexType' in place) {
            var vertexType = place.vertexType;
            if (place.name === null) {
                vertexType += "_EMPTY";
            }
            if (vertexType in this.vertexTypeStrings) {
                const name = `${this.vertexTypeStrings[vertexType]} ${place.name}`;
                if (place.networks && place.networks.length) {
                    const network = place.networks[0];
                    return `${name} (${network})`;
                } else {
                    return name;
                }

            } else {
                if (vertexType !== "NORMAL") {
                    console.log(vertexType + " not found in strings!");
                }
                return place.name;
            }
        } else {
            console.log("vertexType missing in place");
            return place.name;
        }
    },

    getLegStepText : function(step, asHtml) {
        asHtml = (typeof asHtml === "undefined") ? true : asHtml;
        var text = '';
        if(step.relativeDirection == "CIRCLE_COUNTERCLOCKWISE" || step.relativeDirection == "CIRCLE_CLOCKWISE") {
            var sprintf_values = {
                'ordinal_exit_number': otp.util.Text.ordinal(step.exit),
                'street_name': step.streetName
            };
            if (step.relativeDirection == "CIRCLE_COUNTERCLOCKWISE") {
                if (asHtml) {
                    text +=  _tr('Take roundabout <b>counterclockwise</b> to <b>%(ordinal_exit_number)s</b> exit on <b>%(street_name)s</b>', sprintf_values);
                } else {
                    text +=  _tr('Take roundabout counterclockwise to %(ordinal_exit_number)s exit on %(street_name)s', sprintf_values);
                }
            } else {
                if (asHtml) {
                    text +=  _tr('Take roundabout <b>clockwise</b> to <b>%(ordinal_exit_number)s</b> exit on <b>%(street_name)s</b>', sprintf_values);
                } else {
                    text +=  _tr('Take roundabout clockwise to %(ordinal_exit_number)s exit on %(street_name)s', sprintf_values);
                }
            }

        }
        else {
            //TODO: Absolute direction translation
            //TRANSLATORS: Start on [stret name] heading [compas direction]
            if(!step.relativeDirection || step.relativeDirection === "DEPART") {
                text += _tr("Start on") + (asHtml ? " <b>" : " ") + step.streetName + (asHtml ? "</b>" : "") + _tr(" heading ") + (asHtml ? "<b>" : "") + this.getLocalizedAbsoluteDirectionString(step.absoluteDirection) + (asHtml ? "</b>" : "");
            }
            else {
                text += (asHtml ? "<b>" : "") + otp.util.Text.capitalizeFirstChar(this.getLocalizedRelativeDirectionString(step.relativeDirection)) +
                            (asHtml ? "</b>" : "") + ' ' +
                            //TRANSLATORS: [Relative direction (Left/Right...)] to continue
                            //on /on to [streetname]
                            (step.stayOn ? _tr("to continue on") : _tr("on to"))  + (asHtml ? " <b>" : " ") +
                            step.streetName + (asHtml ? "</b>" : "");
            }
        }
        if (step.walkingBike) {
            text += ' 🚶';
        }
        return text;
    },

    getRouteDisplayString : function(routeData) {
        var str = routeData.shortName ? '('+routeData.shortName+') ' : '';
        str += routeData.longName;
        return str;
    },

    getRouteShortReference : function(routeData) {
        return routeData.routeShortName || routeData.id.id;
    },

    /**
     * Takes an agency/stop ID in one of two representations ('agency:id' string
     * or { agencyId, id } object) and returns it as the object form.
     */
    getStopIdObj : function (stopId) {
      if(stopId.agencyId && stopId.id) return stopId
      var parts = stopId.split(':')
      return {
        'agencyId': parts.shift(),
        'id': parts.join(':')
      }
    },

    getModeColor : function(mode) {
        if(mode === "WALK") return '#444';
        if(mode === "BICYCLE") return '#44f';
        if(mode === "SCOOTER") return '#88f';
        if(mode === "CAR") return '#444';
        if(mode === "RAIL") return '#b00';
        if(mode === "COACH") return '#0f0';
        if(mode === "SUBWAY") return '#f00';
        if(mode === "BUS") return '#0f0';
        if(mode === "TRAM") return '#f00';
        if(mode === "TROLLEYBUS") return '#0f0';
        if(mode === "FERRY") return '#f0f';
        if(mode === "AIRPLANE") return '#f0f';
        if(mode === "CABLE_CAR") return '#f0f';
        if(mode === "GONDOLA") return '#f0f';
        if(mode === "FUNICULAR") return '#f0f';
        if(mode === "MONORAIL") return '#f0f';
        if(mode === "CARPOOL") return '#61980a';
        if(mode === "TAXI") return '#ecc029';
        return '#aaa';
    },

    getModeIcon : function (mode) {
        return otp.config.resourcePath + 'images/mode/' + mode.toLowerCase()
            + '.png';
    },

    getLegTextColor : function (leg) {
        if (leg.routeTextColor) {
            return '#' + leg.routeTextColor;
        }
        else if(leg.mode === "WALK") {
            return '#fff';
        }
        else {
            return '#000000';
        }
    },

    getLegBackgroundColor: function (leg) {
        if (leg.routeColor) {
            return '#' + leg.routeColor;
        } else {
            return this.getModeColor(leg.mode);
        }
    },

    getLineStyle: function (leg) {
        if (leg.mode === "WALK") {
            return { weight: 4, opacity: 0.8}
        } else {
            return { weight: 8, opacity: 0.6}
        }
    },
}
