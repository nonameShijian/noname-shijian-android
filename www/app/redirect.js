'use strict';
(function () {
    var url = localStorage.getItem('noname_inited');
    if (url) {
        if (url === 'nodejs' || location.protocol.startsWith('http')) {
            url = '';
        }
        var loadFailed = function () {
            localStorage.removeItem('noname_inited');
            window.location.reload();
        }
        var load = function (src, onload, onerror) {
            var script = document.createElement('script');
            script.src = url + 'game/' + src + '.js';
            script.onload = onload;
            script.onerror = onerror;
            document.head.appendChild(script);
        }
        load('update', function () {
            load('config', function () {
                load('package', function () {
                    load('game', null, loadFailed);
                }, loadFailed);
            }, loadFailed);
        }, loadFailed);
        window.cordovaLoadTimeout = setTimeout(loadFailed, 5000);
    }
}());
