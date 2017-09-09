package limax.auany;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sun.net.httpserver.HttpHandler;

import limax.defines.ErrorCodes;
import limax.defines.ErrorSource;
import limax.endpoint.AuanyService.Result;
import limax.util.ElementHelper;
import limax.util.Trace;

public final class PayManager {
	private static boolean enabled = false;
	private static PayLogger payLogger;
	private final static Map<Integer, PayGateway> gateways = new HashMap<>();

	static void initialize(Element self, Map<String, HttpHandler> httphandlers) throws Exception {
		ElementHelper eh = new ElementHelper(self);
		boolean payEnable = eh.getBoolean("payEnable", false);
		if (payEnable) {
			Path orderQueueHome = Paths.get(eh.getString("orderQueueHome", "queue"));
			Path deliveryQueueHome = Paths.get(eh.getString("deliveryQueueHome", "queue"));
			int orderQueueConcurrencyBits = eh.getInt("orderQueueConcurrencyBits", 3);
			int deliveryQueueConcurrencyBits = eh.getInt("deliveryQueueConcurrencyBits", 3);
			long orderExpire = eh.getLong("orderExpire", 3600000l);
			long deliveryExpire = eh.getLong("deliveryExpire", 604800000l);
			long deliveryQueueCheckPeriod = eh.getLong("deliveryQueueCheckPeriod", 60000l);
			int deliveryQueueBackoffMax = eh.getInt("deliveryQueueBackoffMax", 5);
			int deliveryQueueScheduler = eh.getInt("deliveryQueueScheduler", 4);
			payLogger = (PayLogger) Class.forName(eh.getString("PayLoggerClass", "limax.auany.PayLoggerSimpleFile"))
					.newInstance();
			payLogger.initialize(self);
			FileBundle.initialize(Paths.get(eh.getString("fileTransactionHome", "transactions")));
			PayDelivery.initialize(deliveryQueueHome, deliveryQueueConcurrencyBits, deliveryExpire,
					deliveryQueueCheckPeriod, deliveryQueueBackoffMax, deliveryQueueScheduler);
			PayOrder.initialize(orderQueueHome, orderQueueConcurrencyBits, orderExpire);
			NodeList list = self.getElementsByTagName("pay");
			int count = list.getLength();
			for (int i = 0; i < count; i++)
				parsePayElement((Element) list.item(i), httphandlers);
			enabled = true;
		}
	}

	static void unInitialize() {
		enabled = false;
		for (PayGateway gw : gateways.values()) {
			try {
				gw.unInitialize();
			} catch (Exception e) {
			}
		}
		PayOrder.unInitialize();
		PayDelivery.unInitialize();
		FileBundle.unInitialize();
		try {
			payLogger.close();
		} catch (Exception e) {
		}
	}

	private static void parsePayElement(Element e, Map<String, HttpHandler> httphandlers) throws Exception {
		ElementHelper eh = new ElementHelper(e);
		int gateway = eh.getInt("gateway");
		PayGateway payGateway = (PayGateway) Class.forName(eh.getString("className")).newInstance();
		payGateway.initialize(e, httphandlers);
		gateways.put(gateway, payGateway);
	}

	static void onPay(long sessionid, int gateway, int payid, int product, int price, int quantity, String receipt,
			Result onresult) {
		if (!enabled) {
			onresult.apply(ErrorSource.LIMAX, ErrorCodes.AUANY_SERVICE_PAY_NOT_ENABLED, "");
			return;
		}
		PayGateway gw = gateways.get(gateway);
		if (gw == null) {
			onresult.apply(ErrorSource.LIMAX, ErrorCodes.AUANY_SERVICE_PAY_GATEWAY_NOT_DEFINED, "");
			return;
		}
		try {
			gw.onPay(sessionid, gateway, payid, product, price, quantity, receipt, onresult);
		} catch (Exception e) {
			onresult.apply(ErrorSource.LIMAX, ErrorCodes.AUANY_SERVICE_PAY_GATEWAY_FAIL, "");
			if (Trace.isInfoEnabled())
				Trace.info("PayManager.onPay", e);
		}
	}

	public static PayLogger getLogger() {
		return payLogger;
	}
}
