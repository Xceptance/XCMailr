angular.module('BoxHandler', ['ui.bootstrap']);
function BoxListCtrl($scope, $dialog, $http) 
{
$scope.init = function(cP)
	{ // something like a constructor
		$scope.contextPath = cP;
		//execute the domain-load
		$scope.loadDomains();
	    /*
		 * handle the pagination
		 */
	    $scope.noOfPages = 1;
	    $scope.currentPage = 1;
	    $scope.maxSize = 15;
	    $scope.selected = {};
	    $scope.allBoxes = {};
	    $scope.alerts = [];
	    $scope.updateModel();
	    $scope.noOfPages = $scope.setNumPages();	
	    $scope.allBoxesAreNowSelected = false;
	}
	

	//--------------- get the available domains ---------------//
	$scope.loadDomains = function()
	{
		$http.get($scope.contextPath + '/mail/domainlist').success
		(
			function(data)
			{
				$scope.domains = data;
			}
		);
	};

	//--------------- load all boxes ---------------//
	$scope.updateModel = function()
	{ 
		$http.get($scope.contextPath + '/mail/angget').success
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
			function(returnedData)
			{
				if(returnedData.success)
				{
					$scope.allBoxes.splice(elementIdx+(($scope.currentPage-1)*$scope.maxSize),1);
				}
				else
				{
					$scope.alerts.push({'type': 'success', 'msg': 'errrrror!' });
				}
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
				function(returnedData)
				{
					if(returnedData.success)
					{
					$scope.allBoxes[elementIdx+(($scope.currentPage-1)*$scope.maxSize)].suppressions = 0;
					$scope.allBoxes[elementIdx+(($scope.currentPage-1)*$scope.maxSize)].forwards = 0;
					}
					else
					{
						$scope.alerts.push({'type': 'error', 'msg': returnedData.statusmsg });
					}
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
	
	//--------------- add a box ---------------//
	$scope.addBox = function(boxId, data)
	{
		$http.post($scope.contextPath + '/mail/addJson', data).success
		(
			function(returnedData)
			{
				if(returnedData.success)
				{
					$scope.alerts.push({'type': 'success', 'msg': returnedData.statusmsg });
					$scope.allBoxes.push(returnedData.currentBox);
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
	$scope.openEditBoxDialog = function(elementIdx)
	{
		$scope.currentBox = $scope.filteredBoxes[elementIdx];
  		$scope.opts.resolve = 
  		{
  			currentBox : function() 
  				{
		  			return angular.copy($scope.currentBox);
		  		}, 
	  		domains : function()
	  			{
	  				return angular.copy($scope.domains);
	  			},
	  		contextPath : function()
	  			{
	  				return angular.copy($scope.contextPath);
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
	
	//--------------- Loads the AddBoxData ---------------//
	$scope.getAddBoxData = function()
	{
		var boxData
		$http.get($scope.contextPath + '/mail/addBoxData').success
		(
			function(data)
			{
				boxData = data.currentBox;
			}
		);
		return boxData;
	};
	
	//--------------- Opens the AddBoxDialog ---------------//
	$scope.openAddBoxDialog = function()
	{
  		$scope.opts.resolve = 
  		{ 
  			currentBox : function()
  				{
  					return $scope.getAddBoxData();
  				},
	  		domains : function()
	  			{
	  				return angular.copy($scope.domains);
	  			},
	  		contextPath : function()
	  			{
	  				return angular.copy($scope.contextPath);
	  			}
		 };
		 var d = $dialog.dialog($scope.opts);
		 		 
		 d.open().then
		 (
			function(result)
			{
				if(result)
				{ // boxId, data, elementIdx
					$scope.addBox(result.id, result);
				}
			}
		 );
	};
	
	//--------------- Closes an Alertmessage ---------------//
	$scope.closeAlert = function(elementIdx)
	{
		$scope.alerts.splice(elementIdx, 1);
	}


  
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
  	};
	
	//--------------- BoxCount Listener ---------------//	
	$scope.boxCountListener = $scope.$watch('allBoxes.length', 
		function()
		{
			$scope.noOfPages = $scope.setNumPages();		
		}
	);
	//--------------- Select All available Items  (by checkbox) ---------------//	
	$scope.selectAllItems = function()
	{
		$scope.allBoxesAreNowSelected = !$scope.allBoxesAreNowSelected;
		if($scope.allBoxesAreNowSelected){
			angular.forEach($scope.allBoxes,
					function(mBox, key)
					{
						$scope.selected[mBox.id] = $scope.allBoxesAreNowSelected;
					}
			);
		}
		else
		{
			$scope.selected = {};
		}
		$('.bulkChk').prop("checked", $scope.allBoxesAreNowSelected);
	};
	//--------------- Deletes the selected Boxes ---------------//		
	$scope.bulkDeleteBox = function()
	{
		$http.post($scope.contextPath + '/mail/bulkDelete', $scope.selected).success
		(
			function(returnedData)
			{
				if(returnedData.success)
				{
					$scope.updateModel();
					//$scope.alerts.push({'type': 'success', 'msg': returnedData.statusmsg });
					//$scope.allBoxes[elementIdx+(($scope.currentPage-1)*$scope.maxSize)] = returnedData.currentBox;
				}
			}
		);
	};
	//--------------- Resets the selected Boxes ---------------//	
	$scope.bulkResetBox = function()
	{
		$http.post($scope.contextPath + '/mail/bulkReset', $scope.selected).success
		(
			function(returnedData)
			{
				if(returnedData.success)
				{
					$scope.updateModel();
				}
			}
		);
	};
	
	//--------------- Disables the selected Boxes ---------------//	
	$scope.bulkDisableBox = function()
	{
		$http.post($scope.contextPath + '/mail/bulkDisable', $scope.selected).success
		(
			function(returnedData)
			{
				if(returnedData.success)
				{
					$scope.updateModel();
				}
			}
		);
	};
	
	
}



 // the dialog is injected in the specified controller
function TestDialogController($scope, $http, dialog, currentBox, domains, contextPath)
{
 	/*
	 * handle the addboxdialog (TODO)
	 */
	$scope.domains = domains;
	$scope.contextPath = contextPath;
	$scope.currentBox = currentBox;
	$scope.setValues = function()
	{
		if($scope.currentBox === undefined)
		{
			$http.get($scope.contextPath + '/mail/addBoxData').success
			(
				function(data)
				{
					$scope.currentBox = data.currentBox;
				}
			);
		}
	};
$scope.setValues();
	
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
