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

otp.namespace("otp.systemmap");

/**
 * Popup logic for the system map
 */

otp.systemmap.Popup = {

    // reference to an openlayers map object
    map: null,

    // rich structure representing data for the popup
    doc: null,

    // class to assign to the popup
    // if null, the default openlayers behavior is used
    klass: null,

    // whether to display departure information in the popup
    displayDepartures: null,

    // openlayers popup instance
    popup: null,

    // xy location, like from openlayers event xy
    xy: null,

    // set to the timeout id when the popup is triggered to be closed
    timeoutId: null,

    // reference to the system map
    sysmap: null,

    initialize: function(config) {
        otp.configure(this, config);

//         var routes = this.doc.routes.route;
//         var departures = this.doc.departures.departure;
        var routes = this.doc.routes;
        var departures = this.doc.departures;
        if (!departures instanceof Array) {
            departures = [departures];
        }
        if (!routes instanceof Array) {
            routes = [routes];
        }
        
        var popupTemplate = new Ext.Template(['<h2>{stopName}</h2>',
                                              '<ul class="routelist">',
                                              '{routeList}',
                                              '</ul>']);
                            
        var routeListTemplate = new Ext.Template('<li><img src="{iconPath}" /></li>');
        var routeList = [];
        var routesSeen = {};
        Ext.each(routes, function(route) {
                // uniquify route names
                // for example, in new york we have the 7 and the 7 express
                // these share the same name but a different id
                // we only use the short name for now
                var shortName = route.shortName;
                if (shortName in routesSeen) {
                    return;
                }
                var iconPath = otp.util.imagePathManager.imagePath({agencyId: route.agencyId,
                                                                    mode: route.mode,
                                                                    route: shortName
                    });
                routeList.push(routeListTemplate.apply({iconPath: iconPath}));
                routesSeen[shortName] = iconPath;
            });
        routeList = routeList.join('');

        var html = popupTemplate.apply({stopName: this.doc.name,
                                        routeList: routeList
            });

        if (this.displayDepartures) {
            var upcomingDeparturesTemplate = new Ext.Template(['<h3>Upcoming departures</h3>',
                                                               '<ul class="departures">',
                                                               '{departureList}',
                                                               '</ul>'
                                                               ]);
            
            var departuresTemplate = new Ext.Template(['<li class="departure">',
                                                       '<img src="{iconPath}" class="popup-route-icon" />',
                                                       '<strong>{headsign}</strong> - {dateFormatted}',
                                                       '</li>'
                                                       ]);
            departureList = [];
            Ext.each(departures, function(departure) {
                    var headsign = departure.headsign;
                    var date = departure.date;
                    var iconPath = routesSeen[departure.route.shortName];
                    departureList.push(departuresTemplate.apply({iconPath: iconPath,
                                    headsign: headsign,
                                    dateFormatted: otp.util.DateUtils.prettyTime(date)
                                    }));
                });
            departureList = departureList.join('');

            html += upcomingDeparturesTemplate.apply({departureList: departureList});
        }
        
        var pixel = new OpenLayers.Pixel(this.xy.x, this.xy.y);
        var lonlat = this.map.getLonLatFromPixel(pixel);
        // XXX projection?
        //lonlat = lonlat.transform(new OpenLayers.Projection(), new OpenLayers.Projection());
        
        var self = this;
        var popup = new OpenLayers.Popup(null,
                                         lonlat,
                                         new OpenLayers.Size(200, 200),
                                         html,
                                         true,
                                         function() { self.sysmap.popupClosed(); self.removePopup(); }
                                         );
        if (this.klass) {
            popup.displayClass = this.klass;
            popup.div.className = popup.displayClass;
        }

        this.map.addPopup(popup);
        this.popup = popup;
    },

    removePopup: function() {
        if (this.popup) {
            this.map.removePopup(this.popup);
            this.popup.destroy();
            this.popup = null;
        }
    },

    triggerClose: function(timeout) {
        var self = this;
        this.timeoutId = setTimeout(function() { self.removePopup(); }, timeout);
    },

    CLASS_NAME: 'otp.systemmap.Popup'
};

otp.systemmap.Popup = new otp.Class(otp.systemmap.Popup);
