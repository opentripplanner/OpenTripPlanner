/*!
 * "f" is for Format & WHAT THE diff?? v0.5.0
 *
 * Copyright (c) 2009 Joshua Faulkenberry
 * Dual licensed under the MIT and GPL licenses.
 * http://docs.jquery.com/License
 *
 * Date: 2009-03-20 22:15:23 -0700 (Fri, 20 Mar 2009)
 * Revision: 6
 */

/************** "f" is for Format ***************
 * Outputs a JavaScript Date Object in various
 * customizable formats
 **********************************************
 */
window.Date.prototype.f = function(format) {
   if(format == "@") {
      return this.getTime();
   }
   else if(format == "REL") {
   	var diff = (((new Date()).getTime() - this.getTime()) / 1000), day_diff = Math.floor(diff / 86400);
   	return day_diff == 0 && (
 			diff > -60 && "right now" ||
			diff > -120 && "1 minute from now" ||
			diff > -3600 && -(Math.floor(diff / 60)) + " minutes from now" ||
			diff > -7200 && "1 hour ago" ||
			diff > -86400 && -(Math.floor(diff / 3600)) + " hours from now" ||

			diff < 60 && "just now" ||
			diff < 120 && "1 minute ago" ||
			diff < 3600 && Math.floor( diff / 60 ) + " minutes ago" ||
			diff < 7200 && "1 hour ago" ||
			diff < 86400 && Math.floor( diff / 3600 ) + " hours ago") ||

			day_diff == 0 && "Tomorrow" ||
			day_diff > -7 && -(day_diff) + " days from now" ||
			-(Math.ceil( day_diff / 7 )) == 1 && "1 week from now" ||
			day_diff > -78 && -(Math.ceil( day_diff / 7 )) + " weeks from now" ||
			day_diff > -730 && -(Math.ceil( day_diff / 30 )) + " months from now" ||
			day_diff <= -730 && -(Math.ceil( day_diff / 365 )) + " years from now" ||
	
			day_diff == 1 && "Yesterday" ||
			day_diff < 7 && day_diff + " days ago" ||
			(Math.ceil( day_diff / 7 )) == 1 && "1 week ago" ||
			day_diff < 78 && Math.ceil( day_diff / 7 ) + " weeks ago" ||
			day_diff < 730 && Math.ceil( day_diff / 30 ) + " months ago" ||
			Math.ceil( day_diff / 365 ) + " years ago";
   }
   var MONTH_NAMES = ['January','February','March','April','May','June','July','August','September','October','November','December'],
       DAY_NAMES = ['Sunday','Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'],
       LZ = function(x) {return(x<0||x>9?"":"0")+x},
       date = this,
	    format = format + "",
	    result = "",
	    i_format = 0,
	    c = "",
	    token = "",
	    y = date.getYear() + "",
	    M = date.getMonth() + 1,
	    d = date.getDate(),
	    E = date.getDay(),
	    H = date.getHours(),
	    m = date.getMinutes(),
	    s = date.getSeconds(),   
	    yyyy,yy,MMM,MM,dd,hh,h,mm,ss,ampm,HH,H,KK,K,kk,k,
	    value = new Object();
	// Convert real date parts into formatted versions
	if (y.length < 4) {y=""+(y-0+1900);}
	value["y"]=""+y;
	value["yyyy"]=y;
	value["yy"]=y.substr(2,4);
	value["M"]=M;
	value["MM"]=LZ(M);
	value["MMM"]=MONTH_NAMES[M-1];
	value["NNN"]=MONTH_NAMES[M-1].substr(0,3);
	value["N"]=MONTH_NAMES[M-1].substr(0,1);
	value["d"]=d;
	value["dd"]=LZ(d);
	value["e"]=DAY_NAMES[E].substr(0,1);
	value["ee"]=DAY_NAMES[E].substr(0,2);
	value["E"]=DAY_NAMES[E].substr(0,3);
	value["EE"]=DAY_NAMES[E];
	value["H"]=H;
	value["HH"]=LZ(H);
	if (H==0){value["h"]=12;}
	else if (H>12){value["h"]=H-12;}
	else {value["h"]=H;}
	value["hh"]=LZ(value["h"]);
	if (H>11){value["K"]=H-12;} else {value["K"]=H;}
	value["k"]=H+1;
	value["KK"]=LZ(value["K"]);
	value["kk"]=LZ(value["k"]);
	if (H > 11) { value["a"]="PM"; }
	else { value["a"]="AM"; }
	value["m"]=m;
	value["mm"]=LZ(m);
	value["s"]=s;
	value["ss"]=LZ(s);
	while (i_format < format.length) {
		c=format.charAt(i_format);
		token="";
		while ((format.charAt(i_format)==c) && (i_format < format.length)) {
			token += format.charAt(i_format++);
			}
		if (value[token] != null) { result=result + value[token]; }
		else { result=result + token; }
	}
	return result;
}

/************* WHAT THE diff?? *************
 * Calculates the exact difference between
 * any two dates and outputs the results in
 * a customizable incremental breakdown
 *******************************************
 */
window.Date.prototype.diff = function(date, breakdown) {
   var options = {};
   if( typeof date ==  "string" ) {
      if((new Date(date)) != "Invalid Date" && (new Date(date)) != "NaN") {
         date = new Date(date);
      }
      else {
         breakdown = date;
         date = new Date();
      }
   }
   else if( typeof date ==  "object" && !date.getTime) {
      options = date;
      date = new Date();
   }
   if( typeof breakdown ==  "object") {
      options = breakdown;
      breakdown = options.breakdown || "*";
   }
	options.labels = options.labels || {};
   if(breakdown) {
      function processTime(trg) {
         var result = null;
         if(diff >= tl[trg]) {
            if(trg == "Y" || trg == "D" || trg == "C" || trg == "T") {
               //Catch leap years
               for(var yr = (min);yr.getFullYear() <= max.getFullYear();yr.setYear(yr.getFullYear()+1)) {
                  if(yr.isLeapYear()) {
                     diff -= tl["d"];
                  }
               }
            }
            if(diff >= tl[trg]) {
               result = Math.floor(diff/tl[trg]) + " " + (Math.floor(diff/tl[trg]) == 1 && names[trg][0] || names[trg][1]);
               diff = diff%tl[trg];
            }
         }
         eval('breakdown = breakdown.replace(/'+trg+'/g, "")');
         return result;
      }

      var min   = date <= this && date || date > this && this,
         max    = date > this && date || date <= this && this,
         diff   = (max.getTime() - min.getTime()),
         tl     = {
            T : 1000*60*60*24*365*100*10,
            C : 1000*60*60*24*365*100,
            D : 1000*60*60*24*365*10,
            Y : 1000*60*60*24*365,
            M : 1000*60*60*24*28,
            W : 1000*60*60*24*7,
            d : 1000*60*60*24,
            H : 1000*60*60,
            m : 1000*60,
            S : 1000,
            N : 1
         },
         names = {
            T : options.labels.T || ["Mellinium","Mellinia"],
            C : options.labels.C || ["Century","Centuries"],
            D : options.labels.D || ["Decade","Decades"],
            Y : options.labels.Y || ["Year","Years"],
            M : options.labels.M || ["Month","Months"],
            W : options.labels.W || ["Week","Weeks"],
            d : options.labels.d || ["Day","Days"],
            H : options.labels.H || ["Hour","Hours"],
            m : options.labels.m || ["Minute","Minutes"],
            S : options.labels.S || ["Second","Seconds"],
            N : options.labels.N || ["Millisecond","Milliseconds"]
         };
      if(options.len) {
         for(var x in names) {
            names[x] = names[x].substr(0,options.len);
         }
      }

      //Catch daylight savings year by year
      var testDt = new Date(min.toString());
      if(max.getFullYear() - testDt.getFullYear() > 1) {
         testDt.setYear(max.getFullYear()-1);
      }
      while(testDt < max) {
         if(testDt.isDayLightSavingsDay() && testDt.getMonth() < 5) {
            diff += tl["H"];
         }
         else if(testDt.isDayLightSavingsDay()) {
            diff -= tl["H"];
         }
         testDt.setDate(testDt.getDate()+1);
      }
      //Catch daylight savings for when the max date is before the spring change or after the fall change
      if(((max.getMonth() == max.getDayLightSavingsDays()[0].getMonth() && max.getDate() < max.getDayLightSavingsDays()[0].getDate()) || max.getMonth() < max.getDayLightSavingsDays()[0].getMonth()) && max.getMonth() == min.getMonth() ) {
         diff += tl["H"];
      }
      else if(((min.getMonth() == min.getDayLightSavingsDays()[1].getMonth() && min.getDate() > min.getDayLightSavingsDays()[1].getDate()) || min.getMonth() < min.getDayLightSavingsDays()[1].getMonth()) && min.getMonth() == min.getMonth() ) {
         diff -= tl["H"];
      }
      var result = [], out;
      while(diff>0) {
         if(breakdown == "*") {
           breakdown = "TCDYMWdHmSN";
         }
         else if(breakdown.indexOf("T") > -1) {
           if(out = processTime("T")) {result[result.length] = out};
         }
         else if(breakdown.indexOf("C") > -1) {
           if(out = processTime("C")) {result[result.length] = out};
         }
         else if(breakdown.indexOf("D") > -1) {
           if(out = processTime("D")) {result[result.length] = out};
         }
         else if(breakdown.indexOf("Y") > -1) {
           if(out = processTime("Y")) {result[result.length] = out};
         }
         else if(breakdown.indexOf("M") > -1) {
           if(diff >= tl["M"]) {
              var cur = (new Date(max.getTime() - diff));
              var monthCount = 0;
              var lastVal = 0;
              //Step through each year
              for(var yr = cur.getFullYear();yr<=max.getFullYear();yr++) {
                 //Step through each month
                 while(cur.getFullYear() == yr) {
                    lastVal = cur.getTime();
                    cur.setMonth(cur.getMonth()+1);
                    if( diff - (cur.getTime() - lastVal) >= 0 ) {
                       monthCount++;
                       diff -= (cur.getTime() - lastVal);
                    }
                    if(yr == max.getFullYear() && cur.getMonth() == max.getMonth()) {
                       break;
                    }
                 }
              }
              if(monthCount) {
                 result[result.length] = monthCount + " " + (monthCount == 1 && names["M"][0] || names["M"][1]);
              }
           }
           breakdown = breakdown.replace(/M/g, "");
         }
         else if(breakdown.indexOf("W") > -1) {
           if(out = processTime("W")) {result[result.length] = out};
         }
         else if(breakdown.indexOf("d") > -1) {
           if(out = processTime("d")) {result[result.length] = out};
         }
         else if(breakdown.indexOf("H") > -1) {
           if(out = processTime("H")) {result[result.length] = out};
         }
         else if(breakdown.indexOf("m") > -1) {
           if(out = processTime("m")) {result[result.length] = out};
         }
         else if(breakdown.indexOf("S") > -1) {
           if(out = processTime("S")) {result[result.length] = out};
         }
         else if(breakdown.indexOf("N") > -1) {
           if(out = processTime("N")) {result[result.length] = out};
         }
         else {
           diff = 0;
         }
      }
      options.divider = options.divider || ", ";
      if(options.divider == ", " && result.length > 1 && !options.hideAnd) {
         result[result.length-1] = "and " + result[result.length-1];
      }
      diff = result.join(options.divider);
   }
   if(diff == "") {
      diff = "Same";
   }
   if(options.lc) {
      diff = diff.toLowerCase();
   }
   return diff;
}

/********* Date.getDaysInMonth() *************
 * Returns the number of days in the selected
 * month
 *********************************************
 */
window.Date.prototype.getDaysInMonth = function() {
   return [31,28,31,30,31,30,31,31,30,31,30,31][this.getMonth()];
};

/************* Date.isLeapYear() ***************
 * Returns true if the selected year is a leap
 * year
 ***********************************************
 */
window.Date.prototype.isLeapYear = function() {
   return (new Date(this.getFullYear(),2-1,29)).getDate() == 29;
};

/******* Date.getDayLightSavingsDays() *********
 * Returns an array containing date objects for
 * the two daylight savings change days within
 * the selected year
 ***********************************************
 */
window.Date.prototype.getDayLightSavingsDays = function() {
   var result = [];
   var day1 = new Date("03/07/"+this.getFullYear());
   var day2 = new Date("03/06/"+this.getFullYear());
   while(day1.getMonth() < 3 || (day1.getMonth() == 3  && day1.getDate() < 16)) {
      if((day1.getTime() - day2.getTime())/1000/60/60 != 24) {
         result[result.length] = new Date(day2.getTime());
      }
      day1.setDate(day1.getDate()+1);
      day2.setDate(day2.getDate()+1);
   }
   var day1 = new Date("10/31/"+this.getFullYear());
   var day2 = new Date("10/30/"+this.getFullYear());
   while(day1.getMonth() < 11 || (day1.getMonth() == 10 && day1.getDate() < 9)) {
      if((day1.getTime() - day2.getTime())/1000/60/60 != 24) {
         result[result.length] = new Date(day2.getTime());
      }
      day1.setDate(day1.getDate()+1);
      day2.setDate(day2.getDate()+1);
   }
   return result;
};

/******** Date.isDayLightSavingsDay() **********
 * Returns true if the selected day is a
 * daylight savings change day
 ***********************************************
 */
window.Date.prototype.isDayLightSavingsDay = function() {
   var comp = new Date(this.getTime());
   comp.setDate(comp.getDate()+1);
   return (comp.getTime() - this.getTime())/1000/60/60 != 24;
};