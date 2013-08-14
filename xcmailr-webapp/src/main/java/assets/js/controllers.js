angular.module('BoxHandler', ['ui.bootstrap']);
function BoxListCtrl($scope, $dialog, $http) {

$scope.selected = {};
$scope.allBoxes = {};
	// load the boxes
	$scope.updateModel = function(){ 
		$http.get('/mail/angget').success(
			function(data){
				$scope.allBoxes = data;
			    $scope.noOfPages = $scope.setNumPages();
			}
	
		);
	};
	
	// deleteBox
	$scope.deleteBox = function(boxId, elementIdx, contextPath){
		$http.post(contextPath + '/mail/delete2/' + boxId, null).success(
			function(){
				$scope.allBoxes.splice(elementIdx, 1);
			}
		);
	}
	
	// resetBox
	$scope.resetBox = function(boxId, elementIdx, contextPath){
		$http.post(contextPath + '/mail/reset2/' + boxId, null).success(
				function(){
					$scope.allBoxes[elementIdx].suppressions = 0;
					$scope.allBoxes[elementIdx].forwards = 0;
				}
			);
	}
	
	$scope.toggleMenu = function(boxId){
		return !$scope.toggleMenu;
	}

    $scope.ShowSelected = function() {
    	$scope.allBoxes = $.grep($scope.allBoxes, function( box ) {
    		return $scope.selected[ box.id ];
      });
    };  

    /*
	 * handle the pagination
	 */
    $scope.noOfPages = 1;
    $scope.currentPage = 1;
    $scope.maxSize = 15;
    

    // set the page
    $scope.setPage = function (pageNo) {
        $scope.currentPage = pageNo;
    };
      
      // set the page-size
	$scope.setMaxSize = function (size) {
	    $scope.maxSize = size;
	    $scope.noOfPages = $scope.setNumPages();
	    $scope.currentPage = 1;
	};
        
      // set the number of pages
      $scope.setNumPages = function(){
    	  if($scope.maxSize != 0)
    	  {
    		  return Math.ceil($scope.allBoxes.length / $scope.maxSize);
    	  }else
    	  {
    		  return 1;  
    	  }
      }  

      /*
		 * handle the addboxdialog (TODO)
		 */
  $scope.opts = {
    backdrop: true,
    keyboard: true,
    backdropClick: true,
    templateUrl: '/mail/addBoxDialog.html',
    controller: 'TestDialogController'
  };
  

  $scope.openDialog = function(elementIdx, data){
    $scope.currentBox = $scope.allBoxes[elementIdx];
    var d = $dialog.dialog($scope.opts);
    d.open().then(function(result){
      if(result)
      {
        alert('dialog closed with result: ' + result);
      }
    });
  };

  
  $scope.updateModel();
  $scope.noOfPages = $scope.setNumPages();		
	// listener for page-changes
	$scope.paginationListener = $scope.$watch('currentPage + maxSize + allBoxes.length', 
		function() {
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
	);
	
	$scope.boxCountListener = $scope.$watch('allBoxes.length', 
			function(){
				$scope.noOfPages = $scope.setNumPages();		
			}
		);
}

 // the dialog is injected in the specified controller
 function TestDialogController($scope, dialog){
 	 $scope.close = function(result){
    dialog.close(result);
  };

}
