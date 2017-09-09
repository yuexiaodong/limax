package limax.auany;

import java.util.Map;

import org.w3c.dom.Element;

import com.sun.net.httpserver.HttpHandler;

import limax.util.ElementHelper;
import limax.util.HttpClientService;

public final class HttpClientManager {
	private static HttpClientService service;

	static void initialize(Element self, Map<String, HttpHandler> httphandlers) throws Exception {
		ElementHelper eh = new ElementHelper(self);
		int corePoolSize = eh.getInt("corePoolSize", 8);
		int defaultMaxOutstanding = eh.getInt("defaultMaxOutstanding", 3);
		int defaultMaxQueueCapacity = eh.getInt("defaultMaxQueueCapacity", 16);
		int defaultMaxContentLength = eh.getInt("defaultMaxContentLength", 16384);
		int defaultTimeout = eh.getInt("defaultTimeout", 10000);
		service = new HttpClientService(corePoolSize, defaultMaxOutstanding, defaultMaxQueueCapacity,
				defaultMaxContentLength, defaultTimeout);
	}

	public static HttpClientService getService() {
		return service;
	}

	static void unInitialize() {
		service.shutdown();
	}
}
