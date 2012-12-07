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

otp.modules.calltaker.PastQueryModel = 
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
    
});    


otp.modules.calltaker.CallTakerModule = 
    otp.Class(otp.modules.multimodal.MultimodalPlannerModule, {

    moduleName  : "Call Taker Interface",
    moduleId    : "calltaker",

    queryLogger : null,    
    userName    : "anonymous",
    
    initialize : function(webapp) {
        otp.modules.multimodal.MultimodalPlannerModule.prototype.initialize.apply(this, arguments);

        this.queryLogger = new otp.core.QueryLogger(this);        
        this.userName = "testct";
        this.password = "secret";
        
        this.showIntermediateStops = true;
        
    },
    
    activate : function() {
    
        otp.modules.multimodal.MultimodalPlannerModule.prototype.activate.apply(this);
        
        this.tipWidget.$().css('display','none');

        // set up history widget        
        this.pastQueriesWidget = new otp.widgets.PastQueriesWidget(this.moduleId+"pastQueriesWidget", this);
        this.widgets.push(this.pastQueriesWidget);
        this.pastQueriesWidget.show();
                
        this.pastQueries = new otp.modules.calltaker.PastQueryCollection();
        this.pastQueries.on('reset', this.onResetQueries, this);
        this.fetchQueries();
    },
    
    getExtendedQueryParams : function() {
        return { 
            showIntermediateStops : this.showIntermediateStops,
            minTransferTime : 300 
        };
    },
        
    onResetQueries : function(queries) {
        this.pastQueriesWidget.updateQueries(queries);
    },

    processPlan : function(tripPlan, restoring) {
        otp.modules.multimodal.MultimodalPlannerModule.prototype.processPlan.apply(this, arguments);
        if(!restoring) this.queryLogger.logQuery(tripPlan.queryParams, this.userName, this.password);    
    },

    queryLogged : function() {
        this.fetchQueries();
    },
    
    fetchQueries : function() {
        this.pastQueries.fetch({ data: { userName: this.userName, password: this.password, limit: 10 }});
    },  
        
    CLASS_NAME : "otp.modules.calltaker.CallTakerModule"
});
