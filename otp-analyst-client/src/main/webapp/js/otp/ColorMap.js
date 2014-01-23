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
 * Factory method
 */
OTPA.colorMap = function(min, max) {
    return new OTPA.ColorMap();
}

/**
 * Constructor.
 */
OTPA.ColorMap = function(min, max) {
    this.min = min || 0;
    this.max = max || 3600; // 1h in seconds
    this.range = this.max - this.min;
    this.minCutoff = min;
    this.maxCutoff = max;
    // TODO Make this parametrable
    this.palette = [ 0x7F7FFF, 0x7F99FF, 0x7FB4FF, 0x7FCFFF, 0x7FEAFF,
            0x7FFFF8, 0x7FFFDD, 0x7FFFC2, 0x7FFFA7, 0x7FFF8C, 0x8CFF7F,
            0xA7FF7F, 0xC2FF7F, 0xDDFF7F, 0xF8FF7F, 0xFFEA7F, 0xFFCF7F,
            0xFFB47F, 0xFF997F, 0xFF7F7F, 0xE57D7D, 0xCC7A7A, 0xB27373,
            0x996A6A, 0x7F5F5F, 0x665151, 0x4C4040, 0x332D2D, 0x191818,
            0x000000 ];
};

OTPA.ColorMap.prototype.setMinCutoff = function(minCutoff) {
    this.minCutoff = minCutoff;
    if (this.minCutoff < this.min)
        this.minCutoff = this.min;
};

OTPA.ColorMap.prototype.setMaxCutoff = function(maxCutoff) {
    this.maxCutoff = maxCutoff;
    if (this.maxCutoff > this.max)
        this.maxCutoff = this.max;
};

/**
 * Get the color based on some value. Return null for no value (outside range).
 */
OTPA.ColorMap.prototype.colorize = function(v) {
    if (v < this.minCutoff || v > this.maxCutoff)
        return null;
    return this.palette[Math.round((v - this.min) / this.range
            * this.palette.length - 0.5)];
};
