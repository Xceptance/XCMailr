/**
 * Enables and disables the datetimepicker
 * Patrick Thum, Xceptance Software Technologies GmbH, 2013
 */
  function enable_cb() {
  	if (this.checked) {
  		var dateval = $("input#datetime").attr("value");
  		var input = document.createElement("input");
  		input.setAttribute("type", "hidden");
  		input.setAttribute("id", "storDate");
  		input.setAttribute("value", dateval);
  		document.getElementById("datetimepicker").appendChild(input);

  		$("input#datetime").attr("type", "hidden");
  		$("span#pickr_span").removeAttr("class");
  		$("i#pickr_i").removeAttr("data-date-icon");
  		$("i#pickr_i").removeAttr("data-time-icon");
  		$("i#pickr_i").removeAttr("class");
  		$("input#datetime").attr("value", "0");
  	} else {
  		var dateval = $("input#storDate").attr("value");
  		$("input#datetime").removeAttr("disabled");
  		$("input#datetime").attr("type", "text");
  		$("input#datetime").attr("value", dateval);
  		$("span#pickr_span").attr("class", "add-on");
  		$("i#pickr_i").attr("data-date-icon","icon-calendar");
  		$("i#pickr_i").attr("data-time-icon","icon-time");
  		$("i#pickr_i").attr("class","icon-calendar");
  		$('input#storDate').remove();
  	}
  }
