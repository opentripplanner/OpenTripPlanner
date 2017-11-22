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

otp.namespace("otp.modules.analyst");

otp.modules.analyst.AnalystLegendWidget = 
    otp.Class(otp.widgets.Widget, {

    imgWidth : null,
    imgHeight : null,
    
    initialize : function(id, module, width, height) {
        otp.widgets.Widget.prototype.initialize.call(this, id, module, {
            //TRANSLATORS: Legend title in Analyst
            title : _tr("Legend : travel time in minutes"),
            cssClass : 'otp-analyst-legendWidget'
        });
        
        this.imgWidth = width;
        this.imgHeight = height;
        
        this.img = $('<img />').appendTo(this.mainDiv)
        .css({
            width: width,
            height: height,
        });
    },
    
    refresh : function(params) {
	    this.img.attr('src', otp.config.hostname+ '/otp/analyst/legend.png?width='
       + this.imgWidth+'&height=' + this.imgHeight + '&styles=' + params.styles);
    }
});
