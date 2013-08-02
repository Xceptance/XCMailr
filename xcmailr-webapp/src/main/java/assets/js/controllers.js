angular.module('BoxHandler', ['ui.bootstrap']);
function BoxListCtrl($scope, $dialog, $http) {

$scope.selected = {};

$http.get('/mail/angget').success(function(data){
	$scope.boxes = data;
	});

	$scope.deleteBox = function(boxId, elementIdx, contextPath){
		$http.post(contextPath+'/mail/delete/'+boxId, null).success(function(){
		$scope.boxes.splice(elementIdx, 1);
		});
	}
	$scope.resetBox = function(boxId, elementIdx, contextPath){
		$http.post(contextPath+'/mail/reset2/'+boxId, null).success(function(){
		$scope.boxes[elementIdx].suppressions=0;
		$scope.boxes[elementIdx].forwards=0;
		});
	}

	$scope.toggleMenu = function(boxId){
		return !$scope.toggleMenu;
	}

    $scope.ShowSelected = function() {
      $scope.boxes = $.grep($scope.boxes, function( box ) {
        return $scope.selected[ box.id ];
      });
    };  





  $scope.opts = {
    backdrop: true,
    keyboard: true,
    backdropClick: true,
    templateUrl: '/mail/addBoxDialog.html',
    controller: 'TestDialogController'
  };
  

  $scope.openDialog = function(elementIdx, data){
    $scope.currentBox = $scope.boxes[elementIdx];
    var d = $dialog.dialog($scope.opts);
    d.open().then(function(result){
      if(result)
      {
        alert('dialog closed with result: ' + result);
      }
    });
  };

}

 // the dialog is injected in the specified controller
 function TestDialogController($scope, dialog){
 	 $scope.close = function(result){
    dialog.close(result);
  };

}
