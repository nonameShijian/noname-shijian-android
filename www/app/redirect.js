'use strict';
(async () => {
	let url = localStorage.getItem('noname_inited');

	if (!url) return;

	if (url === 'nodejs' || location.protocol.startsWith('http')) url = '';

	const loadFailed = () => {
		localStorage.removeItem('noname_inited');
		window.location.reload();
	};
	const load = src => new Promise((resolve, reject) => {
		const script = document.createElement('script');
		script.src = `${url}game/${src}.js`;
		script.onload = resolve;
		script.onerror = reject;
		document.head.appendChild(script);
	});
	window.cordovaLoadTimeout = setTimeout(loadFailed, 10000);

	try {
		await Promise.all([
			load('update'),
			load('config'),
			load('package')
		]);
		await load('game');
	} catch {
		loadFailed();
	}
})();
