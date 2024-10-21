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
		async onDeviceReady() {
			if (window.eruda) eruda.init();
			// /** 如果导入了完整包，就直接进入游戏 */
			// const getImportPackage = function () {
			// 	window.cordova && cordova.exec(result => {
			// 		console.log(result);
			// 		if (result && result.type == 'package') {
			// 			localStorage.setItem('noname_inited', result.message);
			// 			cordova.exec(() => location.reload(), () => location.reload(), 'FinishImport', 'importReceived', []);
			// 		}
			// 	}, () => {}, 'FinishImport', 'importReady', []);
			// };

			// getImportPackage();

			// /** 每次切换回应用触发检测 */
			// document.addEventListener('visibilitychange', () => {
			// 	if (document.hidden == false) getImportPackage();
			// });

			/** 添加app/index.css的样式 */
			let link = document.createElement('link');
			link.rel = 'stylesheet';
			link.href = 'app/index.css';
			document.head.appendChild(link);
			await new Promise((resolve, reject) => {
				link.onload = resolve;
				link.onerror = resolve;
			});

			// 加载jszip
			await new Promise((resolve, reject) => {
				var script = document.createElement('script');
				script.src = 'app/jszip.js';
				script.onload = resolve;
				script.onerror = reject;
				document.head.appendChild(script);
			});

			navigator.notification.activityStart('正在检测是否有内置zip资源', '请耐心等待.....');
			/** 读取app的资源目录(在安卓是file:///android_asset/) */
			window.resolveLocalFileSystemURL(cordova.file.applicationDirectory,
				/** @param { DirectoryEntry } entry */
				entry => {
					entry.getDirectory('www/app', { create: false }, appDirEntry => {
						appDirEntry.getFile('noname.zip', { create: false }, fileEntry => {
							navigator.notification.activityStop();
							const zipDataDiv = document.getElementById('changesite');
							if (zipDataDiv) zipDataDiv.innerText = '内置zip资源存在';
							// 跳转到java解压
							if (confirm('检测到内置压缩包存在，是否解压？\n注意: 此客户端不是懒人包的情况建议点击取消，然后在此页面在线下载最新离线包')) cordova.exec(() => { }, () => { }, 'FinishImport', 'assetZip', []);
							// else checkConnection();
						}, error => {
							navigator.notification.activityStop();
							console.error('www/app/noname.zip不存在: ' + error.code);
							const zipDataDiv = document.getElementById('changesite');
							if (zipDataDiv) zipDataDiv.innerText = 'www/app/noname.zip不存在';
							// alert('请用其他方式打开zip文件，选择无名杀导入(诗笺版)，注: 万能导入无效');
							// checkConnection();
						});
					}, error => {
						navigator.notification.activityStop();
						console.error('www/app文件夹不存在: ' + error.code);
						const zipDataDiv = document.getElementById('changesite');
						if (zipDataDiv) zipDataDiv.innerText = 'www/app文件夹不存在';
						// alert('请用其他方式打开zip文件，选择无名杀导入(诗笺版)，注: 万能导入无效');
						// checkConnection();
					});
				});

			var dir;
			var ua = navigator.userAgent.toLowerCase();
			if (ua.indexOf('android') != -1) {
				dir = cordova.file.externalApplicationStorageDirectory;
			}
			else if (ua.indexOf('iphone') != -1 || ua.indexOf('ipad') != -1) {
				dir = cordova.file.documentsDirectory;
			}
			// 如果没有noname_inited那就重试一次
			if (!sessionStorage.getItem('noname_inited')) {
				localStorage.setItem('noname_inited', dir);
				sessionStorage.setItem('noname_inited', dir);
				window.location.reload();
				return;
			}

			/**
			 * 自行扩展创建dom元素方法
			 * @template { keyof HTMLElementTagNameMap } k
			 * @param { k } tag 标签名
			 * @param { options } opts 选项
			 * @return { HTMLElementTagNameMap[k] } dom元素
			 */
			const createElement = function (tag, opts = {}) {
				const d = document.createElement(tag);
				for (const [key, value] of Object.entries(opts)) {
					switch (key) {
						case 'class':
							value.forEach(v => d.classList.add(v));
							break;
						case 'id':
							d.id = value;
							break;
						case 'innerHTML':
						case 'innerText':
							d[key] = value;
							break;
						case 'parentNode':
							value.appendChild(d);
							break;
						case 'listen':
							for (const evt in value) {
								if (typeof value[evt] == 'function') {
									d[evt] = value[evt];
								}
							}
							break;
						case 'style':
							for (const s in value) {
								d.style[s] = value[s];
							}
							break;
					}
				}
				return d;
			};

			/**
			 * 对比版本号
			 * @param { string } ver1 版本号1
			 * @param { string } ver2 版本号2
			 * @returns { -1 | 0 | 1 } -1为ver1 < ver2, 0为ver1 == ver2, 1为ver1 > ver2
			 * @throws {Error}
			 */
			const checkVersion = function (ver1, ver2) {
				if (typeof ver1 !== "string") ver1 = String(ver1);
				if (typeof ver2 !== "string") ver2 = String(ver2);

				// 移除 'v' 开头
				if (ver1.startsWith("v")) ver1 = ver1.slice(1);
				if (ver2.startsWith("v")) ver2 = ver2.slice(1);

				// 验证版本号格式
				if (/[^0-9.-]/i.test(ver1) || /[^0-9.-]/i.test(ver2)) {
					throw new Error("Invalid characters found in the version numbers");
				}

				/** @param { string } str */
				function* walk(str) {
					let part = "";
					for (const char of str) {
						if (char === "." || char === "-") {
							if (part) yield Number(part);
							part = "";
						} else {
							part += char;
						}
					}
					if (part) yield Number(part);
				}

				const iterator1 = walk(ver1);
				const iterator2 = walk(ver2);

				while (true) {
					const iter1 = iterator1.next();
					const iter2 = iterator2.next();
					let { value: item1 } = iter1;
					let { value: item2 } = iter2;

					// 如果任意一个迭代器已经没有剩余值，将该值视为0
					item1 = item1 === undefined ? 0 : item1;
					item2 = item2 === undefined ? 0 : item2;

					if (isNaN(item1) || isNaN(item2)) {
						throw new Error("Non-numeric part found in the version numbers");
					} else if (item1 > item2) {
						return 1;
					} else if (item1 < item2) {
						return -1;
					} else {
						if (iter1.done && iter2.done) break;
					}
				}

				// 若正常遍历结束，说明版本号相等
				return 0;
			};

			/**
			 * HTTP响应头中的Rate Limit相关信息：
			 * X-RateLimit-Limit: 请求总量限制
			 * X-RateLimit-Remaining: 剩余请求次数
			 * X-RateLimit-Reset: 限制重置时间（UTC时间戳）
			 */

			/** @type { HeadersInit } */
			const defaultHeaders = {
				Accept: "application/vnd.github.v3+json",
			};

			/**
			 *
			 * 获取指定仓库的tags
			 * @param { Object } options
			 * @param { string } [options.username = 'libccy'] 仓库拥有者
			 * @param { string } [options.repository = 'noname'] 仓库名称
			 * @param { string } [options.accessToken] 身份令牌
			 * @returns { Promise<{ commit: { sha: string, url: string }, name: string, node_id: string, tarball_url: string, zipball_url: string }[]> }
			 *
			 * @example
			 * ```js
			 * getRepoTags().then(tags => {
			 * 	console.log("All tags:", tags.map(tag => tag.name));
			 * 	// 获取最新tag（假设按时间顺序排列，最新tag在数组首位）
			 * 	const latestTag = tags[0].name;
			 * 	console.log("Latest tag:", latestTag);
			 * });
			 * ```
			 */
			const getRepoTags = async function (options = { username: "libccy", repository: "noname" }) {
				const { username = "libccy", repository = "noname", accessToken } = options;
				const headers = Object.assign({}, defaultHeaders);
				if (accessToken) {
					headers["Authorization"] = `token ${accessToken}`;
				}
				const url = `https://api.github.com/repos/${username}/${repository}/tags`;
				const response = await fetch(url, { headers });
				if (response.ok) {
					const data = await response.json();
					return data;
				} else {
					throw new Error(`Error fetching tags: ${response.statusText}`);
				}
			};

			/**
			 * 获取指定仓库的指定tags的描述
			 * @param { string } tagName tag名称
			 * @param { Object } options
			 * @param { string } [options.username = 'libccy'] 仓库拥有者
			 * @param { string } [options.repository = 'noname'] 仓库名称
			 * @param { string } [options.accessToken] 身份令牌
			 * @example
			 * ```js
			 * getRepoTagDescription('v1.10.10')
			 * 	.then(description => console.log(description))
			 * 	.catch(error => console.error('Failed to fetch description:', error));
			 * ```
			 */

			const getRepoTagDescription = async function (tagName, options = { username: "libccy", repository: "noname" }) {
				const { username = "libccy", repository = "noname", accessToken } = options;
				const headers = Object.assign({}, defaultHeaders);
				if (accessToken) {
					headers["Authorization"] = `token ${accessToken}`;
				}
				const apiUrl = `https://api.github.com/repos/${username}/${repository}/releases/tags/${tagName}`;
				const response = await fetch(apiUrl, { headers });
				if (!response.ok) {
					throw new Error(`Request failed with status ${response.status}`);
				}
				const releaseData = await response.json();
				return {
					/** @type { { browser_download_url: string, content_type: string, name: string, size: number }[] } tag额外上传的素材包 */
					assets: releaseData.assets,
					author: {
						/** @type { string } 用户名 */
						login: releaseData.author.login,
						/** @type { string } 用户头像地址 */
						avatar_url: releaseData.author.avatar_url,
						/** @type { string } 用户仓库地址 */
						html_url: releaseData.author.html_url,
					},
					/** @type { string } tag描述 */
					body: releaseData.body,
					// created_at: (new Date(releaseData.created_at)).toLocaleString(),
					/** @type { string } tag页面 */
					html_url: releaseData.html_url,
					/** @type { string } tag名称 */
					name: releaseData.name,
					/** 发布日期 */
					published_at: new Date(releaseData.published_at).toLocaleString(),
					/** @type { string } 下载地址 */
					zipball_url: releaseData.zipball_url,
				};
			};

			/**
			 * 请求一个文件而不是直接储存为文件，这样可以省内存空间
			 * @param { string } url
			 * @param { (receivedBytes: number, total?:number, filename?: string) => void } [onProgress]
			 * @param { RequestInit } [options={}]
			 * @example
			 * ```js
			 * await getRepoTagDescription('v1.10.10').then(({ zipball_url }) => request(zipball_url));
			 * ```
			 */
			const request = async function (url, onProgress, options = {}) {
				const response = await fetch(
					url,
					Object.assign(
						{
							// 告诉服务器我们期望得到范围请求的支持
							headers: { Range: "bytes=0-" },
						},
						options
					)
				);

				if (!response.ok) {
					throw new Error(`HTTP error! status: ${response.status}`);
				}

				// @ts-ignore
				let total = parseInt(response.headers.get("Content-Length"), 10);
				// 如果服务器未返回Content-Length，则无法准确计算进度
				// @ts-ignore
				if (isNaN(total)) total = null;
				// @ts-ignore
				const reader = response.body.getReader();
				let filename;
				try {
					// @ts-ignore
					filename = response.headers.get("Content-Disposition").split(";")[1].split("=")[1];
				} catch {
					/* empty */
				}
				let receivedBytes = 0;
				let chunks = [];

				while (true) {
					// 使用ReadableStream来获取部分数据并计算进度
					const { done, value } = await reader.read();

					if (done) {
						break;
					}

					chunks.push(value);
					receivedBytes += value.length;

					if (typeof onProgress == "function") {
						if (total) {
							const progress = (receivedBytes / total) * 100;
							onProgress(receivedBytes, progress, filename);
						} else {
							onProgress(receivedBytes, void 0, filename);
						}
					}
				}

				// 合并chunks并转换为Blob
				const blob = new Blob(chunks);

				// 仅做演示，打印已合并的Blob大小
				// console.log(`Download completed. Total size: ${parseSize(blob.size)}.`);

				return blob;
			};

			/**
			 *
			 * @param { string } [title]
			 * @param { string | number } [max]
			 * @param { string } [fileName]
			 * @param { string | number } [value]
			 * @returns { progress }
			 */
			const createProgress = function (title, max, fileName, value) {
				/** @type { progress } */
				const parent = createElement("div", {
					parentNode: document.body,
					style: {
						textAlign: "center",
						width: "300px",
						height: "150px",
						left: "calc(50% - 150px)",
						top: "auto",
						bottom: "calc(50% - 75px)",
						zIndex: "10",
						boxShadow: "rgb(0 0 0 / 40 %) 0 0 0 1px, rgb(0 0 0 / 20 %) 0 3px 10px",
						backgroundImage: "linear-gradient(rgba(0, 0, 0, 0.4), rgba(0, 0, 0, 0.4))",
						borderRadius: "8px",
						overflow: "hidden scroll",
					}
				});

				const container = createElement("div", {
					parentNode: parent,
					style: {
						position: "absolute",
						top: "0",
						left: "0",
						width: "100%",
						height: "100%",
					}
				});

				const caption = createElement("div", {
					parentNode: container,
					innerHTML: title,
					style: {
						position: "relative",
						paddingTop: "8px",
						fontSize: "20px",
					}
				});

				// createElement("br", {
				//     parentNode: container
				// });

				const tip = createElement("div", {
					parentNode: container,
					style: {
						position: "relative",
						paddingTop: "8px",
						fontSize: "20px",
						width: "100%",
						left: "0",
					}
				});

				const file = createElement("span", {
					parentNode: tip,
					innerHTML: fileName,
					style: {
						width: "100%",
						maxWidth: "100",
					}
				});

				createElement("br", {
					parentNode: tip
				});

				const index = createElement("span", {
					parentNode: tip,
					innerHTML: String(value || "0"),
				});

				createElement("span", {
					parentNode: tip,
					innerHTML: "/",
				});

				const maxSpan = createElement("span", {
					parentNode: tip,
					innerHTML: String(max || "未知"),
				});

				// createElement("br", {
				//     parentNode: container
				// });

				const progress = createElement("progress", {
					class: ["progress"],
					parentNode: container,
				});
				progress.setAttribute("value", value || "0");
				progress.setAttribute("max", max);

				parent.getTitle = () => caption.innerText;
				parent.setTitle = (title) => (caption.innerHTML = title);
				parent.getFileName = () => file.innerText;
				parent.setFileName = (name) => (file.innerHTML = name);
				parent.getProgressValue = () => progress.value;
				parent.setProgressValue = (value) => (progress.value = index.innerHTML = value);
				parent.getProgressMax = () => progress.max;
				parent.setProgressMax = (max) => (progress.max = maxSpan.innerHTML = max);
				parent.autoSetFileNameFromArray = (fileNameList) => {
					if (fileNameList.length > 2) {
						parent.setFileName(
							fileNameList
								.slice(0, 2)
								.concat(`......等${fileNameList.length - 2}个文件`)
								.join("<br/>")
						);
					} else if (fileNameList.length == 2) {
						parent.setFileName(fileNameList.join("<br/>"));
					} else if (fileNameList.length == 1) {
						parent.setFileName(fileNameList[0]);
					} else {
						parent.setFileName("当前没有正在下载的文件");
					}
				};
				return parent;
			};

			/**
			 * 从GitHub存储库检索最新版本(tag)，不包括特定tag。
			 * 
			 * 此函数从GitHub存储库中获取由所有者和存储库名称指定的tags列表，然后返回不是“v1998”的最新tag名称。
			 * @param {string} owner GitHub上拥有存储库的用户名或组织名称。
			 * @param {string} repo 要从中提取tag的存储库的名称。
			 * @returns {Promise<string>} 以最新版本tag的名称解析的promise，或者如果操作失败则以错误拒绝。
			 * @throws {Error} 如果获取操作失败或找不到有效tag，将抛出错误。
			 */
			const getLatestVersionFromGitHub = async function (owner = "libccy", repo = "noname") {
				const tags = await getRepoTags({
					username: owner,
					repository: repo,
				});

				for (const tag of tags) {
					const tagName = tag.name;
					if (tagName === "v1998") continue;
					try {
						checkVersion(tagName, '0');
						return tagName;
					} catch {
						// 非标准版本号
					}
				}

				throw new Error("No valid tags found in the repository");
			};

			// 设置触摸和鼠标监听
			const touchstart = function () {
				if (this.classList.contains('disabled')) return;
				this.style.transform = 'scale(0.98)';
			};
			const touchend = function () {
				this.style.transform = '';
			};

			/**
			 * 中心红色按钮
			 */
			const button = createElement('div', {
				id: 'button',
				class: ['disabled'],
				parentNode: document.body,
				innerText: '正在连接',
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
				innerText: '正在加载github资源...',
				style: {
					// opacity: '0.5',
				}
			});

			const versionnode = createElement('div', {
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
				innerHTML: `<div>
					<ol>
						<li>访问
						<a href="https://github.com/libccy/noname/releases/latest">网址1</a>，
						下载zip文件，或者通过其他方式(比如QQ群,QQ频道,微信公众号)下载最新的“无名杀完整包”。
						<li>使用QQ或者文件管理器将完整包导入进无名杀目录
						<li>完成上述步骤后，<a href="javascript:localStorage.setItem(\'noname_inited\',window.tempSetNoname);window.location.reload()">点击此处</a></div>
					</ol>
				</div>`
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

			document.ontouchmove = function (e) {
				e.preventDefault();
			};

			const checkConnection = async function () {
				button.innerHTML = '正在连接';
				button.classList.add('disabled');
				versionnode.innerHTML = '';
			};

			const update = function () {
				checkConnection()
					.then(() => getLatestVersionFromGitHub())
					.then(tagName => {
						return getRepoTagDescription(tagName);
					})
					.then(description => {
						button.classList.remove('disabled');
						button.innerHTML = '下载无名杀';
						versionnode.innerHTML = description.name;
						download(description);
					})
					.catch((e) => {
						alert("获取更新失败: " + e);
						button.classList.add('disabled');
						button.innerHTML = '连接失败';
					});
				/**
				 * @param {{ assets: any; author?: { login: string; avatar_url: string; html_url: string; }; body?: string; html_url?: string; name: any; published_at?: string; zipball_url: any; }} description
				 */
				const download = function (description) {
					button.remove();
					zipDataDiv.remove();
					help.remove();
					versionnode.remove();
					const progress = createProgress(
						"正在更新" + description.name,
						1,
						description.name + ".zip"
					);
					let unZipProgress;
					let url = description.zipball_url;
					if (Array.isArray(description.assets) && description.assets.length > 0) {
						const coreZipData = description.assets.find((v) => v.name == "noname.core.zip");
						// 自动下载离线包
						if (coreZipData) {
							url = "https://ghproxy.cc/" + coreZipData.browser_download_url;
						}
					}
					request(url, (receivedBytes, total, filename) => {
						if (typeof filename == "string") {
							progress.setFileName(filename);
						}
						let received = 0,
							max = 0;
						if (total) {
							max = +(total / (1024 * 1024)).toFixed(1);
						} else {
							max = 1000;
						}
						received = +(receivedBytes / (1024 * 1024)).toFixed(1);
						if (received > max) max = received;
						progress.setProgressMax(max);
						progress.setProgressValue(received);
					})
						.then(async (blob) => {
							progress.remove();
							const zip = new JSZip();
							zip.load(await blob.arrayBuffer());
							const entries = Object.entries(zip.files);
							let root;
							const hiddenFileFlags = [".", "_"];
							unZipProgress = createProgress(
								"正在解压" + progress.getFileName(),
								entries.length
							);
							let i = 0;
							for (const [key, value] of entries) {
								// 第一个是文件夹的话，就是根文件夹
								if (i == 0 && value.dir && !description.name.includes("noname.core.zip")) {
									root = key;
								}
								unZipProgress.setProgressValue(i++);
								const fileName =
									typeof root == "string" && key.startsWith(root)
										? key.replace(root, "")
										: key;
								if (hiddenFileFlags.includes(fileName[0])) continue;
								if (value.dir) {
									await game.promises.createDir(fileName);
									continue;
								}
								unZipProgress.setFileName(fileName);
								const [path, name] = [
									fileName.split("/").slice(0, -1).join("/"),
									fileName.split("/").slice(-1).join("/"),
								];
								await game.promises
									.writeFile(value.asArrayBuffer(), path, name)
									.catch(async (e) => {
										// 特殊处理
										if (
											name == "noname-server.exe" &&
											e.message.includes("resource busy or locked") &&
											location.protocol.startsWith("http")
										) {
											if (
												typeof window.require == "function" &&
												typeof window.process == "object" &&
												typeof window.__dirname == "string"
											) {
												return new Promise((resolve, reject) => {
													const cp = require("child_process");
													cp.exec(`taskkill /IM noname-server.exe /F`, (e) => {
														if (e) reject(e);
														else
															game.promises
																.writeFile(value.asArrayBuffer(), path, name)
																.then(() => {
																	cp.exec(
																		`start /b ${__dirname}\\noname-server.exe -platform=electron`,
																		() => { }
																	);
																	function loadURL() {
																		let myAbortController =
																			new AbortController();
																		let signal = myAbortController.signal;
																		setTimeout(
																			() => myAbortController.abort(),
																			2000
																		);
																		fetch(
																			`http://localhost:8089/app.html`,
																			{ signal }
																		)
																			.then(({ ok }) => {
																				if (ok) resolve(null);
																				else
																					throw new Error(
																						"fetch加载失败"
																					);
																			})
																			.catch(() => loadURL());
																	}
																	loadURL();
																})
																.catch(reject);
													});
												});
											}
										} else throw e;
									});
							}
							unZipProgress.remove();
							if (window.FileTransfer) {
								localStorage.setItem('noname_inited', dir);
							}
							else {
								localStorage.setItem('noname_inited', 'nodejs');
							}
							location.reload();
						})
						.catch((e) => {
							if (progress.parentNode) progress.remove();
							if (unZipProgress && unZipProgress.parentNode) unZipProgress.remove();
							throw e;
						});
				};
			};

			const game = {
				promises: {
					createDir(directory) {
						return new Promise((resolve, reject) => {
							// @ts-ignore
							game.createDir(directory, resolve, reject);
						});
					},
					writeFile(data, path, name) {
						return new Promise((resolve, reject) => {
							// @ts-ignore
							game.writeFile(data, path, name, resolve);
						}).then((result) => {
							return new Promise((resolve, reject) => {
								if (result instanceof Error) {
									reject(result);
								} else {
									resolve(result);
								}
							});
						});
					},
				},
				createDir(directory, successCallback, errorCallback) {
					if (window.cordova) {
						const paths = directory.split("/").reverse();
						new Promise((resolve, reject) =>
							window.resolveLocalFileSystemURL(dir, resolve, reject)
						).then(
							(directoryEntry) => {
								const redo = (entry) =>
									new Promise((resolve, reject) =>
										entry.getDirectory(
											paths.pop(),
											{
												create: true,
											},
											resolve,
											reject
										)
									).then((resolvedDirectoryEntry) => {
										if (paths.length) return redo(resolvedDirectoryEntry);
										if (typeof successCallback == "function") successCallback();
									});
								return redo(directoryEntry);
							},
							(reason) => {
								if (typeof errorCallback != "function") return Promise.reject(reason);
								errorCallback(reason);
							}
						);
					}
					else if (typeof require == "function") {
						const fs = require("fs");
						const path = require("path");
						const target = path.join(__dirname, directory);
						if (fs.existsSync(target)) {
							// 修改逻辑，路径存在且是文件才会报错
							if (!fs.statSync(target).isDirectory()) {
								if (typeof errorCallback == "function") errorCallback(new Error(`${target}文件已存在`));
								else if (typeof successCallback == "function") successCallback();
							}
							else if (typeof successCallback == "function") successCallback();
						} else if (checkVersion(process.versions.node, "10.12.0") > -1) {
							fs.mkdir(target, { recursive: true }, (e) => {
								if (e) {
									if (typeof errorCallback == "function") errorCallback(e);
									else throw e;
								} else {
									if (typeof successCallback == "function") successCallback();
								}
							});
						} else {
							const paths = directory.split("/").reverse();
							let _path = __dirname;
							const redo = () => {
								_path = path.join(_path, paths.pop());
								const exists = fs.existsSync(_path);
								const callback = (e) => {
									if (e) {
										if (typeof errorCallback != "function") throw e;
										errorCallback(e);
										return;
									}
									if (paths.length) return redo();
									if (typeof successCallback == "function") successCallback();
								};
								if (!exists) fs.mkdir(_path, callback);
								else callback();
							};
							redo();
						}
					}
				},
				writeFile(data, path, name, callback) {
					game.ensureDirectory(path, function () {
						if (Object.prototype.toString.call(data) == "[object File]") {
							var fileReader = new FileReader();
							fileReader.onload = function (e) {
								game.writeFile(this.result, path, name, callback);
							};
							fileReader.readAsArrayBuffer(data);
						}
						if (window.cordova) {
							window.resolveLocalFileSystemURL(
								dir + path,
								function (entry) {
									entry.getFile(
										name,
										{ create: true },
										function (fileEntry) {
											fileEntry.createWriter(function (fileWriter) {
												fileWriter.onwriteend = callback;
												fileWriter.write(data);
											}, callback);
										},
										callback
									);
								},
								callback
							);
						}
						else if (typeof require == "function") {
							const zip = new JSZip();
							zip.file("i", data);
							const fs = require("fs");
							fs.writeFile(
								__dirname + "/" + path + "/" + name,
								zip.files.i.asNodeBuffer(),
								null,
								callback
							);
						}
					});
				},
				ensureDirectory(list, callback, file) {
					const directoryList = typeof list == "string" ? [list] : list.slice().reverse(),
						num = file ? 1 : 0;
					let access;
					if (window.cordova) {
						access = (entry, directory, createDirectory) => {
							if (directory.length <= num) {
								createDirectory();
								return;
							}
							const str = directory.pop();
							return new Promise((resolve, reject) =>
								entry.getDirectory(
									str,
									{
										create: false,
									},
									resolve,
									reject
								)
							)
								.catch(
									() =>
										new Promise((resolve) =>
											entry.getDirectory(
												str,
												{
													create: true,
												},
												resolve
											)
										)
								)
								.then((directoryEntry) => access(directoryEntry, directory, createDirectory));
						};
					}
					else if (typeof require == "function") {
						access = (path, directory, createDirectory) => {
							const fs = require("fs");
							if (directory.length <= number) {
								createDirectory();
								return;
							}
							path += `/${directory.pop()}`;
							const fullPath = `${__dirname}${path}`;
							return new Promise((resolve, reject) =>
								fs.access(fullPath, (errnoException) => {
									if (errnoException) reject();
									else resolve();
								})
							)
								.catch(
									() =>
										new Promise((resolve, reject) =>
											fs.mkdir(fullPath, (errnoException) => {
												if (errnoException) reject(errnoException);
												else resolve();
											})
										)
								)
								.then(() => access(path, directory, createDirectory), console.log);
						};
					}
					return new Promise((resolve, reject) => {
						if (window.cordova) {
							window.resolveLocalFileSystemURL(
								dir,
								(rootEntry) => {
									const createDirectory = () => {
										if (directoryList.length)
											access(rootEntry, directoryList.pop().split("/").reverse(), createDirectory);
										if (typeof callback == "function") callback();
										resolve();
									};
									createDirectory();
								},
								reject
							)
						}
						else if (typeof require == "function") {
							const createDirectory = () => {
								if (directoryList.length)
									access("", directoryList.pop().split("/").reverse(), createDirectory);
								else {
									if (typeof callback == "function") callback();
									resolve();
								}
							};
							createDirectory();
						}
					});
				}
			};

			if (window.FileTransfer) {
				window.tempSetNoname = dir;
			} else {
				window.tempSetNoname = 'nodejs';
			}

			window.addEventListener("importPackage", e => {
				localStorage.setItem('noname_inited', dir);
				window.location.reload();
			}, false);

			checkConnection()
				.then(() => getLatestVersionFromGitHub())
				.then(tagName => getRepoTagDescription(tagName))
				.then(description => {
					button.classList.remove('disabled');
					button.innerHTML = '下载无名杀';
					versionnode.innerHTML = description.name;
					zipDataDiv.innerText = 'github资源加载成功';
				})
				.catch((e) => {
					alert("获取更新失败: " + e);
					button.classList.add('disabled');
					button.innerHTML = '连接失败';
					zipDataDiv.innerText = 'github资源加载失败';
				});
		},
	}
	app.initialize();
})();