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
    }
}

otp.util.Itin = {

    getFirstStop : function(itin) {
        for(var l=0; l<itin.legs.length; l++) {
            var leg = itin.legs[l];
            if(otp.util.Itin.isTransit(leg.mode)) {
                return leg.from.stopId.agencyId+"_"+leg.from.stopId.id;
            }
        }
        return null;
    },
    
    isTransit : function(mode) {
        return mode === "TRANSIT" || mode === "SUBWAY" || mode === "BUS" || mode === "TRAM" || mode === "GONDOLA";
    },
    
    getIconSummaryHTML : function(itin, padding) {
        var html = '';
        for(var i=0; i<itin.legs.length; i++) {
            html += '<img src="images/mode/'+itin.legs[i].mode.toLowerCase()+'.png" >';
            if(i < itin.legs.length-1) html += '<img src="images/mode/arrow.png" style="margin: 0px '+(padding || '3')+'px;">';
        }
        return html;
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
    },

    // placeholder until localization is addressed
    directionString : function(dir) {
        return dir.toLowerCase().replace('_',' ').replace('ly','');
    },
    
    distanceString : function(m) {
        var ft = m*3.28084;
        if(ft < 528) return Math.round(ft) + ' feet';
        return Math.round(ft/528)/10+" miles";
    }
    
       
}
