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

otp.namespace("otp.modules.bikeshare");

otp.modules.bikeshare.StationModel = 
    Backbone.Model.extend({
    
    isWalkableFrom: function(point, tolerance) {
        return (Math.abs(this.get('x') - point.lng) < tolerance && 
                Math.abs(this.get('y') - point.lat) < tolerance);
    },
    
    isNearishTo: function(point, tolerance) {
        return (this.distanceTo(point) < tolerance && 
                parseInt(this.get('bikesAvailable')) > 0);
    },
    
    distanceTo: function(point) {
        var distance = otp.modules.bikeshare.Utils.distance;
        return distance(this.get('x'), this.get('y'), point.lng, point.lat);
    }
});

otp.modules.bikeshare.StationCollection = 
    Backbone.Collection.extend({
    
    url: otp.config.hostname + '/' + otp.config.restService + '/bike_rental',
    model: otp.modules.bikeshare.StationModel,
    
    sync: function(method, model, options) {
        options.dataType = 'json';
        options.data = options.data || {};
        if(otp.config.routerId !== undefined) {
            options.data.routerId = otp.config.routerId;
        }
        //Sends wanted translation to server
        options.data.locale = otp.config.locale.config.locale_short;
        return Backbone.sync(method, model, options);
    },
    
    parse: function(rawData, options) {
        return Backbone.Collection.prototype.parse.call(this, rawData.stations, options);
    }
});

otp.modules.bikeshare.Utils = {
    distance : function(x1, y1, x2, y2) {
        return Math.sqrt((x1-x2)*(x1-x2) + (y1-y2)*(y1-y2));
    }    
};

/* main class */

otp.modules.bikeshare.BikeShareModule = 
    otp.Class(otp.modules.planner.PlannerModule, {
    
    moduleName  : _tr("Bike Share Planner"),

    resultsWidget   : null,
    
    stations    : null,    
    stationLookup :   { },
    stationsLayer   : null,
     
    initialize : function(webapp) {
        otp.modules.planner.PlannerModule.prototype.initialize.apply(this, arguments);
    },
    
    activate : function() {
        if(this.activated) return;
        otp.modules.planner.PlannerModule.prototype.activate.apply(this);
        this.mode = "WALK,BICYCLE_RENT";
        
        this.stationsLayer = new L.LayerGroup();
        this.addLayer("Bike Stations", this.stationsLayer);
        
        this.bikestationsWidget = new otp.widgets.BikeStationsWidget(this.id+"-stationsWidget", this);

        this.initStations();

        var this_ = this;
        setInterval(function() {
            this_.reloadStations();
        }, 30000);
        
        
        this.initOptionsWidget();
        
        this.defaultQueryParams.mode = "WALK,BICYCLE_RENT";
        this.optionsWidget.applyQueryParams(this.defaultQueryParams);
                
       
    },
    
    initOptionsWidget : function() {
        this.optionsWidget = new otp.widgets.tripoptions.TripOptionsWidget(
            'otp-'+this.id+'-optionsWidget', this, {
                title : _tr('Trip Options')
            }
        );

        if(this.webapp.geocoders && this.webapp.geocoders.length > 0) {
            this.optionsWidget.addControl("locations", new otp.widgets.tripoptions.LocationsSelector(this.optionsWidget, this.webapp.geocoders), true);
            this.optionsWidget.addVerticalSpace(12, true);
        }
                
        //this.optionsWidget.addControl("time", new otp.widgets.tripoptions.TimeSelector(this.optionsWidget), true);
        //this.optionsWidget.addVerticalSpace(12, true);
        
        
        //var modeSelector = new otp.widgets.tripoptions.ModeSelector(this.optionsWidget);
        //this.optionsWidget.addControl("mode", modeSelector, true);

        this.optionsWidget.addControl("triangle", new otp.widgets.tripoptions.BikeTriangle(this.optionsWidget));

        this.optionsWidget.addControl("biketype", new otp.widgets.tripoptions.BikeType(this.optionsWidget));

        this.optionsWidget.autoPlan = true;

        /*this.optionsWidget.addSeparator();
        this.optionsWidget.addControl("submit", new otp.widgets.tripoptions.Submit(this.optionsWidget));*/
    },
    

    planTripStart : function() {
        this.resetStationMarkers();
    },
    
    processPlan : function(tripPlan, restoring) {
        var itin = tripPlan.itineraries[0];
        var this_ = this;
        
	    /*if(this.resultsWidget == null) {

            this.resultsWidget = new otp.widgets.TripWidget('otp-'+this.id+'-tripWidget', this);
            this.widgets.push(this.resultsWidget);
            
            this.resultsWidget.addPanel("summary", new otp.widgets.TW_TripSummary(this.resultsWidget));
            this.resultsWidget.addSeparator();
            this.resultsWidget.addPanel("triangle", new otp.widgets.TW_BikeTriangle(this.resultsWidget));
            this.resultsWidget.addSeparator();
            this.resultsWidget.addPanel("biketype", new otp.widgets.TW_BikeType(this.resultsWidget));
            
            if(restoring) { //existingQueryParams !== null) {
                console.log("restoring");
                this.resultsWidget.restorePlan(queryParams);
            }
            this.resultsWidget.show();
        }*/
                        
        this.drawItinerary(itin);
        
        if(tripPlan.queryParams.mode === 'WALK,BICYCLE_RENT') { // bikeshare trip
            var polyline = new L.Polyline(otp.util.Geo.decodePolyline(itin.itinData.legs[1].legGeometry.points));
            var start_and_end_stations = this.processStations(polyline.getLatLngs()[0], polyline.getLatLngs()[polyline.getLatLngs().length-1]);
        }
        else { // "my own bike" trip
           	this.resetStationMarkers();
        }	

        //this.resultsWidget.show();
        //this.resultsWidget.newItinerary(itin);
                    
        if(start_and_end_stations !== undefined && tripPlan.queryParams.mode === 'WALK,BICYCLE_RENT') {
            if(start_and_end_stations['start'] && start_and_end_stations['end']) {
           	    this.bikestationsWidget.setContentAndShow(
           	        start_and_end_stations['start'], 
           	        start_and_end_stations['end'],
           	        this);
           	    this.bikestationsWidget.show();
           	}
           	else
           	    this.bikestationsWidget.hide();
        }
       	else {
       	    this.bikestationsWidget.hide();
       	}
    },
    
    noTripFound : function() {
        this.resultsWidget.hide();
    },
        
    processStations : function(start, end) {
        var this_ = this;
        var tol = .0005, distTol = .01;
        var start_and_end_stations = [];
        var distance = otp.modules.bikeshare.Utils.distance;
        
        this.stations.each(function(station) {
            var stationData = station.toJSON();
            
            if (station.isWalkableFrom(start, tol)) {
                // start station
                // TRANSLATORS: Popup title station from where bike is picked
                // up
                this.setStationMarker(station, _tr("PICK UP BIKE"), this.icons.startBike);
                start_and_end_stations['start'] = station;
            }
            else if (station.isNearishTo(this.startLatLng, distTol)) {
                // start-adjacent station
                var distanceToStart = station.distanceTo(this.startLatLng);
                var icon = distanceToStart < distTol/2 ? this.icons.getLarge(stationData) : this.icons.getMedium(stationData);
                // TRANSLATORS: Popup title alternative station to pickup bike on bike
                // sharing
                this.setStationMarker(station, _tr("ALTERNATE PICKUP"), icon);
            }
            else if (station.isWalkableFrom(end, tol)) {
                // end station
                // TRANSLATORS: Popup title 
                this.setStationMarker(station, _tr("DROP OFF BIKE"), this.icons.endBike);
                start_and_end_stations['end'] = station;
            }
            else if (station.isNearishTo(this.endLatLng, distTol)) {
                // end-adjacent station
                var distanceToEnd = station.distanceTo(this.endLatLng);
                var icon = distanceToEnd < distTol/2 ? this.icons.getLarge(stationData) : this.icons.getMedium(stationData);
                // TRANSLATORS: Popup title
                this.setStationMarker(station, _tr("ALTERNATE DROP OFF"), icon);
            }
            else {
                icon = icon || this.icons.getSmall(stationData);
                // TRANSLATORS: Popup title Bike sharing station
                this.setStationMarker(station, _tr("BIKE STATION"), icon);
            }
        }, this);
        
        return start_and_end_stations;
    },
    
    onResetStations : function(stations) {
        this.resetStationMarkers();
    },
    
    resetStationMarkers : function() {
        this.stations.each(function(station) {
            this.setStationMarker(station);
        }, this);
    },

    clearStationMarkers : function() {
        _.each(_.keys(this.markers), function(stationId) {
            this.removeStationMarker(stationId);
        s}, this);
    },
    
    getStationMarker : function(station) {
        if (station instanceof Backbone.Model)
            return this.markers[station.id];
        else
            return this.markers[station];
    },
    
    removeStationMarker : function(station) {
        var marker = this.getStationMarker(station);
        if (marker)
            this.stationsLayer.removeLayer(marker);
    },
    
    addStationMarker : function(station, title, icon) {
        var stationData = station.toJSON(),
            marker;
        icon = icon || this.icons.getSmall(stationData);
        
        //console.log(station);
        marker = new L.Marker(new L.LatLng(stationData.y, stationData.x), {icon: icon});
        this.markers[station.id] = marker;
        this.stationsLayer.addLayer(marker);
        marker.bindPopup(this.constructStationInfo(title, stationData));
    },
    
    setStationMarker : function(station, title, icon) {
        var marker = this.getStationMarker(station);
        if (!marker)
            marker = this.addStationMarker(station, title, icon);
        else {
            this.updateStationMarker(marker, station, title, icon);
        }
    },
    
    updateStationMarker : function(marker, station, title, icon) {
        var stationData = station.toJSON();
        
        marker.setIcon(icon || this.icons.getSmall(stationData));

        marker.bindPopup(this.constructStationInfo(title, stationData));
    },
    
    initStations : function() {
        this.markers = {};
        this.stations = new otp.modules.bikeshare.StationCollection();
        this.stations.on('reset', this.onResetStations, this);
        
        this.stations.fetch();
    },

    reloadStations : function(stations) {
        this.stations.fetch();
    },
            
    constructStationInfo : function(title, station) {
        if(title == null) {
            title = (station.markerTitle !== undefined) ? station.markerTitle : _tr("BIKE STATION");
        }
        var info = "<strong>"+title+"</strong><br/>";
        station.markerTitle = title;
        //TRANSLATORS: Bike sharing station: station name
        info += '<strong>' + _tr("Station:") + '</strong> '+station.name+'<br/>';
        info += ngettext("<strong>%d</strong> bike available", "<strong>%d</strong> bikes available", station.bikesAvailable) + "<br />";
        info += ngettext("<strong>%d</strong> dock available", "<strong>%d</strong> docks available", station.spacesAvailable) + '<br />';
        return info;
    },
                
    CLASS_NAME : "otp.modules.bikeshare.BikeShareModule"
});
