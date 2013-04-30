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
 * Utility routines for geospatial operations
 */
 
otp.util.Geo = {

    stringToLatLng : function(str) {
        var arr = str.split(',');
        return new L.LatLng(parseFloat(arr[0]), parseFloat(arr[1]));
    },
    
    truncatedLatLng : function(latLngStr) {
        var ll = otp.util.Geo.stringToLatLng(latLngStr);
        return Math.round(ll.lat*100000)/100000+","+Math.round(ll.lng*100000)/100000;
    }

};


otp.util.Text = {

    capitalizeFirstChar : function(str) {
        return str.charAt(0).toUpperCase() + str.slice(1);
    },
    
    ordinal : function(n) {
        if(n > 10 && n < 14) return n+"th";
        switch(n % 10) {
            case 1: return n+"st";
            case 2: return n+"nd";
            case 3: return n+"rd";
        }
        return n+"th";
    },
    
    isNumber : function(str) {
        return !isNaN(parseFloat(str)) && isFinite(str);
    },
    
    endsWith : function(str, suffix) {
        return str.indexOf(suffix, str.length - suffix.length) !== -1;
    },
    
    constructUrlParamString : function(params) {
        var encodedParams = [];
        for(param in params) {
            encodedParams.push(param+"="+ encodeURIComponent(params[param]));
        }
        return encodedParams.join("&");
    }
    
}

otp.util.Itin = {

    isTransit : function(mode) {
        return mode === "TRANSIT" || mode === "SUBWAY" || mode === "BUS" || mode === "TRAM" || mode === "GONDOLA" || mode === "TRAINISH" || mode === "BUSISH";
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

    directionString : function(dir) { // placeholder until localization is addressed
        return dir.toLowerCase().replace('_',' ').replace('ly','');
    },
    
    distanceString : function(m) {
        var ft = m*3.28084;
        if(ft < 528) return Math.round(ft) + ' feet';
        return Math.round(ft/528)/10+" miles";
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
    
    getLegStepText : function(step) {
        var text = '';
        if(step.relativeDirection == "CIRCLE_COUNTERCLOCKWISE" || step.relativeDirection == "CIRCLE_CLOCKWISE") {
            text += 'Take roundabout ' +
                (step.relativeDirection == "CIRCLE_COUNTERCLOCKWISE" ? 'counter' : '')+'clockwise to ' +
                otp.util.Text.ordinal(step.exit)+' exit on '+step.streetName;
        }
        else {
            if(!step.relativeDirection) text += "Start on <b>"+step.streetName+"</b>";
            else {
                text += '<b>'+otp.util.Text.capitalizeFirstChar(this.directionString(step.relativeDirection))+'</b> '+
                            (step.stayOn ? "to continue on" : "on to")  + ' <b>' +step.streetName+"</b>";
            }
        }
        return text; // + ' and proceed <b>'+otp.util.Itin.distanceString(step.distance)+'</b>';
    }     
}
