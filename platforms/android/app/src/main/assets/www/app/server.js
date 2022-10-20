window.noname_shijianInterfaces = {
	wsserver: null,
	ipv4Addresses: null,
	port: parseInt(localStorage.getItem('noname_shijianWebSocketPort')),
	isRunning: false,

	bannedKeys: [],
	bannedIps: [],
	rooms: [],
	events: [],
	clients: {},
	bannedKeyWords: [],

	messages: {
		create: function (key, nickname, avatar, config, mode) {
			if (this.onlineKey != key) return;
			let util = noname_shijianInterfaces.util;
			let rooms = noname_shijianInterfaces.rooms;
			this.nickname = util.getNickname(nickname);
			this.avatar = avatar;
			let room = {};
			rooms.push(room);
			this.room = room;
			delete this.status;
			room.owner = this;
			room.key = key;
			this.sendl('createroom', key);
		},
		enter: function (key, nickname, avatar) {
			let util = noname_shijianInterfaces.util;
			let rooms = noname_shijianInterfaces.rooms;
			this.nickname = util.getNickname(nickname);
			this.avatar = avatar;
			let room = false;
			for (let i of rooms) {
				if (i.key == key) {
					room = i;
					break;
				}
			}
			if (!room) {
				this.sendl('enterroomfailed');
				return;
			}
			this.room = room;
			delete this.status;
			if (room.owner) {
				if (room.servermode && !room.owner._onconfig && config && mode) {
					room.owner.sendl('createroom', index, config, mode);
					room.owner._onconfig = this;
					room.owner.nickname = noname_shijianInterfaces.util.getNickname(nickname);
					room.owner.avatar = avatar;
				}
				else if (!room.config || (room.config.gameStarted && (!room.config.observe || !room.config.observeReady))) {
					this.sendl('enterroomfailed');
				}
				else {
					this.owner = room.owner;
					this.owner.sendl('onconnection', this.wsid);
				}
				util.updaterooms();
			}
		},
		changeAvatar: function (nickname, avatar) {
			let util = noname_shijianInterfaces.util;
			this.nickname = util.getNickname(nickname);
			this.avatar = avatar;
			util.updateclients();
		},
		server: function (cfg) {
			let util = noname_shijianInterfaces.util;
			let rooms = noname_shijianInterfaces.rooms;
			if (cfg) {
				this.servermode = true;
				let room = rooms[cfg[0]];
				if (!room || room.owner) {
					this.sendl('reloadroom', true);
				} else {
					room.owner = this;
					this.room = room;
					this.nickname = util.getNickname(cfg[1]);
					this.avatar = cfg[2];
					this.sendl('createroom', cfg[0], {}, 'auto');
				}
			} else {
				for (let i = 0; i < rooms.length; i++) {
					if (!rooms[i].owner) {
						rooms[i].owner = this;
						rooms[i].servermode = true;
						this.room = rooms[i];
						this.servermode = true;
						break;
					}
				}
				util.updaterooms();
			}
		},
		key: function (id) {
			if (!id || typeof id != 'object') {
				this.sendl('denied', 'key');
				this.close();
				clearTimeout(this.keyCheck);
				delete this.keyCheck;
				return;
			}
			else if (noname_shijianInterfaces.bannedKeys.indexOf(id[0]) != -1) {
				bannedIps.push(this._socket.remoteAddress);
				this.close();
			}
			this.onlineKey = id[0];
			clearTimeout(this.keyCheck);
			delete this.keyCheck;
		},
		events: function (cfg, id, type) {
			let util = noname_shijianInterfaces.util;
			if (noname_shijianInterfaces.bannedKeys.indexOf(id) != -1 || typeof id != 'string' || this.onlineKey != id) {
				bannedIps.push(this._socket.remoteAddress);
				console.log(id, this._socket.remoteAddress);
				this.close();
				return;
			}
			let changed = false;
			let time = (new Date()).getTime();
			if (cfg && id) {
				if (typeof cfg == 'string') {
					for (let i = 0; i < events.length; i++) {
						if (events[i].id == cfg) {
							if (type == 'join') {
								if (events[i].members.indexOf(id) == -1) {
									events[i].members.push(id);
								}
								changed = true;
							}
							else if (type == 'leave') {
								let index = events[i].members.indexOf(id);
								if (index != -1) {
									events[i].members.splice(index, 1);
									if (events[i].members.length == 0) {
										events.splice(i--, 1);
									}
								}
								changed = true;
							}
						}
					}
				}
				else if (cfg.hasOwnProperty('utc') &&
					cfg.hasOwnProperty('day') &&
					cfg.hasOwnProperty('hour') &&
					cfg.hasOwnProperty('content')) {
					if (events.length >= 20) {
						this.sendl('eventsdenied', 'total');
					}
					else if (cfg.utc <= time) {
						this.sendl('eventsdenied', 'time');
					}
					else if (util.isBanned(cfg.content)) {
						this.sendl('eventsdenied', 'ban');
					}
					else {
						cfg.nickname = util.getNickname(cfg.nickname);
						cfg.avatar = cfg.nickname || 'caocao';
						cfg.creator = id;
						cfg.id = util.getid();
						cfg.members = [id];
						events.unshift(cfg);
						changed = true;
					}
				}
			}
			if (changed) {
				util.updateevents();
			}
		},
		config: function (config) {
			let room = this.room;
			let util = noname_shijianInterfaces.util;
			let clients = noname_shijianInterfaces.clients;
			if (room && room.owner == this) {
				if (room.servermode) {
					room.servermode = false;
					if (this._onconfig) {
						if (clients[this._onconfig.wsid]) {
							this._onconfig.owner = this;
							this.sendl('onconnection', this._onconfig.wsid);
						}
						delete this._onconfig;
					}
				}
				room.config = config;
			}
			util.updaterooms();
		},
		status: function (str) {
			let util = noname_shijianInterfaces.util;
			if (typeof str == 'string') {
				this.status = str;
			}
			else {
				delete this.status;
			}
			util.updateclients();
		},
		send: function (id, message) {
			let clients = noname_shijianInterfaces.clients;
			if (clients[id] && clients[id].owner == this) {
				try {
					clients[id].send(message);
				} catch (e) {
					clients[id].close();
				}
			}
		},
		close: function (id) {
			let clients = noname_shijianInterfaces.clients;
			if (clients[id] && clients[id].owner == this) {
				clients[id].close();
			}
		},
	},

	util: {
		createWs: function (uuid, ip) {
			return {
				uuid, ip,
				send(msg) {
					let wsserver = noname_shijianInterfaces.wsserver;
					wsserver.send({ 'uuid': this.uuid }, msg);
				},
				close() {
					let wsserver = noname_shijianInterfaces.wsserver;
					wsserver.close({ 'uuid': this.uuid });
				}
			}
		},
		getNickname: function (str) {
			return typeof str == 'string' ? (str.slice(0, 12)) : '无名玩家';
		},
		isBanned: function (str) {
			for (var i of noname_shijianInterfaces.bannedKeyWords) {
				if (str.indexOf(i) != -1) return true;
			}
			return false;
		},
		sendl: function () {
			var args = [];
			for (var i = 0; i < arguments.length; i++) {
				args.push(arguments[i]);
			}
			try {
				this.send(JSON.stringify(args));
			}
			catch (e) {
				this.close();
			}
		},
		getid: function () {
			return (Math.floor(1000000000 + 9000000000 * Math.random())).toString();
		},
		getroomlist: function () {
			let clients = noname_shijianInterfaces.clients;
			let rooms = noname_shijianInterfaces.rooms;
			let roomlist = [];
			for (var i = 0; i < rooms.length; i++) {
				rooms[i]._num = 0;
			}
			for (var i in clients) {
				if (clients[i].room && !clients[i].servermode) {
					clients[i].room._num++;
				}
			}
			for (var i = 0; i < rooms.length; i++) {
				if (rooms[i].servermode) {
					roomlist[i] = 'server';
				}
				else if (rooms[i].owner && rooms[i].config) {
					if (rooms[i]._num == 0) {
						rooms[i].owner.sendl('reloadroom');
					}
					roomlist.push([rooms[i].owner.nickname, rooms[i].owner.avatar,
					rooms[i].config, rooms[i]._num, rooms[i].key]);
				}
				delete rooms[i]._num;
			}
			return roomlist;
		},
		getclientlist: function () {
			let clients = noname_shijianInterfaces.clients;
			var clientlist = [];
			for (var i in clients) {
				clientlist.push([clients[i].nickname, clients[i].avatar, !clients[i].room, clients[i].status, clients[i].wsid, clients[i].onlineKey]);
			}
			return clientlist;
		},
		updaterooms: function () {
			let util = noname_shijianInterfaces.util;
			let roomlist = util.getroomlist();
			let clientlist = util.getclientlist();
			let clients = noname_shijianInterfaces.clients;
			for (var i in clients) {
				if (!clients[i].room) {
					clients[i].sendl('updaterooms', roomlist, clientlist);
				}
			}
		},
		updateclients: function () {
			let util = noname_shijianInterfaces.util;
			var clientlist = util.getclientlist();
			let clients = noname_shijianInterfaces.clients;
			for (var i in clients) {
				if (!clients[i].room) {
					clients[i].sendl('updateclients', clientlist);
				}
			}
		},
		checkevents: function () {
			let events = noname_shijianInterfaces.events;
			if (events.length) {
				var time = (new Date()).getTime();
				for (var i = 0; i < events.length; i++) {
					if (events[i].utc <= time) {
						events.splice(i--, 1);
					}
				}
			}
			return events;
		},
		updateevents: function () {
			let util = noname_shijianInterfaces.util;
			let clients = noname_shijianInterfaces.clients;
			util.checkevents();
			for (var i in clients) {
				if (!clients[i].room) {
					clients[i].sendl('updateevents', events);
				}
			}
		}
	}
};

document.addEventListener('deviceready', () => {

	noname_shijianInterfaces.wsserver = cordova.plugins.wsserver;

	noname_shijianInterfaces.start = function (port) {
		if (typeof port != "number") return false;
		if (this.isRunning) {
			this.isRunning = false;
			this.reload();
			return false;
		}
		this.isRunning = true;
		this.wsserver.start(port, {
			// WebSocket Server handlers
			'onFailure': (addr, port, reason) => {
				console.log('Stopped listening on %s:%d. Reason: %s', addr, port, reason);
			},
			// WebSocket Connection handlers
			'onOpen': (conn) => {
				/* conn: {
				'uuid' : '8e176b14-a1af-70a7-3e3d-8b341977a16e',
				'remoteAddr' : '192.168.1.10',
				'httpFields' : {...},
				'resource' : '/?param1=value1&param2=value2'
				} */
				console.log(conn);
				console.log('A user connected from %s', conn.remoteAddr);

				const { bannedIps, clients, util } = window.noname_shijianInterfaces;
				const { uuid, remoteAddr: ip } = conn;
				const ws = util.createWs(uuid, ip);
				ws.sendl = util.sendl;

				if (bannedIps.indexOf(ip) != -1) {
					ws.sendl('denied', 'banned');
					setTimeout(() => ws.close(), 500);
					return;
				}

				ws.keyCheck = setTimeout(() => {
					ws.sendl('denied', 'key');
					setTimeout(() => ws.close(), 500);
				}, 2000);

				// ws.wsid = util.getid();
				ws.wsid = uuid;
				clients[ws.wsid] = ws;
				ws.sendl('roomlist', util.getroomlist(), util.checkevents(), util.getclientlist(ws), ws.wsid);
				ws.heartbeat = setInterval(() => {
					if (ws.beat) {
						ws.close();
						clearInterval(ws.heartbeat);
					} else {
						ws.beat = true;
						try {
							ws.send('heartbeat');
						} catch (e) {
							ws.close();
						}
					}
				}, 60000);

			},
			'onMessage': (conn, message) => {
				const { bannedIps, clients, util, messages } = window.noname_shijianInterfaces;
				const { uuid, remoteAddr: ip } = conn;
				const ws = clients[uuid];

				// console.log(ip, message); // msg can be a String (text message) or ArrayBuffer (binary message)

				if (!clients[uuid]) return;
				if (message == 'heartbeat') {
					ws.beat = false;
				}
				else if (ws.owner) {
					ws.owner.sendl('onmessage', ws.wsid, message);
				}
				else {
					let arr;
					try {
						arr = JSON.parse(message);
						if (!Array.isArray(arr)) {
							throw ('err');
						}
					}
					catch (e) {
						ws.sendl('denied', 'banned');
						return;
					}
					if (arr.shift() == 'server') {
						let type = arr.shift();
						if (messages[type]) {
							messages[type].apply(ws, arr);
						}
					}
				}
			},
			'onClose': (conn, code, reason, wasClean) => {
				console.log('A user disconnected from %s', conn.remoteAddr);
				console.log('code: %s, reason: %s, wasClean: %s', code, reason, wasClean);

				const { clients, util, rooms } = window.noname_shijianInterfaces;
				const { uuid, remoteAddr: ip } = conn;
				const ws = clients[uuid];

				for (let i = 0; i < rooms.length; i++) {
					if (rooms[i].owner == ws) {
						for (let j in clients) {
							if (clients[j].room == rooms[i] && clients[j] != ws) {
								clients[j].sendl('selfclose');
								// clients[j].close();
								// delete clients[j];
							}
						}
						rooms.splice(i--, 1);
					}
				}
				if (clients[ws.wsid]) {
					if (ws.owner) {
						ws.owner.sendl('onclose', ws.wsid);
					}
					delete clients[ws.wsid];
				}
				if (ws.room) util.updaterooms();
				else util.updateclients();
			},
			// Other options
			// 'origins': ['file://'], // validates the 'Origin' HTTP Header.
			// 'protocols': ['my-protocol-v1', 'my-protocol-v2'], // validates the 'Sec-WebSocket-Protocol' HTTP Header.
			'tcpNoDelay': true // disables Nagle's algorithm.
		}, (addr, port) => {
			this.wsserver.getInterfaces(result => {
				this.port = port;
				console.log('Listening on %s:%d', result.wlan0.ipv4Addresses[0], port);
				this.ipv4Addresses = result.wlan0.ipv4Addresses[0];
				if (sessionStorage.getItem('webSocketSeverTip') != 'true' && result.wlan0.ipv4Addresses[0] && port) {
					sessionStorage.setItem('webSocketSeverTip', 'true');
					navigator.notification.alert(`本地服务器开启成功：${result.wlan0.ipv4Addresses[0]}:${port}`);
				}
			});
		}, (reason) => {
			console.log('Did not start. Reason: %s', reason);
			this.reload();
		});
	}.bind(window.noname_shijianInterfaces);

	noname_shijianInterfaces.stop = function () { 
		this.wsserver.stop((addr, port) => {
			console.log('Stopped listening on %s:%d', addr, port);
		});
	}.bind(window.noname_shijianInterfaces);

	noname_shijianInterfaces.reload = function () {
		this.wsserver.stop((addr, port) => console.log('Stopped listening on %s:%d', addr, port));
		setTimeout(() => this.start(this.port || 8080), 150);
	}.bind(window.noname_shijianInterfaces);

	if (localStorage.getItem('noname_shijianWebSocketOpen') == 'true') {
		noname_shijianInterfaces.start(noname_shijianInterfaces.port || 8080);
	}	
}, false);