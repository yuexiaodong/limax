package limax.endpoint;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import limax.codec.CharSink;
import limax.codec.JSON;
import limax.codec.JSONDecoder;
import limax.defines.ErrorSource;
import limax.defines.SessionType;
import limax.endpoint.script.ScriptEngineHandle;
import limax.net.Engine;
import limax.util.Closeable;
import limax.util.ConcurrentEnvironment;
import limax.util.Dispatcher.Dispatchable;
import limax.util.HttpClient;

public final class Endpoint {

	private Endpoint() {
	}

	public static void openEngine(int netProcessors, int protocolSchedulers, int applicationExecutors)
			throws Exception {
		Engine.open(netProcessors, protocolSchedulers, applicationExecutors);
	}

	public static void openEngine() throws Exception {
		openEngine(1, 1, 4);
	}

	public static void closeEngine(final Runnable done) {
		AuanyService.cleanup();
		ConcurrentEnvironment.getInstance().executeStandaloneTask(new Runnable() {
			@Override
			public void run() {
				try {
					Engine.close();
				} finally {
					if (done != null)
						done.run();
				}
			}
		});
	}

	public static EndpointConfigBuilder createLoginConfigBuilder(String serverIp, int serverPort, String username,
			String token, String platflag) {
		final EndpointConfigBuilderImpl config = new EndpointConfigBuilderImpl(serverIp, serverPort, platflag, username,
				token, false);
		return config.isConfigCanLogin() ? config : null;
	}

	public static EndpointConfigBuilder createLoginConfigBuilder(ServiceInfo service, String username, String token,
			String platflag) {
		ServiceInfo.SwitcherConfig switcher = service.randomSwitcherConfig();
		return createLoginConfigBuilder(switcher.host, switcher.port, username, token, platflag);
	}

	public static EndpointConfigBuilder createPingOnlyConfigBuilder(String serverIp, int serverPort) {
		return new EndpointConfigBuilderImpl(serverIp, serverPort, "", "", "", true);
	}

	private static void mapPvidsAppendValue(Map<Integer, Byte> pvids, Integer s, int nv) {
		Byte v = pvids.get(s);
		if (null == v)
			v = Byte.valueOf((byte) nv);
		else
			v = Byte.valueOf((byte) (v | nv));
		pvids.put(s, v);
	}

	private static Collection<Integer> makeProtocolProviderIds(EndpointConfig config) {
		final Set<Integer> pvids = new HashSet<Integer>();
		for (int type : config.getEndpointState().getSizePolicy().keySet()) {
			int pvid = type >>> 8;
			if (pvid > 0)
				pvids.add(pvid);
		}
		return pvids;
	}

	private static Collection<Integer> makeScriptProviderIds(EndpointConfig config) {
		if (config.getScriptEngineHandle() != null)
			return config.getScriptEngineHandle().getProviders();
		return Collections.emptySet();
	}

	private static Map<Integer, Byte> makeProviderMap(EndpointConfig config) {
		final Map<Integer, Byte> pvids = new HashMap<Integer, Byte>();
		for (Integer s : makeProtocolProviderIds(config))
			mapPvidsAppendValue(pvids, s, SessionType.ST_PROTOCOL);
		for (Integer s : config.getStaticViewClasses().keySet())
			mapPvidsAppendValue(pvids, s, SessionType.ST_STATIC);
		for (Integer s : config.getVariantProviderIds())
			mapPvidsAppendValue(pvids, s, SessionType.ST_VARIANT);
		for (Integer s : makeScriptProviderIds(config))
			mapPvidsAppendValue(pvids, s, SessionType.ST_SCRIPT);
		if (config.auanyService())
			mapPvidsAppendValue(pvids, AuanyService.providerId, SessionType.ST_STATIC);
		return pvids;
	}

	public static void start(final EndpointConfig config, final EndpointListener listener) {
		ConcurrentEnvironment.getInstance().executeStandaloneTask(new Runnable() {
			@Override
			public void run() {
				try {
					if (null == Engine.getProtocolScheduler())
						throw new Exception("endpoint need call openEngine");
					Map<Integer, Byte> pvids = makeProviderMap(config);
					if (!config.isPingServerOnly() && pvids.isEmpty())
						throw new Exception("endpoint no available provider");
					new EndpointManagerImpl(config, listener, pvids);
				} catch (final Exception e) {
					config.getClientManagerConfig().getDispatcher().run(new Dispatchable() {
						@Override
						public void run() {
							listener.onErrorOccured(ErrorSource.ENDPOINT, 0, e);
						}
					});
				}
			}
		});
	}

	public static Closeable start(String host, int port, String username, String token, String platflag,
			ScriptEngineHandle handle, Executor executor) throws Exception {
		return new WebSocketConnector(host, port, username, token, platflag, handle, executor);
	}

	public static List<ServiceInfo> loadServiceInfos(String httpHost, int httpPort, int appid, long timeout,
			int maxsize, File cacheDir, boolean staleEnable) throws Exception {
		JSONDecoder decoder = new JSONDecoder();
		new HttpClient("http://" + httpHost + ":" + httpPort + "/app?native=" + appid, timeout, maxsize, cacheDir,
				staleEnable).transfer(new CharSink(decoder));
		List<ServiceInfo> services = new ArrayList<ServiceInfo>();
		for (JSON json : decoder.get().get("services").toArray())
			services.add(new ServiceInfo(appid, json));
		return services;
	}

	private static AtomicReference<EndpointManagerImpl> defaultEndpointManager = new AtomicReference<EndpointManagerImpl>();

	static void setDefaultEndpointManager(EndpointManagerImpl manager) {
		defaultEndpointManager.compareAndSet(null, manager);
	}

	static void clearDefaultEndpointManager(EndpointManagerImpl manager) {
		defaultEndpointManager.compareAndSet(manager, null);
	}

	public static EndpointManager getDefaultEndpointManager() {
		return defaultEndpointManager.get();
	}
}
