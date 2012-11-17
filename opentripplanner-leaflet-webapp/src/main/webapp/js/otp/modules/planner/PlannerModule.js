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

otp.namespace("otp.modules.planner");


otp.modules.planner.PlannerModule = 
    otp.Class(otp.modules.Module, {

    moduleName  : "Trip Planner",
    moduleId    : "planner",
    
    markerLayer     : null,
    pathLayer       : null,
    highlightLayer  : null,
    
    tipWidget       : null,
    noTripWidget    : null,
    tipStep         : 0,
    
    currentRequest  : null,
    currentHash : null,


    // current trip query parameters:
    startLatLng             : null,
    endLatLng               : null,
    time                    : null,
    date                    : null,
    arriveBy                : false,
    mode                    : "TRANSIT,WALK",
    optimize                : null,
    triangleTimeFactor      : 0.333,
    triangleSlopeFactor     : 0.333,
    triangleSafetyFactor    : 0.334,
    
    // copy of query param set from last /plan request
    lastQueryParams : null,
    
    icons       : null,
                        
    initialize : function(webapp) {
        otp.modules.Module.prototype.initialize.apply(this, arguments);
        this.icons = new otp.modules.planner.IconFactory();        
    },
    
    activate : function() {
        if(this.activated) return;
        
        this.markerLayer = new L.LayerGroup();
        this.pathLayer = new L.LayerGroup();
        this.highlightLayer = new L.LayerGroup();
    
        this.addLayer("Highlights", this.highlightLayer);
        this.addLayer("Paths", this.pathLayer);
        this.addLayer("Path Markers", this.markerLayer);
        
        /*this.mapLayers.push(this.pathLayer);
        this.mapLayers.push(this.markerLayer);
        this.mapLayers.push(this.stationsLayer);*/

        this.tipWidget = this.createWidget("otp-tipWidget", "");
        this.updateTipStep(1);
        
        this.bikestationsWidget = new otp.widgets.BikeStationsWidget('otp-bikestationsWidget');
        this.widgets.push(this.bikestationsWidget);

        this.noTripWidget = new otp.widgets.Widget('otp-noTripWidget');
        this.widgets.push(this.noTripWidget);
        //this.noTripWidget.hide();
                
        this.activated = true;
    },
    

    handleClick : function(event) {
        if(this.startLatLng == null) {
        	this.startLatLng = new L.LatLng(event.latlng.lat, event.latlng.lng);
        	this.setStartPoint(this.startLatLng, true);
        }
        
        else if(this.endLatLng == null) {
        	this.endLatLng = new L.LatLng(event.latlng.lat, event.latlng.lng);
        	this.setEndPoint(this.endLatLng, true);
        }
    },
    
    setStartPoint : function(latlng, update) {
    
    	 var this_ = this;
    	 
         var start = new L.Marker(this.startLatLng, {icon: this.icons.startFlag, draggable: true}); 
         start.bindPopup('<strong>Start</strong>');
         start.on('dragend', function() {
        	 this_.webapp.hideSplash();
             this_.startLatLng = start.getLatLng();
             this_.planTrip();
         });
         this.markerLayer.addLayer(start);
         
         if(update)
        	 this.updateTipStep(2);         
    },
    
    setEndPoint : function(latlng, update) {
    	 var this_ = this;
    	 
         var end = new L.Marker(this.endLatLng, {icon: this.icons.endFlag, draggable: true}); 
         end.bindPopup('<strong>Destination</strong>');
         this.markerLayer.addLayer(end);
         end.on('dragend', function() {
        	 this_.webapp.hideSplash();
             this_.endLatLng = end.getLatLng();
             this_.planTrip();
         });
         
         if(update)
        	 this.planTrip();
    },
    
    restoreTrip : function(queryParams) {
    
        this.markerLayer.clearLayers(); 
      	this.startLatLng = otp.util.Geo.stringToLatLng(queryParams.fromPlace);
    	this.setStartPoint(this.startLatLng, false);
    	
      	this.endLatLng = otp.util.Geo.stringToLatLng(queryParams.toPlace);
    	this.setEndPoint(this.endLatLng, false);

        this.planTrip(queryParams);
    },
    
    planTrip : function(existingQueryParams, skipSave) {
    
        $('#otp-spinner').show();

        if(typeof this.planTripStart == 'function') this.planTripStart();
        
        this.noTripWidget.hide();
    	
    	if(this.currentRequest !== null)
        {
    		//console.log("Canceling current request.");
        	this.currentRequest.abort();
        	this.currentRequest = null;
        }
    	
        var url = otp.config.hostname + '/opentripplanner-api-webapp/ws/plan';
        this.pathLayer.clearLayers();        
        
        var this_ = this;
        
        var queryParams = null;
        
        if(existingQueryParams)
        	queryParams = existingQueryParams; 	        	
        else
        {
            
       	    queryParams = {             
                fromPlace: this.startLatLng.lat+','+this.startLatLng.lng,
                toPlace: this.endLatLng.lat+','+this.endLatLng.lng,
                mode: this.mode
            };
            if(this.time !== null) _.extend(queryParams, { time : this.time } );
            if(this.date !== null) _.extend(queryParams, { date : this.date } );
            if(this.optimize !== null) _.extend(queryParams, { optimize : this.optimize } );
            if(this.optimize === 'TRIANGLE') {
                _.extend(queryParams, {
                    triangleTimeFactor: this_.triangleTimeFactor,
                    triangleSlopeFactor: this_.triangleSlopeFactor,
                    triangleSafetyFactor: this_.triangleSafetyFactor
                });
            } 
            if(otp.config.routerId !== undefined) {
                queryParams.routerId = otp.config.routerId;
            }
        } 	
        
        this.lastQueryParams = queryParams;
        this.currentRequest = $.ajax(url, {
            data:       queryParams,
            dataType:   'jsonp',
                
            success: function(data) {
                $('#otp-spinner').hide();
                
                if(data.plan) {
                    var itin = data.plan.itineraries[0];
                    this_.processPlan(data.plan, queryParams, (existingQueryParams !== undefined));

                    this_.updateTipStep(3);
                    
                    if(!skipSave)
                    	this_.savePlan(queryParams);
                    
                }
                else {
                    this_.noTripFound();
                    this_.noTripWidget.setContent(data.error.msg);
                    this_.noTripWidget.show();
                }
            }
        });

    },
    
    processPlan : function(tripPlan, queryParams, restoring) {
    },
    
    noTripFound : function() {
    },
    
    drawItinerary : function(itin) {
        var queryParams = this.lastQueryParams;
        this.pathLayer.clearLayers(); 

        for(var i=0; i < itin.legs.length; i++) {
            var polyline = new L.Polyline(otp.util.Polyline.decode(itin.legs[i].legGeometry.points));
            polyline.setStyle({ color : this.getModeColor(itin.legs[i].mode), weight: 8});
            this.pathLayer.addLayer(polyline);
            if(itin.legs[i].mode === 'BICYCLE') {
                if(queryParams.mode === 'WALK,BICYCLE') { // bikeshare trip
                	polyline.bindPopup('Your '+otp.config.bikeshareName+' route');
                    //var start_and_end_stations = this.processStations(polyline.getLatLngs()[0], polyline.getLatLngs()[polyline.getLatLngs().length-1]);
                }
                else { // regular bike trip
                	polyline.bindPopup('Your bike route');
                	//this.resetStationMarkers();
                }	
            }
            else if(itin.legs[i].mode === 'WALK') {
                if(queryParams.mode === 'WALK,BICYCLE') { 
                    if(i == 0) {
                    	polyline.bindPopup('Walk to the '+otp.config.bikeshareName+' dock.');
                    }
                    if(i == 2) {
                    	polyline.bindPopup('Walk from the '+otp.config.bikeshareName+' dock to your destination.');
                    }
                }
                else { // regular walking trip
                	polyline.bindPopup('Your walk route');
                	//this.resetStationMarkers();
                }
            }
        }
    },
    
    highlightLeg : function(leg) {
        var polyline = new L.Polyline(otp.util.Polyline.decode(leg.legGeometry.points));
        polyline.setStyle({ color : "yellow", weight: 16, opacity: 0.3 });
        this.highlightLayer.addLayer(polyline);
    },
    
    clearHighlights : function() {
        this.highlightLayer.clearLayers(); 
    },

    getModeColor : function(mode) {
        if(mode === "WALK") return '#444';
        if(mode === "BICYCLE") return '#0073e5';
        if(mode === "SUBWAY") return '#f00';
        if(mode === "BUS") return '#080';
        return '#aaa';
    },
        
    savePlan : function(data) {
    	
    	var data_ = {data: data, startLat: this.startLatLng.lat, startLon: this.startLatLng.lng, endLat: this.endLatLng.lat, endLon: this.endLatLng.lng, parrent : this.currentHash };
    	otp.util.DataStorage.store(data_, this );
    },
    
    // legacy -- deprecated by restoreTrip (above)
    restorePlan : function(data){
    	
    	this.startLatLng = new L.LatLng(data.startLat, data.startLon);
    	this.setStartPoint(this.startLatLng, false);
    	
    	this.endLatLng = new L.LatLng(data.endLat, data.endLon);
    	this.setEndPoint(this.endLatLng, false);
    	
    	this.webapp.setBounds(new L.LatLngBounds([this.startLatLng, this.endLatLng]));
    	
    	this.planTrip(data.data, true);
    },
        
    
    newTrip : function(hash) {
    	this.currentHash = hash;	
    	
    	window.location.hash = this.currentHash;
    	
        /*var shareRoute = $("#share-route");
        shareRoute.find(".addthis_toolbox").attr("addthis:url", otp.config.siteURL+"/#"+this.currentHash);
        addthis.toolbox(".addthis_toolbox_route");*/
    },
    
    distance : function(x1, y1, x2, y2) {
        return Math.sqrt((x1-x2)*(x1-x2) + (y1-y2)*(y1-y2));
    },
    
    updateTipStep : function(step) {
        if (step <= this.tipStep) return;
        if(step == 1) this.tipWidget.setContent("To Start: Click on the Map to Plan a Trip.");
        if(step == 2) this.tipWidget.setContent("Next: Click Again to Add Your Trip's End Point.");
        if(step == 3) this.tipWidget.setContent("Tip: Drag the Start or End Flags to Modify Your Trip.");
        
        this.tipStep = step;
    },
    
    CLASS_NAME : "otp.modules.planner.PlannerModule"
});

