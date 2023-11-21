/// <reference path="./index.d.ts" />
'use strict';
(function () {
	if (localStorage.getItem('noname_inited')) return;
	const app = {
		initialize() {
			this.bindEvents();
		},
		bindEvents() {
			if (typeof window.require == 'function' && typeof window.__dirname == 'string') {
				this.onDeviceReady();
			} else {
				/** 导入最主要的cordova.js */
				const script = document.createElement('script');
				script.src = 'cordova.js';
				document.head.appendChild(script);
				document.addEventListener('deviceready', this.onDeviceReady, false);
			}
		},
		onDeviceReady() {
		    if (window.eruda) eruda.init();
			/** 如果导入了完整包，就直接进入游戏 */
			const getImportPackage = function () {
				window.cordova && cordova.exec(result => {
					console.log(result);
					if (result && result.type == 'package') {
						localStorage.setItem('noname_inited', result.message);
						cordova.exec(() => location.reload(), () => location.reload(), 'FinishImport', 'importReceived', []);
					}
				}, () => {}, 'FinishImport', 'importReady', []);
			};

			getImportPackage();

			/** 每次切换回应用触发检测 */
			document.addEventListener('visibilitychange', () => {
				if (document.hidden == false) getImportPackage();
			});

			/** 无名杀资源目录 */
			let appPath = '';
			if (typeof window.require == 'function' && typeof window.__dirname == 'string') {
				appPath = __dirname;
			} else if (window.cordova) {
				appPath = cordova.file.externalApplicationStorageDirectory;
			}

			/**
			 * @param { DirectoryEntry } [entry] 
			 * @param { string } [name] 文件/文件夹名
			 * @param { boolean } [isdir] 是否是文件夹
			 * @return { Promise<DirectoryEntry | FileEntry> }
			 */
			function getEntry(entry, name, isdir) {
				return new Promise((resolve, reject) => {
					if (!entry) {
						window.resolveLocalFileSystemURL(appPath,
							/** @param { DirectoryEntry } entry */
							entry => resolve(entry),
							e => reject(e));
					} else if (typeof name == 'string') {
						entry[isdir ? 'getDirectory' : 'getFile'](name, { create: true },
							/** @param { DirectoryEntry | FileEntry } e */
							e => resolve(e),
							e => reject(e));
					} else {
						resolve(entry);
					}
				});
			}

			navigator.notification.activityStart('正在检测是否有内置zip资源', '请耐心等待.....');
			/** 读取app的资源目录(在安卓是file:///android_asset/) */
			window.resolveLocalFileSystemURL(cordova.file.applicationDirectory,
				/** @param { DirectoryEntry } entry */
				entry => {
					entry.getDirectory('www/app', { create: false }, appDirEntry => {
						appDirEntry.getFile('noname.zip', { create: false }, fileEntry => {
							navigator.notification.activityStop();
							const zipDataDiv = document.getElementById('changesite');
							zipDataDiv.innerText = '内置zip资源存在';
							// 跳转到java解压
							if (confirm('检测到内置压缩包存在，是否解压？')) cordova.exec(() => { }, () => { }, 'FinishImport', 'assetZip', []);
							else checkConnection();
						}, error => {
							navigator.notification.activityStop();
							console.error('www/app/noname.zip不存在: ' + error.code);
							const zipDataDiv = document.getElementById('changesite');
							zipDataDiv.innerText = 'www/app/noname.zip不存在';
							// alert('请用其他方式打开zip文件，选择无名杀导入(诗笺版)，注: 万能导入无效');
							checkConnection();
						});
					}, error => {
						navigator.notification.activityStop();
						console.error('www/app文件夹不存在: ' + error.code);
						const zipDataDiv = document.getElementById('changesite');
						zipDataDiv.innerText = 'www/app文件夹不存在';
						// alert('请用其他方式打开zip文件，选择无名杀导入(诗笺版)，注: 万能导入无效');
						checkConnection();
					});
			});

			/** 添加app/index.css的样式 */
			let link = document.createElement('link');
			link.rel = 'stylesheet';
			link.href = 'app/index.css';
			document.head.appendChild(link);

			/**
			 * 自行扩展创建dom元素方法
			 * @template { keyof HTMLElementTagNameMap } k
			 * @param { k } tag 标签名
			 * @param { options } opts 选项
			 * @return { HTMLElementTagNameMap[k] } dom元素
			 */
			function createElement(tag, opts = {}) {
				const d = document.createElement(tag);
				for (const key in opts) {
					switch (key) {
						case 'class':
							opts[key].forEach(v => d.classList.add(v));
							break;
						case 'id':
							d.id = opts[key];
							break;
						case 'innerHTML':
						case 'innerText':
							d[key] = opts[key];
							break;
						case 'parentNode':
							opts[key].appendChild(d);
							break;
						case 'listen':
							for (const evt in opts[key]) {
								if (typeof opts[key][evt] == 'function') {
									d[evt] = opts[key][evt];
								}
							}
							break;
						case 'style':
							for (const s in opts[key]) {
								d.style[s] = opts[key][s];
							}
							break;
					}
				}
				return d;
			}

			// 设置触摸和鼠标监听
			let touchstart = function () {
				if (this.classList.contains('disabled')) return;
				this.style.transform = 'scale(0.98)';
			};
			let touchend = function () {
				this.style.transform = '';
			};

			/**
			 * 中心红色按钮
			 */
			const button = createElement('div', {
				id: 'button',
				class: ['disabled'],
				parentNode: document.body,
				innerText: '解压资源包',
				listen: {
					ontouchstart: touchstart,
					ontouchend: touchend,
					onmousedown: touchstart,
					onmouseup: touchend,
					onmouseleave: touchend,
					onclick: function () {
						if (button.classList.contains('disabled')) return;
						update();
					},
					ontouchmove: function (e) {
						e.preventDefault();
					}
				}
			});

			const zipDataDiv = createElement('div', {
				id: 'changesite',
				parentNode: document.body,
				innerText: '正在加载内置zip资源...',
				style: {
					// opacity: '0.5',
				}
			});

			const version = createElement('div', {
				id: 'version',
				parentNode: document.body,
			});

			const help = createElement('div', {
				id: 'help',
				innerHTML: '通过其他方式初始化游戏',
				parentNode: document.body,
				listen: {
					onclick: function () {
						document.body.appendChild(helpnode);
					}
				}
			});

			const helpnode = createElement('div', {
				id: 'noname_init_help',
			});

			const helpnodetext = createElement('div', {
				parentNode: helpnode,
			});

			const back = createElement('div', {
				id: 'back',
				innerHTML: '返回',
				parentNode: helpnode,
				listen: {
					onclick: function () {
						helpnode.remove();
					}
				}
			});

			const importdata = createElement('div', {
				id: 'importdata',
				innerHTML: '导入数据文件',
				parentNode: document.body,
			});

			const importselect = createElement('select', {
				id: 'importselect',
				parentNode: document.body,
			});

			/** FASTGIT更新源 */
			const site_c = 'https://raw.fgit.cf/libccy/noname/master/';
			/** URC更新源 */
			const site_urc = 'https://unitedrhythmized.club/libccy/noname/master/';
			/** 现在使用的更新源 */
			let site = site_c;
			
			/**
			 * @description 请求指定网址的js文件并执行
			 * @param { string } url 请求指定网址的js文件
			 * @param { (target?: string, result?: any) => void } onLoad 请求成功后执行回调
			 * @param { VoidFunction } onError 请求失败后执行回调
			 * @param { string } [target] 用window[target]是否存在来判断js是否加载成功
			 */
			const req = async (url, onLoad, onError, target) => {
				if (!onLoad) return;

				try {
					eval(await (await fetch(url, {
						referrerPolicy: 'no-referrer'
					})).text());

					if (target) {
						if (!window[target]) throw new ReferenceError();

						await onLoad();

						delete window[target];
					} else await onLoad();
				} catch(e) {
					if (onError) await onError();
				}
			}

			/** @type { string } 应用文件夹 **/
			let dir;
			const ua = navigator.userAgent.toLowerCase();
			if (ua.includes('android')) dir = cordova.file.externalApplicationStorageDirectory;
			else if (ua.includes('iphone') || ua.includes('ipad')) dir = cordova.file.documentsDirectory;

			var game = {
				putDB: (storeName, idbValidKey, value, onSuccess, onError) => {
					if (!lib.db) return Promise.resolve(value);
					return new Promise((resolve, reject) => {
						const record = lib.db.transaction([storeName], 'readwrite').objectStore(storeName).put(value, idbValidKey);
						record.onerror = event => {
							if (typeof onError == 'function') {
								onError(event);
								resolve(null);
							}
							else {
								reject(event);
							}
						};
						record.onsuccess = event => {
							if (typeof onSuccess == 'function') {
								onSuccess(event);
							}
							resolve(event);
						};
					});
				}
			};
			var lib = {
				configprefix: 'noname_0.9_',
				init: {
					decode: function (str) {
						var strUtf = atob(str);
						var strUni = strUtf.replace(
							/[\u00e0-\u00ef][\u0080-\u00bf][\u0080-\u00bf]/g, function (c) {
								var cc = ((c.charCodeAt(0) & 0x0f) << 12) | ((c.charCodeAt(1) & 0x3f) << 6) | (c.charCodeAt(2) & 0x3f);
								return String.fromCharCode(cc);
							});
						strUni = strUni.replace(
							/[\u00c0-\u00df][\u0080-\u00bf]/g, function (c) {
								var cc = (c.charCodeAt(0) & 0x1f) << 6 | c.charCodeAt(1) & 0x3f;
								return String.fromCharCode(cc);
							});
						return strUni;
					}
				}
			};

			if (typeof __dirname === 'string' && __dirname.length) {
				var dirsplit = __dirname.split('/');
				for (var i = 0; i < dirsplit.length; i++) {
					if (dirsplit[i]) {
						var c = dirsplit[i][0];
						lib.configprefix += /[A-Z]|[a-z]/.test(c) ? c : '_';
					}
				}
				lib.configprefix += '_';
			}

			var index = window.location.href.indexOf('index.html?server=');
			if (index != -1) {
				window.isNonameServer = window.location.href.slice(index + 18);
				window.nodb = true;
			}
			if (localStorage.getItem(`${lib.configprefix}nodb`)) window.nodb = true;

			if (window.FileTransfer) {
				// @ts-ignore
				window.tempSetNoname = dir;

				game.getFileList = (_dir, success, failure) => {
					var files = [], folders = [];
					window.resolveLocalFileSystemURL(dir + _dir, entry => {
						var dirReader = entry.createReader();
						var entries = [];
						var readEntries = () => {
							dirReader.readEntries(results => {
								if (!results.length) {
									entries.sort();
									for (var i = 0; i < entries.length; i++) {
										if (entries[i].isDirectory) {
											folders.push(entries[i].name);
										}
										else {
											files.push(entries[i].name);
										}
									}
									success(folders, files);
								}
								else {
									entries = entries.concat(Array.from(results));
									readEntries();
								}
							}, failure);
						};
						readEntries();
					}, failure);
				};

				game.readFileAsText = function (filename, callback, onerror) {
					window.resolveLocalFileSystemURL(dir, function (entry) {
						entry.getFile(filename, {}, function (fileEntry) {
							fileEntry.file(function (fileToLoad) {
								var fileReader = new FileReader();
								fileReader.onload = function (e) {
									callback(e.target.result);
								};
								fileReader.readAsText(fileToLoad, "UTF-8");
							}, onerror);
						}, onerror);
					}, onerror);
				};
			}
			else {
				window.tempSetNoname = 'nodejs';

				game.getFileList = (dir, success, failure) => {
					var files = [], folders = [];
					dir = __dirname + '/' + dir;
					if (typeof failure == "undefined") {
						failure = err => {
							throw err;
						};
					}
					else if (failure == null) {
						failure = () => { };
					}
					try {
						require('fs').readdir(dir, (err, filelist) => {
							if (err) {
								failure(err);
								return;
							}
							for (var i = 0; i < filelist.length; i++) {
								if (filelist[i][0] != '.' && filelist[i][0] != '_') {
									if (require('fs').statSync(dir + '/' + filelist[i]).isDirectory()) {
										folders.push(filelist[i]);
									}
									else {
										files.push(filelist[i]);
									}
								}
							}
							success(folders, files);
						});
					}
					catch (e) {
						failure(e);
					}
				};

				game.readFileAsText = function (filename, callback, onerror) {
					require('fs').readFile(__dirname + '/' + filename, 'utf-8', function (err, data) {
						if (err) {
							onerror(err);
						}
						else {
							callback(data);
						}
					});
				};
			}

			function addOption(name) {
				var option = document.createElement('option');
				option.value = name;
				option.innerHTML = name;
				importselect.appendChild(option);
				return option;
			}

			addOption('默认');
			addOption('选择系统文件');

			game.getFileList('files/', (_, files) => {
				files.filter(fileName => fileName.startsWith('无名杀 - 数据 -')).forEach(v => {
					addOption(v);
				});
			}, error => {
				console.error('读取无名杀数据文件失败: ' + error);
			});

			importselect.onchange = function (e) {
				if (importselect.value == '默认') return;
				if (confirm(`是否从“${importselect.value}”导入无名杀数据？`)) {
					new Promise((resolve, reject) => {
						if (lib.db || window.nodb) return resolve(null);
						const idbOpenDBRequest = window.indexedDB.open(`${lib.configprefix}data`);
						idbOpenDBRequest.onerror = reject;
						idbOpenDBRequest.onsuccess = resolve;
						idbOpenDBRequest.onupgradeneeded = idbVersionChangeEvent => {
							const idbDatabase = idbVersionChangeEvent.target.result;
							if (!idbDatabase.objectStoreNames.contains('video')) idbDatabase.createObjectStore('video', {
								keyPath: 'time'
							});
							if (!idbDatabase.objectStoreNames.contains('image')) idbDatabase.createObjectStore('image');
							if (!idbDatabase.objectStoreNames.contains('audio')) idbDatabase.createObjectStore('audio');
							if (!idbDatabase.objectStoreNames.contains('config')) idbDatabase.createObjectStore('config');
							if (!idbDatabase.objectStoreNames.contains('data')) idbDatabase.createObjectStore('data');
						};
					}).then(event => {
						if (!lib.db && !window.nodb) lib.db = event.target.result;
						return new Promise((resolve, reject) => {
							if (importselect.value != '选择系统文件') {
								game.readFileAsText('files/' + importselect.value, data => {
									if (!data) return reject('no data');
									try {
										data = JSON.parse(lib.init.decode(data));
										if (!data || typeof data != 'object') {
											throw ('err');
										}
										if (lib.db && (!data.config || !data.data)) {
											throw ('err');
										}
									}
									catch (e) {
										return reject(e);
									}
									return resolve(data);
								}, reject);
							} else {
								if (document.getElementById("fileNameInput")) {
									document.body.removeChild(document.getElementById("fileNameInput"));
								}
								var inputObj = document.createElement('input');
								inputObj.setAttribute('id', 'fileNameInput');
								inputObj.setAttribute('type', 'file');
								inputObj.setAttribute('name', 'fileNameInput');
								inputObj.setAttribute("style", 'visibility:hidden');
								document.body.appendChild(inputObj);
								inputObj.value;
								inputObj.click();
								inputObj.addEventListener('change', e => {
									if (!inputObj.files) return;
									var fileToLoad = inputObj.files[0];
									if (fileToLoad) {
										var fileReader = new FileReader();
										fileReader.onload = function (fileLoadedEvent) {
											var data = fileLoadedEvent.target.result;
											if (!data) return reject('no data');
											try {
												data = JSON.parse(lib.init.decode(data));
												if (!data || typeof data != 'object') {
													throw ('err');
												}
												if (lib.db && (!data.config || !data.data)) {
													throw ('err');
												}
											}
											catch (e) {
												return reject(e);
											}
											return resolve(data);
										};
										fileReader.readAsText(fileToLoad, "UTF-8");
									}
								});
							}
						});
					}).then(async data => {
						if (!lib.db) {
							var noname_inited = localStorage.getItem('noname_inited');
							var onlineKey = localStorage.getItem(lib.configprefix + 'key');
							localStorage.clear();
							if (noname_inited) {
								localStorage.setItem('noname_inited', noname_inited);
							}
							if (onlineKey) {
								localStorage.setItem(lib.configprefix + 'key', onlineKey);
							}
							for (var i in data) {
								localStorage.setItem(i, data[i]);
							}
						}
						else {
							for (var i in data.config) {
								await game.putDB('config', i, data.config[i]);
							}
							for (var i in data.data) {
								await game.putDB('data', i, data.data[i]);
							}
						}
						localStorage.setItem('noname_inited', window.tempSetNoname);
						alert('导入成功');
						setTimeout(() => {
							window.location.reload();
						}, 1000);
					}).catch(error => {
						alert('导入失败: ' + error);
						console.error(error);
					})
				}
			};

			/** 点击更新源按钮后根据req的结果更改显示文字 */
			function checkConnection() {
				zipDataDiv.innerHTML = `更新源: ${ site == site_c ? 'FAST GIT' : 'URC' }(点击此处更换更新源)`;
				// 赋值更换更新源的点击事件
				zipDataDiv.onclick = function() {
					if (this.classList.toggle('bluetext')) {
						site = site_urc;
						zipDataDiv.innerHTML = `更新源: URC`;
					}
					else {
						site = site_c;
						zipDataDiv.innerHTML = `更新源: GitHub Proxy`;
					}
					checkConnection();
				};
				button.innerHTML = '正在连接';
				button.classList.add('disabled');
				version.innerHTML = '';

				function failed() {
					button.classList.add('disabled');
					button.innerHTML = '连接失败';
					// 把下面显示版本的div改为重试a标签
					var a = document.createElement('div');
					a.innerText = '点击重试';
					a.style['text-decoration'] = 'underline';
					a.addEventListener('click', checkConnection);
					version.innerHTML = '';
					version.appendChild(a);
				}

				function success() {
					button.classList.remove('disabled');
					button.innerHTML = '下载无名杀';
					version.innerHTML = 'v' + window.noname_update.version;
				}

				/*if (!window.noname_android_extension) {
					// 默认从site_c拿最新版本号
					req(site_c + 'game/update.js', function () {
						req(my_ext_site + 'update.js', success, failed, 'noname_android_extension');
					}, failed, 'noname_update');
				} else {
					req(site_c + 'game/update.js', success, failed, 'noname_update');
				}*/
				req(site + 'game/update.js', success, failed, 'noname_update');
				return false;
			};

			/** 下载素材 */
			function update() {
				button.innerHTML = '获取下载文件';
				button.classList.add('disabled');
				version.innerHTML = '';
				req(`${ site }v${ version }game/source.js`, () => {
					button.remove();
					zipDataDiv.remove();
					help.remove();
					version.remove();
					
					const prompt = createElement('div', {
						innerText: '正在下载游戏文件',
						parentNode: document.body,
						style: {
							height: '40px',
							top: 'calc(50% - 40px)',
							lineHeight: '40px'
						}
					});

					const progress = createElement('div', {
						innerText: '0/0',
						parentNode: document.body,
						style: {
							top: 'calc(50% + 20px)',
							fontSize: '20px'
						}
					});

					let updates = window.noname_source_list;
					delete window.noname_source_list;

					let n1 = 0;
					let n2 = updates.length;
					progress.innerHTML = n1 + '/' + n2;

					function finish() {
						prompt.innerHTML = '游戏文件下载完毕';
						progress.innerHTML = n1 + '/' + n2;
						if (window.FileTransfer) {
							localStorage.setItem('noname_inited', dir);
						}
						else {
							localStorage.setItem('noname_inited', 'nodejs');
						}
						setTimeout(function () {
							let arr = [];
							function finishCopy() {
								window.location.reload();
							}
							navigator.notification.activityStart('正在检测是否有内置的SJ Settings扩展', '请耐心等待.....');
							/** 读取app的资源目录(在安卓是file:///android_asset/) */
							window.resolveLocalFileSystemURL(cordova.file.applicationDirectory,
								/** @param { DirectoryEntry } entry */
								entry => {
									entry.getDirectory('www/SJSettings', { create: false }, appDirEntry => {
										appDirEntry.getFile('extension.js', { create: false }, fileEntry => {
											navigator.notification.activityStop();
											getEntry().then(appEntry => {
												getEntry(appEntry, 'extension/SJ Settings', true).then(extEntry => {
													fileEntry.copyTo(extEntry, 'extension.js', () => {
														arr.push(true);
														if (arr.length == 3) finishCopy();
													}, () => {
														console.error('SJSettings/extension.js复制失败');
														alert('内置扩展SJ Settings添加失败，请进入游戏后手动安装扩展');
														finishCopy();
													});
												});
											});
											appDirEntry.getFile('extension.css', { create: false }, fileEntry => {
												getEntry().then(appEntry => {
													getEntry(appEntry, 'extension/SJ Settings', true).then(extEntry => {
														fileEntry.copyTo(extEntry, 'extension.css', () => {
															arr.push(true);
															if (arr.length == 3) finishCopy();
														}, () => {
															console.error('SJSettings/extension.css复制失败');
															alert('内置扩展SJ Settings添加失败，请进入游戏后手动安装扩展');
															finishCopy();
														});
													});
												});
											});
										}, error => {
											navigator.notification.activityStop();
											console.error('www/SJSettings/extension.js不存在: ' + error.code);
											alert('内置扩展SJ Settings添加失败，请进入游戏后手动安装扩展');
											finishCopy();
										});
										entry.getFile('www/game/config.js', { create: false }, fileEntry => {
											getEntry().then(appEntry => {
												getEntry(appEntry, 'game', true).then(extEntry => {
													fileEntry.copyTo(extEntry, 'config.js', () => {
														arr.push(true);
														if (arr.length == 3) finishCopy();
													}, () => {
														console.error('game/config.js复制失败');
														alert('内置扩展SJ Settings添加失败，请进入游戏后手动安装扩展');
														finishCopy();
													});
												});
											});
										}, error => {
											navigator.notification.activityStop();
											console.error('www/game/config.js: ' + error.code);
										});
									}, error => {
										navigator.notification.activityStop();
										console.error('www/SJSettings文件夹不存在: ' + error.code);
										// alert('请用其他方式打开zip文件，选择无名杀导入(诗笺版)，注: 万能导入无效');
									});
								});
						}, 1000);
					}

					function downloadFile(url, folder, onsuccess, onerror) {
						console.log(url);
						var fileTransfer = new FileTransfer();
						url = site + url;
						folder = dir + folder;
						fileTransfer.download(encodeURI(url), folder, onsuccess, onerror);
					};

					/** 下载失败重新下载文件 */
					function multiDownload(list, onsuccess, onerror, onfinish) {
						list = list.slice(0);
						let download = function (current) {
							if (current) {
								downloadFile(current, current, function () {
									if (onsuccess) onsuccess();
									download(list.shift());
								}, function (e) {
									// 跳过404
									if (e.http_status && (e.http_status == 404 || e.http_status == '404')) {
										if (onsuccess) onsuccess();
										download(list.shift());
									} else {
										if (onerror) {
											console.log(e);
											onerror(e);
										}
										setTimeout(() => {
											download(current);
										}, 350);
									}
								});
							}
							else {
								if (onfinish) onfinish();
							}
						}
						download(list.shift());
					};

					multiDownload(updates/* [] */, function () {
						n1++;
						progress.innerHTML = n1 + '/' + n2;
					}, null, function () {
						setTimeout(finish, 500);
					});
				}, () => {}, 'noname_source_list');
			}

			helpnodetext.innerHTML =
				`<div>
					<ol>
						<li>访问
						<a href="https://github.com/libccy/noname/releases/latest">网址1</a>，
						下载zip文件，或者通过其他方式(比如QQ群,QQ频道,微信公众号)下载最新的“无名杀完整包”。
						<li>使用QQ或者文件管理器选择zip文件,然后用其他方式-无名杀导入(诗笺版)进行导入
						<li>完成上述步骤后，<a href="javascript:localStorage.setItem(\'noname_inited\',window.tempSetNoname);window.location.reload()">点击此处</a></div>
					</ol>
				</div>`;
		},
	}
	app.initialize();
})();