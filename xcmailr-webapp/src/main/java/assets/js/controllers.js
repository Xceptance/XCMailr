angular.module('BoxHandler', [
	'ui.bootstrap'
]);

function BoxListCtrl($scope, $dialog, $http, $window)
{

	$scope.init = function(cP)
	{ // something like a constructor
		$scope.contextPath = cP;
		// execute the domain-load
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
		// AddEditDialog-Options
		$scope.opts =
		{
			backdrop : true,
			keyboard : true,
			backdropClick : true,
			templateUrl : '/mail/editBoxDialog.html',
			controller : 'AddEditDialogController'
		};
		$scope.optsDeleteDialog =
		{
			backdrop : true,
			keyboard : true,
			backdropClick : true,
			templateUrl : '/mail/deleteBoxDialog.html',
			controller : 'DeleteDialogsController'
		};
		$scope.optsNewDateDialog =
		{
			backdrop : true,
			keyboard : true,
			backdropClick : true,
			templateUrl : '/mail/newDateDialog.html',
			controller : 'NewDateController'
		};
	};

	// --------------- get the available domains --------------- //
	$scope.loadDomains = function()
	{
		$http.get($scope.contextPath + '/mail/domainlist').success(function(data)
		{
			$scope.checkForLogin(data);
			$scope.domains = data;
		});
	};

	// --------------- load all boxes --------------- //
	$scope.updateModel = function()
	{
		$http.get($scope.contextPath + '/mail/angget').success(function(data)
		{
			$scope.checkForLogin(data);
			$scope.allBoxes = data;
			$scope.noOfPages = $scope.setNumPages();
		});
	};

	// --------------- delete a box --------------- //
	$scope.deleteBox = function(boxId, elementIdx)
	{
		$http.post($scope.contextPath + '/mail/delete2/' + boxId, null).success(function(returnedData)
		{
			$scope.checkForLogin(data);
			if (returnedData.success)
			{
				$scope.allBoxes.splice(elementIdx + (($scope.currentPage - 1) * $scope.maxSize), 1);
			}
			else
			{
				$scope.pushAlert(returnedData.success, 'errrror!');
			}
		});
	};

	// --------------- activate or deactivate a box --------------- //
	$scope.expireBox = function(boxId, elementIdx)
	{
		$http.post($scope.contextPath + '/mail/expire2/' + boxId, null).success(function(returnedData)
		{
			if (returnedData.success)
			{
				$scope.checkForLogin(data);
				var curBox = $scope.filteredBoxes[elementIdx];
				curBox.expired = !curBox.expired;
				// update the allBoxes-model, this should also refresh the filteredboxes
				$scope.allBoxes[elementIdx + (($scope.currentPage - 1) * $scope.maxSize)] = curBox;
			}
			else
			{
				$scope.openEditBoxDialog(elementIdx);
			}
		});
	};

	// --------------- reset the forward and suppression-count --------------- //
	$scope.resetBox = function(boxId, elementIdx)
	{
		$http.post($scope.contextPath + '/mail/reset2/' + boxId, null).success(function(returnedData)
		{
			$scope.checkForLogin(data);
			if (returnedData.success)
			{
				$scope.allBoxes[elementIdx + (($scope.currentPage - 1) * $scope.maxSize)].suppressions = 0;
				$scope.allBoxes[elementIdx + (($scope.currentPage - 1) * $scope.maxSize)].forwards = 0;
			}
			else
			{
				$scope.pushAlert(returnedData.success, returnedData.statusmsg);
			}
		});
	};

	// --------------- edit a box --------------- //
	$scope.editBox = function(boxId, data, elementIdx)
	{
		$http.post($scope.contextPath + '/mail/edit2/' + boxId, data).success(function(returnedData)
		{
			$scope.checkForLogin(data);
			if (returnedData.success)
			{
				$scope.allBoxes[elementIdx + (($scope.currentPage - 1) * $scope.maxSize)] = returnedData.currentBox;
			}

			$scope.pushAlert(returnedData.success, returnedData.statusmsg);
		});
	};

	// --------------- add a box --------------- //
	$scope.addBox = function(boxId, data)
	{
		$http.post($scope.contextPath + '/mail/addJson', data).success(function(returnedData)
		{
			$scope.checkForLogin(data);
			if (returnedData.success)
			{
				$scope.allBoxes.push(returnedData.currentBox);
			}
			$scope.pushAlert(returnedData.success, returnedData.statusmsg);
		});
	};

	// --------------- show the selected boxes --------------- //
	$scope.showSelected = function()
	{
		$scope.filteredBoxes = $.grep($scope.allBoxes, function(box)
		{
			return $scope.selected[box.id];
		});
	};

	// --------------- show all boxes on one page --------------- //
	$scope.showAll = function()
	{
		$scope.setMaxSize($scope.allBoxes.length);
		// $scope.filteredBoxes = $scope.allBoxes;
	};

	// --------------- set the page --------------- //
	$scope.setPage = function(pageNo)
	{
		$scope.currentPage = pageNo;
	};

	// --------------- set the page-size --------------- //
	$scope.setMaxSize = function(size)
	{
		$scope.maxSize = size;
		$scope.noOfPages = $scope.setNumPages();
		$scope.currentPage = 1;
	};

	// --------------- set the number of pages --------------- //
	$scope.setNumPages = function()
	{
		return ($scope.maxSize != 0) ? Math.ceil($scope.allBoxes.length / $scope.maxSize) : 1;
	};

	// --------------- Opens the EditBoxDialog --------------- //
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

		d.open().then(function(result)
		{
			if (result)
			{
				$scope.editBox(result.id, result, elementIdx);
			}
		});
	};

	// --------------- Opens the AddBoxDialog --------------- //
	$scope.openAddBoxDialog = function()
	{
		$scope.opts.resolve =
		{
			currentBox : function()
			{
				// return $scope.getAddBoxData();
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

		d.open().then(function(result)
		{
			if (result)
			{
				$scope.addBox(result.id, result);
			}
		});
	};

	// --------------- Opens the deleteBoxDialog --------------- //
	$scope.openDeleteBoxDialog = function(elementIdx)
	{
		$scope.optsDeleteDialog.resolve =
		{
			currentBox : function()
			{
				return angular.copy($scope.filteredBoxes[elementIdx]);
			},
			contextPath : function()
			{
				return angular.copy($scope.contextPath);
			},
			isBulkAction : function()
			{
				return false;
			}

		};
		var d = $dialog.dialog($scope.optsDeleteDialog);

		d.open().then(function(result)
		{
			if (result)
			{
				$scope.deleteBox(result.id, elementIdx);
			}
		});
	};

	// --------------- Opens the bulkDeleteBoxDialog --------------- //
	$scope.openbulkDeleteBoxDialog = function()
	{
		$scope.optsDeleteDialog.resolve =
		{
			currentBox : function()
			{
				var boxes = $.grep($scope.allBoxes, function(box)
				{
					return $scope.selected[box.id];
				});
				return angular.copy(boxes);
			},
			contextPath : function()
			{
				return angular.copy($scope.contextPath);
			},
			isBulkAction : function()
			{
				return true;
			}
		};
		var d = $dialog.dialog($scope.optsDeleteDialog);

		d.open().then(function(result)
		{
			if (result)
			{
				$scope.bulkDeleteBox();
			}
		});
	};

	// --------------- Opens the NewDateDialog --------------- //
	$scope.openNewDateDialog = function()
	{
		$scope.optsNewDateDialog.resolve =
		{
			currentBoxes : function()
			{
				var boxes = $.grep($scope.allBoxes, function(box)
				{
					return $scope.selected[box.id];
				});
				return angular.copy(boxes);
			},
			contextPath : function()
			{
				return angular.copy($scope.contextPath);
			}
		};
		var d = $dialog.dialog($scope.optsNewDateDialog);

		d.open().then(function(newDateTime)
		{
			if (!(newDateTime === undefined))
			{
				$scope.bulkSetNewDate(newDateTime);
			}
		});
	};

	// --------------- Closes an Alert-Message --------------- //
	$scope.closeAlert = function(elementIdx)
	{
		$scope.alerts.splice(elementIdx, 1);
	};

	// --------------- Page-Change Listener --------------- //
	$scope.paginationListener = $scope.$watch('currentPage + maxSize', function()
	{
		$scope.updateView();
	}, true);

	// --------------- All-Boxes Update Listener --------------- //
	$scope.updateListener = $scope.$watch('allBoxes', function()
	{
		$scope.updateView();
	}, true);

	// --------------- Update the View --------------- //
	$scope.updateView = function()
	{
		if ($scope.maxSize > 0 && $scope.allBoxes.length > 0)
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

	// --------------- BoxCount Listener --------------- //
	$scope.boxCountListener = $scope.$watch('allBoxes.length', function()
	{
		$scope.noOfPages = $scope.setNumPages();
	});

	// --------------- Select All available Items (by checkbox) --------------- //
	$scope.selectAllItems = function()
	{

		$scope.allBoxesAreNowSelected = !$scope.allBoxesAreNowSelected;
		if ($scope.allBoxesAreNowSelected)
		{
			angular.forEach($scope.allBoxes, function(mBox, key)
			{
				$scope.selected[mBox.id] = $scope.allBoxesAreNowSelected;
			});
		}
		else
		{
			$scope.selected = {};
		}
		$('.bulkChk').prop("checked", $scope.allBoxesAreNowSelected);
	};

	// --------------- Deletes the selected Boxes --------------- //
	$scope.bulkDeleteBox = function()
	{
		$http.post($scope.contextPath + '/mail/bulkDelete', $scope.selected).success(function(returnedData)
		{
			$scope.checkForLogin(data);
			if (returnedData.success)
			{
				$scope.updateModel();
			}
		});
	};

	// --------------- Resets the selected Boxes --------------- //
	$scope.bulkResetBox = function()
	{
		$http.post($scope.contextPath + '/mail/bulkReset', $scope.selected).success(function(returnedData)
		{
			$scope.checkForLogin(data);
			if (returnedData.success)
			{
				$scope.updateModel();
			}
		});
	};

	// --------------- Disables the selected Boxes --------------- //
	$scope.bulkDisableBox = function()
	{
		$http.post($scope.contextPath + '/mail/bulkDisable', $scope.selected).success(function(returnedData)
		{
			$scope.checkForLogin(data);
			if (returnedData.success)
			{
				$scope.updateModel();
			}
		});
	};

	// --------------- Enables the selected Boxes --------------- //
	$scope.bulkEnablePossibleBox = function()
	{
		$http.post($scope.contextPath + '/mail/bulkEnablePossible', $scope.selected).success(function(returnedData)
		{
			$scope.checkForLogin(data);
			if (returnedData.success)
			{
				$scope.updateModel();
			}
		});
	};

	// --------------- Enables the selected Boxes --------------- //
	$scope.bulkSetNewDate = function(newDateTime)
	{
		var postData = {};
		postData["boxes"] = $scope.selected;
		postData["newDateTime"] = newDateTime;
		$http.post($scope.contextPath + '/mail/bulkNewDate', postData).success(function(returnedData)
		{
			$scope.checkForLogin(data);
			if (returnedData.success)
			{
				$scope.updateModel();
			}
		});
	};

	// --------------- Pushes Alerts to the stack --------------- //
	// pushes an array to the alert-stack, at least 5 errors
	// type: true = success, false = error
	$scope.pushAlert = function(type, message)
	{
		if (type)
		{
			if ($scope.alerts.length >= 5)
			{
				$scope.alerts.shift();
			}
			$scope.alerts.push(
			{
				'type' : 'success',
				'message' : message
			});
		}
		else
		{
			if ($scope.alerts.length >= 5)
			{
				$scope.alerts.shift();
			}
			$scope.alerts.push(
			{
				'type' : 'error',
				'message' : message
			});
		}
	};

	/*
	 * Handles the Session-Timeout (or user is not logged in)
	 */
	$scope.checkForLogin = function(input)
	{
		if (input.error == 'nologin')
		{
			$window.location.href = $scope.contextPath + "/login";
		}
	};

};

/*
 * ADD and EDIT Mailaddress Dialog Controller
 */
// the dialog is injected in the specified controller
function AddEditDialogController($scope, $http, dialog, currentBox, domains, contextPath)
{
	$scope.domains = domains;
	$scope.contextPath = contextPath;
	$scope.currentBox = currentBox;
	$scope.setValues = function()
	{
		if ($scope.currentBox === undefined)
		{
			$http.get($scope.contextPath + '/mail/addBoxData').success(function(data)
			{
				$scope.checkForLogin(data);
				$scope.currentBox = data.currentBox;
			});
		}
	};
	$scope.setValues();

	$scope.close = function(data)
	{
		var chkBoxIdString = '#chkUnlimited' + data.id;
		if ($(chkBoxIdString).is(':checked'))
		{
			data.datetime = 0;
		}
		else
		{
			var idString = '#datetime' + data.id;
			var newTime = $(idString).val();
			data.datetime = newTime;
		}
		dialog.close(data);
	};

	$scope.dismiss = function()
	{ // ignore changes, just close the dialog
		dialog.close();
	};

	/*
	 * Handles the Session-Timeout (or user is not logged in)
	 */
	$scope.checkForLogin = function(input)
	{
		if (input.error == 'nologin')
		{
			$window.location.href = $scope.contextPath + "/login";
		}
	};
};

/*
 * Controller to handle small Dialogs e.g. "are you sure that you want to delete this address?"
 */
function DeleteDialogsController($scope, dialog, currentBox, contextPath, isBulkAction)
{
	$scope.contextPath = contextPath;
	$scope.currentBox = currentBox;
	$scope.isBulkAction = isBulkAction;
	$scope.close = function(result)
	{
		dialog.close(result);
	};

	$scope.dismiss = function()
	{ // ignore changes, just close the dialog
		dialog.close();
	};
};

/*
 * Controller to handle small Dialogs e.g. "are you sure that you want to delete this address?"
 */
function NewDateController($scope, dialog, currentBoxes, contextPath)
{
	$scope.contextPath = contextPath;
	$scope.currentBoxes = currentBoxes;
	$scope.close = function(newDateTime)
	{
		if ($('#chkUnlimitedNew').is(':checked'))
		{
			newDateTime = 0;
		}
		dialog.close(newDateTime);
	};

	$scope.dismiss = function()
	{ // ignore changes, just close the dialog
		dialog.close();
	};
};
