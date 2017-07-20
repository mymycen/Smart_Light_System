(function () {
    'use strict';

    var SmartLightApp = angular.module('SmartLightApp');

    /**
     * This controller calls 'rooms' and 'devices' request handlers which are registered in the ExampleApp.java.
     */
    SmartLightApp.controller('configController', ['$scope', '$http', function ($scope, $http) {
        $scope.rooms = [];
        $scope.devices = [];
        $scope.modes = [];
        $scope.status = [];
        $scope.dimmLevel = 100;
        $scope.selectedMode = "romantic";

        $scope.apartement = {"name": "Whole Apartement", "identifier": "apartement"};

        $scope.selected = {"name": "Whole Apartement", "identifier": "apartement"};

        $('#toggle-two').change(function () {
            startVoice();
        });

        $('#toggle-one').change(function () {
            changeLights();
        });

        $('#toggle-three').change(function () {
            startDetect();
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

        $scope.setSelected = function (roomName) {
            console.log(roomName);
            $scope.devices = roomName.devices;
            $scope.selected = roomName;
        };

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

        var startDetect = function () {
            $http.post('startDetect').then(function onSuccess(response) {
                console.debug("successfully changed voice recognition");
                $scope.value = response.data.value;
            }, function onFailure(response) {
                console.error("problem changing voice recognition" + response.toString());
            });
        };


        var changeLights = function () {
            $http.post('changeAllLights').then(function onSuccess(response) {
                console.debug("successfully changed all lightss");
                $scope.value = response.data.value;
            }, function onFailure(response) {
                console.error("problem changing all lights" + response.toString());
            });
        };


        $scope.sendSettings = function () {
            var color = $('#cp7').colorpicker('getValue');
            var rgb = color;
            rgb = rgb.replace(/[^\d,]/g, '').split(',');
            $('#densitySlider').slider({
                formatter: function (value) {
                    $scope.dimmLevel = value;
                }
            });
            console.log($scope.dimmLevel);
            var result = rgbToHsv(rgb[0], rgb[1], rgb[2]);
            var settings = {
                "dimLevel": $scope.dimmLevel.toString(),
                "hue": result[0].toString(),
                "sat": result[1].toString(),
                "dim": result[2].toString(),
                "where": $scope.selected.identifier
            };

            $http.post('changeSettings', {
                "settings": settings
            }).then(function onSuccess(response) {
                console.debug("fetchig devices was successfull");
                console.debug(response.data);
                $scope.value = response.data.value;
            }, function onFailure(response) {
                console.error("can't get devices");
            });
        };

        $scope.sendLightmode = function () {
            console.log($scope.selectedMode);
            var settings = {
                "modeName": $scope.selectedMode,
                "where": $scope.selected.identifier
            };
            $http.post('changeLightmode', {
                "settings": settings
            }).then(function onSuccess(response) {
                console.debug("fetchig devices was successfull");
                console.debug(response.data);
                $scope.value = response.data.value;
            }, function onFailure(response) {
                console.error("can't get devices");
            });
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