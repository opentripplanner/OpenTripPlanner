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
    
    div :       null,
    id :        null,
    
    initialize : function(id) {
        //otp.configure(this, config);
        this.id = id;
        //console.log('widget constructor: '+this.id);
        
        this.div = document.createElement('div');
        this.div.setAttribute('id', this.id);
        this.div.className = 'otp-widget';
        document.body.appendChild(this.div);
        var this_ = this;
        $(this.div).draggable({ 
            containment: "#map",
            start: function(event, ui) {
                console.log("dragging: "+$(this_.div).css('bottom'));
                $(this_.div).css({'bottom' : 'auto'});
            }
        });
        //$(this.div).resizable();
    },
    
    setContent : function(content) {
        this.div.innerHTML = content;
    },
    
    show : function() {
        $(this.div).fadeIn();//show();
    },

    hide : function() {
        $(this.div).fadeOut();//hide();
    },
    
    $ : function() {
        return $(this.div);
    },
    
    CLASS_NAME : "otp.widgets.Widget"
});


