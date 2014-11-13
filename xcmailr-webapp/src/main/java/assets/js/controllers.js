angular.module('BoxHandler', [ 'ui.bootstrap' ]);

function BoxListCtrl($scope, $modal, $http, $window) {

	$scope.init = function(cP) { // something like a constructor/initializer
		$scope.contextPath = cP;
		// execute the domain-load
		$scope.loadDomains();
		// init the search-string
		$scope.searchString = '';

		// handle the pagination
		$scope.noOfPages = 1;
		$scope.currentPage = 1;
		$scope.maxSize = 5;
		$scope.itemsPerPage = 15;
		$scope.selected = {};
		$scope.allBoxes = {};
		$scope.alerts = [];
		$scope.updateModel();
		$scope.noOfPages = $scope.setNumPages();
		$scope.allBoxesAreNowSelected = false;
		// AddEditDialog-Options
		$scope.opts = {
			backdrop : true,
			keyboard : true,
			backdropClick : true,
			templateUrl : $scope.contextPath + '/mail/editBoxDialog.html',
			controller : 'AddEditDialogController'
		};
		// DeleteDialog-Options
		$scope.optsDeleteDialog = {
			backdrop : true,
			keyboard : true,
			backdropClick : true,
			templateUrl : $scope.contextPath + '/mail/deleteBoxDialog.html',
			controller : 'DeleteDialogsController'
		};
		// NewDateDialog-Options
		$scope.optsNewDateDialog = {
			backdrop : true,
			keyboard : true,
			backdropClick : true,
			templateUrl : $scope.contextPath + '/mail/newDateDialog.html',
			controller : 'NewDateController'
		};
	};

	// --------------- get the available domains --------------- //
	$scope.loadDomains = function() { // just load the data
		$http.get($scope.contextPath + '/mail/domainlist').success(
				function(data) {
					$scope.checkForLogin(data);
					$scope.domains = data;
				});
	};

	// --------------- load all boxes --------------- //
	$scope.updateModel = function() {
		$http.get($scope.contextPath + '/mail/getmails').success(
				function(data) {
					$scope.checkForLogin(data);
					$scope.allBoxes = data;
					$scope.noOfPages = $scope.setNumPages();
				});
	};

	// --------------- delete a box --------------- //
	$scope.deleteBox = function(boxId, elementIdx) {
		$http
				.post($scope.contextPath + '/mail/delete/' + boxId, null)
				.success(
						function(returnedData) {
							$scope.checkForLogin(returnedData);
							if (returnedData.success) { // remove the address
								// from the list of all
								// addresses and clean
								// the list of selected
								// Elements
								$scope.allBoxes
										.splice(
												elementIdx
														+ (($scope.currentPage - 1) * $scope.itemsPerPage),
												1);
								$scope.selected = {};
							} else {
								$scope.pushAlert(returnedData.success,
										returnedData.statusMsg);
							}
						});
	};

	// --------------- activate or deactivate a box --------------- //
	$scope.expireBox = function(boxId, elementIdx) {
		$http
				.post($scope.contextPath + '/mail/expire/' + boxId, null)
				.success(
						function(returnedData) {
							if (returnedData.success) {
								$scope.checkForLogin(returnedData);
								var curBox = $scope.filteredBoxes[elementIdx];
								curBox.expired = !curBox.expired;
								// update the allBoxes-model, this should also
								// refresh the filteredboxes
								$scope.allBoxes[elementIdx
										+ (($scope.currentPage - 1) * $scope.itemsPerPage)] = curBox;
							} else {
								$scope.openEditBoxDialog(elementIdx);
							}
						});
	};

	// --------------- reset the forward and suppression-count ---------------
	// //
	$scope.resetBox = function(boxId, elementIdx) {
		$http
				.post($scope.contextPath + '/mail/reset/' + boxId, null)
				.success(
						function(returnedData) {
							$scope.checkForLogin(returnedData);
							if (returnedData.success) {
								$scope.allBoxes[elementIdx
										+ (($scope.currentPage - 1) * $scope.itemsPerPage)].suppressions = 0;
								$scope.allBoxes[elementIdx
										+ (($scope.currentPage - 1) * $scope.itemsPerPage)].forwards = 0;
							} else {
								$scope.pushAlert(returnedData.success,
										returnedData.statusmsg);
							}
						});
	};

	// --------------- edit a box --------------- //
	$scope.editBox = function(boxId, data, elementIdx) {
		$http
				.post($scope.contextPath + '/mail/edit/' + boxId, data)
				.success(
						function(returnedData) {
							$scope.checkForLogin(returnedData);
							if (returnedData.success) {
								$scope.allBoxes[elementIdx
										+ (($scope.currentPage - 1) * $scope.itemsPerPage)] = returnedData.currentBox;
							}

							$scope.pushAlert(returnedData.success,
									returnedData.statusmsg);
						});
	};

	// --------------- Search for the given String --------------- //
	$scope.search2 = function() {
		$http.get(
				$scope.contextPath + '/mail/getmails?s=' + $scope.searchString)
				.success(function(data) {
					$scope.checkForLogin(data);
					$scope.allBoxes = data;
				});
	};

	// --------------- add an address --------------- //
	$scope.addBox = function(boxId, data) {
		$http.post($scope.contextPath + '/mail/addAddress', data).success(
				function(returnedData) {
					$scope.checkForLogin(returnedData);
					if (returnedData.success) {
						$scope.allBoxes.push(returnedData.currentBox);
					}
					$scope.pushAlert(returnedData.success,
							returnedData.statusmsg);
				});
	};

	// --------------- show the selected boxes --------------- //
	$scope.showSelected = function() {
		$scope.cleanSelected();
		if (!jQuery.isEmptyObject($scope.selected)) {
			$scope.filteredBoxes = $.grep($scope.allBoxes, function(box) {
				return $scope.selected[box.id];
			});
		} else {
			$scope.getStatusMessageAndPushAlert("mailbox_Flash_NoBoxSelected",
					false);
		}
	};

	// --------------- show the selected boxes --------------- //
	$scope.cleanSelected = function() {
		for ( var itm in $scope.selected) {
			if (!$scope.selected[itm]) {
				delete $scope.selected[itm];
			}
		}
	};

	// --------------- show all boxes on one page --------------- //
	$scope.showAll = function() {
		$scope.setItemsPerPage($scope.allBoxes.length);
	};

	// --------------- set the page --------------- //
	$scope.setPage = function(pageNo) {
		$scope.currentPage = pageNo;
	};

	// --------------- set the page-size --------------- //
	$scope.setItemsPerPage = function(size) {
		$scope.itemsPerPage = size;
		$scope.noOfPages = $scope.setNumPages();
		$scope.currentPage = 1;
	};

	// --------------- set the number of pages --------------- //
	$scope.setNumPages = function() {
		return ($scope.itemsPerPage != 0) ? Math.ceil($scope.allBoxes.length
				/ $scope.itemsPerPage) : 1;
	};

	// --------------- Opens the EditBoxDialog --------------- //
	$scope.openEditBoxDialog = function(elementIdx) {
		$scope.currentBox = $scope.filteredBoxes[elementIdx];
		$scope.opts.resolve = {
			currentBox : function() {
				return angular.copy($scope.currentBox);
			},
			domains : function() {
				return angular.copy($scope.domains);
			},
			contextPath : function() {
				return angular.copy($scope.contextPath);
			}
		};
		var d = $modal.open($scope.opts);

		d.result.then(function(result) {
			if (result) {
				$scope.editBox(result.id, result, elementIdx);
			}
		});
	};

	// --------------- Opens the AddBoxDialog --------------- //
	$scope.openAddBoxDialog = function() {
		$scope.opts.resolve = {
			currentBox : function() {
				// return $scope.getAddBoxData();
			},
			domains : function() {
				return angular.copy($scope.domains);
			},
			contextPath : function() {
				return angular.copy($scope.contextPath);
			}
		};
		var d = $modal.open($scope.opts);

		d.result.then(function(result) {
			if (result) {
				$scope.addBox(result.id, result);
			}
		});
	};

	// --------------- Opens the deleteBoxDialog --------------- //
	$scope.openDeleteBoxDialog = function(elementIdx) {
		$scope.optsDeleteDialog.resolve = {
			currentBox : function() {
				return angular.copy($scope.filteredBoxes[elementIdx]);
			},
			contextPath : function() {
				return angular.copy($scope.contextPath);
			},
			isBulkAction : function() {
				return false;
			}
		};
		var d = $modal.open($scope.optsDeleteDialog);

		d.result.then(function(result) {
			if (result) {
				$scope.deleteBox(result.id, elementIdx);
			}
		});
	};

	// --------------- Opens the bulkDeleteBoxDialog --------------- //
	$scope.openbulkDeleteBoxDialog = function() {
		$scope.cleanSelected();
		if (!jQuery.isEmptyObject($scope.selected)) {
			$scope.optsDeleteDialog.resolve = {
				currentBox : function() {
					var boxes = $.grep($scope.allBoxes, function(box) {
						return $scope.selected[box.id];
					});
					return angular.copy(boxes);
				},
				contextPath : function() {
					return angular.copy($scope.contextPath);
				},
				isBulkAction : function() {
					return true;
				}
			};
			var d = $modal.open($scope.optsDeleteDialog);

			d.result.then(function(result) {
				if (result) {
					$scope.bulkDeleteBox();
				}
			});
		} else {
			$scope.getStatusMessageAndPushAlert("mailbox_Flash_NoBoxSelected",
					false);
		}
	};

	// --------------- Opens the NewDateDialog --------------- //
	$scope.openNewDateDialog = function() {
		$scope.cleanSelected();
		if (!jQuery.isEmptyObject($scope.selected)) {
			$scope.optsNewDateDialog.resolve = {
				currentBoxes : function() {
					var boxes = $.grep($scope.allBoxes, function(box) {
						return $scope.selected[box.id];
					});
					return angular.copy(boxes);
				},
				contextPath : function() {
					return angular.copy($scope.contextPath);
				}
			};
			var d = $modal.open($scope.optsNewDateDialog);

			d.result.then(function(newDateTime) {
				if (!(newDateTime === undefined)) {
					$scope.bulkSetNewDate(newDateTime);
				}
			});
		} else {
			$scope.getStatusMessageAndPushAlert("mailbox_Flash_NoBoxSelected",
					false);
		}
	};

	// --------------- Returns the localized message for the given key
	// ---------//
	$scope.getStatusMessageAndPushAlert = function(messageKey, isSuccess) {
		$http.post($scope.contextPath + '/getMessage', '"' + messageKey + '"')
				.success(function(returnedData) {
					$scope.pushAlert(isSuccess, returnedData.message);
				});
	};

	// --------------- Closes an Alert-Message --------------- //
	$scope.closeAlert = function(elementIdx) {
		$scope.alerts.splice(elementIdx, 1);
	};

	// --------------- Page-Change Listener --------------- //
	$scope.paginationListener = $scope.$watch('currentPage + itemsPerPage',
			function() {
				$scope.updateView();
			}, true);

	// --------------- All-Boxes Update Listener --------------- //
	$scope.updateListener = $scope.$watch('allBoxes', function() {
		$scope.updateView();
	}, true);

	// --------------- Update the View --------------- //
	$scope.updateView = function() {
		if ($scope.itemsPerPage > 0 && $scope.allBoxes.length > 0) {
			var begin = (($scope.currentPage - 1) * $scope.itemsPerPage);
			var end = begin + $scope.itemsPerPage;
			$scope.filteredBoxes = $scope.allBoxes.slice(begin, end);
		} else {
			$scope.filteredBoxes = $scope.allBoxes;
		}
	};

	// --------------- BoxCount Listener --------------- //
	$scope.boxCountListener = $scope.$watch('allBoxes.length', function() {
		$scope.noOfPages = $scope.setNumPages();
	});

	// --------------- Select All available Items (by checkbox) ---------------
	// //
	$scope.selectAllItems = function() {
		$scope.allBoxesAreNowSelected = !$scope.allBoxesAreNowSelected;
		if ($scope.allBoxesAreNowSelected) {
			angular.forEach($scope.allBoxes, function(mBox, key) {
				$scope.selected[mBox.id] = $scope.allBoxesAreNowSelected;
			});
		} else {
			$scope.selected = {};
		}
	};

	// --------------- Deletes the selected Boxes --------------- //
	$scope.bulkDeleteBox = function() {
		$http.post($scope.contextPath + '/mail/bulkDelete', $scope.selected)
				.success(function(returnedData) {
					$scope.checkForLogin(returnedData);
					if (returnedData.success) {
						$scope.updateModel();
					}
				});
	};

	// --------------- Resets the selected Boxes --------------- //
	$scope.bulkResetBox = function() {
		$scope.cleanSelected();
		if (!jQuery.isEmptyObject($scope.selected)) {
			$http.post($scope.contextPath + '/mail/bulkReset', $scope.selected)
					.success(function(returnedData) {
						$scope.checkForLogin(returnedData);
						if (returnedData.success) {
							$scope.updateModel();
						}
					});
		} else {
			$scope.getStatusMessageAndPushAlert("mailbox_Flash_NoBoxSelected",
					false);
		}
	};

	// --------------- Disables the selected Boxes --------------- //
	$scope.bulkDisableBox = function() {
		$scope.cleanSelected();
		if (!jQuery.isEmptyObject($scope.selected)) {
			$http.post($scope.contextPath + '/mail/bulkDisable',
					$scope.selected).success(function(returnedData) {
				$scope.checkForLogin(returnedData);
				if (returnedData.success) {
					$scope.updateModel();
				}
			});
		} else {
			$scope.getStatusMessageAndPushAlert("mailbox_Flash_NoBoxSelected",
					false);
		}
	};

	// --------------- Enables the selected Boxes --------------- //
	$scope.bulkEnablePossibleBox = function() {
		$scope.cleanSelected();
		if (!jQuery.isEmptyObject($scope.selected)) {
			$http.post($scope.contextPath + '/mail/bulkEnablePossible',
					$scope.selected).success(function(returnedData) {
				$scope.checkForLogin(returnedData);
				if (returnedData.success) {
					$scope.updateModel();
				}
			});
		} else {
			$scope.getStatusMessageAndPushAlert("mailbox_Flash_NoBoxSelected",
					false);
		}
	};

	// --------------- Enables the selected Boxes --------------- //
	$scope.bulkSetNewDate = function(newDateTime) {
		var postData = {};
		postData["boxes"] = $scope.selected;
		postData["newDateTime"] = newDateTime;
		$http.post($scope.contextPath + '/mail/bulkNewDate', postData).success(
				function(returnedData) {
					$scope.checkForLogin(returnedData);
					if (returnedData.success) {
						$scope.updateModel();
					}
				});
	};

	// --------------- Pushes Alerts to the stack --------------- //
	// pushes an array to the alert-stack, at least 5 errors
	// type: true = success, false = error
	$scope.pushAlert = function(type, message) {
		if (type) {
			if ($scope.alerts.length >= 3) {
				$scope.alerts.shift();
			}
			$scope.alerts.push({
				'type' : 'success',
				'message' : message
			});
		} else {
			if ($scope.alerts.length >= 3) {
				$scope.alerts.shift();
			}
			$scope.alerts.push({
				'type' : 'danger',
				'message' : message
			});
		}
	};

	/*
	 * Handles the Session-Timeout (or user is not logged in)
	 */
	$scope.checkForLogin = function(input) {
		if (input.error == 'nologin') {
			$window.location.href = $scope.contextPath + "/login";
		}
	};

};

/*
 * ADD and EDIT Mailaddress Dialog Controller
 */
// the dialog is injected in the specified controller
function AddEditDialogController($scope, $http, $modalInstance, currentBox,
		domains, contextPath) {
	$scope.domains = domains;
	$scope.initialData = angular.copy(currentBox);
	$scope.contextPath = contextPath;
	$scope.currentBox = currentBox;
	$scope.setValues = function() {
		if ($scope.currentBox === undefined) {
			$http.get($scope.contextPath + '/mail/addAddressData').success(
					function(returnedData) {
						$scope.checkForLogin(returnedData);
						$scope.currentBox = returnedData.currentBox;
						$scope.initialData = angular.copy($scope.currentBox);

					});
		}
	};

	$scope.setValues();

	$scope.close = function(data) {
		var chkBoxIdString = '#chkUnlimited' + data.id;
		if ($(chkBoxIdString).is(':checked')) {
			data.datetime = "0";
		} else {
			var idString = '#datetimepicker' + data.id;
			// var newTime = $(idString).val();
			console.log($(idString))
			var newDateTime = $(idString).data("DateTimePicker").getDate()
					.format("YYYY-MM-DD HH:mm");
			data.datetime = newDateTime;
		}
		$modalInstance.close(data);
	};
	$scope.setBoxDate = function(timestamp, lang) {
		var now = new Date();
		if (timestamp === "unlimited") {
			var sDate = new Date();
		} else {
			var sDate = moment(timestamp, "YYYY-MM-DD HH:mm");
			if (sDate < now) {
				sDate = new Date();
				sDate.setTime(sDate.getTime() + (3600 * 1000));
			}
		}

		$('.pickrBoxEdit').datetimepicker({
			language : lang,
			pick12HourFormat : false,
			minDate : now,
			defaultDate : sDate
		});
	};

	$scope.dismiss = function() { // ignore changes, just close the dialog
		$modalInstance.close();
	};

	/*
	 * Handles the Session-Timeout (or user is not logged in)
	 */
	$scope.checkForLogin = function(input) {
		if (input.error == 'nologin') {
			$window.location.href = $scope.contextPath + "/login";
		}
	};

	$scope.reset = function() {
		$scope.currentBox = angular.copy($scope.initialData);

		if ($scope.unlimitedBoxChecked
				&& !($scope.initialData.datetime == 'unlimited')) {
			$scope.unlimitedBoxChecked = false;
		}
		if ($scope.initialData.datetime == 'unlimited') {
			$scope.unlimitedBoxChecked = true;
		}
		$scope.form.$setPristine();
	};
};

/*
 * Controller to handle small Dialogs e.g. "are you sure that you want to delete
 * this address?"
 */
function DeleteDialogsController($scope, $modalInstance, currentBox,
		contextPath, isBulkAction) {
	$scope.contextPath = contextPath;
	$scope.currentBox = currentBox;
	$scope.isBulkAction = isBulkAction;
	$scope.close = function(result) {
		$modalInstance.close(result);
	};

	$scope.dismiss = function() { // ignore changes, just close the dialog
		$modalInstance.close();
	};
};

/*
 * Controller to handle the "set new date"-dialog
 */
function NewDateController($scope, $modalInstance, currentBoxes, contextPath) {
	$scope.contextPath = contextPath;
	$scope.currentBoxes = currentBoxes;
	$scope.close = function(newDateTime) {
		if ($('#chkUnlimitedNew').is(':checked')) {
			newDateTime = "0";
			$modalInstance.close(newDateTime);
			return;
		}
		newDateTime = $('#datetimepickerNew').data("DateTimePicker").getDate()
				.format("YYYY-MM-DD HH:mm");
		$modalInstance.close(newDateTime);
	};

	$scope.dismiss = function() { // ignore changes, just close the dialog
		$modalInstance.close();
	};
};
