package limax.auany;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import limax.auany.appconfig.AppManager;
import limax.auanymonitor.AuthApp;
import limax.codec.MarshalException;
import limax.codec.Octets;
import limax.codec.OctetsStream;
import limax.defines.ErrorCodes;
import limax.defines.ErrorSource;
import limax.defines.TemporaryCredentialUsage;
import limax.endpoint.AuanyService.Result;
import limax.util.Pair;
import limax.zdb.Procedure;

public final class Account {
	private static class SessionCredential {
		protected final String uid;
		protected final int appid;
		protected final long mainid;
		protected final int serial;

		SessionCredential(String uid, int appid, long mainid, int serial) {
			this.uid = uid;
			this.appid = appid;
			this.mainid = mainid;
			this.serial = serial;
		}

		SessionCredential(SessionCredential r) {
			this.uid = r.uid;
			this.appid = r.appid;
			this.mainid = r.mainid;
			this.serial = r.serial;
		}

		SessionCredential(OctetsStream os) throws MarshalException {
			this.uid = os.unmarshal_String();
			this.appid = os.unmarshal_int();
			this.mainid = os.unmarshal_long();
			this.serial = os.unmarshal_int();
		}

		OctetsStream marshal(OctetsStream os) {
			return os.marshal(uid).marshal(appid).marshal(mainid).marshal(serial);
		}
	}

	private static class TemporaryCredential extends SessionCredential {
		private final long expire;
		private final byte usage;
		private final String subid;

		TemporaryCredential(SessionCredential r, long millisecond, byte usage, String subid) {
			super(r);
			this.expire = System.currentTimeMillis() + millisecond;
			this.usage = usage;
			this.subid = subid;
		}

		TemporaryCredential(String uid, int appid, long mainid, int serial, long millisecond, byte usage,
				String subid) {
			super(uid, appid, mainid, serial);
			this.expire = System.currentTimeMillis() + millisecond;
			this.usage = usage;
			this.subid = subid;
		}

		TemporaryCredential(OctetsStream os) throws MarshalException {
			super(os);
			this.expire = os.unmarshal_long();
			this.usage = os.unmarshal_byte();
			this.subid = os.unmarshal_String();
		}

		OctetsStream marshal(OctetsStream os) {
			return super.marshal(os).marshal(expire).marshal(usage).marshal(subid);
		}
	}

	private static String encode(Consumer<OctetsStream> cos, String authcode) throws GeneralSecurityException {
		KeyManager km = OperationEnvironment.getKeyManager();
		int keyindex = km.getRecentIndex();
		byte[] key = km.getKey(keyindex);
		OctetsStream os = new OctetsStream();
		cos.accept(os);
		os.marshal(OperationEnvironment.getEnvironmentIdentity());
		os.marshal(keyindex);
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(key, "HmacSHA256"));
		mac.update(authcode.getBytes(StandardCharsets.UTF_8));
		os.marshal(mac.doFinal(os.getBytes()));
		return Base64.getEncoder().encodeToString(os.getBytes());
	}

	@FunctionalInterface
	private interface Decode<T> {
		T apply(OctetsStream os) throws MarshalException;
	}

	private static <T> T decode(String cred, String authcode, Decode<T> cos)
			throws MarshalException, GeneralSecurityException {
		int pos = cred.indexOf(",");
		if (pos != -1)
			cred = cred.substring(0, pos);
		OctetsStream os = OctetsStream.wrap(Octets.wrap(Base64.getDecoder().decode(cred)));
		T credential = cos.apply(os);
		if (os.unmarshal_int() != OperationEnvironment.getEnvironmentIdentity())
			throw new RuntimeException("not local auany");
		KeyManager km = OperationEnvironment.getKeyManager();
		byte[] key = km.getKey(os.unmarshal_int());
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(key, "HmacSHA256"));
		mac.update(authcode.getBytes(StandardCharsets.UTF_8));
		mac.update(os.getBytes(), 0, os.position());
		if (!Arrays.equals(mac.doFinal(), os.unmarshal_bytes()))
			throw new RuntimeException("Invalid Credential");
		return credential;
	}

	private static String encode(SessionCredential c, String authcode, Collection<Long> subordinates)
			throws GeneralSecurityException {
		return Stream
				.concat(Stream.of(encode(os -> c.marshal(os), authcode)),
						subordinates.stream().map(i -> Long.toUnsignedString(i, Character.MAX_RADIX)))
				.collect(Collectors.joining(","));
	}

	private static SessionCredential decodeSessionCredential(String cred, String authcode) throws Exception {
		return decode(cred, authcode, os -> new SessionCredential(os));
	}

	private static String encode(TemporaryCredential c, String authcode) throws GeneralSecurityException {
		return encode(os -> c.marshal(os), authcode);
	}

	private static TemporaryCredential decodeTemporaryCredential(String cred, String authcode) throws Exception {
		return decode(cred, authcode, os -> new TemporaryCredential(os));
	}

	private static Procedure.Done<Procedure> done(SessionCredential[] c, String authcode, Collection<Long> subordinates,
			int[] e, Result onresult) {
		return (p, r) -> {
			try {
				if (r.isSuccess()) {
					onresult.apply(ErrorSource.LIMAX, ErrorCodes.SUCCEED, encode(c[0], authcode, subordinates));
					return;
				}
			} catch (Exception ex) {
			}
			onresult.apply(ErrorSource.LIMAX, e[0], "");
		};
	}

	private static void derive(long sessionid, Result onresult) {
		int[] e = new int[] { ErrorCodes.AUANY_CALL_PROCEDURE_FAILED };
		long[] subid = new long[1];
		Procedure.execute(() -> {
			xbean.Session s = table.Session.update(sessionid);
			if (s == null)
				return false;
			if (s.getSubordinates().size() >= AppManager.getMaxSubordinates(s.getAppid())) {
				e[0] = ErrorCodes.AUANY_SERVICE_ACCOUNT_TOO_MANY_SUBORDINATES;
				return false;
			}
			s.getSubordinates().add(subid[0] = table.Session.newKey());
			return true;
		} , (p, r) -> {
			if (r.isSuccess())
				onresult.apply(ErrorSource.LIMAX, ErrorCodes.SUCCEED,
						Long.toUnsignedString(subid[0], Character.MAX_RADIX));
			else
				onresult.apply(ErrorSource.LIMAX, e[0], "");
		});
	}

	public static void derive(String cred, String authcode, long sessionid, Result onresult) {
		if (cred.isEmpty() && authcode.isEmpty()) {
			derive(sessionid, onresult);
			return;
		}
		if (Long.toString(sessionid).equals(cred)) {
			if (!Invite.test1(sessionid)) {
				onresult.apply(ErrorSource.LIMAX, ErrorCodes.AUANY_SERVICE_INVALID_INVITE, "");
				return;
			}
			int appid = (int) sessionid;
			SessionCredential c[] = new SessionCredential[1];
			int e[] = new int[] { ErrorCodes.AUANY_CALL_PROCEDURE_FAILED };
			Procedure.execute(() -> {
				Pair<Long, xbean.Session> p = table.Session.insert();
				p.getValue().setSerial(0);
				p.getValue().setAppid(appid);
				c[0] = new SessionCredential("", appid, p.getKey(), 0);
				return true;
			} , done(c, authcode, Collections.emptyList(), e, onresult));
		} else {
			SessionCredential c[] = new SessionCredential[1];
			try {
				c[0] = decodeSessionCredential(cred, authcode);
			} catch (Exception e) {
				onresult.apply(ErrorSource.LIMAX, ErrorCodes.AUANY_SERVICE_INVALID_CREDENTIAL, "");
				return;
			}
			int maxSubordinates = AppManager.getMaxSubordinates(c[0].appid);
			if (maxSubordinates == 0) {
				onresult.apply(ErrorSource.LIMAX, ErrorCodes.AUANY_SERVICE_ACCOUNT_TOO_MANY_SUBORDINATES, "");
				return;
			}
			int e[] = new int[] { ErrorCodes.AUANY_CALL_PROCEDURE_FAILED };
			Collection<Long> subordinates = new ArrayList<>();
			Procedure.execute(() -> {
				xbean.Session s = table.Session.update(sessionid);
				if (sessionid != c[0].mainid && !s.getSubordinates().contains(sessionid)) {
					e[0] = ErrorCodes.AUANY_SERVICE_CREDENTIAL_NOT_MATCH;
					return false;
				}
				if (s.getSubordinates().size() >= maxSubordinates) {
					e[0] = ErrorCodes.AUANY_SERVICE_ACCOUNT_TOO_MANY_SUBORDINATES;
					return false;
				}
				s.getSubordinates().add(table.Session.newKey());
				subordinates.addAll(s.getSubordinates());
				return true;
			} , done(c, authcode, subordinates, e, onresult));
		}
	}

	public static void bind(String cred, String authcode, String uid, long sessionid, Result onresult) {
		if (Long.toString(sessionid).equals(cred)) {
			if (!Invite.test1(sessionid)) {
				onresult.apply(ErrorSource.LIMAX, ErrorCodes.AUANY_SERVICE_INVALID_INVITE, "");
				return;
			}
			int appid = (int) sessionid;
			SessionCredential c[] = new SessionCredential[1];
			int e[] = new int[] { ErrorCodes.AUANY_CALL_PROCEDURE_FAILED };
			Collection<Long> subordinates = new ArrayList<>();
			Procedure.execute(() -> {
				xbean.Account a = table.Account.update(uid);
				if (a == null)
					a = table.Account.insert(uid);
				Long mainid = a.getApplication().get(appid);
				if (mainid != null) {
					xbean.Session s = table.Session.update(mainid);
					int serial = s.getSerial() + 1;
					s.setSerial(serial);
					c[0] = new SessionCredential(uid, appid, mainid, serial);
					subordinates.addAll(s.getSubordinates());
				} else {
					Pair<Long, xbean.Session> p = table.Session.insert();
					p.getValue().setSerial(0);
					p.getValue().setAppid(appid);
					a.getApplication().put(appid, p.getKey());
					c[0] = new SessionCredential(uid, appid, p.getKey(), 0);
				}
				return true;
			} , done(c, authcode, subordinates, e, onresult));
		} else {
			SessionCredential c[] = new SessionCredential[1];
			try {
				c[0] = decodeSessionCredential(cred, authcode);
			} catch (Exception e) {
				onresult.apply(ErrorSource.LIMAX, ErrorCodes.AUANY_SERVICE_INVALID_CREDENTIAL, "");
				return;
			}
			if (c[0].uid.length() > 0 && !uid.equals(c[0].uid)) {
				onresult.apply(ErrorSource.LIMAX, ErrorCodes.AUANY_SERVICE_BIND_HAS_BEEN_BOUND, "");
				return;
			}
			int appid = c[0].appid;
			int e[] = new int[] { ErrorCodes.AUANY_CALL_PROCEDURE_FAILED };
			Collection<Long> subordinates = new ArrayList<>();
			Procedure.execute(() -> {
				xbean.Session s = table.Session.select(c[0].mainid);
				if (sessionid != c[0].mainid && !s.getSubordinates().contains(sessionid)) {
					e[0] = ErrorCodes.AUANY_SERVICE_CREDENTIAL_NOT_MATCH;
					return false;
				}
				c[0] = new SessionCredential(uid, s.getAppid(), c[0].mainid, s.getSerial());
				subordinates.addAll(s.getSubordinates());
				xbean.Account a = table.Account.update(uid);
				if (a == null)
					a = table.Account.insert(uid);
				Long mainid = a.getApplication().get(appid);
				if (mainid == null)
					a.getApplication().put(appid, c[0].mainid);
				else if (mainid != c[0].mainid) {
					e[0] = ErrorCodes.AUANY_SERVICE_BIND_ACCOUNT_HAS_BEEN_USED;
					return false;
				}
				return true;
			} , done(c, authcode, subordinates, e, onresult));
		}
	}

	interface LoginResult {
		void apply(int errorSource, int errorCode, long sessionid, long mainid, String uid, int serial);
	}

	private static Procedure.Done<Procedure> done(long[] sessionid, String uid, int serial[], int[] e,
			LoginResult onresult) {
		return (p, r) -> {
			if (r.isSuccess())
				onresult.apply(ErrorSource.LIMAX, e[0], sessionid[0], sessionid[1], uid, serial[0]);
			else
				onresult.apply(ErrorSource.LIMAX, ErrorCodes.AUANY_CALL_PROCEDURE_FAILED, 0, 0, "", 0);
		};
	}

	private static void login(SessionCredential c, String subid, int appid, LoginResult onresult) {
		long[] sessionid = new long[] { 0, c.mainid };
		int[] e = new int[] { ErrorCodes.AUANY_AUTHENTICATE_FAIL };
		Procedure.execute(() -> {
			xbean.Session s = table.Session.select(c.mainid);
			if (c.serial - s.getSerial() < 0)
				return true;
			if (c.uid.length() > 0) {
				xbean.Account a = table.Account.select(c.uid);
				if (a == null)
					return true;
				Long mainid = a.getApplication().get(appid);
				if (mainid == null || mainid != c.mainid)
					return true;
			}
			if (subid.isEmpty()) {
				sessionid[0] = c.mainid;
				e[0] = ErrorCodes.SUCCEED;
			} else {
				long id = Long.parseUnsignedLong(subid, Character.MAX_RADIX);
				if (s.getSubordinates().contains(id)) {
					sessionid[0] = id;
					e[0] = ErrorCodes.SUCCEED;
				}
			}
			AuthApp.increment_auth(appid);
			return true;
		} , done(sessionid, c.uid, new int[] { c.serial }, e, onresult));
	}

	public static void login(String cred, String authcode, String subid, int appid, LoginResult onresult) {
		SessionCredential c;
		try {
			c = decodeSessionCredential(cred, authcode);
		} catch (Exception e) {
			onresult.apply(ErrorSource.LIMAX, ErrorCodes.AUANY_SERVICE_INVALID_CREDENTIAL, 0, 0, "", 0);
			return;
		}
		if (appid != c.appid) {
			onresult.apply(ErrorSource.LIMAX, ErrorCodes.AUANY_SERVICE_INVALID_CREDENTIAL, 0, 0, "", 0);
			return;
		}
		login(c, subid, appid, onresult);
	}

	private static void login(String uid, String subid, int appid, LoginResult onresult, boolean autocreate) {
		long[] sessionid = new long[2];
		int[] serial = new int[1];
		int[] e = new int[] { ErrorCodes.AUANY_AUTHENTICATE_FAIL };
		Procedure.execute(() -> {
			if (subid.isEmpty()) {
				xbean.Account a = table.Account.update(uid);
				if (a == null)
					a = table.Account.insert(uid);
				Long mainid = a.getApplication().get(appid);
				if (mainid == null) {
					if (!autocreate)
						return true;
					Pair<Long, xbean.Session> p = table.Session.insert();
					p.getValue().setSerial(0);
					p.getValue().setAppid(appid);
					a.getApplication().put(appid, sessionid[0] = sessionid[1] = p.getKey());
					serial[0] = 0;
					AuthApp.increment_newaccount(appid);
				} else {
					sessionid[0] = sessionid[1] = mainid;
					serial[0] = table.Session.select(mainid).getSerial();
				}
			} else {
				xbean.Account a = table.Account.select(uid);
				if (a == null)
					return true;
				Long mainid = a.getApplication().get(appid);
				if (mainid == null)
					return true;
				xbean.Session s = table.Session.select(mainid);
				if (s == null || !s.getSubordinates().contains(subid))
					return true;
				sessionid[0] = Long.parseUnsignedLong(subid, Character.MAX_RADIX);
				sessionid[1] = mainid;
				serial[0] = s.getSerial();
			}
			AuthApp.increment_auth(appid);
			e[0] = ErrorCodes.SUCCEED;
			return true;
		} , done(sessionid, uid, serial, e, onresult));
	}

	public static void login(String uid, String subid, int appid, LoginResult onresult) {
		login(uid, subid, appid, onresult, true);
	}

	public static void temporary(String cred, String authcode, String authcode2, long millisecond, byte usage,
			String subid, Result onresult) {
		SessionCredential c;
		try {
			c = decodeSessionCredential(cred, authcode);
		} catch (Exception e) {
			onresult.apply(ErrorSource.LIMAX, ErrorCodes.AUANY_SERVICE_INVALID_CREDENTIAL, "");
			return;
		}
		login(c, subid, c.appid, (errorSource, errorCode, sessionid, mainid, uid, serial) -> {
			if (errorSource != ErrorSource.LIMAX || errorCode != ErrorCodes.SUCCEED) {
				onresult.apply(errorSource, errorCode, "");
				return;
			}
			try {
				onresult.apply(ErrorSource.LIMAX, ErrorCodes.SUCCEED,
						encode(new TemporaryCredential(c, millisecond, usage, subid), authcode2));
			} catch (Exception e) {
				onresult.apply(ErrorSource.LIMAX, ErrorCodes.AUANY_SERVICE_INVALID_CREDENTIAL, "");
			}
		});
	}

	public static void temporary(String uid, int appid, String authcode, long millisecond, byte usage, String subid,
			Result onresult) {
		login(uid, subid, appid, (errorSource, errorCode, sessionid, mainid, _uid, serial) -> {
			if (errorSource != ErrorSource.LIMAX || errorCode != ErrorCodes.SUCCEED) {
				onresult.apply(errorSource, errorCode, "");
				return;
			}
			try {
				onresult.apply(ErrorSource.LIMAX, ErrorCodes.SUCCEED, encode(
						new TemporaryCredential(uid, appid, mainid, serial, millisecond, usage, subid), authcode));
			} catch (Exception e) {
				onresult.apply(ErrorSource.LIMAX, ErrorCodes.AUANY_SERVICE_INVALID_CREDENTIAL, "");
			}
		} , false);
	}

	public static void temporaryLogin(String temp, String authcode, int appid, LoginResult onresult) {
		TemporaryCredential c;
		try {
			c = decodeTemporaryCredential(temp, authcode);
		} catch (Exception e) {
			onresult.apply(ErrorSource.LIMAX, ErrorCodes.AUANY_SERVICE_INVALID_CREDENTIAL, 0, 0, "", 0);
			return;
		}
		if (appid != c.appid || c.usage != TemporaryCredentialUsage.USAGE_LOGIN
				|| c.expire < System.currentTimeMillis()) {
			onresult.apply(ErrorSource.LIMAX, ErrorCodes.AUANY_SERVICE_INVALID_CREDENTIAL, 0, 0, "", 0);
			return;
		}
		login(c, c.subid, appid, onresult);
	}

	public static void transfer(String uid, String authcode, String temp, String authtemp, long sessionid,
			Result onresult) {
		if (!Invite.test1(sessionid)) {
			onresult.apply(ErrorSource.LIMAX, ErrorCodes.AUANY_SERVICE_INVALID_INVITE, "");
			return;
		}
		TemporaryCredential c;
		try {
			c = decodeTemporaryCredential(temp, authtemp);
		} catch (Exception e) {
			onresult.apply(ErrorSource.LIMAX, ErrorCodes.AUANY_SERVICE_INVALID_CREDENTIAL, "");
			return;
		}
		if (c.appid != (int) sessionid || c.usage != TemporaryCredentialUsage.USAGE_TRANSFER
				|| c.expire < System.currentTimeMillis() || uid.equals(c.uid)) {
			onresult.apply(ErrorSource.LIMAX, ErrorCodes.AUANY_SERVICE_INVALID_CREDENTIAL, "");
			return;
		}
		SessionCredential[] cred = new SessionCredential[1];
		Collection<Long> subordinates = new ArrayList<>();
		int[] e = new int[] { ErrorCodes.AUANY_CALL_PROCEDURE_FAILED };
		if (c.subid.isEmpty()) {
			Procedure.execute(() -> {
				xbean.Session s = table.Session.update(c.mainid);
				if (s.getSerial() != c.serial) {
					e[0] = ErrorCodes.AUANY_SERVICE_INVALID_CREDENTIAL;
					return false;
				}
				s.setSerial(c.serial + 1);
				xbean.Account adst = table.Account.update(uid);
				if (adst == null)
					adst = table.Account.insert(uid);
				else if (adst.getApplication().containsKey(c.appid)) {
					e[0] = ErrorCodes.AUANY_SERVICE_TRANSFER_APPID_COLLISION;
					return false;
				}
				adst.getApplication().put(c.appid, c.mainid);
				if (!c.uid.isEmpty()) {
					xbean.Account asrc = table.Account.update(c.uid);
					if (asrc != null)
						asrc.getApplication().remove(c.appid);
				}
				cred[0] = new SessionCredential(uid, c.appid, c.mainid, s.getSerial());
				subordinates.addAll(s.getSubordinates());
				return true;
			} , done(cred, authcode, subordinates, e, onresult));
		} else {
			long subid = Long.parseUnsignedLong(c.subid, Character.MAX_RADIX);
			Procedure.execute(() -> {
				xbean.Session s = table.Session.update(c.mainid);
				if (s.getSerial() != c.serial || !s.getSubordinates().remove(subid)) {
					e[0] = ErrorCodes.AUANY_SERVICE_INVALID_CREDENTIAL;
					return false;
				}
				xbean.Account adst = table.Account.update(uid);
				if (adst == null)
					adst = table.Account.insert(uid);
				Long mainid = adst.getApplication().get(c.appid);
				if (mainid == null) {
					Pair<Long, xbean.Session> p = table.Session.insert();
					p.getValue().setSerial(0);
					p.getValue().setAppid(c.appid);
					p.getValue().getSubordinates().add(subid);
					adst.getApplication().put(c.appid, p.getKey());
					cred[0] = new SessionCredential(uid, c.appid, p.getKey(), 0);
					subordinates.add(subid);
				} else {
					xbean.Session sdst = table.Session.update(mainid);
					if (sdst == null)
						return false;
					if (sdst.getSubordinates().size() >= AppManager.getMaxSubordinates(c.appid)) {
						e[0] = ErrorCodes.AUANY_SERVICE_ACCOUNT_TOO_MANY_SUBORDINATES;
						return false;
					}
					sdst.getSubordinates().add(subid);
					sdst.setSerial(s.getSerial() + 1);
					cred[0] = new SessionCredential(uid, c.appid, mainid, 0);
					subordinates.addAll(sdst.getSubordinates());
				}
				return true;
			} , done(cred, authcode, subordinates, e, onresult));
		}
	}
}
