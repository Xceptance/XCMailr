function replaceUserName() {
	//get the username 
	var userName = $('#headerUsername').text();
	if ((userName.length > 28)) {
		//replace everything over 25 chars with dots if the userName is longer than 28 characters
		var newName = userName.substr(0, 25) + "...";
		
		//add the tooltip
		$('#headerUsername').text(newName);
		$('#headerUsername').tooltip({
			toggle : 'tooltip',
			placement : 'bottom',
			trigger : 'hover focus',
			title : userName + ' '
		});
		$('#headerUsername').tooltip();
	}
}


function replaceMailBoxName() {
	//get the mailbox 
	var mailAddressCells = $('.mbAddress');
	$(mailAddressCells).each(
		function(){
			var boxName = $(this).text();
			if ((boxName.length > 28)) {
				//replace everything over 25 chars with dots if the userName is longer than 28 characters
				var newName = boxName.substr(0, 25) + "...";
				
				//add the tooltip
				$(this).text(newName);
				var parent = $(this).parent();
				$(this).tooltip({
					toggle : 'tooltip',
					placement : 'bottom',
					trigger : 'hover focus',
					title : boxName + ' ',
					container: parent
				});
				$(this).tooltip();
			}
		}
	);
}