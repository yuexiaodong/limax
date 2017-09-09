package limax.auany;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sun.net.httpserver.HttpHandler;

import limax.auany.Account.LoginResult;
import limax.auany.appconfig.AppManager;
import limax.auany.switcherauany.SessionAuthByToken;
import limax.auanymonitor.AuthPlat;
import limax.defines.ErrorCodes;
import limax.defines.ErrorSource;
import limax.defines.SessionFlags;
import limax.endpoint.AuanyService.Result;
import limax.switcherauany.AuanyAuthArg;
import limax.switcherauany.AuanyAuthRes;
import limax.util.Trace;

final class PlatManager {
	private final static Map<String, PlatProcess> plats = new ConcurrentHashMap<>();

	static void initialize(Element self, Map<String, HttpHandler> httphandlers) throws Exception {
		NodeList list = self.getElementsByTagName("plat");
		int count = list.getLength();
		for (int i = 0; i < count; i++)
			parsePlatElement((Element) list.item(i), httphandlers);
	}

	static void check(String username, String token, String platflag, Result result) {
		String platname = platflag.toLowerCase();
		PlatProcess pp = plats.get(platname);
		if (pp == null)
			result.apply(ErrorSource.LIMAX, ErrorCodes.AUANY_UNKNOWN_PLAT, "");
		else {
			AuthPlat.increment_auth(platflag);
			pp.check(username, token, (errorSource, errorCode, uid) -> result.apply(errorSource, errorCode,
					uid.toLowerCase() + "@" + platname));
		}
	}

	static void check(String username, String token, String platflag, Result result, Consumer<String> uidConsumer) {
		check(username, token, platflag, (errorSource, errorCode, uid) -> {
			if (errorSource != ErrorSource.LIMAX || errorCode != ErrorCodes.SUCCEED)
				result.apply(errorSource, errorCode, "");
			else
				uidConsumer.accept(uid);
		});
	}

	private static void parsePlatElement(Element e, Map<String, HttpHandler> httphandlers)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		String classname = e.getAttribute("className");
		String platname = e.getAttribute("name").toUpperCase();
		if (null == classname || classname.isEmpty())
			return;
		if (Trace.isDebugEnabled())
			Trace.debug("Config.load plat classname = " + classname + " platname = " + platname);
		Class<?> cls = Class.forName(classname);
		PlatProcess process = (PlatProcess) cls.newInstance();
		process.init(e, httphandlers);
		if (null != plats.put(platname.toLowerCase(), process))
			throw new RuntimeException("duplicate plat process type " + platname);
	}

	private PlatManager() {
	}

	public static PlatProcess getPlatProcess(String platflag) {
		return plats.get(platflag.toLowerCase());
	}

	private static void response(SessionAuthByToken rpc) {
		try {
			rpc.response();
		} catch (Exception e) {
			if (Trace.isWarnEnabled())
				Trace.warn(rpc, e);
		}
	}

	private static LoginResult done(SessionAuthByToken rpc) {
		return (errorSource, errorCode, sessionid, mainid, uid, serial) -> {
			AuanyAuthRes res = rpc.getResult();
			res.errorSource = errorSource;
			res.errorCode = errorCode;
			res.sessionid = sessionid;
			res.mainid = mainid;
			res.uid = uid;
			if (!uid.isEmpty())
				res.flags |= SessionFlags.FLAG_ACCOUNT_BOUND;
			response(rpc);
		};
	}

	static void process(SessionAuthByToken rpc) {
		try {
			_process(rpc);
		} catch (Exception e) {
			if (Trace.isWarnEnabled())
				Trace.warn(rpc, e);
			AuanyAuthRes res = rpc.getResult();
			res.errorSource = ErrorSource.LIMAX;
			res.errorCode = ErrorCodes.AUANY_AUTHENTICATE_FAIL;
			response(rpc);
		}
	}

	private static void _process(SessionAuthByToken rpc) {
		AuanyAuthArg arg = rpc.getArgument();
		AuanyAuthRes res = rpc.getResult();
		SocketAddress peeraddress;
		@SuppressWarnings("unused")
		SocketAddress reportaddress;
		try (final ObjectInputStream ois = new ObjectInputStream(
				new ByteArrayInputStream(arg.clientaddress.array(), 0, arg.clientaddress.size()))) {
			peeraddress = (SocketAddress) ois.readObject();
			reportaddress = (SocketAddress) ois.readObject();
		} catch (Exception e) {
			peeraddress = reportaddress = new SocketAddress() {
				private static final long serialVersionUID = -210184168558204443L;
			};
		}
		if (!Firewall.checkPermit(peeraddress, arg.pvids.keySet())) {
			res.errorSource = ErrorSource.LIMAX;
			res.errorCode = ErrorCodes.AUANY_CHECK_LOGIN_IP_FAILED;
			response(rpc);
			return;
		}
		String platflag;
		String subid;
		int pos = arg.platflag.indexOf(":");
		if (pos == -1) {
			platflag = arg.platflag;
			subid = "";
		} else {
			platflag = arg.platflag.substring(0, pos);
			subid = arg.platflag.substring(pos + 1);
		}
		Integer appid = AppManager.checkAppId(arg.pvids.keySet());
		if (appid == null) {
			if (platflag.equals("invite"))
				Invite.check(arg.username, arg.token, arg.pvids.keySet(), (errorSource, errorCode, uid) -> {
					res.errorSource = errorSource;
					res.errorCode = errorCode;
					res.sessionid = Long.parseLong(arg.username);
					response(rpc);
				});
			else {
				res.errorSource = ErrorSource.LIMAX;
				res.errorCode = ErrorCodes.AUANY_AUTHENTICATE_FAIL;
				response(rpc);
			}
			return;
		}
		switch (platflag) {
		case "credential":
			Account.login(arg.username, arg.token, subid, appid, done(rpc));
			return;
		case "temporary":
			res.flags |= SessionFlags.FLAG_TEMPORARY_LOGIN;
			Account.temporaryLogin(arg.username, arg.token, appid, done(rpc));
			return;
		default:
			check(arg.username, arg.token, platflag, (errorSource, errorCode, uid) -> {
				if (errorSource != ErrorSource.LIMAX || errorCode != ErrorCodes.SUCCEED) {
					res.errorSource = errorSource;
					res.errorCode = errorCode;
					response(rpc);
				} else {
					if ("flowcontrol".equals(platflag))
						res.flags |= SessionFlags.FLAG_CAN_FLOW_CONTROL;
					Account.login(uid, subid, appid, done(rpc));
				}
			});
		}
	}
}
