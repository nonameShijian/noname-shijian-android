'use strict';
(async () => {
    if (!localStorage.getItem('noname_freeTips')) {
    	alert("【无名杀】属于个人开发软件且【完全免费】，如非法倒卖用于牟利将承担法律责任 开发团队将追究到底");
    	localStorage.setItem('noname_freeTips', true);
    }
	let url = localStorage.getItem('noname_inited');

	if (!url) return;

	if (url === 'nodejs' || location.protocol.startsWith('http')) url = '';

	const loadFailed = () => {
		localStorage.removeItem('noname_inited');
		localStorage.removeItem('noname_freeTips');
		window.location.reload();
	};

	const load = src => new Promise((resolve, reject) => {
		const script = document.createElement('script');
		script.src = `${url}game/${src}.js`;
		script.onload = resolve;
		script.onerror = (e) => {
		    alert('在载入' + 'game/' + src + '.js时发生错误');
		    reject(e);
		};
		document.head.appendChild(script);
	});
	window.cordovaLoadTimeout = setTimeout(loadFailed, 10000);

	try {
		await Promise.all([
		    load('importmap'),
			load('update'),
			load('config'),
			load('package')
		]);
		await load('game');
	} catch(e) {
		loadFailed();
	}
})();
