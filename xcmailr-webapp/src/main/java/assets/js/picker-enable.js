/**
 * Enables and disables the datetimepicker
 * Patrick Thum, Xceptance Software Technologies GmbH, 2013
 */

function toggle(chkbox) {
	var picker = $('#datetimepicker')
			.data('datetimepicker');
	var divDateTimePicker = chkbox.parentNode;
	var visSetting = (chkbox.checked) ? "display: none" : "display: inline-block";
	$('input#datetime').attr("style", visSetting);
	$('span#pickr_span').attr("style", visSetting);
	$('i#pickr_i').attr("style", visSetting);

	if (chkbox.checked) {
		var dateval = $('input#datetime').attr("value");
		//remove the attribute from datetime input field
		$('input#datetime').attr("value", 0);
		//add it to the checkbox 
		$('input#chkUnlimited').attr("value", dateval);

	} else {
		var dateval = $('input#chkUnlimited').attr("value");
		//add the attribute to datetime input field 
		$('input#datetime').attr("value", dateval);
		//remove it from the checkbox 
		$('input#chkUnlimited').removeAttr("value");
	}
}
