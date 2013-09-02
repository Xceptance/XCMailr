angular.module('BoxHandler', ['ui.bootstrap']);
function BoxListCtrl($scope, $dialog, $http) 
{

$scope.selected = {};
$scope.allBoxes = {};
$scope.alerts = [];

	//--------------- get the available domains ---------------//
	$scope.loadDomains = function()
	{
		$http.get('/mail/domainlist').success
		(
			function(data)
			{
				$scope.domains = data;
			}
		);
	};
	
//execute the domain-load
$scope.loadDomains();

	//--------------- load all boxes ---------------//
	$scope.updateModel = function()
	{ 
		$http.get('/mail/angget').success
		(
			function(data)
			{
				$scope.allBoxes = data;
			    $scope.noOfPages = $scope.setNumPages();
			}
	
		);
	};
	
	//--------------- delete a box ---------------//
	$scope.deleteBox = function(boxId, elementIdx)
	{
		$http.post($scope.contextPath + '/mail/delete2/' + boxId, null).success
		(
			function()
			{
				//$scope.filteredBoxes.splice(elementIdx, 1);
				$scope.allBoxes.splice(elementIdx+(($scope.currentPage-1)*$scope.maxSize),1);
			}
		);
	};
	
	//--------------- activate or deactivate a box ---------------//
	$scope.expireBox = function(boxId, elementIdx)
	{
		$http.post($scope.contextPath + '/mail/expire2/' + boxId, null).success
		(
			function(returnedData)
			{
				if(returnedData.success)
				{
					var curBox = $scope.filteredBoxes[elementIdx];
					curBox.expired = !curBox.expired;
					//$scope.filteredBoxes[elementIdx] = curBox;
					//update the allBoxes-model, this should also refresh the filteredboxes
					$scope.allBoxes[elementIdx+(($scope.currentPage-1)*$scope.maxSize)] = curBox;
				}
				else
				{
					$scope.openDialog(elementIdx);
				}
				
			}
		);
	};
	
	//--------------- reset the forward and suppression-count ---------------//
	$scope.resetBox = function(boxId, elementIdx)
	{
		$http.post($scope.contextPath + '/mail/reset2/' + boxId, null).success
		(
				function()
				{
					$scope.allBoxes[elementIdx+(($scope.currentPage-1)*$scope.maxSize)].suppressions = 0;
					$scope.allBoxes[elementIdx+(($scope.currentPage-1)*$scope.maxSize)].forwards = 0;
				}
		);
	};
	
	//--------------- edit a box ---------------//
	$scope.editBox = function(boxId, data, elementIdx)
	{
		$http.post($scope.contextPath + '/mail/edit2/' + boxId, data).success
		(
			function(returnedData)
			{
				
				if(returnedData.success)
				{
					$scope.alerts.push({'type': 'success', 'msg': returnedData.statusmsg });
					$scope.allBoxes[elementIdx+(($scope.currentPage-1)*$scope.maxSize)] = returnedData.currentBox;
				}
				else
				{
					$scope.alerts.push({'type': 'error', 'msg': returnedData.statusmsg });
				}
			}
		);
	};
	
	//--------------- toggle the inlined menu on/off ---------------//
	$scope.toggleMenu = function(boxId)
	{
		return !$scope.toggleMenu;
	};
	
	//--------------- show the selected boxes ---------------//
    $scope.showSelected = function() 
    {
    	$scope.filteredBoxes = $.grep($scope.allBoxes, function( box ) 
    			{
    				return $scope.selected[ box.id ];
    			}
    	);
    };  
    
	//--------------- show all boxes on one page ---------------//
    $scope.showAll = function()
    {
    	$scope.filteredBoxes = $scope.allBoxes;
    };

    /*
	 * handle the pagination
	 */
    $scope.noOfPages = 1;
    $scope.currentPage = 1;
    $scope.maxSize = 15;
    
	//--------------- set the page ---------------//
    $scope.setPage = function (pageNo) 
    {
        $scope.currentPage = pageNo;
    };
    
	//--------------- set the page-size ---------------//
	$scope.setMaxSize = function (size) 
	{
	    $scope.maxSize = size;
	    $scope.noOfPages = $scope.setNumPages();
	    $scope.currentPage = 1;
	};
	
	//--------------- set the number of pages ---------------//
	  $scope.setNumPages = function()
	  {
		  if($scope.maxSize != 0)
		  {
			  return Math.ceil($scope.allBoxes.length / $scope.maxSize);
		  }
		  else
		  {
			  return 1;  
		  }
	  };  
	  
 	/*
	 * handle the addboxdialog (TODO)
	 */
	$scope.opts = {
		 backdrop: true,
		 keyboard: true,
		 backdropClick: true,
		 templateUrl: '/mail/editBoxDialog.html',
		 controller: 'TestDialogController'
	};
	
	//--------------- Opens the EditBoxDialog ---------------//
	$scope.openDialog = function(elementIdx)
	{
		$scope.currentBox = $scope.filteredBoxes[elementIdx];
  		$scope.opts.resolve = 
  		{
  			currentBox : 
  				function() 
  				{
		  			return angular.copy($scope.currentBox);
		  		}, 
		  		domains : 
		  			function()
		  			{
		  				return angular.copy($scope.domains);
		  			}
		 };
		 var d = $dialog.dialog($scope.opts);
		 		 
		 d.open().then
		 (

			function(result)
			{
				if(result)
				{ //boxId, data, elementIdx
					$scope.editBox(result.id, result, elementIdx);
				}
			}
		 );
	};
	
	//--------------- Closes an Alertmessage ---------------//
	$scope.closeAlert = function(elementIdx)
	{
		$scope.alerts.splice(elementIdx, 1);
	}

  $scope.updateModel();
  $scope.noOfPages = $scope.setNumPages();		
  
	//--------------- Page-Change Listener ---------------//
	$scope.paginationListener = $scope.$watch('currentPage + maxSize', //+ allBoxes.length + filteredBoxes.length
		function() 
		{
			$scope.updateView();
	  	}, true
	);
	
	//--------------- Update Listener ---------------//
	$scope.updateListener = $scope.$watch('allBoxes', 
		function() 
		{
			$scope.updateView();
	  	}, true
	);
	
	//--------------- Update the View ---------------//	
	$scope.updateView = function() 
	{
		if($scope.maxSize > 0 && $scope.allBoxes.length > 0)
		{
	  	    var begin = (($scope.currentPage - 1) * $scope.maxSize);
	  	    var end = begin + $scope.maxSize;
	  	    $scope.filteredBoxes = $scope.allBoxes.slice(begin, end);
		}
		else
		{
			$scope.filteredBoxes = $scope.allBoxes;
		}
  	}
	
	//--------------- BoxCount Listener ---------------//	
	$scope.boxCountListener = $scope.$watch('allBoxes.length', 
		function()
		{
			$scope.noOfPages = $scope.setNumPages();		
		}
	);
}

 // the dialog is injected in the specified controller
function TestDialogController($scope, dialog, currentBox, domains)
{
 	/*
	 * handle the addboxdialog (TODO)
	 */
	$scope.domains = domains;
	$scope.currentBox = currentBox; 
	$scope.close = function(data)
	{
		var chkBoxIdString ='#chkUnlimited' + data.id;
		if($(chkBoxIdString).is(':checked'))
		{
			data.datetime = 0;
		}
		else
		{
			var idString = '#datetime'+data.id;
			var newTime = $(idString).val();
			data.datetime = newTime;
		}
		dialog.close(data);
	};
	$scope.dismiss = function()
	{ // ignore changes, just close the dialog
		dialog.close();
	};
}
