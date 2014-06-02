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
        return mode === "TRANSIT" || mode === "SUBWAY" || mode === "RAIL" || mode === "BUS" || mode === "TRAM" || mode === "GONDOLA" || mode === "TRAINISH" || mode === "BUSISH";
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

    includesBicycle : function(mode) {
        var modeArr = mode.split(",");
        for(var i = 0; i < modeArr.length; i++) {
            if(modeArr[i] === "BICYCLE") return true;
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

    directionString : function(dir) { // placeholder until localization is addressed
        return dir.toLowerCase().replace('_',' ').replace('ly','');
    },
    
    distanceString : function(m) {
        return otp.util.Geo.distanceString(m);
    },
    
    modeStrings : {
        'BUS' : 'Bus',
        'SUBWAY' : 'Subway',
        'TRAM' : 'Light Rail',
        'GONDOLA' : 'Aerial Tram',
    },
    
    modeString : function(mode) {
        if(mode in this.modeStrings) return this.modeStrings[mode];
        return mode;
    },
    
    getLegStepText : function(step, asHtml) {
        asHtml = (typeof asHtml === "undefined") ? true : asHtml;
        var text = '';
        if(step.relativeDirection == "CIRCLE_COUNTERCLOCKWISE" || step.relativeDirection == "CIRCLE_CLOCKWISE") {
            text += 'Take roundabout ' +
                (step.relativeDirection == "CIRCLE_COUNTERCLOCKWISE" ? 'counter' : '')+'clockwise to ' +
                otp.util.Text.ordinal(step.exit)+' exit on '+step.streetName;
        }
        else {
            if(!step.relativeDirection) text += "Start on" + (asHtml ? " <b>" : " ") + step.streetName + (asHtml ? "</b>" : "") + " heading " + step.absoluteDirection.toLowerCase();
            else {
                text += (asHtml ? "<b>" : "") + otp.util.Text.capitalizeFirstChar(this.directionString(step.relativeDirection)) +
                            (asHtml ? "</b>" : "") + ' ' + (step.stayOn ? "to continue on" : "on to")  + (asHtml ? " <b>" : " ") +
                            step.streetName + (asHtml ? "</b>" : "");
            }
        }
        return text;
    },
    
    getRouteDisplayString : function(routeData) {
        var str = routeData.routeShortName ? '('+routeData.routeShortName+') ' : '';
        str += routeData.routeLongName;
        return str;
    },
    
    getRouteShortReference : function(routeData) {
        return routeData.routeShortName || routeData.id.id;
    },    
}
