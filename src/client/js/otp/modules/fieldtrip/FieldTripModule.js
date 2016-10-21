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

otp.namespace("otp.modules.fieldtrip");


otp.modules.fieldtrip.FieldTripModule =
    otp.Class(otp.modules.multimodal.MultimodalPlannerModule, {

    moduleName  : "Field Trip Planner",

    itinWidget  : null,

    requestWidgets : null, // object, maps request ids to widget instances

    groupSize   : 100,
    bannedSegments : null,
    //itineraries : null,
    groupPlan : null,

    datastoreUrl : otp.config.datastoreUrl,

    geocoderWidgets : null,
    geocodedOrigins : null,
    geocodedDestinations : null,

    sessionManager : null,

    showIntermediateStops : true,


    initialize : function(webapp, id, options) {
        otp.modules.multimodal.MultimodalPlannerModule.prototype.initialize.apply(this, arguments);
        this.templateFiles.push('otp/modules/fieldtrip/fieldtrip-templates.html');

        this.requiresAuth = true;
        this.authUserRoles = ['fieldtrip', 'all'];

        this.planTripFunction = this.ftPlanTrip;
        this.requestWidgets = {};
        this.geocoderWidgets = {};
        this.geocodedOrigins = {};
        this.geocodedDestinations = {};

        this.tripHashLookup = {} // maps tripId to tripHash
    },

    activate : function() {
        if(this.activated) return;
        otp.modules.multimodal.MultimodalPlannerModule.prototype.activate.apply(this);

        var modeSelector = this.optionsWidget.controls['mode'];
        modeSelector.addModeControl(new otp.widgets.tripoptions.GroupTripOptions(this.optionsWidget, "Group Size: "));
        modeSelector.refreshModeControls();

        // use app-wide session manager
        this.sessionManager = this.webapp.sessionManager;

        this.fieldTripManager = new otp.modules.fieldtrip.FieldTripManagerWidget('otp-'+this.id+'-fieldTripWidget', this);
        this.searchWidget = new otp.modules.fieldtrip.SearchWidget(this.id+'-searchWidget', this);

    },

    restore : function() {
        if(_.has(this.webapp.urlParams, 'groupSize')) {
            this.groupSize = parseInt(this.webapp.urlParams['groupSize']);
        }
        if("fromPlace" in this.webapp.urlParams && "toPlace" in this.webapp.urlParams) {
            this.optionsWidget.restorePlan({queryParams : this.webapp.urlParams});
        }
        otp.modules.multimodal.MultimodalPlannerModule.prototype.restore.apply(this);
    },

    getExtendedQueryParams : function() {
        return {
            numItineraries : 1,
        };
    },

    getAdditionalUrlParams : function() {
        return { groupSize : this.groupSize };
    },

    ftPlanTrip : function(queryParams) {

        if(this.updateActiveOnly) { // single itin modified by first/last/previous/next

            if(this.groupPlan) {
                var bannedTripArr = [];
                for(var i = 0; i < this.groupPlan.itineraries.length; i++) {
                    bannedTripArr = bannedTripArr.concat(this.groupPlan.itineraries[i].getTripIds());
                }
                queryParams.bannedTrips = bannedTripArr.join(',');
            }

            this.planTrip(queryParams);
            return;
        }

        var planDate = moment(this.optionsWidget.controls['time'].getDate()).format("MM/DD/YYYY");
        this.currentGroupSize = this.groupSize;
        this.bannedSegments = [];
        this.groupPlan = null;

        var this_ = this;

        // query for trips in use by other field trip itineraries in the DB
        this.serverRequest('/fieldtrip/getTrips', 'GET', {
            date : planDate
        }, _.bind(function(data) {

            // store the trips in use for reference by the checkTripValidity() method
            this.tripsInUse = [];

            for(var t = 0; t < data.length; t++) {
                var fieldTrip = data[t];
                for(var i = 0; i < fieldTrip.groupItineraries.length; i++) {
                    var grpItin = fieldTrip.groupItineraries[i];
                    for(var gt =0 ; gt < grpItin.trips.length; gt++) {
                        var gtfsTrip = grpItin.trips[gt];
                        // Note: tripIds still stored as 'agencyAndId' in DB
                        gtfsTrip.tripId = convertAgencyAndId(gtfsTrip.agencyAndId);
                        gtfsTrip.passengers = grpItin.passengers;
                        // (gtfsTrip already includes fields fromStopIndex and toStopIndex)

                        this.tripsInUse.push(gtfsTrip);
                    }
                }
            }

            // kick off the main planTrip request
            this.itinCapacity = null;
            this.planTrip();

        }, this));

        this.setBannedTrips();
    },

    preprocessPlan : function(tripPlan, queryParams, callback) {
        var hashQueryLegs = [];

        _.each(tripPlan.itineraries, function(itin) {
            _.each(itin.legs, function(leg) {
                if(leg.tripId) {
                    if(!(leg.tripId in this.tripHashLookup)) {
                        hashQueryLegs.push(leg);
                    }
                }
            }, this);
        }, this);

        if(hashQueryLegs.length === 0) {
            callback.call(this);
        }
        else {
            var queriesFinished = 0;
            for(var i =0; i < hashQueryLegs.length; i++) {
                var leg = hashQueryLegs[i];

                this.webapp.indexApi.getTripHash(leg.tripId, this, _.bind(function(data) {
                    this.ftmodule.tripHashLookup[this.tripId] = data;
                    queriesFinished++;
                    if(queriesFinished == hashQueryLegs.length) {
                        callback.call(this);
                    }
                }, { ftmodule: this, tripId : leg.tripId}));
            }
        }
    },

    processPlan : function(tripPlan, restoring) {
        if(this.updateActiveOnly) {
            var itinIndex = this.itinWidget.activeIndex;
            tripPlan.itineraries[0].groupSize = this.groupPlan.itineraries[itinIndex].groupSize;
            this.itinWidget.updateItineraries(tripPlan.itineraries);
            this.updateActiveOnly = false;
            this.drawItinerary(tripPlan.itineraries[0]);
            return;
        }

        if(this.groupPlan == null)
            this.groupPlan = new otp.modules.planner.TripPlan(null, _.extend(tripPlan.queryParams, { groupSize : this.groupSize }));

        if(this.itinWidget == null) this.createItinerariesWidget();

        var itin = tripPlan.itineraries[0];
        var capacity = itin.getGroupTripCapacity();

        // if this itin shares a vehicle trip with another one already in use, only use the remainingCapacity (as set in checkTripValidity())
        if(this.itinCapacity) capacity = Math.min(capacity, this.itinCapacity);

        this.groupPlan.addItinerary(itin);

        var transitLegs = itin.getTransitLegs();
        for(var i = 0; i < transitLegs.length; i++) {
            var leg = transitLegs[i];
            this.bannedSegments.push({
                tripId : leg.tripId,
                fromStopIndex : leg.from.stopIndex,
                toStopIndex : leg.to.stopIndex,
            });
        }

        this.setBannedTrips();

        if(this.currentGroupSize > capacity) { // group members remain; plan another trip
            this.currentGroupSize -= capacity;
            itin.groupSize = capacity;
            //console.log("remaining: "+this.currentGroupSize);
            this.itinCapacity = null;
            this.planTrip();
        }
        else { // we're done; show the results
            itin.groupSize = this.currentGroupSize;
            this.showResults();
        }

    },

    showResults : function() {
        this.itinWidget.show();
        this.itinWidget.bringToFront();
        this.itinWidget.updatePlan(this.groupPlan);
        this.drawItinerary(this.groupPlan.itineraries[0]);

        var requestWidgets = _.values(this.requestWidgets);
        for(var i = 0; i <= requestWidgets.length; i++) {
            if(requestWidgets[i]) {
                requestWidgets[i].tripPlanned();
            }
        }
    },


    createItinerariesWidget : function() {
        this.itinWidget = new otp.widgets.ItinerariesWidget(this.id+"-itinWidget", this);
        this.itinWidget.showItineraryLink = false;
    },

    setBannedTrips : function() {
        var tripIds = [];
        for(var i=0; i<this.bannedSegments.length; i++) {
            tripIds.push(this.bannedSegments[i].tripId);
        }

        this.bannedTrips = tripIds.length > 0 ? tripIds.join(',') : null;
        //console.log("set bannedTrips: "+this.bannedTrips);
    },

    showSaveWidget : function() {
        new otp.modules.fieldtrip.SaveFieldTripWidget('otp-'+this.id+'-saveFieldTripWidget', this);
    },

    saveTrip : function(request, requestOrder, successCallback) {
        var this_ = this;

        var widget = this.requestWidgets[request.id];
        if(widget) widget.savingTrip(requestOrder);

        var data = {
            sessionId : this.sessionManager.sessionId,
            requestId : request.id,
            'trip.requestOrder' : requestOrder,
            'trip.origin' : this.getStartOTPString(),
            'trip.destination' : this.getEndOTPString(),
            'trip.createdBy' : this.userName,
            'trip.passengers' : this.groupSize,
            'trip.departure' : moment(this.groupPlan.earliestStartTime).add("hours", otp.config.timeOffset).format("YYYY-MM-DDTHH:mm:ss"),
            'trip.queryParams' : JSON.stringify(this.groupPlan.queryParams),
        };

        for(var i = 0; i < this.groupPlan.itineraries.length; i++) {
            var itin = this.groupPlan.itineraries[i];
            //console.log("saving itin for trip "+tripId);
            //console.log(itin.itinData);

            data['itins['+i+'].passengers'] = itin.groupSize;
            data['itins['+i+'].itinData'] = otp.util.Text.lzwEncode(JSON.stringify(itin.itinData));
            data['itins['+i+'].timeOffset'] = otp.config.timeOffset || 0;

            var legs = itin.getTransitLegs();

            for(var l = 0; l < legs.length; l++) {
                var leg = legs[l];
                var routeName = (leg.routeShortName !== null ? ('(' + leg.routeShortName + ') ') : '') + (leg.routeLongName || "");
                var tripHash = this.tripHashLookup[leg.tripId];

                data['gtfsTrips['+i+']['+l+'].depart'] = moment(leg.startTime).format("HH:mm:ss");
                data['gtfsTrips['+i+']['+l+'].arrive'] = moment(leg.endTime).format("HH:mm:ss");
                data['gtfsTrips['+i+']['+l+'].agencyAndId'] = leg.tripId;
                data['gtfsTrips['+i+']['+l+'].tripHash'] = tripHash;
                data['gtfsTrips['+i+']['+l+'].routeName'] = routeName;
                data['gtfsTrips['+i+']['+l+'].fromStopIndex'] = leg.from.stopIndex;
                data['gtfsTrips['+i+']['+l+'].toStopIndex'] = leg.to.stopIndex;
                data['gtfsTrips['+i+']['+l+'].fromStopName'] = leg.from.name;
                data['gtfsTrips['+i+']['+l+'].toStopName'] = leg.to.name;
                data['gtfsTrips['+i+']['+l+'].headsign'] = leg.headsign;
                data['gtfsTrips['+i+']['+l+'].capacity'] = itin.getModeCapacity(leg.mode);
                if(leg.tripBlockId) data['gtfsTrips['+i+']['+l+'].blockId'] = leg.tripBlockId;
            }
        }

        this.serverRequest('/fieldtrip/newTrip', 'POST', data, _.bind(function(data) {
            if(data === -1) {
                otp.widgets.Dialogs.showOkDialog("This plan could not be saved due to a lack of capacity on one or more vehicles. Please re-plan your trip.", "Cannot Save Plan");
            }
            else successCallback.call(this, data);
        }, this));
    },

    checkTripValidity : function(tripId, leg, itin) {
        var capacityInUse = 0;
        for(var i = 0; i < this.tripsInUse.length; i++) {
            var tripInUse  = this.tripsInUse[i];

            // first test: are these the same vehicle trip? if not, we're ok
            var sameVehicleTrip = false;
            if(tripId in this.tripHashLookup && tripInUse.tripHash) { // use the trip hashes if available
                sameVehicleTrip = (this.tripHashLookup[tripId] === tripInUse.tripHash);
            }
            else { // as fallback, compare the tripId strings
                sameVehicleTrip = (tripId === tripInUse.tripId);
            }
            if(!sameVehicleTrip) continue;

            // second test: do the stop ranges overlap? if not, we're ok
            if(leg.from.stopIndex >= tripInUse.toStopIndex || leg.to.stopIndex <= tripInUse.fromStopIndex) continue;

            // if ranges overlap, calculate remaining capacity
            capacityInUse += tripInUse.passengers;
        }

        var remainingCapacity = itin.getModeCapacity(leg.mode) - capacityInUse;
        if(remainingCapacity < 10) return false; // consider trip 'full' if < 10 spots remain

        this.itinCapacity = this.itinCapacity ? Math.min(this.itinCapacity, remainingCapacity) : remainingCapacity;

        return true;
    },


    renderTrip : function(tripData) {
        var queryParams = JSON.parse(tripData.queryParams);
        this.groupPlan = new otp.modules.planner.TripPlan(null, queryParams);//_.extend(queryParams, { groupSize : this.groupSize }));
        for(var i = 0; i < tripData.groupItineraries.length; i++) {
            var itinData = otp.util.FieldTrip.readItinData(tripData.groupItineraries[i]);
            var itin = new otp.modules.planner.Itinerary(itinData, this.groupPlan);
            itin.groupSize = tripData.groupItineraries[i].passengers;
            this.groupPlan.addItinerary(itin);
        }
        if(this.itinWidget == null) this.createItinerariesWidget();
        this.showResults();
        this.restoreMarkers(queryParams);
        this.optionsWidget.restorePlan({ queryParams : queryParams });
    },


    //** requests functions **//

    loadRequests : function() {
        this.serverRequest('/fieldtrip/getRequestsSummary', 'GET', {}, _.bind(function(data) {

            this.fieldTripManager.updateRequests(data);

            for(var i = 0; i < data.length; i++) {
                var req = data[i];
                if(_.has(this.requestWidgets, req.id)) {
                    this.loadFullRequest(req, _.bind(function(fullReq) {
                        var widget = this.requestWidgets[fullReq.id];
                        widget.request = fullReq;
                        widget.render();
                    }, this));
                }
            }
        }, this));
    },

    setRequestStatus : function(request, status) {
        this.serverRequest('/fieldtrip/setRequestStatus', 'POST', {
            requestId : request.id,
            status : status
        }, _.bind(function(data) {
            this.loadRequests();
        }, this));
    },

    cancelRequest : function(request) {
        this.setRequestStatus(request, "cancelled");
    },

    setClasspassId : function(request, classpassId) {
        this.serverRequest('/fieldtrip/setRequestClasspassId', 'POST', {
            requestId : request.id,
            classpassId : classpassId
        }, _.bind(function(data) {
            this.loadRequests();
        }, this));
    },

    showRequest : function(request) {
        this.loadFullRequest(request, _.bind(function(fullReq) {
            if(_.has(this.requestWidgets, fullReq.id)) {
                var widget = this.requestWidgets[fullReq.id];
                if(widget.minimized) widget.unminimize();
            }
            else {
                var widget = new otp.modules.fieldtrip.FieldTripRequestWidget('otp-'+this.id+'-requestWidget-'+fullReq.id, this, fullReq);
                this.requestWidgets[fullReq.id] = widget;
            }
            widget.bringToFront();
        }, this));
    },

    loadFullRequest : function(request, callback) {
        this.serverRequest('/fieldtrip/getRequest', 'GET', {
            requestId : request.id
        }, _.bind(function(data) {
            _.extend(request, _.omit(data, 'id'));
            if(callback) callback.call(this, request);
        }, this));
    },

    planOutbound : function(request) {
        this.clearTrip();
        var queryParams = {
            time : moment(request.arriveDestinationTime).format("h:mma"),
            date : moment(request.travelDate).format("MM-DD-YYYY"),
            arriveBy : true,
            groupSize : otp.util.FieldTrip.getGroupSize(request),
        };

        this.optionsWidget.applyQueryParams(queryParams);

        var geocodedOrigin = this.geocodedOrigins[request.id];
        var geocodedDestination = this.geocodedDestinations[request.id];

        if(geocodedOrigin) {
            this.setStartPoint(new L.LatLng(geocodedOrigin.lat, geocodedOrigin.lng),
                               false, geocodedOrigin.description);
        }
        if(geocodedDestination) {
            this.setEndPoint(new L.LatLng(geocodedDestination.lat, geocodedDestination.lng),
                             false, geocodedDestination.description);
        }
        if(!geocodedOrigin || !geocodedDestination) {
            this.showGeocoder(request, "outbound");
        }
    },

    planInbound : function(request) {
        this.clearTrip();
        var queryParams = {
            time : moment(request.leaveDestinationTime).format("h:mma"),
            date : moment(request.travelDate).format("MM-DD-YYYY"),
            arriveBy : false,
            groupSize : otp.util.FieldTrip.getGroupSize(request),
        };

        this.optionsWidget.applyQueryParams(queryParams);

        var geocodedOrigin = this.geocodedOrigins[request.id];
        var geocodedDestination = this.geocodedDestinations[request.id];

        if(geocodedOrigin) {
            this.setEndPoint(new L.LatLng(geocodedOrigin.lat, geocodedOrigin.lng),
                             false, geocodedOrigin.description);
        }
        if(geocodedDestination) {
            this.setStartPoint(new L.LatLng(geocodedDestination.lat, geocodedDestination.lng),
                               false, geocodedDestination.description);
        }
        if(!geocodedOrigin || !geocodedDestination) {
            this.showGeocoder(request, "inbound");
        }
    },

    showGeocoder : function(request, mode) {
        if(_.has(this.geocoderWidgets, request.id)) {
            var widget = this.geocoderWidgets[request.id];
            widget.mode = mode;
            if(widget.minimized) widget.unminimize();
            widget.show();
        }
        else {
            var widget = new otp.modules.fieldtrip.FieldTripGeocoderWidget('otp-'+this.id+'-geocoderWidget-'+request.id, this, request, mode);
            this.geocoderWidgets[request.id] = widget;
        }
        widget.bringToFront();

    },

    saveRequestTrip : function(request, type) {
        if(!this.checkPlanValidity(request)) return;

        this.itinWidget.close();

        var this_ = this;

        var outboundTrip = otp.util.FieldTrip.getOutboundTrip(request);
        if(type === 'outbound') {
            if(outboundTrip) {
                var msg = "This action will overwrite a previously planned outbound itinerary for this request. Do you wish to continue?";
                otp.widgets.Dialogs.showYesNoDialog(msg, "Overwrite Outbound Itinerary?", function() {
                    this_.saveTrip(request, 0, function(tripId) {
                        this_.loadRequests();
                    });
                });
            }
            else {
                this_.saveTrip(request, 0, function(tripId) {
                    this_.loadRequests();
                });
            }
        }

        var inboundTrip = otp.util.FieldTrip.getInboundTrip(request);
        if(type === 'inbound') {
            if(inboundTrip) {
                var msg = "This action will overwrite a previously planned inbound itinerary for this request. Do you wish to continue?";
                otp.widgets.Dialogs.showYesNoDialog(msg, "Overwrite Inbound Itinerary?", function() {
                    this_.saveTrip(request, 1, function(tripId) {
                        this_.loadRequests();
                    });
                });
            }
            else {
                this_.saveTrip(request, 1, function(tripId) {
                    this_.loadRequests();
                });
            }
        }
    },

    checkPlanValidity : function(request) {
        if(this.groupPlan == null) {
            otp.widgets.Dialogs.showOkDialog("No active plan to save", "Cannot Save Plan");
            return false;
        }

        var planDeparture = moment(this.groupPlan.earliestStartTime).add("hours", otp.config.timeOffset);
        var requestDate = moment(request.travelDate);

        if(planDeparture.date() != requestDate.date() ||
                planDeparture.month() != requestDate.month() ||
                planDeparture.year() != requestDate.year()) {
            var msg = "Planned trip date (" + planDeparture.format("MM/DD/YYYY") + ") is not the requested day of travel (" + requestDate.format("MM/DD/YYYY") + ")";
            otp.widgets.Dialogs.showOkDialog(msg, "Cannot Save Plan");
            return false;
        }

        return true;
    },

    addNote: function(request, note, type) {
        this.serverRequest('/fieldtrip/addNote', 'POST', {
            requestId : request.id,
            'note.note' : note,
            'note.type' : type,
            'note.userName' : this.sessionManager.username,
        }, _.bind(function(data) {
            this.loadRequests();
        }, this));
    },

    editTeacherNotes: function(request, notes) {
        this.serverRequest('/fieldtrip/editSubmitterNotes', 'POST', {
            requestId : request.id,
            notes : notes
        }, _.bind(function(data) {
            this.loadRequests();
        }, this));
    },

    deleteNote: function(note) {
        this.serverRequest('/fieldtrip/deleteNote', 'POST', {
            noteId : note.id
        }, _.bind(function(data) {
            this.loadRequests();
        }, this));
    },

    setRequestDate: function(request, date) {
        this.serverRequest('/fieldtrip/setRequestDate', 'POST', {
            requestId : request.id,
            date: moment(date).format("MM/DD/YYYY")
        }, _.bind(function(data) {
            this.loadRequests();
        }, this));
    },

    editGroupSize: function(request, numStudents, numChaperones, minimumAge, maximumAge) {
        this.serverRequest('/fieldtrip/setRequestGroupSize', 'POST', {
            requestId: request.id,
            numStudents: numStudents,
            numChaperones: numChaperones,
            minimumAge: minimumAge,
            maximumAge: maximumAge
        }, _.bind(function(data) {
            this.loadRequests();
        }, this));
    },

    serverRequest: function(method, requestType, params, successCallback, errorCallback) {
        params.sessionId = this.sessionManager.sessionId;

        $.ajax(this.datastoreUrl + method, {
            type: requestType,

            data: params,

            success: function(data) {
                if((typeof data) == "string") data = jQuery.parseJSON(data);
                if(successCallback) successCallback.call(this, data);
            },

            error: function(data) {
                console.log("error calling method " + method);
                console.log(data);
                if(errorCallback) errorCallback.call(this);
            }
        });
    }
});


otp.util.FieldTrip = {

    constructPlanInfo : function(trip) {
        return trip.groupItineraries.length + " group itineraries, planned by " + trip.createdBy + " at " + trip.timeStamp;
    },

    getOutboundTrip : function(request) {
        if(request.outboundTrip) return request.outboundTrip;
        return otp.util.FieldTrip.tripAtIndex(request, 0);
    },

    getInboundTrip : function(request) {
        if(request.inboundTrip) return request.inboundTrip;
        return otp.util.FieldTrip.tripAtIndex(request, 1);
    },

    tripAtIndex : function(request, index) {
        if(request.trips) {
            for(var i = 0; i < request.trips.length; i++) {
                if(request.trips[i].requestOrder === index) return request.trips[i];
            }
        }
        return null;
    },

    getGroupSize : function(request) {
        var groupSize = 0;
        if(request.numStudents) groupSize += request.numStudents;
        if(request.numChaperones) groupSize += request.numChaperones;
        return groupSize;
    },

    readItinData : function(groupItin) {
        if(groupItin.timeOffset) otp.config.timeOffset = groupItin.timeOffset;
        return JSON.parse(otp.util.Text.lzwDecode(groupItin.itinData));
    },

    getRequestContext : function(req) {
        var context = _.clone(req);
        if(req.travelDate) context.formattedDate = moment(req.travelDate).format("MMM Do YYYY");
        return context;
    }
};

function convertAgencyAndId(agencyAndId) {
    return agencyAndId.replace('_', ':');
}
