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

otp.namespace("otp.modules");

otp.modules.Module = otp.Class({

    webapp      : null,

    moduleName  : "N/A",
    widgets     : null,
    mapLayers   : null,
    
    activated   : false,
    
    options     : null,
    
    handlers    : null,

    templateFiles   : null,

    initialize : function(webapp, id, options) {
        this.webapp = webapp;
        this.id = id;
        this.options = options || {};
        this.widgets = [];
        this.mapLayers = {};
        this.handlers = {};
        this.templateFiles = [];

        this.requiresAuth = false;
        this.authUserRoles = [];
    },
    

    addLayer : function(name, layer) {
        this.mapLayers[name] = layer;
    },
    
    addWidget : function(widget) {
        this.widgets.push(widget);
        this.webapp.widgetManager.addWidget(widget);
    },
    
    getWidgetManager : function() {
        return this.webapp.widgetManager;
    },
    
    createWidget : function(id, content) {
        var widget = new otp.widgets.Widget(id, this.webapp.widgetManager); 
        widget.setContent(content);
        return widget;
    },
    
    on : function(eventName, handler) {    
        if(!_.has(this.handlers, eventName)) {
            this.handlers[eventName] = [];
        }
        this.handlers[eventName].push(handler);
    },

    invokeHandlers : function(eventName, args) {
        if(_.has(this.handlers, eventName)) {
            var handlerArr = this.handlers[eventName];
            for(var i = 0; i < handlerArr.length; i++) {
                handlerArr[i].apply(this, args);
            }
        }
    },

    // functions to be overridden by subclasses
    
    /**
     * Called when the module is made active for the first time.
     */
         
    activate : function() {
    },


    /**
     * Called to restore module state based on url parameters. Called when the
     * affected module is made active for the first time, following activate().
     */

    restore : function() {
    },


    /**
     * Called when the module is selected as active by the user. When the module
     * is selected for the first time, the call to selected() follows the calls
     * to activate() and restore().
     */

    selected : function() {
    },
     

    /**
     * Called when the module loses focus due to another being selected as
     * active by the user.
     */
        
    deselected : function() {
    },


    /**
     * Called by the Map object when the user clicks on the map.
     */

    handleClick : function(event) {
    },


    /**
     * Called by the Map object when the map is panned or zoomed
     */

    mapBoundsChanged : function(event) {
    },


    /**
     * Called by the Map object to trigger the module to add any module-specific
     * items to the map context menu.
     */

    addMapContextMenuItems : function() {
    },
       
    CLASS_NAME : "otp.modules.Module"
});

