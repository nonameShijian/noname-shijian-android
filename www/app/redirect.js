'use strict';
(function() {
	if (!localStorage.getItem('noname_freeTips')) {
		alert("【无名杀】属于个人开发软件且【完全免费】，如非法倒卖用于牟利将承担法律责任 开发团队将追究到底");
		localStorage.setItem('noname_freeTips', true);
	}
	var url = localStorage.getItem('noname_inited');
	if (url) {
		if (url === 'nodejs') {
			url = '';
		}
		var loadFailed = function() {
			localStorage.removeItem('noname_inited');
			window.location.reload();
		}
		var load = function(src, onload, onerror) {
			var script = document.createElement('script');
			script.src = url + 'game/' + src + '.js';
			script.onload = onload;
			script.onerror = onerror;
			document.head.appendChild(script);
		}
		load('update', function() {
			load('config', function() {
				load('package', function() {
					load('game', null, loadFailed);
				}, loadFailed);
			}, loadFailed);
		}, loadFailed);
		window.cordovaLoadTimeout = setTimeout(loadFailed, 5000);
	}
}());