package limax.auany;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import limax.auany.appconfig.AppManager;
import limax.auany.switcherauany.CheckProviderKey;
import limax.auany.switcherauany.KeepAlive;
import limax.auany.switcherauany.Kick;
import limax.auany.switcherauany.OnlineAnnounce;
import limax.defines.ErrorCodes;
import limax.net.Config;
import limax.net.Engine;
import limax.net.Manager;
import limax.net.ServerManager;
import limax.net.Transport;
import limax.provider.ProviderListener;
import limax.provider.ProviderManagerConfig;
import limax.switcherauany.CheckProviderKeyArg;
import limax.switcherauany.CheckProviderKeyRes;
import limax.util.Trace;

public class SessionManager implements ProviderListener {
	private Future<?> keepAliveFuture;
	public final static int providerId = 1;
	private static final SessionManager instance = new SessionManager();

	private SessionManager() {
	}

	public static SessionManager getInstance() {
		return instance;
	}

	@Override
	public void onManagerInitialized(Manager manager, Config config) {
		if (Trace.isInfoEnabled())
			Trace.info("ServiceManagerListener onManagerInitialized");
		ProviderManagerConfig pmc = (ProviderManagerConfig) config;
		if (providerId != pmc.getProviderId())
			throw new RuntimeException("AuanyProvider must config pvid = 1, but " + pmc.getProviderId());
		try {
			((ServerManager) manager).openListen();
		} catch (IOException e) {
			if (Trace.isErrorEnabled())
				Trace.error("ServiceManagerListener openListen", e);
			throw new RuntimeException(e);
		}
		if (Trace.isInfoEnabled())
			Trace.info("ServiceManagerListener openListen");
		long keepAliveTimeout = Engine.getIntranetKeepAliveTimeout();
		if (keepAliveTimeout > 0)
			keepAliveFuture = Engine.getProtocolScheduler().scheduleWithFixedDelay(() -> {
				Collection<Transport> transports = pvidmap.values().stream().collect(HashSet<Transport>::new,
						Collection::addAll, Collection::addAll);
				KeepAlive p = new KeepAlive(keepAliveTimeout);
				try {
					for (Transport transport : transports)
						p.send(transport);
				} catch (Exception e) {
				}
			} , 0, keepAliveTimeout / 2, TimeUnit.MILLISECONDS);
	}

	@Override
	public void onManagerUninitialized(Manager manager) {
		if (Trace.isInfoEnabled())
			Trace.info("ServiceManagerListener onManagerUninitialized");
		if (keepAliveFuture != null)
			keepAliveFuture.cancel(true);
	}

	@Override
	public void onTransportAdded(Transport transport) throws Exception {
		if (Trace.isInfoEnabled())
			Trace.info("ServiceManagerListener onTransportAdded " + transport);
	}

	@Override
	public void onTransportRemoved(Transport transport) throws Exception {
		unbind(transport);
		if (Trace.isInfoEnabled())
			Trace.info("ServiceManagerListener onTransportRemoved " + transport);
	}

	@Override
	public void onTransportDuplicate(Transport transport) throws Exception {
		if (Trace.isInfoEnabled())
			Trace.info("ServiceManagerListener onTransportDuplicate " + transport);
	}

	private final static Map<Integer, Set<Transport>> pvidmap = new ConcurrentHashMap<>();
	private final static Map<Transport, SwitcherAnnounce> switchermap = new ConcurrentHashMap<>();

	private static class SwitcherAnnounce {
		private final OnlineAnnounce announce;

		SwitcherAnnounce(OnlineAnnounce announce) {
			this.announce = announce;
			update(announce.getTransport(), true);
		}

		void update(Transport transport, boolean on) {
			String message = AppManager.updateSwitcher(on ? transport.getPeerAddress().toString() : null, announce.key,
					announce.nativeIds, announce.wsIds, announce.wssIds);
			if (!message.isEmpty()) {
				try {
					new Kick(message).send(transport);
				} catch (Exception e) {
				}
			}
		}
	}

	private static int checkKey(int pvid, String key) {
		try {
			return AppManager.verifyProviderKey(pvid, key) ? ErrorCodes.SUCCEED
					: ErrorCodes.AUANY_CHECK_PROVIDER_KEY_BAD_KEY;
		} catch (Exception e) {
			e.printStackTrace();
			return ErrorCodes.AUANY_CHECK_PROVIDER_KEY_UNKNOWN_PVID;
		}
	}

	private static void bind(int pvid, boolean paySupported, Transport transport) {
		Set<Transport> set = Collections.newSetFromMap(new ConcurrentHashMap<Transport, Boolean>());
		Set<Transport> oldset = pvidmap.putIfAbsent(pvid, set);
		if (oldset != null)
			set = oldset;
		set.add(transport);
		AppManager.providerUp(pvid, paySupported);
	}

	static void unbind(Transport transport) {
		SwitcherAnnounce sa = (SwitcherAnnounce) switchermap.remove(transport);
		if (sa != null)
			sa.update(transport, false);
		pvidmap.forEach((pvid, transports) -> {
			transports.remove(transport);
			if (transports.isEmpty())
				AppManager.providerDown(pvid);
		});
	}

	static Transport getTransport(int pvid) {
		Set<Transport> transports = pvidmap.get(pvid);
		return transports == null ? null : transports.stream().findAny().orElse(null);
	}

	static void process(OnlineAnnounce announce) {
		Transport transport = announce.getTransport();
		switchermap.put(transport, new SwitcherAnnounce(announce));
		announce.pvids.forEach((pvid, paySupported) -> bind(pvid, paySupported, transport));
	}

	static void process(CheckProviderKey rpc) {
		CheckProviderKeyArg arg = rpc.getArgument();
		CheckProviderKeyRes res = rpc.getResult();
		res.error = checkKey(arg.pvid, arg.pvkey);
		if (res.error == ErrorCodes.SUCCEED) {
			bind(arg.pvid, arg.paySupported, rpc.getTransport());
			res.jsonPublishDelayMin = AppManager.initProvider(arg.pvid, arg.json);
		}
		if (Trace.isInfoEnabled())
			Trace.info("CheckProviderKey pvid=" + arg.pvid + ",error=" + res.error);
	}

}
