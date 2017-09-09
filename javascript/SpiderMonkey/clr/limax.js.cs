namespace limax.script.codes { internal partial struct js{ public static string limax =
"function Limax(initializer, cache) {\n"+
"	function Parser() {\n"+
"		var s, p, d, _ = {};\n"+
"		_.I = function() {\n"+
"			var q = s.indexOf(\":\", p);\n"+
"			var r = parseInt(s.substring(p, q), 36);\n"+
"			p = q + 1;\n"+
"			return r;\n"+
"		}\n"+
"		_.J = function() {\n"+
"			var q = s.indexOf(\":\", p);\n"+
"			var r = s.substring(p, q);\n"+
"			p = q + 1;\n"+
"			return r;\n"+
"		}\n"+
"		_.F = function() {\n"+
"			var q = s.indexOf(\":\", p);\n"+
"			var r = parseFloat(s.substring(p, q));\n"+
"			p = q + 1;\n"+
"			return r;\n"+
"		}\n"+
"		_.B = function() {\n"+
"			return s.charAt(p++) == \"T\";\n"+
"		}\n"+
"		_.D = function() {\n"+
"			return _.U;\n"+
"		}\n"+
"		_.S = function() {\n"+
"			var l = _.I();\n"+
"			return s.substring(p, p += l);\n"+
"		}\n"+
"		_.O = function() {\n"+
"			var l = _.I();\n"+
"			var q = p;\n"+
"			var r = new Array(l);\n"+
"			for (var i = 0; i < l; i++)\n"+
"				r[i] = parseInt(s.substring(q, q += 2), 16);\n"+
"			p = q;\n"+
"			return r;\n"+
"		}\n"+
"		_.P = function() {\n"+
"			var r = [];\n"+
"			while (s.charAt(p) != \":\")\n"+
"				r.push(_.R());\n"+
"			p++;\n"+
"			return r;\n"+
"		}\n"+
"		_.L = function() {\n"+
"			if (s.charAt(p) != \"?\")\n"+
"				return _.P()\n"+
"			var r = {}\n"+
"			while (s.charAt(p) != \":\") {\n"+
"				var o = _.R()\n"+
"				for ( var i in o)\n"+
"					r[i] = o[i];\n"+
"			}\n"+
"			p++;\n"+
"			return r;\n"+
"		}\n"+
"		_.W = function() {\n"+
"			var r = _.P()\n"+
"			return function() {\n"+
"				return r;\n"+
"			}\n"+
"		}\n"+
"		_.X = function() {\n"+
"			var r = {};\n"+
"			r.f = d[_.I()];\n"+
"			r.v = _.R();\n"+
"			return r;\n"+
"		}\n"+
"		_.Y = function() {\n"+
"			var r = {};\n"+
"			r.f = d[_.I()];\n"+
"			r.a = _.R();\n"+
"			r.r = _.R();\n"+
"			return r;\n"+
"		}\n"+
"		_.Z = function() {\n"+
"			var r = {};\n"+
"			r.f = d[_.I()];\n"+
"			r.c = _.R();\n"+
"			r.r = _.R();\n"+
"			return r;\n"+
"		}\n"+
"		_.M = function() {\n"+
"			var r = new Map();\n"+
"			while (s.charAt(p) != \":\")\n"+
"				r.set(_.R(), _.R());\n"+
"			p++;\n"+
"			return r;\n"+
"		}\n"+
"		_.U = function() {\n"+
"		}\n"+
"		_['?'] = function() {\n"+
"			var q = s.indexOf(\"?\", p);\n"+
"			var k = d[parseInt(s.substring(p, q), 36)];\n"+
"			p = q + 1;\n"+
"			var r = {};\n"+
"			r[k] = _.R();\n"+
"			return r;\n"+
"		}\n"+
"		return _.R = function() {\n"+
"			if (arguments.length == 0)\n"+
"				return _[s.charAt(p++)]();\n"+
"			var arg = arguments[0];\n"+
"			if (typeof arg == 'string') {\n"+
"				s = arg;\n"+
"				p = 0;\n"+
"			} else {\n"+
"				d = arg;\n"+
"			}\n"+
"			return _.R();\n"+
"		}\n"+
"	}\n"+
"	var p = Parser(), c = {}, d = {}, t = {};\n"+
"	function R(r, l, m) {\n"+
"		function f(r, s, v, o, b) {\n"+
"			var e = {\n"+
"				view : r,\n"+
"				sessionid : s,\n"+
"				fieldname : v,\n"+
"				value : o,\n"+
"				type : b\n"+
"			};\n"+
"			if (r.onchange instanceof Function)\n"+
"				r.onchange(e);\n"+
"			if (typeof r.__e__ != \"undefined\")\n"+
"				if (r.__e__[v] instanceof Function)\n"+
"					r.__e__[v](e);\n"+
"				else\n"+
"					for ( var i in r.__e__[v])\n"+
"						r.__e__[v][i](e);\n"+
"		}\n"+
"		function u(r, s, w, i, v) {\n"+
"			var b = typeof v;\n"+
"			var o = w[i];\n"+
"			if (b == \"undefined\") {\n"+
"				b = 2;\n"+
"			} else if (b == \"function\") {\n"+
"				var z = v();\n"+
"				if (typeof z == \"undefined\") {\n"+
"					delete w[i];\n"+
"					b = 3;\n"+
"				} else {\n"+
"					if (typeof o == \"undefined\") {\n"+
"						o = w[i] = {};\n"+
"						b = 0;\n"+
"					} else\n"+
"						b = 1;\n"+
"					for ( var j in z) {\n"+
"						v = z[j];\n"+
"						if (typeof v.v != \"undefined\")\n"+
"							o[v.f] = v.v;\n"+
"						else if (typeof v.c == \"undefined\") {\n"+
"							if (typeof o[v.f] == \"undefined\")\n"+
"								o[v.f] = []\n"+
"							var n = o[v.f]\n"+
"							for ( var x in v.r)\n"+
"								for ( var y in n)\n"+
"									if (v.r[x] == n[y]) {\n"+
"										n.splice(y, 1);\n"+
"										break;\n"+
"									}\n"+
"							for ( var x in v.a)\n"+
"								n.push(v.a[x]);\n"+
"						} else {\n"+
"							if (typeof o[v.f] == \"undefined\")\n"+
"								o[v.f] = new Map();\n"+
"							var n = o[v.f];\n"+
"							for ( var x in v.r)\n"+
"								n[\"delete\"](v.r[x]);\n"+
"							v.c.forEach(function(v, k) {\n"+
"								n.set(k, v);\n"+
"							})\n"+
"						}\n"+
"					}\n"+
"				}\n"+
"			} else {\n"+
"				b = typeof o == \"undefined\" ? 0 : 1;\n"+
"				o = w[i] = v;\n"+
"			}\n"+
"			f(r, s, i, o, b);\n"+
"		}\n"+
"		for ( var i in l)\n"+
"			for ( var j in l[i])\n"+
"				u(r, c.i, r, j, l[i][j]);\n"+
"		for (var i = 0; i < m.length; i += 2)\n"+
"			if (typeof m[i + 1] != \"undefined\")\n"+
"				for ( var j in m[i + 1]) {\n"+
"					if (typeof r[m[i]] == \"undefined\")\n"+
"						r[m[i]] = {};\n"+
"					u(r, m[i], r[m[i]], j, m[i + 1][j]);\n"+
"				}\n"+
"	}\n"+
"	var h = {\n"+
"		0 : function(r, s, l, m) {\n"+
"			R(r, l, m);\n"+
"		},\n"+
"		1 : function(r, s, l, m) {\n"+
"			r[s] = {\n"+
"				__p__ : r.__p__,\n"+
"				__c__ : r.__c__,\n"+
"				__n__ : r.__n__,\n"+
"				__i__ : s\n"+
"			};\n"+
"			if (typeof r.onopen == \"function\") {\n"+
"				var k = [];\n"+
"				for (var i = 0; i < m.length; i += 2)\n"+
"					k.push(m[i]);\n"+
"				r.onopen(s, k);\n"+
"			} else\n"+
"				onerror(r.__n__ + \" onopen not defined\");\n"+
"			R(r[s], l, m);\n"+
"		},\n"+
"		2 : function(r, s, l, m) {\n"+
"			R(r[s], l, m);\n"+
"		},\n"+
"		3 : function(r, s, l, m) {\n"+
"			if (typeof r.onattach == \"function\")\n"+
"				r.onattach(s, m[0]);\n"+
"			else\n"+
"				onerror(r.__n__ + \" onattach not defined\");\n"+
"			R(r[s], l, m);\n"+
"		},\n"+
"		4 : function(r, s, l, m) {\n"+
"			if (typeof r.ondetach == \"function\")\n"+
"				r.ondetach(s, m[0], m[1]);\n"+
"			else\n"+
"				onerror(r.__n__ + \" ondetach not defined\");\n"+
"			delete r[s][m[0]];\n"+
"		},\n"+
"		5 : function(r, s, l, m) {\n"+
"			if (typeof r.onclose == \"function\")\n"+
"				r.onclose(s);\n"+
"			else\n"+
"				onerror(r.__n__ + \" onclose not defined\");\n"+
"			delete r[s];\n"+
"		}\n"+
"	};\n"+
"	if (!cache)\n"+
"		cache = {\n"+
"			put : function(key, value) {\n"+
"			},\n"+
"			get : function(key) {\n"+
"			},\n"+
"			keys : function() {\n"+
"			}\n"+
"		};\n"+
"	function init(s) {\n"+
"		var r = [ p(s), p() ];\n"+
"		if (r[1] != 0)\n"+
"			return r[1];\n"+
"		c.i = p();\n"+
"		c.f = p();\n"+
"		var l = p();\n"+
"		for (var i = 0; i < l.length; i += 3) {\n"+
"			var ck = (d[l[i]] = l[i + 1].split(\",\")).pop();\n"+
"			var cv = cache.get(ck);\n"+
"			if (cv) {\n"+
"				d[l[i]] = p(cv).split(\",\");\n"+
"				l[i + 2] = p();\n"+
"			} else {\n"+
"				cv = d[l[i]].join(\",\");\n"+
"				cv = \"S\" + cv.length.toString(36) + \":\" + cv + \"M\";\n"+
"				l[i + 2].forEach(function(v, k) {\n"+
"					cv += 'I' + k.toString(36) + ':L';\n"+
"					for ( var j in v)\n"+
"						cv += 'I' + v[j].toString(36) + ':';\n"+
"					cv += \":\"\n"+
"				});\n"+
"				cache.put(ck, cv + \":\");\n"+
"			}\n"+
"			t[l[i]] = {};\n"+
"			var s = c[l[i]] = {};\n"+
"			l[i + 2].forEach(function(v, k) {\n"+
"				var r = s;\n"+
"				var m = \"\";\n"+
"				for ( var j in v) {\n"+
"					var n = d[l[i]][v[j]];\n"+
"					m = m + n + \".\";\n"+
"					if (!(n in r))\n"+
"						r[n] = {};\n"+
"					r = r[n];\n"+
"				}\n"+
"				t[l[i]][k] = r;\n"+
"				r.__p__ = l[i];\n"+
"				r.__c__ = k;\n"+
"				r.__n__ = m.substring(0, m.length - 1);\n"+
"			});\n"+
"		}\n"+
"	}\n"+
"	function update(s) {\n"+
"		var v = p(s);\n"+
"		var r = t[v][p(d[v])];\n"+
"		var i = p();\n"+
"		h[p()](r, i, p(), p());\n"+
"	}\n"+
"	var z = false, g = init;\n"+
"	function onerror(e) {\n"+
"		try {\n"+
"			c.onerror(e);\n"+
"		} catch (e) {\n"+
"		}\n"+
"	}\n"+
"	function onclose(p) {\n"+
"		z = true;\n"+
"		try {\n"+
"			c.onclose(p);\n"+
"		} catch (e) {\n"+
"		}\n"+
"		return 3;\n"+
"	}\n"+
"	function onmessage(s) {\n"+
"		if (g == init) {\n"+
"			var r = g(s);\n"+
"			if (r)\n"+
"				return onclose(r);\n"+
"			if (typeof c.onopen == \"function\")\n"+
"				c.onopen();\n"+
"			else\n"+
"				onerror(\"context onopen not defined\");\n"+
"			g = update;\n"+
"			return 2;\n"+
"		}\n"+
"		g(s);\n"+
"		return 0;\n"+
"	}\n"+
"	initializer(c);\n"+
"	return function(t, p) {\n"+
"		if (arguments.length == 0)\n"+
"			return cache.keys();\n"+
"		if (z)\n"+
"			return 3;\n"+
"		else if (t == 0) {\n"+
"			c.send = function(r, s) {\n"+
"				if (!z) {\n"+
"					var e = p(r.__p__.toString(36) + \",\" + r.__c__.toString(36)\n"+
"							+ \",\" + (!r.__i__ ? \"0\" : r.__i__.toString(36))\n"+
"							+ \":\" + s);\n"+
"					if (typeof e != \"undefined\")\n"+
"						onclose(e);\n"+
"				}\n"+
"			}\n"+
"			c.register = function(r, v, f) {\n"+
"				if (typeof r.__e__ == \"undefined\")\n"+
"					r.__e__ = {}\n"+
"				if (typeof r.__e__[v] == \"undefined\")\n"+
"					r.__e__[v] = f;\n"+
"				else if (r.__e__[v] instanceof Function)\n"+
"					r.__e__[v] = [ r.__e__[v], f ];\n"+
"				else\n"+
"					r.__e__[v].push(f);\n"+
"			}\n"+
"			return 0;\n"+
"		} else if (t == 1) {\n"+
"			try {\n"+
"				return onmessage(p)\n"+
"			} catch (e) {\n"+
"				onerror(e);\n"+
"			}\n"+
"		}\n"+
"		return onclose(p);\n"+
"	}\n"+
"}\n"+
"\n"+
"function WebSocketConnector(action, login) {\n"+
"	var keys = action();\n"+
"	keys = keys ? \";\" + keys.join() : \"\";\n"+
"	var uri = encodeURI(login.scheme + \"://\" + login.host + \"/?username=\"\n"+
"			+ login.username + \"&token=\" + login.token + \"&platflag=\"\n"+
"			+ login.platflag + keys + \"&pvids=\" + login.pvids.join());\n"+
"	var s = new WebSocket(uri);\n"+
"	var process = function(t, p) {\n"+
"		switch (action(t, p)) {\n"+
"		case 2:\n"+
"			var w = setInterval(function() {\n"+
"				s.send(\" \");\n"+
"				console.log(\"keepalive\");\n"+
"			}, 50000);\n"+
"			s.onerror = s.onclose = function(e) {\n"+
"				clearInterval(w);\n"+
"				process(2, e);\n"+
"			}\n"+
"			break;\n"+
"		case 3:\n"+
"			s.close();\n"+
"		}\n"+
"	}\n"+
"	s.onopen = function() {\n"+
"		process(0, function(t) {\n"+
"			try {\n"+
"				s.send(t);\n"+
"			} catch (e) {\n"+
"				return e;\n"+
"			}\n"+
"		});\n"+
"	}\n"+
"	s.onmessage = function(e) {\n"+
"		process(1, e.data);\n"+
"	}\n"+
"	s.onerror = s.onclose = function(e) {\n"+
"		process(2, e);\n"+
"	}\n"+
"	return {\n"+
"		close : function() {\n"+
"			s.onclose();\n"+
"		}\n"+
"	}\n"+
"}\n"+
"\n"+
"function loadJSON(uri, onjson, timeout, cacheDir, staleEnable) {\n"+
"	var w, c;\n"+
"	var r = new XMLHttpRequest();\n"+
"	r.open(\"GET\", uri, true);\n"+
"	if (typeof cacheDir != \"undefined\") {\n"+
"		c = cacheDir[uri];\n"+
"		if (typeof c != \"undefined\") {\n"+
"			r.setRequestHeader(\"If-None-Match\", c.etag);\n"+
"			c = c.json;\n"+
"		}\n"+
"	}\n"+
"	r.onreadystatechange = function() {\n"+
"		if (r.readyState == r.DONE) {\n"+
"			if (typeof w == \"undefined\")\n"+
"				clearTimeout(w);\n"+
"			switch (r.status) {\n"+
"			case 200:\n"+
"				try {\n"+
"					var json = JSON.parse(r.responseText);\n"+
"					if (typeof cacheDir != \"undefined\")\n"+
"						cacheDir[uri] = {\n"+
"							etag : r.getResponseHeader(\"ETag\"),\n"+
"							json : json\n"+
"						};\n"+
"					onjson(json);\n"+
"				} catch (e) {\n"+
"					r.onerror(e);\n"+
"				}\n"+
"				return;\n"+
"			case 304:\n"+
"				onjson(c);\n"+
"				return;\n"+
"			}\n"+
"			r.onerror(\"status = \" + r.status);\n"+
"		}\n"+
"	}\n"+
"	r.onerror = r.onabort = r.ontimeout = function(e) {\n"+
"		staleEnable ? onjson(c) : onjson(undefined, e);\n"+
"	}\n"+
"	r.send();\n"+
"	if (timeout > 0) {\n"+
"		w = setTimeout(function() {\n"+
"			r.abort();\n"+
"		}, timeout);\n"+
"	}\n"+
"	return {\n"+
"		close : function() {\n"+
"			r.abort();\n"+
"		}\n"+
"	};\n"+
"}\n"+
"\n"+
"function loadServiceInfos(host, port, appid, timeout, cacheDir, staleEnable,\n"+
"		onresult, wss) {\n"+
"	var l = loadJSON(encodeURI(\"http://\" + host + \":\" + port + \"/app?\"\n"+
"			+ (wss ? \"wss=\" : \"ws=\") + appid), function(json, err) {\n"+
"		if (typeof json == \"undefined\")\n"+
"			onresult(json, err);\n"+
"		else\n"+
"			try {\n"+
"				for ( var i in json.services) {\n"+
"					var s = json.services[i], t = [];\n"+
"					for ( var j in s.userjsons)\n"+
"						t[j] = JSON.parse(s.userjsons[j]);\n"+
"					s.userjsons = t;\n"+
"					s.pvids.push(1);\n"+
"					s.host = function() {\n"+
"						var a = s.switchers[Math.floor(Math.random()\n"+
"								* s.switchers.length)];\n"+
"						return {\n"+
"							host : a.host,\n"+
"							port : a.port,\n"+
"						}\n"+
"					};\n"+
"					s.login = function(username, token, platflag) {\n"+
"						var h = s.host();\n"+
"						return {\n"+
"							scheme : wss ? \"wss\" : \"ws\",\n"+
"							host : h.host + \":\" + h.port,\n"+
"							username : username,\n"+
"							token : token,\n"+
"							platflag : platflag,\n"+
"							pvids : s.pvids,\n"+
"							appid : appid\n"+
"						};\n"+
"					};\n"+
"				}\n"+
"				onresult(json.services);\n"+
"			} catch (e) {\n"+
"				onresult(undefined, e);\n"+
"			}\n"+
"	}, timeout, cacheDir, staleEnable);\n"+
"}\n"+
"\n";} }