(function () {
    'use strict';

    var SmartLightApp = angular.module('SmartLightApp');

    /**
     * This controller calls 'rooms' and 'devices' request handlers which are registered in the ExampleApp.java.
     */
    SmartLightApp.controller('configController', ['$scope', '$http', function ($scope, $http) {

        $scope.modes = [];

        $scope.partymode = {"name": "Party mode"};

        $scope.selected = {"name": "Party mode"};


        $http.get('getAllModes').then(function onSuccess(response) {
            console.debug("fetchig modes was successfull");
            $scope.modes = response.data.modes;
            console.log($scope.modes);
        }, function onFailure(response) {
            console.error("can't get modes");
        });

        $scope.setSelected = function (mode) {
            console.log(mode);
            $scope.selected = mode;
        };


        $scope.sendSettings = function () {
            var color = $('#cp7').colorpicker('getValue');
            var rgb = color;
            rgb = rgb.replace(/[^\d,]/g, '').split(',');
            var result = rgbToHsv(rgb[0], rgb[1], rgb[2]);
            var settings = {
                "hue": result[0].toString(),
                "sat": result[1].toString(),
                "dim": result[2].toString(),
                "mode": $scope.selected.name
            };

            $http.post('configureLightmode', {
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