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

var OTPA = OTPA || {}; // namespace

/**
 * Color map class.
 */

/**
 * Factory method.
 * 
 * @params options Contains: delta: true or false (impact color palette only) -
 *         min, max: total value range - minCutoff, maxCutoff: displayed range
 *         (default to min/max).
 */
OTPA.colorMap = function(options) {
    return new OTPA.ColorMap(options);
};

/**
 * Constructor.
 */
OTPA.ColorMap = function(options) {
    var options = options || {};
    this.delta = options.delta === undefined ? false : options.delta;
    this.discrete = options.discrete === undefined ? false : options.discrete;
    this.max = options.max || (this.discrete ? 10 : this.delta ? 600 : 3600);
    this.min = options.min || (this.delta ? -this.max : 0);
    this.unit = options.unit === undefined ? "s" : options.unit;
    this.range = this.max - this.min;
    this.minCutoff = this.min;
    this.maxCutoff = this.max;
    // TODO Make this parametrable
    if (this.discrete) {
        this.palette = [ 0x7FCAFF, 0x7F97FF, 0xA77FFF, 0xE77FFF, 0xFF7FB0,
                0xFF9C7E, 0xFFBD7E, 0xFFD77E, 0xFFF17E, 0xCAF562 ];
    } else if (this.delta) {
        // red - grey - green, symetrical
        this.palette = [ 0xFF0000, 0xEF0F0F, 0xDF1F1F, 0xCF2F2F, 0xBF3F3F,
                0xAF4F4F, 0x9F5F5F, 0x8F6F6F, 0x7F7F7F, 0x7F7F7F, 0x6F8F6F,
                0x5F9F5F, 0x4FAF4F, 0x3FBF3F, 0x2FCF2F, 0x1FDF1F, 0x0FEF0F,
                0x00FF00 ];
        this.minusInf = 0x7F0000;
        this.plusInf = 0x007F00;
    } else {
        this.palette = [ 0x7F7FFF, 0x7F99FF, 0x7FB4FF, 0x7FCFFF, 0x7FEAFF,
                0x7FFFF8, 0x7FFFDD, 0x7FFFC2, 0x7FFFA7, 0x7FFF8C, 0x8CFF7F,
                0xA7FF7F, 0xC2FF7F, 0xDDFF7F, 0xF8FF7F, 0xFFEA7F, 0xFFCF7F,
                0xFFB47F, 0xFF997F, 0xFF7F7F, 0xE57D7D, 0xCC7A7A, 0xB27373,
                0x996A6A, 0x7F5F5F, 0x665151, 0x4C4040, 0x332D2D, 0x191818,
                0x000000 ];
    }
};

OTPA.ColorMap.prototype.setMinCutoff = function(minCutoff) {
    this.minCutoff = minCutoff;
    if (this.minCutoff < this.min)
        this.minCutoff = this.min;
    this._redrawLegend();
};

OTPA.ColorMap.prototype.setMaxCutoff = function(maxCutoff) {
    this.maxCutoff = maxCutoff;
    if (this.maxCutoff > this.max)
        this.maxCutoff = this.max;
    this._redrawLegend();
};

/**
 * Get the color based on some value. Return null for no value (outside range).
 */
OTPA.ColorMap.prototype.colorize = function(v) {
    if (this.delta && !this.discrete) {
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
    // Note: if we are in "discrete" mode,
    // color frontier is at value + 0.5 to make smoother transition
    var index = Math.round((v - this.min) / this.range * this.palette.length
            - (this.discrete ? 0.0 : 0.5));
    if (this.discrete) {
        // Discrete mode: we loop the palette
        index = index % this.palette.length;
    } else {
        // Other modes: we limit ourselves to the range
        if (index < 0)
            index = 0;
        if (index >= this.palette.length)
            index = this.palette.length - 1;
    }
    return this.palette[index];
};

/**
 * Set the canvas where the legend is to be drawned. Keep it as a reference: the
 * legend is updated when colormap range is updated. Only one legend is
 * supported.
 */
OTPA.ColorMap.prototype.setLegendCanvas = function(canvas) {
    this.legendCanvas = canvas;
    this._redrawLegend();
};

/**
 * Redraw the legend.
 */
OTPA.ColorMap.prototype._redrawLegend = function() {
    var ctx = this.legendCanvas.getContext("2d");
    var width = this.legendCanvas.width;
    var height = this.legendCanvas.height;
    for (var x = 0; x < width; x++) {
        var v = x / width * this.range + this.min;
        var color = this.colorize(v);
        if (color != null) {
            ctx.fillStyle = "#" + color.toString(16);
        } else {
            ctx.fillStyle = "#888";
        }
        ctx.fillRect(x, 0, 1, height);
    }
    var nLabels = width / 40; // Max 1 label / n pixels
    var vDelta = this.range / nLabels;
    var mod = 1;
    if (this.unit == "s") {
        mod = vDelta > 1800 ? 600 : vDelta > 900 ? 300 : 60;
    } else if (this.unit == "m") {
        var mod = vDelta > 1000 ? 200 : vDelta > 500 ? 100 : 50;
    }
    vDelta = Math.floor(vDelta / mod) * mod;
    if (vDelta == 0)
        vDelta = mod;
    nLabels = Math.round(this.range / vDelta);
    ctx.strokeStyle = "#000";
    ctx.fillStyle = "#000";
    ctx.font = "8pt Sans-Serif";
    for (var i = 0; i < nLabels; i++) {
        var v = this.min + vDelta * i;
        var txt;
        if (this.unit == "s" && !this.delta) {
            var h = Math.floor(v / 3600);
            var mm = Math.floor(v / 60) % 60;
            // Where is snprintf in JS?
            txt = h + ":" + (mm < 10 ? "0" : "") + mm;
        } else if (this.unit == "s") {
            var mm = Math.floor(v / 60);
            var ss = Math.floor(v % 60);
            txt = mm + ":" + (ss < 10 ? "0" : "") + ss;
        } else {
            txt = v + this.unit;
        }
        // floor() + 0.5: Align to pixel boundary
        var x = Math.floor((v - this.min) / this.range * width) + 0.5;
        ctx.beginPath();
        ctx.moveTo(x, 0);
        ctx.lineTo(x, height);
        ctx.stroke();
        ctx.fillText(txt, x, height - 2);
    }
};
