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

otp.namespace("otp.core");

otp.core.Webapp = otp.Class({

    map     : null,
    
    modules : [ ],
    moduleMenu : null,
    
    activeModule : null,
    
    widgetManager   : null,
    infoWidgets     : { },

    geocoders : [ ],
    
    transitIndex : null,
    
    urlParams : null,

    initialize : function() {


        // misc. housekeeping
        
        if(typeof console == 'undefined') console = { log: function(str) {} };
        $.support.cors = true;
        var this_ = this;

        // set the urlRoot variable, if needed
        /*if(!otp.config.urlRoot) {
            otp.config.urlRoot = window.location.pathname;
            if(otp.util.Text.endsWith(otp.config.urlRoot, "index.html"))
                otp.config.urlRoot = otp.config.urlRoot.substring(0, otp.config.urlRoot.length-10);
        }
        if(!otp.util.Text.endsWith(otp.config.urlRoot, "/"))
            otp.config.urlRoot += "/";
        console.log("urlRoot "+otp.config.urlRoot);*/

        // init url params
        this.urlParams = { };
        var match,
            pl     = /\+/g,  // Regex for replacing addition symbol with a space
            search = /([^&=]+)=?([^&]*)/g,
            decode = function (s) { return decodeURIComponent(s.replace(pl, " ")); },
            query  = window.location.search.substring(1);

        while (match = search.exec(query))
            this.urlParams[decode(match[1])] = decode(match[2]);
            
        
        // init siteUrl, if necessary
        
        if(typeof otp.config.siteUrl === 'undefined') {
            otp.config.siteUrl = window.location.protocol + '//' + window.location.host + window.location.pathname;
        }
            
        // set the logo & title
        
        if(otp.config.showLogo) {
          $('<div id="logo"><a href="'+otp.config.siteURL+'"><img src="'+otp.config.logoGraphic+'" style="height:100%"></a></div>').appendTo('#branding');

        }
        
        if(otp.config.siteName !== undefined) {
            document.title = otp.config.siteName;
            if(otp.config.showTitle) {
                $("<div id='site-title'><a href='"+otp.config.siteURL+"'>"+otp.config.siteName+"</a></div>").appendTo('#branding');
            }
        }
        
        // create the Webapp-owned objects
        
        this.map = new otp.core.Map(this);        
        this.transitIndex = new otp.core.TransitIndex(this);
        this.widgetManager = new otp.widgets.WidgetManager();
        
        
        if(otp.config.geocoders) {
            for(var i=0; i<otp.config.geocoders.length; i++) {
                var gcConfig = otp.config.geocoders[i];
                console.log('init geocoder: '+gcConfig.name);
                //var geocoder = window[gcConfig.classname](gcConfig.url, gcConfig.addressParam);
                
                var gcClass = this.stringToFunction(gcConfig.className);
                var geocoder = new gcClass(gcConfig.url, gcConfig.addressParam);
                geocoder.name = gcConfig.name;
                //console.log(geocoder);
                
                this.geocoders.push(geocoder);
                //var geocoder = new otp.core.Geocoder(otp.config.geocoder.url, otp.config.geocoder.addressParam);
            }
        }
       
        // initialize the AddThis widget
        
        if(otp.config.showAddThis) {
            var addThisHtml = '<div id="addthis" class="addthis_toolbox addthis_default_style"\n';
            addThisHtml += 'addthis:url="'+otp.config.siteURL+'"\n';
            addThisHtml += 'addthis:title="'+otp.config.addThisTitle+'"\n';
            addThisHtml += 'addthis:description="'+otp.config.siteDescription+'">\n';
            addThisHtml += '<a class="addthis_button_twitter"></a>\n';
            addThisHtml += '<a class="addthis_button_facebook"></a>\n';
            addThisHtml += '<a class="addthis_button_google_plusone_share"></a>\n';
            addThisHtml += '<a class="addthis_button_preferred_1"></a>\n';
            addThisHtml += '<a class="addthis_button_compact"></a>\n';
            addThisHtml += '<a class="addthis_counter addthis_bubble_style"></a>\n';
            addThisHtml += '</div>';
            
            $(addThisHtml).appendTo('#branding');
            
            addthis_config = {
		         pubid: otp.config.addThisPubId,
		         data_track_clickback: false
		    };
		    $.getScript("http://s7.addthis.com/js/250/addthis_widget.js#pubid="+otp.config.addThisPubId);
        }		
        
        // create the widget manager menu & icon
        
        this.widgetManagerMenu = new otp.core.WidgetManagerMenu(this);

        var widgetManagerIcon = $('<div id="otp-widgetManager"></div>')
        .appendTo('#branding')
        .click(function(event) {
            this_.widgetManagerMenu.show(); // showWidgetManagerMenu();
        });
        
        
        // create the info widgets and links along header bar
        
        if(otp.config.infoWidgets !== undefined && otp.config.infoWidgets.length > 0) {
            var nav = $('<nav id="main-menu" role="article">').appendTo('#branding');
            var ul = $('<ul>').appendTo(nav);
            
            for(var i=0; i<otp.config.infoWidgets.length; i++) {
            
                if(otp.config.infoWidgets[i] == undefined) continue;
    
                var id = "otp-infoWidget-"+i;            
                
                var options = {};
                if(_.has(otp.config.infoWidgets[i], 'title')) options.title = otp.config.infoWidgets[i].title;
                if(_.has(otp.config.infoWidgets[i], 'cssClass')) options.cssClass = otp.config.infoWidgets[i].cssClass;
                
                this.infoWidgets[id] = new otp.widgets.InfoWidget(otp.config.infoWidgets[i].styleId,
                                                                  this, options, otp.config.infoWidgets[i].content);
                
                $("<li id='"+id+"'><a href='#'>"+otp.config.infoWidgets[i].title+"</a></li>").appendTo(ul).click(function(e) {
                    e.preventDefault();
                    var widget = this_.infoWidgets[this.id];
                    if(!widget.isOpen) widget.show();
                    widget.bringToFront();
                });
            
            }
        }


        // initialize the modules 
        
        if(this.urlParams['module'])
            console.log("startup module: "+this.urlParams['module'])
        if(otp.config.modules) {
            var setDefault = false, defaultModule = null;
            for(var i=0; i<otp.config.modules.length; i++) {
                var modConfig = otp.config.modules[i];
                var modClass = this.stringToFunction(modConfig.className);
                var module = new modClass(this);
                if(modConfig.id) module.id = modConfig.id;
                if(modConfig.defaultBaseLayer) module.defaultBaseLayer = modConfig.defaultBaseLayer;
                
                var isDefault = false;
                if(_.has(this.urlParams, 'module') && this.urlParams['module'] === module.id) {
                    isDefault = setDefault = true;
                }
                if(modConfig.isDefault) {
                    if(!_.has(this.urlParams, 'module')) isDefault = true;
                    defaultModule = module;
                }
                    
                this.addModule(module, isDefault);//modConfig.isDefault || false);
            }
            if(_.has(this.urlParams, 'module') && !setDefault) {
                console.log("OTP module with id="+this.urlParams['module']+" not found");
                if(defaultModule) this.setActiveModule(defaultModule);
            }
        }                

        // create the module selector
        
        if(otp.config.showModuleSelector && this.modules.length > 1) {

            var selector = $('<select id="otp_moduleSelector"></select>').appendTo('#branding');
            for(i in this.modules) {
                var module = this.modules[i];
                var option = $('<option'+(module == this_.activeModule ? ' selected' : '')+'>'+module.moduleName+'</option>').appendTo(selector);
            }        
            selector.change(function() {
                this_.setActiveModule(this_.modules[this.selectedIndex]);
            });
                       
        }
                
        // retrieve a saved trip, if applicable
		//if(window.location.hash !== "")
		//	otp.util.DataStorage.retrieve(window.location.hash.replace("#", ""), this.activeModule);
			
		
    },
    
    addModule : function(module, makeActive) {
        makeActive = typeof makeActive !== 'undefined' ? makeActive : false;
        this.modules.push(module);
        if(makeActive) {
            this.setActiveModule(module);
        }
    },
    
    setActiveModule : function(module) {
        //console.log("set active module: "+module.moduleName);
        if(this.activeModule != null) {
            this.activeModule.deselected();
            
            for(var i = 0; i < this.activeModule.widgets.length; i++) {
                this.activeModule.widgets[i].hide();
            }
        }
        
        $('#otp_toptitle').html(module.moduleName);
        
        for(var i = 0; i < module.widgets.length; i++) {
            if(module.widgets[i].isOpen) {
                console.log(" - showing widget: "+module.widgets[i].id);
                module.widgets[i].show();
            }
        }        
        if(!module.activated) {
            module.activate();
            if(_.has(this.urlParams, 'module') && this.urlParams.module == module.id) module.restore();
        }
        module.selected();
        
        this.map.activeModuleChanged(this.activeModule, module);
        
        this.activeModule = module;
    },   
    
          
    hideSplash : function() {
    	$("#splash-text").hide();
    	for(widgetId in this.infoWidgets) {
        	this.infoWidgets[widgetId].hide();
    	}
    },
        
    setBounds : function(bounds)
    {
    	this.map.lmap.fitBounds(bounds);
    },
        
   
    mapClicked : function(event) {
        $(this.moduleMenu).hide();
        this.hideSplash();
        this.activeModule.handleClick(event);
    },
    
    addWidget : function(widget) {
        //this.widgets.push(widget);
        this.widgetManager.addWidget(widget);
    },
    
    getWidgetManager : function() {
        return this.widgetManager;
    },
    
    // TODO: move to Util library
    
    stringToFunction : function(str) {
        var arr = str.split(".");

        var fn = (window || this);
        for(var i = 0, len = arr.length; i < len; i++) {
            fn = fn[arr[i]];
        }

        if(typeof fn !== "function") {
            throw new Error("function not found");
        }

        return  fn;
    },

    CLASS_NAME : "otp.core.Webapp"
});

