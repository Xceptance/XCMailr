/**
 * Enables and disables the datetimepicker
 * Patrick Thum, Xceptance Software Technologies GmbH, 2013
 */

function toggle(chkbox) {
	var visSetting = (chkbox.checked) ? 'display: none' : 'display: inline-block';
	$('#datetime').attr('style', visSetting);
	$('#datetimepicker').attr('style', visSetting);
	$('#pickr_span').attr('style', visSetting);
	$('#pickr_i').attr('style', visSetting);

	if (chkbox.checked) {
		var dateval = $('#datetime').attr("value");
		//remove the attribute from datetime input field
		$('#datetime').attr("value", 0);
		//add it to the checkbox 
		$('#chkUnlimited').attr("value", dateval);

	} else {
		var dateval = $('#chkUnlimited').attr("value");
		//add the attribute to datetime input field 
		$('#datetime').attr("value", dateval);
		//remove it from the checkbox 
		$('#chkUnlimited').removeAttr("value");
	}
}



function toggleBatch(chkbox, page) {
	var visSetting = (chkbox.checked) ? 'display: none' : 'display: inline-block';
	$('#datetime'+page).attr('style', visSetting);
	$('#datetimepicker'+page).attr('style', visSetting);
	$('#pickr_span'+page).attr('style', visSetting);
	$('#pickr_i'+page).attr('style', visSetting);

	if (chkbox.checked) {
		var dateval = $('#datetime'+page).attr("value");
		//remove the attribute from datetime input field
		$('#datetime'+page).attr("value", 0);
		//add it to the checkbox 
		$('#chkUnlimited'+page).attr("value", dateval);

	} else {
		var dateval = $('#chkUnlimited'+page).attr("value");
		//add the attribute to datetime input field 
		$('#datetime'+page).attr("value", dateval);
		//remove it from the checkbox 
		$('#chkUnlimited'+page).removeAttr("value");
	}
}