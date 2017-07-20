// jshint ignore: start
/**
 * Created by Leo on 18.07.2017.
 */
angular.module('exampleApp')
    .directive("devicediv", function ($compile, $http) {
        return {
            restrict: 'A',
            link: function (scope, element) {
                element.bind("click", function (e) {

                    var childNode = $compile('<button ng-click="getDevsInRoom(room)" >new button</button>')(scope)
                    element.parent().append(childNode);

                });

                scope.getDevsInRoom = function (roomName) {
                    $http.post('devsPerRoom', {'roomName': roomName}).then(function onSuccess(response) {
                        scope.devsInRoom = response.data.devices;
                    }, function onFailure(response) {
                        console.error("can't get devices per room" + response.toString());
                    });
                };

            }
        }
    });