/* Hungarian initialisation for the jQuery UI date picker plugin. */
/* Written by Gábor Babos (gabor.babos@gmail.com). */
jQuery(function ($) {
  $.datepicker.regional['hu'] = {
    closeText: 'bezárás',
    prevText: '&#x3C; előző',
    nextText: 'következő &#x3E;',
    currentText: 'ma',
    monthNames: ['Január', 'Február', 'Március', 'Április', 'Május', 'Június',
      'Július', 'Augusztus', 'Szeptember', 'Október', 'November', 'December'],
    monthNamesShort: ['Jan', 'Feb', 'Már', 'Ápr', 'Máj', 'Jún',
      'Júl', 'Aug', 'Szep', 'Okt', 'Nov', 'Dec'],
    dayNames: ['Vasárnap', 'Hétfő', 'Kedd', 'Szerda', 'Csütörtök', 'Péntek',
      'Szombat'],
    dayNamesShort: ['Vas', 'Hét', 'Ked', 'Sze', 'Csüt', 'Pén', 'Szo'],
    dayNamesMin: ['Va', 'Hé', 'Ke', 'Sze', 'Cs', 'Pé', 'Szo'],
    weekHeader: 'Hét',
    dateFormat: 'yy.mm.dd.',
    firstDay: 1,
    isRTL: false,
    showMonthAfterYear: true,
    yearSuffix: '.'
  };
  $.datepicker.setDefaults($.datepicker.regional['hu']);
});
