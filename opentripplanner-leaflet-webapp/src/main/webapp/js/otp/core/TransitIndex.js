/* This program is free software: you can redistribute it and/or
   modify it under the teMap.jsrms of the GNU Lesser General Public License
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

otp.core.TransitIndex = otp.Class({

    webapp          : null,

    routes  : { },
    
    initialize : function(webapp) {
        var this_ = this;
        this.webapp = webapp;
        
        
        // load route data from server
        
        var url = otp.config.hostname + '/opentripplanner-api-webapp/ws/transit/routes';
        $.ajax(url, {
            dataType:   'jsonp',
                
            success: function(data) {
                console.log(data);
                for(var i=0; i<data.routes.length; i++) {
                    var routeData = data.routes[i];
                    var id = routeData.id.agencyId+"_"+routeData.id.id;
                    this_.routes[id] = {
                        routeData : routeData,
                        variants : null
                    };
                }
            }
        });        
    }
});
