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

otp.namespace("otp.layers");

var StopIcon20 = L.Icon.extend({
    options: {
        iconUrl: resourcePath + 'images/stop20.png',
        shadowUrl: null,
        iconSize: new L.Point(20, 20),
        iconAnchor: new L.Point(10, 10),
        popupAnchor: new L.Point(0, -5)
    }
});

otp.layers.StopsLayer =
    otp.Class(L.LayerGroup, {

    module : null,

    minimumZoomForStops : 15,

    initialize : function(module) {
        var this_ = this;
        L.LayerGroup.prototype.initialize.apply(this);
        this.module = module;

        this.stopsLookup = {};

        this.module.addLayer("stops", this);
        this.module.webapp.map.lmap.on('dragend zoomend', $.proxy(this.refresh, this));
    },

    refresh : function() {
        this.clearLayers();
        var lmap = this.module.webapp.map.lmap;
        if(lmap.getZoom() >= this.minimumZoomForStops) {
            this.module.webapp.indexApi.loadStopsInRectangle(null, lmap.getBounds(), this, function(data) {
                this.stopsLookup = {};
                for(var i = 0; i < data.length; i++) {
                    var agencyAndId = data[i].id;
                    this.stopsLookup[agencyAndId] = data[i];
                }
                this.updateStops();
            });
        }
    },

    updateStops : function(stops) {
        var stops = _.values(this.stopsLookup);
        var this_ = this;

        for(var i=0; i<stops.length; i++) {

            var stop = stops[i];
            stop.lat = stop.lat || stop.stopLat;
            stop.lon = stop.lon || stop.stopLon;

            var icon = new StopIcon20();

            m = L.marker([stop.lat, stop.lon], {
                icon : icon,
            });
            m._stop = stop;
            m._stopId = stop.id;
            m.addTo(this)
             .bindPopup('')
             .on('click', function() {
                var stopId = this._stopId;
                var stopIdArr = stopId.split(':');
                var marker = this;
                this_.module.webapp.indexApi.loadStopById(stopIdArr[0], stopIdArr[1], this_, function(detailedStop) {
                    marker.setPopupContent(this_.getPopupContent(detailedStop));
                    this_.module.webapp.indexApi.loadRoutesForStop(stopId, this_, function(data) {
                        _.each(data, function(route) {
                            ich['otp-stopsLayer-popupRoute'](route).appendTo($('.routeList'));
                        });
                    });
                });
             });


        }
    },

    getPopupContent : function(stop) {
        var this_ = this;

        var stop_viewer_trans = _tr('Stop Viewer');
        //TRANSLATORS: Plan Trip [From Stop| To Stop] Used in stoplayer popup
        var plan_trip_trans = _tr('Plan Trip');
        //TRANSLATORS: Plan Trip [From Stop| To Stop] Used in stoplayer popup
        var from_stop_trans = _tr('From Stop');
        //TRANSLATORS: Plan Trip [From Stop| To Stop] Used in stoplayer popup
        var to_stop_trans = _tr('To Stop');
        var routes_stop_trans = _tr('Routes Serving Stop');

        // TriMet-specific code
        if(stop.url && stop.url.indexOf("http://trimet.org") === 0) {
            var stopId = stop.id.split(':')[1];
            stop.titleLink = 'http://www.trimet.org/go/cgi-bin/cstops.pl?action=entry&resptype=U&lang=en&noCat=Landmark&Loc=' + stopId;
        }
        var context = _.clone(stop);
        context.agencyStopLinkText = otp.config.agencyStopLinkText || "Agency Stop URL";
        context.stop_viewer = stop_viewer_trans;
        context.routes_on_stop = routes_stop_trans;
        context.plan_trip = plan_trip_trans;
        context.from_stop = from_stop_trans;
        context.to_stop = to_stop_trans;
        var popupContent = ich['otp-stopsLayer-popup'](context);

        popupContent.find('.stopViewerLink').data('stop', stop).click(function() {
            var thisStop = $(this).data('stop');
            this_.module.stopViewerWidget.show();
            this_.module.stopViewerWidget.setActiveTime(moment().add("hours", -otp.config.timeOffset).unix()*1000);
            this_.module.stopViewerWidget.setStop(thisStop.id, thisStop.name);
            this_.module.stopViewerWidget.bringToFront();
        });

        popupContent.find('.planFromLink').data('stop', stop).click(function() {
            var thisStop = $(this).data('stop');
            this_.module.setStartPoint(new L.LatLng(thisStop.lat, thisStop.lon), false, thisStop.stopName);
            this_.module.webapp.map.lmap.closePopup();
        });

        popupContent.find('.planToLink').data('stop', stop).click(function() {
            var thisStop = $(this).data('stop');
            this_.module.setEndPoint(new L.LatLng(thisStop.lat, thisStop.lon), false, thisStop.stopName);
            this_.module.webapp.map.lmap.closePopup();
        });

        return popupContent.get(0);
    }
});
