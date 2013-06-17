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

otp.namespace("otp.modules.calltaker");


otp.modules.calltaker.CallTakerModule = 
    otp.Class(otp.modules.multimodal.MultimodalPlannerModule, {

    moduleName  : "Call Taker Interface",
    
    activeCall : null,
    
    sessionId : null,
    username: null,
    
    initialize : function(webapp, id, options) {
        otp.modules.multimodal.MultimodalPlannerModule.prototype.initialize.apply(this, arguments);
        
        this.showIntermediateStops = true;
        
    },
    
    activate : function() {    
        if(this.activated) return;
        otp.modules.multimodal.MultimodalPlannerModule.prototype.activate.apply(this);
        console.log("activate ctm");        
        
        this.initSession();
    },
    
    getExtendedQueryParams : function() {
        return { 
            showIntermediateStops : this.showIntermediateStops,
            minTransferTime : 300 
        };
    },
    
    initSession : function() {
        var this_ = this;
        var url = otp.config.datastoreUrl+'/auth/initLogin';
        $.ajax(url, {
            type: 'GET',
            dataType: 'json',
            
            success: function(data) {
                this_.sessionId = data.sessionId;
                this_.username = data.username;
                this_.showHistoryWidget();
            },
            
            error: function(data) {
                console.log("auth error");
                console.log(data);
            }
        });
                
    },
    
    showHistoryWidget : function() {
        this.callHistoryWidget = new otp.modules.calltaker.CallHistoryWidget(this.id+"-callHistoryWidget", this);
        this.callHistoryWidget.show();
    },
        
    processPlan : function(tripPlan, restoring) {
        otp.modules.multimodal.MultimodalPlannerModule.prototype.processPlan.apply(this, arguments);
        if(!restoring) {
            if(this.activeCall == null) {
                this.startCall();
            }
            
            var query = new otp.modules.calltaker.Query();
            query.set({
                queryParams : JSON.stringify(tripPlan.queryParams),
                fromPlace: this.fromPlaceName || otp.util.Geo.truncatedLatLng(otp.util.Itin.getLocationPlace(tripPlan.queryParams.fromPlace)),
                toPlace: this.toPlaceName || otp.util.Geo.truncatedLatLng(otp.util.Itin.getLocationPlace(tripPlan.queryParams.toPlace)),
                timeStamp : moment().format("YYYY-MM-DDTHH:mm:ss"),
            });
            
            this.callHistoryWidget.queryListView.addQuery(query);
            this.activeCall.queries.push(query);
        }
    },

    startCall : function() {
        this.activeCall = new otp.modules.calltaker.Call();
        this.activeCall.set({
            startTime : moment().format("YYYY-MM-DDTHH:mm:ss"),
        });
        this.callHistoryWidget.callStarted();
    },

    endCall : function() {
        this.activeCall.set({
            endTime : moment().format("YYYY-MM-DDTHH:mm:ss")
        });
        var this_ = this;
        this.saveModel(this.activeCall, function(model) {
            console.log("saveModel call:");
            console.log(model.queries);
            for(var i=0; i < model.queries.length; i++) {
                var query = model.queries[i];
                query.set({
                    "call.id" : model.id
                });
                console.log("updated query:");
                console.log(query);
                this_.saveModel(query);
            }
        });
        this.callHistoryWidget.callEnded();
        this.activeCall = null;
        
        this.clearTrip();
        this.optionsWidget.restoreDefaults();
    },
            
    saveModel : function(model, successCallback) {
        console.log("saveModel");
        console.log(model);
        
        var data = {
            sessionId : this.sessionId,
        };
        
        
        for(var attr in model.attributes){
            //console.log(attr + ': ' + model.attributes[attr]);
            data[model.playName+"."+attr] = model.attributes[attr];
        }
        
        console.log(data);
        $.ajax(model.url, {
            type: 'POST',
            
            data: data,
                
            success: function(data) {
                console.log("success saving model");
                //console.log(data);
                model.id = data;
                if(successCallback) successCallback.call(this, model);
            },
            
            error: function(data) {
                console.log("error logging call");
            }
        });
        
    },
      
        
    CLASS_NAME : "otp.modules.calltaker.CallTakerModule"
});


