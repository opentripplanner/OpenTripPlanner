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

otp.core.QueryLogger = otp.Class({
    
    serverURL  : null,
    
    module     : null,
    
    queryLoggedCallback : null,
    
    initialize : function(module) {
        this.module = module;
        this.serverURL = otp.config.loggerURL;
    },
    
    logQuery : function(queryParams, userName, password, fromPlaceName, toPlaceName) {
        if(this.serverURL == null) return;
        var this_ = this;
        
        // use truncated lat/lng if geocoded names not provided
        fromPlaceName = fromPlaceName || this.truncatedLatLng(queryParams.fromPlace);
        toPlaceName = toPlaceName || this.truncatedLatLng(queryParams.toPlace);
        
        this.currentRequest = $.ajax(this.serverURL+"/newQuery", {
            type: 'POST',
            data: {
                queryParams : JSON.stringify(queryParams),
                fromPlace : fromPlaceName,
                toPlace : toPlaceName,
                userName : userName,
                password : password 
            },
                
            success: function(data) {
                //console.log("logged query (post): from "+fromPlace+" to "+toPlace+" by "+this_.userName);
                this_.module.queryLogged();
            },
            
            error: function(data) {
                console.log("error logging query (post): from "+fromPlace+" to "+toPlace+" by "+this_.userName);
            }
        });
    },
    
    truncatedLatLng : function(latLngStr) {
        var ll = otp.util.Geo.stringToLatLng(latLngStr);
        return Math.round(ll.lat*100000)/100000+","+Math.round(ll.lng*100000)/100000;
    },

    CLASS_NAME : "otp.core.QueryLogger"
});
