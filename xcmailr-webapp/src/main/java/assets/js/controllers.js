function BoxListCtrl($scope, $http) {

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


}
