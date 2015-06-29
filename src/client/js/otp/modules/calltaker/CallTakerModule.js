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
        
    tripOptionsWidgetCssClass : 'otp-calltaker-tripOptionsWidget',
    itinerariesWidgetCssClass : 'otp-calltaker-itinerariesWidget',
    
    initialize : function(webapp, id, options) {
        otp.modules.multimodal.MultimodalPlannerModule.prototype.initialize.apply(this, arguments);
        this.templateFiles.push('otp/modules/calltaker/calltaker-templates.html');

        this.requiresAuth = true;
        this.authUserRoles = ['calltaker', 'all'];
    },
    
    activate : function() {    
        if(this.activated) return;
        console.log("activate ctm: " + this.tripOptionsWidgetCssClass);
        otp.modules.multimodal.MultimodalPlannerModule.prototype.activate.apply(this);
        
        // use app-wide session manager
        this.sessionManager = this.webapp.sessionManager;

        this.showHistoryWidget();
        this.mailablesWidget = new otp.modules.calltaker.MailablesWidget(this.id+'-mailablesWidget', this);
    },
    
    getExtendedQueryParams : function() {
        return { 
            showIntermediateStops : true,
            minTransferTime : 180
        };
    },
    
    /*
    newSession : function() {
        var this_ = this;
        var url = otp.config.datastoreUrl+'/auth/newSession';
        $.ajax(url, {
            type: 'GET',
            dataType: 'json',
            
            success: function(data) {
                console.log("newSession success: "+data.sessionId);
                var redirectUrl = this_.options.trinet_verify_login_url + "?session=" + data.sessionId + "&redirect=" + this_.options.module_redirect_url;
                console.log("redirect url: "+redirectUrl);
                window.location = redirectUrl;
            },
            
            error: function(data) {
                console.log("newSession error");
                console.log(data);
            }
        });
    },
    
    checkSession : function(sessionId) {
        var this_ = this;
        var url = otp.config.datastoreUrl+'/auth/checkSession';
        $.ajax(url, {
            type: 'GET',
            data: {
                sessionId : sessionId,
            },
            dataType: 'json',
            
            success: function(data) {
                if(data.username) {
                    this_.sessionId = sessionId;
                    this_.username = data.username;
                    this_.showHistoryWidget();
                }
                else {
                    console.log("bad session id: " + sessionId);
                }
            },
            
            error: function(data) {
                console.log("checkSession error");
                console.log(data);
            }
        });
    },*/
    
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
                fromPlace: this.startName || otp.util.Geo.truncatedLatLng(otp.util.Itin.getLocationPlace(tripPlan.queryParams.fromPlace)),
                toPlace: this.endName || otp.util.Geo.truncatedLatLng(otp.util.Itin.getLocationPlace(tripPlan.queryParams.toPlace)),
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
            for(var i=0; i < model.queries.length; i++) {
                var query = model.queries[i];
                query.set({
                    "call.id" : model.id
                });
                this_.saveModel(query);
            }
        });
        this.callHistoryWidget.callEnded();
        this.activeCall = null;
        
        this.clearTrip();
        this.optionsWidget.restoreDefaults(true);
    },
            
    saveModel : function(model, successCallback) {
        //console.log("saveModel");
        //console.log(model);
        
        var data = {
            sessionId : this.sessionManager.sessionId,
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


