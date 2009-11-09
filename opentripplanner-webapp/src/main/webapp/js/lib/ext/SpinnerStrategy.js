/**
 * Copyright (c) 2008, Steven Chim
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 *     * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *     * The names of its contributors may not be used to endorse or promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/***
 * Abstract Strategy
 */
Ext.ux.form.Spinner.Strategy = function(config){
	Ext.apply(this, config);
};

Ext.extend(Ext.ux.form.Spinner.Strategy, Ext.util.Observable, {
	defaultValue : 0,
	minValue : undefined,
	maxValue : undefined,
	incrementValue : 1,
	alternateIncrementValue : 5,
	
	onSpinUp : function(field){
		this.spin(field, false, false);
	},

	onSpinDown : function(field){
		this.spin(field, true, false);
	},

	onSpinUpAlternate : function(field){
		this.spin(field, false, true);
	},

	onSpinDownAlternate : function(field){
		this.spin(field, true, true);
	},

	spin : function(field, down, alternate){
		//extend
	},

	fixBoundries : function(value){
		return value;
		//overwrite
	}
	
});

/***
 * Concrete Strategy: Numbers
 */
Ext.ux.form.Spinner.NumberStrategy = function(config){
	Ext.ux.form.Spinner.NumberStrategy.superclass.constructor.call(this, config);
};

Ext.extend(Ext.ux.form.Spinner.NumberStrategy, Ext.ux.form.Spinner.Strategy, {

    allowDecimals : true,
    decimalPrecision : 2,
    
	spin : function(field, down, alternate){
		Ext.ux.form.Spinner.NumberStrategy.superclass.spin.call(this, field, down, alternate);

		var v = parseFloat(field.getValue());
		var incr = (alternate == true) ? this.alternateIncrementValue : this.incrementValue;

		(down == true) ? v -= incr : v += incr ;
		v = (isNaN(v)) ? this.defaultValue : v;
		v = this.fixBoundries(v);
		field.setRawValue(v);
	},

	fixBoundries : function(value){
		var v = value;

		if(this.minValue != undefined && v < this.minValue){
			v = this.minValue;
		}
		if(this.maxValue != undefined && v > this.maxValue){
			v = this.maxValue;
		}

		return this.fixPrecision(v);
	},
	
    // private
    fixPrecision : function(value){
        var nan = isNaN(value);
        if(!this.allowDecimals || this.decimalPrecision == -1 || nan || !value){
            return nan ? '' : value;
        }
        return parseFloat(parseFloat(value).toFixed(this.decimalPrecision));
    }
});


/***
 * Concrete Strategy: Date
 */
Ext.ux.form.Spinner.DateStrategy = function(config){
	Ext.ux.form.Spinner.DateStrategy.superclass.constructor.call(this, config);
};

Ext.extend(Ext.ux.form.Spinner.DateStrategy, Ext.ux.form.Spinner.Strategy, {
	defaultValue : new Date(),
	format : "Y-m-d",
	incrementValue : 1,
	incrementConstant : Date.DAY,
	alternateIncrementValue : 1,
	alternateIncrementConstant : Date.MONTH,

	spin : function(field, down, alternate){
		Ext.ux.form.Spinner.DateStrategy.superclass.spin.call(this);

		var v = field.getRawValue();
		
		v = Date.parseDate(v, this.format);
		var dir = (down == true) ? -1 : 1 ;
		var incr = (alternate == true) ? this.alternateIncrementValue : this.incrementValue;
		var dtconst = (alternate == true) ? this.alternateIncrementConstant : this.incrementConstant;

		if(typeof this.defaultValue == 'string'){
			this.defaultValue = Date.parseDate(this.defaultValue, this.format);
		}

		v = (v) ? v.add(dtconst, dir*incr) : this.defaultValue;

		v = this.fixBoundries(v);
		field.setRawValue(Ext.util.Format.date(v,this.format));
	},
	
	//private
	fixBoundries : function(date){
		var dt = date;
		var min = (typeof this.minValue == 'string') ? Date.parseDate(this.minValue, this.format) : this.minValue ;
		var max = (typeof this.maxValue == 'string') ? Date.parseDate(this.maxValue, this.format) : this.maxValue ;

		if(this.minValue != undefined && dt < min){
			dt = min;
		}
		if(this.maxValue != undefined && dt > max){
			dt = max;
		}

		return dt;
	}

});


/***
 * Concrete Strategy: Time
 */
Ext.ux.form.Spinner.TimeStrategy = function(config){
	Ext.ux.form.Spinner.TimeStrategy.superclass.constructor.call(this, config);
};

Ext.extend(Ext.ux.form.Spinner.TimeStrategy, Ext.ux.form.Spinner.DateStrategy, {
	format : "H:i",
	incrementValue : 1,
	incrementConstant : Date.MINUTE,
	alternateIncrementValue : 1,
	alternateIncrementConstant : Date.HOUR
});
