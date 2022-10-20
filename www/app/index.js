'use strict';
(function() {
	if (localStorage.getItem('noname_inited')) return;
	var app = {
		initialize: function() {
			this.bindEvents();
		},
		bindEvents: function() {
			if (window.require && window.__dirname) {
				this.onDeviceReady();
			} else {
				var script = document.createElement('script');
				script.src = 'cordova.js';
				document.head.appendChild(script);
				document.addEventListener('deviceready', this.onDeviceReady, false);
			}
		},
		onDeviceReady: function() {
		    cordova.exec(() => {
        	    console.log('success');
        	    console.log(localStorage.getItem('noname_inited'));
            }, e => {
                console.error(e);
        	    console.log(localStorage.getItem('noname_inited'));
            }, 'FinishImport', 'ready', []);

			/** github镜像网址 */
			var site_g = 'https://raw.fastgit.org/libccy/noname/master/';
			/** 苏婆config镜像网址 */
			var site_c = 'https://nakamurayuri.coding.net/p/noname/d/noname/git/raw/master/';
			/** 星城玄武镜像网址 */
			var site_xw = 'https://kuangthree.coding.net/p/nonamexwjh/d/nonamexwjh/git/raw/master/';
			/** 目前的下载地址，默认为github镜像网址 */
			var site = site_g;
			
			
			/** @type { HTMLDivElement } 下载按钮 */
			var button;
			/**
			 * @type { HTMLDivElement } 更换更新源
			 */
			var changesite;
			/**
			 * @type { HTMLDivElement } 帮助（无法在线下载？）
			 */
			var help;
			/**
			 * @type { string } 无名杀版本
			 */
			var version;
			/**
			 * @type { HTMLDivElement } 显示无名杀版本号的div
			 */
			var versionnode;
			
			/**
			 * @description 请求指定网址的js文件并执行
			 * @param { string } url 请求指定网址的js文件
			 * @param { VoidFunction } onload 请求成功后执行回调
			 * @param { VoidFunction } onerror 请求失败后执行回调
			 * @param { any } [target] 用window[target]是否存在来判断js是否加载成功
			 */
			var req = function(url, onload, onerror, target) {
				fetch(url)
					.then(res => {
						return res.text();
					})
					.then(responseText => {
						eval(responseText);
						if (target && !window[target]) {
							throw ('err');
						}
						onload();
						if (target) {
							delete window[target];
						}
					})
					.catch(onerror);
			}

			/** 点击按钮后根据req的结果更改显示文字 */
			var checkConnection = function() {
				button.innerHTML = '正在连接';
				button.classList.add('disabled');
				versionnode.innerHTML = '';
				req(site + 'game/update.js', function() {
					button.classList.remove('disabled');
					button.innerHTML = '下载无名杀';
					version = window.noname_update.version;
					versionnode.innerHTML = 'v' + version;
				}, function() {
					button.classList.add('disabled');
					button.innerHTML = '连接失败';
					// 把下面显示版本的div改为重试a标签
					var a = document.createElement('div');
					a.innerText = '点击重试';
					a.style['text-decoration'] = 'underline';
					a.addEventListener('click', checkConnection);
					versionnode.innerHTML = '';
					versionnode.appendChild(a);
				}, 'noname_update');
				return false;
			};

			/** 
			 * @type { string }
			 * 文件夹
			 **/
			var dir;
			var ua = navigator.userAgent.toLowerCase();
			if (ua.indexOf('android') != -1) {
				dir = cordova.file.externalApplicationStorageDirectory;
			} else if (ua.indexOf('iphone') != -1 || ua.indexOf('ipad') != -1) {
				dir = cordova.file.documentsDirectory;
			}

			/** 开始下载游戏文件 */
			var update = function() {
				button.innerHTML = '正在连接';
				button.classList.add('disabled');
				versionnode.innerHTML = '';
				req(site + 'game/source.js', function() {
					button.remove();
					changesite.remove();
					help.remove();
					versionnode.remove();

					var prompt = document.createElement('div');
					prompt.style.height = '40px';
					prompt.style.top = 'calc(50% - 40px)';
					prompt.style.lineHeight = '40px';
					prompt.innerHTML = '正在下载游戏文件';
					document.body.appendChild(prompt);

					var progress = document.createElement('div');
					progress.style.top = 'calc(50% + 20px)';
					progress.style.fontSize = '20px';
					progress.innerHTML = '0/0';
					document.body.appendChild(progress);

					var updates = window.noname_source_list;
					delete window.noname_source_list;

					var n1 = 0;
					var n2 = updates.length;
					progress.innerHTML = n1 + '/' + n2;
					var finish = function() {
						prompt.innerHTML = '游戏文件下载完毕';
						progress.innerHTML = n1 + '/' + n2;
						if (window.FileTransfer) {
							localStorage.setItem('noname_inited', dir);
						} else {
							localStorage.setItem('noname_inited', 'nodejs');
						}
						setTimeout(function() {
							window.location.reload();
						}, 1000);
					}
					/**
					 * @type { (url: string, folder: string, onsuccess: VoidFunction, onerror: (e?: FileTransferError | Error) => void) => void } downloadFile
					 * @param url 下载文件的url地址
					 * @param folder 下载文件的父目录
					 * @param onsuccess 成功回调
					 * @param onerror 失败回调
					 */
					var downloadFile;
					if (window.FileTransfer) {
						downloadFile = function(url, folder, onsuccess, onerror) {
							var fileTransfer = new FileTransfer();
							url = site + url;
							folder = dir + folder;
							fileTransfer.download(encodeURI(url), folder, onsuccess, onerror);
						};
					} else {
						var fs = require('fs');
						var http = require('https');
						downloadFile = function(url, folder, onsuccess, onerror) {
							url = site + url;
							var dir = folder.split('/');
							var str = '';
							var download = function() {
								try {
									var file = fs.createWriteStream(__dirname + '/' + folder);
								} catch (e) {
									onerror();
								}
								var opts = require('url').parse(encodeURI(url));
								// @ts-ignore
								opts.headers = {
									'User-Agent': 'AppleWebkit'
								};
								
								var request = http.get(opts, function(response) {
									console.log({
										url: url.slice(url.lastIndexOf('/') + 1),
										statusCode: response.statusCode
									});
									if (response.statusCode != 200) return onerror(new Error(response.statusCode));
									var stream = response.pipe(file);
									stream.on('finish', onsuccess);
									stream.on('error', onerror);
								});
								request.on('error', onerror);
							}
							/** 自动创建文件夹，然后继续下载 */
							var access = function() {
								if (dir.length <= 1) {
									download();
								} else {
									str += '/' + dir.shift();
									fs.access(__dirname + str, function(e) {
										if (e) {
											return fs.mkdir(__dirname + str, e => {
												if (e) return onerror(e);
												access();
											});
										}
										access();
									});
								}
							}
							access();
						};
					}
					/**
					 * 
					 * @param { string[] } list 下载列表
					 * @param { VoidFunction } onsuccess 成功回调
					 * @param { (e?: FileTransferError | Error) => void } onerror 
					 * @param { VoidFunction } onfinish 
					 */
					var multiDownload = function(list, onsuccess, onerror, onfinish) {
						list = list.slice(0);
						var download = function() {
							if (list.length) {
								var current = list.shift();
								var reload = () => {
									downloadFile(current, current, function () {
										if (onsuccess) onsuccess();
										download();
									}, function (e) {
										if (onerror) onerror(e);
										//download();
										reload();
									});
								};
								reload();
							} else {
								if (onfinish) onfinish();
							}
						}
						download();
					};
					multiDownload(updates, function() {
						n1++;
						progress.innerHTML = n1 + '/' + n2;
					}, function(e) {
						console.error(e);
					}, function() {
						setTimeout(finish, 500);
					});
				}, function() {
					button.classList.add('disabled');
					button.innerHTML = '连接失败';
					// 把下面显示版本的div改为重试a标签
					var a = document.createElement('div');
					a.innerText = '点击重试';
					a.style['text-decoration'] = 'underline';
					a.addEventListener('click', update);
					versionnode.innerHTML = '';
					versionnode.appendChild(a);
				}, 'noname_source_list');
			}

			/** 添加app/index.css的样式 */
			var link = document.createElement('link');
			link.rel = 'stylesheet';
			link.href = 'app/index.css';
			document.head.appendChild(link);

			button = document.createElement('div');
			button.id = 'button';

			// 设置触摸和鼠标监听 
			var touchstart = function(e) {
				if (this.classList.contains('disabled')) return;
				this.style.transform = 'scale(0.98)';
			};
			var touchend = function() {
				this.style.transform = '';
			};
			button.ontouchstart = touchstart;
			button.ontouchend = touchend;
			button.onmousedown = touchstart;
			button.onmouseup = touchend;
			button.onmouseleave = touchend;
			button.onclick = function() {
				if (button.classList.contains('disabled')) return;
				update();
			};
			document.body.appendChild(button);
			document.ontouchmove = function(e) {
				e.preventDefault();
			};

			changesite = document.createElement('div');
			changesite.id = 'changesite';
			changesite.innerHTML = '下载源: GitHub镜像';
			document.body.appendChild(changesite);

			versionnode = document.createElement('div');
			versionnode.id = 'version';
			help = document.createElement('div');
			help.id = 'help';
			help.innerHTML = '无法在线下载？';
			var helpnode = document.createElement('div');
			helpnode.id = 'noname_init_help';
			var helpnodetext = document.createElement('div');

			/**
			 * 根据不同操作平台返回不同路径（不考虑mac和ios）
			 * @returns { { winOrLinux: string, android:string } }
			 */
			var getAppPath = function() {
				if (window.require && window.__dirname) {
					return {
						winOrLinux: require('path').join(__dirname),
						android: 'android/data/com.widget.noname'
					};
				} else if (window.cordova && window.cordova.platformId == 'android'){
					return {
						winOrLinux: 'resources/app',
						android: cordova.file.externalApplicationStorageDirectory
					};
				} else {
					return {
						winOrLinux: 'resources/app',
						android: 'android/data/com.widget.noname'
					};
				}
			};
			var winOrLinux = getAppPath().winOrLinux;
			var android = getAppPath().android;
			helpnodetext.innerHTML =
				`<div>
					<ol>
						<li>访问
						<a href="https://hub.fastgit.org/libccy/noname/archive/refs/heads/master.zip">https://hub.fastgit.org/libccy/noname/archive/refs/heads/master.zip</a>，
						或者
						<a href="https://hub.fastgit.xyz/libccy/noname/archive/refs/heads/master.zip">https://hub.fastgit.xyz/libccy/noname/archive/refs/heads/master.zip</a>
						下载zip文件
						<li>解压后将noname-master目录内的所有文件放入对应文件夹：<br>windows/linux：${ winOrLinux }<br>mac：（右键显示包内容）contents/resources/app<br>android：${android }<br>ios：documents（itunes—应用—文件共享）
						<li>完成上述步骤后，<a href="javascript:localStorage.setItem(\'noname_inited\',window.tempSetNoname);window.location.reload()">点击此处</a>
					</ol>
				</div>`;

			helpnode.appendChild(helpnodetext);
			help.onclick = function() {
				document.body.appendChild(helpnode);
			}

			var back = document.createElement('div');
			back.id = 'back';
			back.innerHTML = '返回';
			back.onclick = function() {
				helpnode.remove();
			};
			helpnode.appendChild(back);
			document.body.appendChild(help);
			document.body.appendChild(versionnode);
			checkConnection();

			if (window.FileTransfer) {
				window.tempSetNoname = dir;
			} else {
				window.tempSetNoname = 'nodejs';
			}
			
			changesite.addEventListener('click', function () {
				// 三个下载源挨个更换
				if (site == site_c) {
					site = site_g;
					this.innerHTML = '下载源: GitHub镜像';
				} else if (site == site_g) {
					site = site_xw;
					this.innerHTML = '下载源: 玄武镜像';
				} else {
					site = site_c;
					this.innerHTML = '下载源: Coding';
				}
				checkConnection();
			});
		}
	};

	app.initialize();
}())
