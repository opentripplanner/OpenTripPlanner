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

/*otp.modules.calltaker.PastQueryModel = 
    Backbone.Model.extend({});
    
otp.modules.calltaker.PastQueryCollection = 
    Backbone.Collection.extend({
    
    url: otp.config.loggerURL+'/getQueries',
    model: otp.modules.calltaker.PastQueryModel,
   
    sync: function(method, model, options) {
        //options.dataType = 'jsonp';
        options.data = options.data || {};
        return Backbone.sync(method, model, options);
    },
    
});*/    


otp.modules.calltaker.CallTakerModule = 
    otp.Class(otp.modules.multimodal.MultimodalPlannerModule, {

    moduleName  : "Call Taker Interface",
    moduleId    : "calltaker",

    callTaker : null,
    
    activeCall : null,
    
    initialize : function(webapp) {
        otp.modules.multimodal.MultimodalPlannerModule.prototype.initialize.apply(this, arguments);

        this.callTaker = new otp.modules.calltaker.CallTaker('admin', 'secret');
        
        this.showIntermediateStops = true;
        
    },
    
    activate : function() {    
        if(this.activated) return;
        otp.modules.multimodal.MultimodalPlannerModule.prototype.activate.apply(this);
        console.log("activate ctm");        
        //this.tipWidget.$().css('display','none');

        this.callHistoryWidget = new otp.widgets.CallHistoryWidget(this.moduleId+"-callHistoryWidget", this);
        this.widgets.push(this.callHistoryWidget);
        this.callHistoryWidget.show();
    },
    
    getExtendedQueryParams : function() {
        return { 
            showIntermediateStops : this.showIntermediateStops,
            minTransferTime : 300 
        };
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
                fromPlace: this.fromPlaceName || otp.util.Geo.truncatedLatLng(tripPlan.queryParams.fromPlace),
                toPlace: this.toPlaceName || otp.util.Geo.truncatedLatLng(tripPlan.queryParams.toPlace),
                timeStamp : moment().format("YYYY-MM-DDTHH:mm:ss"),
            });
            
            this.callHistoryWidget.queryListView.addQuery(query);
            this.activeCall.queries.push(query);
            //this.queryLogger.logQuery(tripPlan.queryParams, this.userName, this.password);    
        }
    },

    /*queryLogged : function() {
        this.fetchQueries();
    },
    
    fetchQueries : function() {
        this.pastQueries.fetch({ data: { userName: this.userName, password: this.password, limit: 10 }});
    },*/

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
    },
            
    saveModel : function(model, successCallback) {
        console.log("saveModel");
        console.log(model);
        
        var data = {
            userName : this.callTaker.userName,
            password : this.callTaker.password
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

otp.modules.calltaker.CallTaker = otp.Class({
    
    userName : null,    
    password : null,
   
    initialize : function(userName, password) {
        this.userName = userName;
        this.password = password;
    }
});


otp.modules.calltaker.Call = Backbone.Model.extend({
    
    url : otp.config.loggerURL+'/call',
    playName : 'call',
    queries : null,   
    
    initialize : function() {
        this.queries = [ ];
    },
    
    defaults: {
        startTime: null,
        endTime: null
    },

});

otp.modules.calltaker.CallList = Backbone.Collection.extend({
    model: otp.modules.calltaker.Call,
    url: otp.config.loggerURL+'/call',
});


otp.modules.calltaker.Query = Backbone.Model.extend({
    
    url : otp.config.loggerURL+'/callQuery',
    playName : 'query'
   
});
 
otp.modules.calltaker.QueryList = Backbone.Collection.extend({

    model: otp.modules.calltaker.Query,
    url: otp.config.loggerURL+'/callQuery',
       
});

  

