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
   Hebrew translation by Yaron Shahrabani <sh.yaron@gmail.com>, 2012.
*/

otp.namespace("otp.locale");

/**
  * @class
  */
otp.locale.Hebrew = {

    config :
    {
        metricsSystem : "international",
        rtl           : true,
        rightClickMsg : "יש ללחוץ על המפה במקש ימני כדי להקצות את נקודות ההתחלה והסיום של המסלול שלך.",
        attribution   : {
            title   : "אודות האתר",
            content : 'אתר זה הוקם ע"י יהודה ב. על בסיס נתוני "לו"ז ומידע גאוגראפי בתחבורה הציבורית" הזמינים מאתר <a href="http://data.gov.il/">data.gov.il</a> ועל בסיס תוכנת הקוד הפתוח <a href="http://www.opentripplanner.org/">OpenTripPlanner</a>.' +
            '<br>' +
            'תרגום התוכנה לעברית בוצע ע"י ירון שהרבני. אירוח אתר זה בחסות <a href="http://www.hamakor.org.il/">עמותת המקור</a> ובאדיבות <a href="http://www.hetz.biz/">חץ ביז</a>.' +
            '<br>',
            content2:
            'תנאי שימוש: עמותת המקור עושה כמיטב יכולתה בכדי לספק שירות זה על בסיס נתונים מדוייקים ועדכניים אולם ייתכנו טעויות. המידע ניתן כפי שהוא ללא אחריות. ראו גם <a href="http://data.gov.il/dataset/383">תנאי שימוש של משרד התחבורה</a>.' 
        }
    },

    contextMenu : 
    {
        fromHere         : "להתחיל במסלול מכאן",
        toHere           : "לסיים את המסלול כאן",
        intermediateHere : "הוספת נקודת ביניים",

        centerHere       : "מרכוז המפה לכאן",
        zoomInHere       : "התקרבות",
        zoomOutHere      : "התרחקות",
        previous         : "המיקום האחרון במפה",
        next             : "המיקום הבא במפה"
    },

    // TODO Localize Me
    bikeTriangle : 
    {
        safeName : "ידידותי לאופניים",
        safeSym  : "א",

        hillName : "ישיר",
        hillSym  : "י",

        timeName : "מהיר",
        timeSym  : "מ"
    },

    service : 
    {
        weekdays:  "ימי השבוע",
        saturday:  "שבת",
        sunday:    "ראשון",
        schedule:  "תזמון"
    },

    indicators : 
    {
        ok         : "אישור",
        date       : "תאריך",
        loading    : "בטעינה",
        searching  : "מתבצע חיפוש...",
        qEmptyText : "כתובת, צומת, סימן דרך או מזהה עצירה..."
    },

    buttons: 
    {
        reverse       : "היפוך",
        reverseTip    : "<b>היפוך הכיוונים</b><br/>תכנון מסלול חזרה על ידי היפוך מיקומי ההתחלה והסיום של מסלול זה והתאמה לפי פרק הזמן הבא.",
        reverseMiniTip: "היפוך הכיוונים",

        edit          : "עריכה",
        editTip       : "<b>עריכת המסלול</b><br/>חזרה לקלט תכנון המסלול הראשי עם פרטי מסלול זה.",

        clear         : "מחיקה",
        clearTip      : "<b>מחיקה</b><br/>מחיקת המפה וכל הכלים הפעילים.",

        fullScreen    : "מסך מלא",
        fullScreenTip : "<b>מסך מלא</b><br/>הצגה -או- הסתרה של סרגלי הכלים",

        print         : "הדפסה",
        printTip      : "<b>הדפסה</b><br/>הדפסת גרסה ידידותית של תכנית המסלול (ללא המפה).",

        link          : "קישור",
        linkTip      : "<b>קישור</b><br/>הצגת כתובת הקישור עבור תכנית מסלול זאת.",

        feedback      : "משוב",
        feedbackTip   : "<b>משוב</b><br/>שליחת הגיגיך ואת ההתנסות שלך עם המפה",

        submit       : "שליחה",
        clearButton  : "מחיקה",
        ok           : "אישור",
        cancel       : "ביטול",
        yes          : "כן",
        no           : "לא",
        showDetails  : "&darr; הצגת הפרטים &darr;",
        hideDetails  : "&uarr; הסתרת הפרטים &uarr;"
    },

    // note: keep these lower case (and uppercase via template / code if needed)
    directions : 
    {
        southeast:      "דרום־מזרחה",
        southwest:      "דרום־מערבה",
        northeast:      "צפון־מזרחה",
        northwest:      "צפון־מערבה",
        north:          "צפונה",
        west:           "מערבה",
        south:          "דרומה",
        east:           "מזרחה",
        bound:          "גבול",
        left:           "שמאלה",
        right:          "ימינה",
        slightly_left:  "סטייה שמאלה",
        slightly_right: "סטייה ימינה",
        hard_left:      "פנייה חדה שמאלה",
        hard_right:     "פנייה חדה ימינה",
        'continue':     "להמשיך ישר",
        to_continue:    "להמשיך ישר על",
        becomes:        "הופך ל",
        at:             "ב",
        on:             "ב",
        to:             "אל",
        via:            "דרך",
        circle_counterclockwise: "להסתובב בכיכר נגד כיוון השעון",
        circle_clockwise:        "להסתובב בכיכר עם כיוון השעון",
        // rather than just being a direction, this should be
        // full-fledged to take just the exit name at the end
        elevator: "לעלות במעלית לקומה"
    },

    // see otp.planner.Templates for use
    instructions :
    {
        walk         : "הליכה",
        walk_toward  : "ללכת",
        walk_verb    : "הליכה",
        bike         : "אופניים",
        bike_toward  : "לרכב",
        bike_verb    : "רכיבה",
        drive        : "ליסוע",
        drive_toward : "ליסוע",
        drive_verb   : "נסיעה",
        move         : "להמשיך",
        move_toward  : "להמשיך",

        transfer     : "העברה",
        transfers    : "העברות",

        continue_as  : "מתמזג לדרך",
        stay_aboard  : "להישאר על הסיפון",

        depart       : "יציאה",
        arrive       : "הגעה",

        start_at     : "התחלה ב",
        end_at       : "סיום ב"
    },

    // see otp.planner.Templates for use
    labels : 
    {
        agency_msg   : "השירות מופעל על ידי",
        agency_msg_tt: "פתיחת אתר הסוכנות בחלון נפרד...",
        about        : "בערך",
        stop_id      : "מזהה תחנה",
        trip_details : "פרטי המסלול",
        fare         : "תשלום",
        fare_symbol  : "₪",
        travel       : "מסלול",
        valid        : "נכון ל",
        trip_length  : "זמן",
        with_a_walk  : "עם הליכה של",
        alert_for_rt : "התראה על מסלול"
    },

    // see otp.planner.Templates for use -- one output are the itinerary leg headers
    modes :
    {
        WALK:           "הליכה",
        BICYCLE:        "אופניים",
        CAR:            "רכב",
        TRAM:           "רכבת קלה",
        SUBWAY:         "רכבת תחתית",
        RAIL:           "רכבת",
        BUS:            "אוטובוס",
        FERRY:          "מעבורת",
        CABLE_CAR:      "רכבל",
        GONDOLA:        "גונדולה",
        FUNICULAR:      "פוניקולר"
    },

    ordinal_exit:
    {
        1:  "ביציאה הראשונה",
        2:  "ביציאה השנייה",
        3:  "ביציאה השלישית",
        4:  "ביציאה הרביעית",
        5:  "ביציאה החמישית",
        6:  "ביציאה השישית",
        7:  "ביציאה השביעית",
        8:  "ביציאה השמינית",
        9:  "ביציאה התשיעית",
        10: "ביציאה העשירית"
    },

    time:
    {
        minute         : "דקה",
        minutes        : "דקות",
        minute_abbrev  : "דקה",
        minutes_abbrev : "דקות",
        second_abbrev  : "שנ׳",
        seconds_abbrev : "שנ׳",
        format         : "ה־j בF Y @ G:i",
        date_format    : "j/n/Y",
        time_format    : "G:i",
        months         : ['ינו', 'פבר', 'מרץ', 'אפר', 'מאי', 'יונ', 'יול', 'אוג', 'ספט', 'אוק', 'נוב', 'דצמ']
    },

    systemmap :
    {
        labels :
        {
            panelTitle : "מפת המערכת"
        }
    },

    tripPlanner :
    {
        // see otp/planner/*.js for where these values are used
        labels : 
        {
            panelTitle    : "מתכנן המסלולים",
            tabTitle      : "תכנון מסלול",
            inputTitle    : "פרטי המסלול",
            optTitle      : "העדפות המסלול (רשות)",
            submitMsg     : "המסלול שלך מתוכנן...",
            optionalTitle : "",
            date          : "תאריך",
            time          : "שעה",
            when          : "מתי",
            from          : "מהמיקום",
            fromHere      : "מכאן",
            to            : "למיקום",
            toHere        : "לכאן",
            intermediate  : "נקודות ביניים",
            minimize      : "סוג המסלול",
            maxWalkDistance: "הליכה מרבית",
            maxBikeDistance: "רכיבה מרבית",
            arriveDepart  : "הגעה עד/יציאה ב",
            mode          : "אמצעי תחבורה",
            wheelchair    : "מסלול נגיש לכיסא גלגלים", 
            go            : "לדרך",
            planTrip      : "תכנון המסלול שלך",
            newTrip       : "מסלול חדש"
        },

        // see otp/config.js for where these values are used
        link : 
        {
            text           : "קישור למסלול זה (OTP)",
            trip_separator : "מסלול זה במתכנני תחבורה ציבורית שונים",
            bike_separator : "במתכנני מסלולי אופניים שונים",
            walk_separator : "במתכנני מסלולי הליכה שונים",
            google_transit : "תחבורה ציבורית של Google",
            google_bikes   : "הנחיות רכיבה באופניים של Google",
            google_walk    : "הנחיות הליכה של Google",
            google_domain  : "http://www.google.com"
        },

        // see otp.planner.Forms for use
        geocoder:
        {
            working      : "הכתובת בחיפוש ....",
            error        : "לא התקבלו תוצאות",
            msg_title    : "סקירת תכנית המסלול",
            msg_content  : "נא לתקן את השגיאות לפני תכנון המסלול שלך",
            select_result_title : "נא לבחור בתוצאה",
            address_header : "כתובת"
        },

        error:
        {
            title        : 'שגיאה במתכנן המסלולים',
            deadMsg      : "תכנון המסלול במפות אינו מגיב כעת. נא להמתין מספר דקות ולנסות שוב, או לנסות את מתכנן המסלולים הטקסטואלי (קישור להלן).",
            geoFromMsg   : "נא לבחור את מיקום ה'מוצא' של המסלול שלך: ",
            geoToMsg     : "נא לבחור את מיקום ה'יעד' של המסלול שלך: "
        },
        
        // default messages from server if a message was not returned ... 'Place' error messages also used when trying to submit without From & To coords.
        msgcodes:
        {
            200: "המסלול תקין OK",
            500: "שגיאת שרת",
            400: "המסלול חורג מהגבולות",
            404: "הנתיב לא נמצא",
            406: "אין זמני תחבורה ציבורית",
            408: "זמן הבקשה פג",
            413: "פרמטר שגוי",
            440: "מיקום ה'מוצא' לא נמצא ... נא להזין אותו שוב.",
            450: "מיקום ה'יעד' לא נמצא ... נא להזין אותו מחדש.",
            460: "מיקומי ה'מוצא וה'יעד' לא נמצאו ... נא להזין אותם מחדש.",
            470: "מיקומי ה'מוצא' וה'יעד' אינם נגישים לכיסאות גלגלים",
            409: "קרוב מדי",
            340: "קוד הגאו של ה'מוצא' כללי מדי",
            350: "קוד הגאו של ה'יעד' כללי מדי",
            360: "הקודים הגאוגרפיים של ה'מוצא' וה'יעד' כלליים מדי"
        },

        options: 
        [
          ['TRANSFERS', 'מספר מזערי של העברות'],
          ['QUICK',     'מסלול מהיר'],
          ['SAFE',      'מסלול ידידותי לאופניים'],
          ['TRIANGLE',  'מסלול בהתאמה אישית...']
        ],
    
        arriveDepart: 
        [
          ['false', 'יציאה'], 
          ['true',  'הגעה']
        ],
    
        maxWalkDistance : [ [ '200', '200 מ׳' ], [ '500', '500 מ׳' ],
                [ '1000', 'ק״מ אחד' ], [ '1500', '1.5 ק״מ' ], [ '5000', '5 ק״מ' ],
                [ '10000', '10 ק״מ' ] ],

        mode : 
        [
            ['TRANSIT,WALK', 'תחבורה ציבורית'],
            ['BUSISH,WALK', 'אוטובוס בלבד'],
            ['TRAINISH,WALK', 'רכבת בלבד'],
            ['WALK', 'הליכה בלבד'],
            ['BICYCLE', 'אופניים'],
            ['TRANSIT,BICYCLE', 'תחבורה ציבורית ואופניים']
        ],

        // TODO: remove this hack, and provide code that allows the mode array to be configured with different transit modes.
        //       (note that we've been broken for awhile here, since many agencies don't have a 'Train' mode either...this needs attention)
        // IDEA: maybe we start with a big array (like below), and the pull out modes from this array when turning off various modes...
        with_bikeshare_mode : 
        [
            ['TRANSIT,WALK', 'תחבורה ציבורית'],
            ['BUSISH,WALK', 'אוטובוס בלבד'],
            ['TRAINISH,WALK', 'רכבת בלבד'],
            ['WALK', 'הליכה בלבד'],
            ['BICYCLE', 'אופניים'],
            ['WALK,BICYCLE', 'אופניים שכורים'],
            ['TRANSIT,BICYCLE', 'תחבורה ציבורית ואופניים'],
            ['TRANSIT,WALK,BICYCLE', 'תחבורה ציבורית ואופניים שכורים']
        ],

        wheelchair :
        [
            ['false', 'לא הכרחי'],
            ['true', 'הכרחי']
        ]
    },

    CLASS_NAME : "otp.locale.Hebrew"
};

