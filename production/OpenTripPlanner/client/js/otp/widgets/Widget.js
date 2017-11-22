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

otp.namespace("otp.widgets");

otp.widgets.Widget = otp.Class({
    
    id              : null,
    owner           : null,
    mainDiv         : null,
    header          : null,
    minimizedTab    : null,

    // fields that can be set via the options parameter, and default values:
    draggable       : true,
    minimizable     : true,
    closeable       : false,
    resizable       : false,
    showHeader      : true,
    title           : '', // string
    openInitially   : true, 
    persistOnClose  : false, // whether widget can be opened via 'toolbar' dropdown when closed
    transparent     : false, // whether to hide the default gray background / frame

    isOpen          : true, // whether or not widget is displayed in applicable module view
    isMinimized     : false,

    
    initialize : function(id, owner, options) {
        //console.log('widget constructor: '+this.id);
        
        if(typeof options !== 'undefined') {
            _.extend(this, options);
        }

        this.id = id;        
        this.owner = owner;

        this.owner.addWidget(this);

        // set up the main widget DOM element:
        this.mainDiv = $('<div />').attr('id', id).addClass('otp-widget').appendTo('body');
        if(!this.transparent) this.mainDiv.addClass('otp-widget-nonTransparent');

        if(!this.openInitially) {
            this.isOpen = false;
            this.mainDiv.css('display','none');
        }
        
        if(typeof this.cssClass !== 'undefined') {
            this.mainDiv.addClass(this.cssClass);
        }
        
        if(this.resizable) this.mainDiv.resizable();
        
        if(this.showHeader) this.addHeader();
        
        var this_ = this;
        if(this.draggable) {
            this.mainDiv.draggable({ 
                containment: "#map",
                start: function(event, ui) {
                    $(this_.mainDiv).css({'bottom' : 'auto', 'right' : 'auto'});
                },
                cancel: '.notDraggable'
            });
        }
    },
        
    addHeader : function() {
        var this_ = this;
        //this.title = title;
        this.header = $('<div class="otp-widget-header">'+this.title+'</div>').appendTo(this.mainDiv);
        var buttons = $('<div class="otp-widget-header-buttons"></div>').appendTo(this.mainDiv);

        if(this.closeable) {
		    $("<div class='otp-widget-header-button'>&times;<div>").appendTo(buttons)
		    .click(function(evt) {
			    evt.preventDefault();
			    this_.close();
		    });				
		}
        if(this.minimizable) {
            $('<div class="otp-widget-header-button">&ndash;</div>').appendTo(buttons)
            .click(function(evt) {
			    evt.preventDefault();
                this_.minimize();
            });
        }

        // set up context menu
        this.contextMenu = new otp.core.ContextMenu(this.mainDiv, function() {
            //console.log("widget cm clicked");
        });
        //TRANSLATORS: Minimize widget in context menu
        this.contextMenu.addItem(_tr("Minimize"), function() {
            this_.minimize();
        //TRANSLATORS: Bring widget to front in context menu
        }).addItem(_tr("Bring to front"), function() {
            this_.bringToFront();            
        //TRANSLATORS: Send widget to back in context menu
        }).addItem(_tr("Send to back"), function() {
            this_.sendToBack();            
        });
        
        this.header.dblclick(function() {
            this_.bringToFront();            
        });
    },
    
    setTitle : function(title) {
        this.title = title;
        this.header.html(title);    
    },

    minimize : function() {
        var this_ = this;
        this.hide();
        this.minimizedTab = $('<div class="otp-minimized-tab">'+this.title+'</div>')
        .appendTo($('#otp-minimize-tray'))
        .click(function () {
            this_.unminimize();
        });
        this.isMinimized = true;
    },

    unminimize : function(tab) {
        this.isMinimized = false;
        this.show();
        this.minimizedTab.hide();
    },
    
    bringToFront : function() {
        var frontIndex = this.owner.getWidgetManager().getFrontZIndex();
        this.$().css("zIndex", frontIndex+1);
    },

    sendToBack : function() {
        var backIndex = this.owner.getWidgetManager().getBackZIndex();
        this.$().css("zIndex", backIndex-1);
    },
    
    center : function() {
        var left = $(window).width()/2 - this.$().width()/2;
        var top = $(window).height()/2 - this.$().height()/2;
        
        this.$().offset({ top : top, left: left });
    },

    centerX : function() {
        var left = $(window).width()/2 - this.$().width()/2;  
        this.$().offset({ left: left });
    },

    close : function() {
        if(typeof this.onClose === 'function') this.onClose();
        this.isOpen = false;
        this.hide();
    },
            
    setContent : function(htmlContent) {
        $('<div />').html(htmlContent).appendTo(this.mainDiv);
    },
    
    show : function() {
        this.isOpen = true;
        if(this.isMinimized) this.minimizedTab.show();
        else this.mainDiv.fadeIn(); //show();
    },

    hide : function() {
        if(this.isMinimized) this.minimizedTab.hide();
        else this.mainDiv.fadeOut(); //hide();
    },

    $ : function() {
        return this.mainDiv;
    },
    
    CLASS_NAME : "otp.widgets.Widget"
});


