package limax.auany;

import limax.auany.appconfig.AppManager;
import limax.auany.switcherauany.CheckProviderKey;
import limax.auany.switcherauany.JSONPublish;
import limax.auany.switcherauany.OnlineAnnounce;
import limax.auany.switcherauany.PayAck;
import limax.auany.switcherauany.SessionAuthByToken;
import limax.endpoint.AuanyService.Result;

public class __ProtocolProcessManager {
	private __ProtocolProcessManager() {
	}

	public static void process(CheckProviderKey rpc) {
		SessionManager.process(rpc);
	}

	public static void process(SessionAuthByToken rpc) {
		PlatManager.process(rpc);
	}

	public static void process(PayAck ack) {
		PayDelivery.ack(ack);
	}

	public static void process(OnlineAnnounce announce) {
		SessionManager.process(announce);
	}

	public static void process(JSONPublish p) {
		AppManager.updateJSON(p.pvid, p.json);
	}

	public static void onPay(long sessionid, int gateway, int pvid, int product, int price, int quantity,
			String receipt, Result onresult) {
		PayManager.onPay(sessionid, gateway, pvid, product, price, quantity, receipt, onresult);
	}

	public static void onBind(long sessionid, String credential, String authcode, String username, String token,
			String platflag, Result onresult) {
		PlatManager.check(username, token, platflag, onresult,
				uid -> Account.bind(credential, authcode, uid, sessionid, onresult));
	}

	public static void onTemporaryFromCredential(String cred, String authcode, String authcode2, long millisecond,
			byte usage, String subid, Result onresult) {
		Account.temporary(cred, authcode, authcode2, millisecond, usage, subid, onresult);
	}

	public static void onTemporaryFromLogin(String username, String token, String platflag, int appid, String authcode,
			long millisecond, byte usage, String subid, Result onresult) {
		PlatManager.check(username, token, platflag, onresult,
				uid -> Account.temporary(uid, appid, authcode, millisecond, usage, subid, onresult));
	}

	public static void onDerive(long sessionid, String credential, String authcode, Result onresult) {
		Account.derive(credential, authcode, sessionid, onresult);
	}

	public static void onTransfer(long sessionid, String username, String token, String platflag, String authcode,
			String temp, String authtemp, Result onresult) {
		PlatManager.check(username, token, platflag, onresult,
				uid -> Account.transfer(uid, authcode, temp, authtemp, sessionid, onresult));
	}
}
