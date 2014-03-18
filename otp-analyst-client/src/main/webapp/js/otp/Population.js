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
 * Population class. A population is a list of (weighted) locations.
 */

otp.analyst.Population = otp.Class({

    /**
     * Constructor. Create an empty population.
     */
    initialize : function() {
        this.data = [];
        this.onLoadCallbacks = $.Callbacks();
    },

    /**
     * Load population from JSON data.
     */
    loadFromJson : function(jsonUrl, options) {
        var thisPl = this;
        this._loadAjax(jsonUrl, options, function(payload) {
            payload = $.parseJSON(payload);
            for (var i = 0; i < payload.length; i++) {
                thisPl.data.push(payload[i]);
            }
        });
        return this;
    },

    /**
     * Load population from CSV data.
     */
    loadFromCsv : function(csvUrl, options) {
        var thisPl = this;
        this._loadAjax(csvUrl, options, function(payload) {
            payload = thisPl._parseCsv(payload);
            if (payload.length < 1)
                return;
            // Find the various column indexes from CSV header
            var latCol, lonCol, nameCol, weightCol;
            for (var col = 0; col < payload[0].length; col++) {
                var header = payload[0][col];
                if (header == options.latColName) {
                    latCol = col;
                } else if (header == options.lonColName) {
                    lonCol = col;
                } else if (header == options.nameColName) {
                    nameCol = col;
                } else if (header == options.weightColName) {
                    weightCol = col;
                }
            }
            if (latCol == null || lonCol == null)
                return; // Mandatory fields
            for (var row = 1; row < payload.length; row++) {
                var item = {
                    location : {
                        lat : payload[row][latCol],
                        lng : payload[row][lonCol]
                    },
                    w : weightCol ? payload[row][weightCol] : 1.0
                };
                if (nameCol)
                    item.name = payload[row][nameCol];
                thisPl.data.push(item);
            }
        });
        return this;
    },

    /**
     * Generic ajax-loading.
     */
    _loadAjax : function(url, options, dataCallback) {
        var thisPl = this;
        $.ajax({
            url : url,
            success : function(result) {
                if (typeof result === 'string') {
                    dataCallback(result);
                }
                thisPl.onLoadCallbacks.fire(thisPl);
            },
            async : options.async
        });
        return this;
    },

    /**
     * Add a callback when loaded.
     */
    onLoad : function(callback) {
        this.onLoadCallbacks.add(callback);
        return this;
    },

    /**
     * Add a location programmatically.
     */
    add : function(poi) {
        this.data.push(poi);
        return this;
    },

    /**
     * TODO How to properly implement iterators in JS?
     * 
     * @return The number of elements in the list.
     */
    size : function(poi) {
        return this.data.length;
    },

    /**
     * @return The nth element in the list.
     */
    get : function(index) {
        return this.data[index];
    },

    /**
     * Parse CSV data. See http://stackoverflow.com/questions/1293147
     */
    _parseCsv : function(csv, reviver) {
        reviver = reviver || function(r, c, v) {
            return v;
        };
        var chars = csv.split(''), c = 0, cc = chars.length, start, end, table = [], row;
        while (c < cc) {
            table.push(row = []);
            while (c < cc && '\r' !== chars[c] && '\n' !== chars[c]) {
                start = end = c;
                if ('"' === chars[c]) {
                    start = end = ++c;
                    while (c < cc) {
                        if ('"' === chars[c]) {
                            if ('"' !== chars[c + 1]) {
                                break;
                            } else {
                                chars[++c] = '';
                            } // unescape ""
                        }
                        end = ++c;
                    }
                    if ('"' === chars[c])
                        ++c;
                    while (c < cc && '\r' !== chars[c] && '\n' !== chars[c] && ',' !== chars[c])
                        ++c;
                } else {
                    while (c < cc && '\r' !== chars[c] && '\n' !== chars[c] && ',' !== chars[c])
                        end = ++c;
                }
                row.push(reviver(table.length - 1, row.length, chars.slice(start, end).join('')));
                if (',' === chars[c])
                    ++c;
            }
            if ('\r' === chars[c])
                ++c;
            if ('\n' === chars[c])
                ++c;
        }
        return table;
    },
});
