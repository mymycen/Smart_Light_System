(function () {
    'use strict';

    var exampleApp = angular.module('exampleApp');

    /**
     * This controller calls 'rooms' and 'devices' request handlers which are registered in the ExampleApp.java.
     */
    exampleApp.controller('ExampleController1', ['$scope', '$http', function ($scope, $http) {
        $scope.rooms = [];
        $scope.devices = [];
        $scope.modes = [];
        $scope.status = [];

        $scope.selected = {"name": "hallo", "dicker": "oha"};

        $('#toggle-two').change(function () {
            startVoice();
        });

        $http.get('getStatus').then(function onSuccess(response) {
            console.debug("fetchig status was successfull");
            console.log(response.data.status);
            $scope.status = response.data.status;
            if (response.data.status[3].value) {
                $('#toggle-one').bootstrapToggle('on');
            }
            if (response.data.status[0].value) {
                $('#toggle-two').bootstrapToggle('on');
            }
            if (response.data.status[1].value) {
                $('#toggle-three').bootstrapToggle('on');
            }
            if (response.data.status[2].value) {
                $('#toggle-four').bootstrapToggle('on');
            }
        }, function onFailure(response) {
            console.error("can't get status");
        });

        // the App had registered 'rooms' path, request it with the angular service $http.
        $http.get('roomsWithDevs').then(function onSuccess(response) {
            console.debug("fetchig rooms was successfull");
            $scope.rooms = response.data.rooms;
        }, function onFailure(response) {
            console.error("can't get rooms");
        });

        $http.get('getAllModes').then(function onSuccess(response) {
            console.debug("fetchig modes was successfull");
            $scope.modes = response.data.modes;
            console.log($scope.modes);
        }, function onFailure(response) {
            console.error("can't get modes");
        });

        $scope.getDevsInRoom = function (roomName) {
            $http.post('devsPerRoom', {'roomName': roomName}).then(function onSuccess(response) {
                $scope.devsInRoom = response.data.devices;
            }, function onFailure(response) {
                console.error("can't get devices per room" + response.toString());
            });
        };

        // example request with POST with a parameter
        $http.post('devices', {'propertyType': "http://iolite.de#on"}).then(function onSuccess(response) {
            console.debug("fetchig devices was successfull");
            console.debug(response.data);
            $scope.devices = response.data.devices;
        }, function onFailure(response) {
            console.error("can't get devices");
        });


        $scope.turnValue = function (deviceID) {
            $http.post('setValue', {'deviceType': deviceID}).then(function onSuccess(response) {
                console.debug("fetchig devices was successfull");
                console.debug(response.data);
                $scope.value = response.data.value;
            }, function onFailure(response) {
                console.error("can't get devices");
            });
        };

        var startVoice = function () {
            $http.post('startVoice').then(function onSuccess(response) {
                console.debug("successfully changed voice recognition");
                $scope.value = response.data.value;
            }, function onFailure(response) {
                console.error("problem changing voice recognition" + response.toString());
            });
        };


        $scope.sendSettings = function () {
            var color = $('#cp7').colorpicker('getValue');
            var rgb = color;
            rgb = rgb.replace(/[^\d,]/g, '').split(',');
            console.log(rgbToHsv(rgb[0], rgb[1], rgb[2]));
        };


        function rgbToHsv(r, g, b) {
            var
                min = Math.min(r, g, b),
                max = Math.max(r, g, b),
                delta = max - min,
                h, s, v = max;

            v = Math.floor(max / 255 * 100);
            if (max !== 0)
                s = Math.floor(delta / max * 100);
            else {
                // black
                return [0, 0, 0];
            }

            if (r === max)
                h = ( g - b ) / delta;         // between yellow & magenta
            else if (g === max)
                h = 2 + ( b - r ) / delta;     // between cyan & yellow
            else
                h = 4 + ( r - g ) / delta;     // between magenta & cyan

            h = Math.floor(h * 60);            // degrees
            if (h < 0) h += 360;

            return [h, s, v];
        }
    }]);
})();