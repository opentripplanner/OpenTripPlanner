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

otp.namespace("otp.analyst");

/**
 * Color map class.
 */
otp.analyst.ColorMap = otp.Class({

    /**
     * Constructor.
     * 
     * @params options Contains: delta: true or false (impact color palette
     *         only) - min, max: total value range - minCutoff, maxCutoff:
     *         displayed range (default to min/max).
     */
    initialize : function(options) {
        this.options = $.extend({
            delta : false,
            min : 0,
            zDataType : "TIME",
            saturated : true
        }, options);
        this.options.discrete = this.options.discrete || this.options.zDataType == "BOARDINGS";
        this.options.max = this.options.max
                || (this.options.discrete ? (this.options.delta ? 4 : 9) : (this.options.delta ? 600 : 3600));
        this.options.min = this.options.min || (this.options.delta ? -this.options.max : 0);
        this.range = this.options.max - this.options.min;
        this.minCutoff = this.options.min;
        this.maxCutoff = this.options.max;
        // TODO Make this parametrable
        if (this.options.discrete) {
            if (this.options.delta) {
                this.minusInf = 0x7F0000;
                this.plusInf = 0x007F00;
                // Grey to red
                this.paletteMinus = [ 0x7F7F7F, 0x9F5F5F, 0xBF3F3F, 0xDF1F1F, 0xFF0000 ];
                // Grey to green
                this.palettePlus = [ 0x7F7F7F, 0x5F9F5F, 0x3FBF3F, 0x1FDF1F, 0x00FF00 ];
            } else {
                // Various colors
                if (this.options.saturated)
                    this.palette = [ 0x0095FF, 0x0050FF, 0x5000FF, 0xCF00FF, 0xFF0062, 0xFF3B00, 0xFF7D00, 0xFFB000,
                            0xFFE300, 0xADF500 ];
                else
                    this.palette = [ 0x7FCAFF, 0x7F97FF, 0xA77FFF, 0xE77FFF, 0xFF7FB0, 0xFF9C7E, 0xFFBD7E, 0xFFD77E,
                            0xFFF17E, 0xCAF562 ];
            }
        } else if (this.options.delta) {
            this.minusInf = 0x7F0000;
            this.plusInf = 0x007F00;
            // Grey to red
            this.paletteMinus = [ 0x7F7F7F, 0x8F6F6F, 0x9F5F5F, 0xAF4F4F, 0xBF3F3F, 0xCF2F2F, 0xDF1F1F, 0xEF0F0F,
                    0xFF0000 ];
            // Grey to green
            this.palettePlus = [ 0x7F7F7F, 0x6F8F6F, 0x5F9F5F, 0x4FAF4F, 0x3FBF3F, 0x2FCF2F, 0x1FDF1F, 0x0FEF0F,
                    0x00FF00 ];
        } else {
            // Blue - Green - Yellow - Red - Black gradient
            if (this.options.saturated) {
                this.palette = [ 0x0000FF, 0x0033FF, 0x0065FF, 0x0099FF, 0x00CBFF, 0x00FFFF, 0x00FF99, 0x00FF33,
                        0x33FF00, 0x99FF00, 0xFFFF00, 0xFFE200, 0xFFC600, 0xFFAA00, 0xFF8D00, 0xFF7100, 0xFF5400,
                        0xFF3800, 0xFF1C00, 0xFF0000, 0xE50000, 0xCC0000, 0xB20000, 0x990000, 0x7F0000, 0x660000,
                        0x4C0000, 0x330000, 0x190000, 0x000000 ];
            } else
                this.palette = [ 0x7F7FFF, 0x7F99FF, 0x7FB4FF, 0x7FCFFF, 0x7FEAFF, 0x7FFFF8, 0x7FFFDD, 0x7FFFC2,
                        0x7FFFA7, 0x7FFF8C, 0x8CFF7F, 0xA7FF7F, 0xC2FF7F, 0xDDFF7F, 0xF8FF7F, 0xFFEA7F, 0xFFCF7F,
                        0xFFB47F, 0xFF997F, 0xFF7F7F, 0xE57D7D, 0xCC7A7A, 0xB27373, 0x996A6A, 0x7F5F5F, 0x665151,
                        0x4C4040, 0x332D2D, 0x191818, 0x000000 ];
        }
    },

    setMinCutoff : function(minCutoff) {
        this.minCutoff = minCutoff;
        if (this.minCutoff < this.options.min)
            this.minCutoff = this.options.min;
        this._redrawLegend();
    },

    setMaxCutoff : function(maxCutoff) {
        this.maxCutoff = maxCutoff;
        if (this.maxCutoff > this.max)
            this.maxCutoff = this.max;
        this._redrawLegend();
    },

    /**
     * Get the color based on some value. Return null for no value (outside
     * range).
     */
    colorize : function(v) {
        if (this.options.delta && !this.options.discrete) {
            // Delta mode, if outside bounds, return +/- inf color
            if (v < this.minCutoff)
                return this.minusInf;
            if (v > this.maxCutoff)
                return this.plusInf;
        } else {
            // Other modes: if outside bounds, return NULL
            if (v < this.minCutoff || v > this.maxCutoff)
                return null;
        }
        var palette = this.palette;
        var min, range;
        if (this.options.delta) {
            // Pick among two palettes
            if (v < 0) {
                palette = this.paletteMinus;
                v = -v;
                range = -this.options.min;
                min = 0;
            } else {
                palette = this.palettePlus;
                min = 0;
                range = this.options.max;
            }
        } else {
            min = this.options.min;
            range = this.range;
        }
        var index;
        if (this.options.discrete) {
            // Discrete mode: we loop the palette
            // color frontier is at value + 0.5 to make smoother transition
            index = Math.floor(v + 0.5) % palette.length;
        } else {
            // Other modes: we limit ourselves to the range
            index = Math.round((v - min) / range * palette.length);
            if (index < 0)
                index = 0;
            if (index >= palette.length)
                index = palette.length - 1;
        }
        return palette[index];
    },

    /**
     * Set the canvas where the legend is to be drawned. Keep it as a reference:
     * the legend is updated when colormap range is updated. Only one legend is
     * supported.
     */
    setLegendCanvas : function(canvas) {
        this.legendCanvas = canvas;
        this._redrawLegend();
        return this;
    },

    /**
     * Redraw the legend.
     */
    _redrawLegend : function() {
        var ctx = this.legendCanvas.getContext("2d");
        var width = this.legendCanvas.width;
        var height = this.legendCanvas.height;
        for (var x = 0; x < width; x++) {
            var v = x / width * this.range + this.options.min;
            var color = this.colorize(v);
            if (color != null) {
                ctx.fillStyle = "#" + ("000000" + color.toString(16)).slice(-6);
            } else {
                ctx.fillStyle = "#888";
            }
            ctx.fillRect(x, 0, 1, height);
        }
        var nLabels = width / 40; // Max 1 label / n pixels
        var vDelta = this.range / nLabels;
        var mod = 1;
        if (this.options.zDataType == "TIME") {
            mod = vDelta > 1800 ? 900 : vDelta > 900 ? 600 : vDelta > 600 ? 300 : 60;
        } else if (this.options.zDataType == "WALK_DISTANCE") {
            mod = vDelta > 1000 ? 200 : vDelta > 500 ? 100 : 50;
        }
        vDelta = Math.floor(vDelta / mod + 1) * mod;
        if (vDelta == 0)
            vDelta = mod;
        var vs = [];
        for (var i = 0; i < Math.round(this.options.max / vDelta + 0.5); i++) {
            vs.push(vDelta * i);
        }
        for (var i = -1; i > Math.round(this.options.min / vDelta - 0.5); i--) {
            vs.push(vDelta * i);
        }
        ctx.strokeStyle = "#000";
        ctx.fillStyle = "#000";
        ctx.font = "8pt Sans-Serif";
        for (var i = 0; i < vs.length; i++) {
            var v = vs[i];
            var txt;
            if (this.options.zDataType == "TIME" && !this.options.delta) {
                var h = Math.floor(v / 3600);
                var mm = Math.floor(v / 60) % 60;
                // Where is snprintf in JS?
                txt = h + ":" + (mm < 10 ? "0" : "") + mm;
            } else if (this.options.zDataType == "TIME") {
                var mm = Math.floor(v / 60);
                var ss = Math.floor(v % 60);
                txt = mm + ":" + (ss < 10 ? "0" : "") + ss;
            } else {
                txt = v + (this.options.zDataType == "WALK_DISTANCE" ? "m" : "");
            }
            // floor() + 0.5: Align to pixel boundary
            var x = Math.floor((v - this.options.min) / this.range * width) + 0.5;
            ctx.beginPath();
            ctx.moveTo(x, 0);
            ctx.lineTo(x, height);
            ctx.stroke();
            ctx.fillText(txt, x, height - 2);
        }
    }
});
