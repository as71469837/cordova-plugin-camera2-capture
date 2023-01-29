var exec = require('cordova/exec');

// exports.coolMethod = function (arg0, success, error) {
//     exec(success, error, 'huayu-cordova-plugin-camera2capture', 'coolMethod', [arg0]);
// };

var camera2capture = {

    init: function (height, width, duration, success, error) {
        exec(success, error, 'Camera2Capture', 'init', [height, width, duration]);
    },

    initWithChannel: function (height, width, duration, success, error) {
        cordova.require('cordova/channel').onCordovaReady.subscribe(function () {
            exec(success, error, 'Camera2Capture', 'initWithChannel', [height, width, duration]);
        });
    },
    startVideoCapture: function (success, error) {
        exec(success, error, 'Camera2Capture', 'startVideoCapture', null);
    },
    endVideoCapture: function (success, error) {
        exec(success, error, 'Camera2Capture', 'endVideoCapture', null);
    },
    recordVideo: function (success, error) {
        cordova.require('cordova/channel').onCordovaReady.subscribe(function () {
            exec(success, error, 'Camera2Capture', 'recordVideo', null);
        });
    },
    selectCamera: function (success, error) {
        exec(success, error, 'Camera2Capture', 'selectCamera', null);
    },

}
module.exports = camera2capture;