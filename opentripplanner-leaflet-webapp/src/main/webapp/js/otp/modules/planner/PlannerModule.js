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
        
    startLatLng : null,
    endLatLng   : null,
    
    markerLayer     : null,
    pathLayer       : null,
    
    resultsWidget   : null,
    tipWidget       : null,
    noTripWidget    : null,
    tipStep         : 0,
    
    currentRequest  : null,
    currentHash : null,


    triangleTimeFactor     : 0.333,
    triangleSlopeFactor    : 0.333,
    triangleSafetyFactor   : 0.334,
    
    icons       : null,
                        
    initialize : function(webapp) {
        otp.modules.Module.prototype.initialize.apply(this, arguments);
        this.icons = new otp.modules.planner.IconFactory();        
    },
    
    activate : function() {
        if(this.activated) return;
        
        this.markerLayer = new L.LayerGroup();
        this.pathLayer = new L.LayerGroup();
    
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
        //console.log('bikeshare click at '+event.latlng.lat+", "+event.latlng.lng);
        
        if(this.startLatLng == null) {
        	this.startLatLng = new L.LatLng(event.latlng.lat, event.latlng.lng);
        	this.setStartPoint(this.startLatLng, true);
        }
        
        else if(this.endLatLng == null) {
        	this.endLatLng = new L.LatLng(event.latlng.lat, event.latlng.lng);
        	this.setEndPoint(this.endLatLng, true);
        }
    },
    
    trianglePlanTrip : function() {
        var triParams = this.resultsWidget.panels['triangle'].bikeTriangle.getFormData();
        this.triangleTimeFactor = triParams.triangleTimeFactor;
        this.triangleSlopeFactor = triParams.triangleSlopeFactor;
        this.triangleSafetyFactor = triParams.triangleSafetyFactor;
        this.planTrip();
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
    
    
    planTrip : function(existingData, skipSave) {
    
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
        //this.stationsLayer.clearLayers(); 
        
        var this_ = this;
        
        var data_ = null;
        
        if(existingData)
        	data_ = existingData;
        else
        {
            var bikeType = $('input:radio[name=bikeType]:checked').val();
            var mode = 'BICYCLE'; //'WALK,BICYCLE';
            if(bikeType !== undefined)
                mode = (bikeType == "shared_bike") ? 'WALK,BICYCLE' : 'BICYCLE';
       	    data_ = {             
                fromPlace: this.startLatLng.lat+','+this.startLatLng.lng,
                toPlace: this.endLatLng.lat+','+this.endLatLng.lng,
                mode: mode,
                optimize: 'TRIANGLE',
                triangleTimeFactor: this_.triangleTimeFactor,
                triangleSlopeFactor: this_.triangleSlopeFactor,
                triangleSafetyFactor: this_.triangleSafetyFactor
            };
            if(otp.config.routerId !== undefined) {
                data_.routerId = otp.config.routerId;
            }
        } 	
        

        this.currentRequest = $.ajax(url, {
            data:       data_,
            dataType:   'jsonp',
                
            success: function(data) {
            
                $('#otp-spinner').hide();
            	
            	if(this_.resultsWidget == null) {

                    this_.resultsWidget = new otp.widgets.TripWidget('otp-mainTSW', function() {
                        this_.trianglePlanTrip();
                    });
                    this_.widgets.push(this_.resultsWidget);
                    
                    this_.resultsWidget.addPanel("summary", new otp.widgets.TW_TripSummary(this_.resultsWidget));
                    this_.resultsWidget.addSeparator();
                    this_.resultsWidget.addPanel("triangle", new otp.widgets.TW_BikeTriangle(this_.resultsWidget));
                    this_.resultsWidget.addSeparator();
                    this_.resultsWidget.addPanel("biketype", new otp.widgets.TW_BikeType(this_.resultsWidget));
                    
                    if(existingData !== null) {
                        this_.resultsWidget.restorePlan(existingData);
                    }
                    this_.resultsWidget.show();
                                	
                    /*this_.resultsWidget = new otp.widgets.TripSummaryWidget('otp-mainTSW', function() {
                        this_.trianglePlanTrip();
                    });
                    this_.widgets.push(this_.resultsWidget);
                    
                    if(existingData !== null) {
                        this_.resultsWidget.restorePlan(existingData);
                    }
                    this_.resultsWidget.show();*/

                }
                
                //console.log(data);
                var resultsContent = '';

                if(data.plan) {
                    var itin = data.plan.itineraries[0];
                    this_.processItinerary(itin, data_);

                    this_.resultsWidget.show();

                    this_.resultsWidget.newItinerary(itin);
                    this_.updateTipStep(3);
                    
                    if(!skipSave)
                    	this_.savePlan(data_);
                    
                }
                else {
                    this_.resultsWidget.hide();
                    this_.noTripWidget.setContent(data.error.msg);
                    this_.noTripWidget.show();
                }
                
                this_.webapp.queryLogger.logQuery(data_.fromPlace, data_.toPlace);
            }
        });
        
        //console.log("rw "+this.resultsWidget);
    },
    
    processItinerary : function(itin, data) {
    },
    
    savePlan : function(data){
    	
    	var data_ = {data: data, startLat: this.startLatLng.lat, startLon: this.startLatLng.lng, endLat: this.endLatLng.lat, endLon: this.endLatLng.lng, parrent : this.currentHash };
    	otp.util.DataStorage.store(data_, this );
    },
    
    restorePlan : function(data){
    	
    	this.startLatLng = new L.LatLng(data.startLat, data.startLon);
    	this.setStartPoint(this.startLatLng, false);
    	
    	this.endLatLng = new L.LatLng(data.endLat, data.endLon);
    	this.setEndPoint(this.endLatLng, false);
    	
    	this.webapp.setBounds(new L.LatLngBounds([this.startLatLng, this.endLatLng]));
    	
    	this.planTrip(data.data, true);
    },
        
    getModeColor : function(mode) {
        if(mode === "WALK") return '#444';
        if(mode === "BICYCLE") return '#0073e5';
        return '#aaa';
    },
    
    newTrip : function(hash) {
    	this.currentHash = hash;	
    	
    	window.location.hash = this.currentHash;
    	
        var shareRoute = $("#share-route");
        shareRoute.find(".addthis_toolbox").attr("addthis:url", otp.config.siteURL+"/#"+this.currentHash);
        addthis.toolbox(".addthis_toolbox_route");
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

