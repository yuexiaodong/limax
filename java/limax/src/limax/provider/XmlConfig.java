package limax.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.w3c.dom.Element;

import limax.net.Config;
import limax.net.Engine;
import limax.net.Manager;
import limax.net.ManagerConfig;
import limax.net.State;
import limax.net.Transport;
import limax.provider.states.ProviderClient;
import limax.util.Dispatcher;
import limax.util.ElementHelper;
import limax.util.Enable;
import limax.util.Trace;
import limax.util.XMLUtils;
import limax.xmlconfig.ClientManagerConfigBuilder;
import limax.xmlconfig.ConfigParser;
import limax.xmlconfig.ConfigParserCreator;
import limax.xmlconfig.ServerManagerConfigBuilder;
import limax.xmlconfig.Service;
import limax.xmlconfig.XmlConfigs;
import limax.zdb.Zdb;

public final class XmlConfig {

	private XmlConfig() {
	}

	public interface ProviderDataMXBean {
		String getName();

		int getProviderId();

		String getViewManagerClassName();

		String getAllowUseVariant();

		String getAllowUseScript();

		long getSessionTimeout();
	}

	public final static class ProviderData implements ProviderDataMXBean, ConfigParser {
		private String name = "default";
		private int pvid = 0;
		private String pvkey = "";
		private String tunnelMac = "HmacSHA256";
		private byte[] tunnelKey = "tunnelKey".getBytes();
		private long tunnelKeyExpireTime = Long.MAX_VALUE;
		private String viewManagerClassName;
		private Enable allowUseVariant = Enable.Default;
		private Enable allowUseScript = Enable.Default;
		private long sessionTimeout = 0;
		private final State state = new State();

		@Override
		public void parse(Element self) throws Exception {
			final ElementHelper eh = new ElementHelper(self);
			name = eh.getString("name", name);
			pvid = eh.getInt("pvid");
			if (pvid < 1 || pvid > 0xffffff)
				throw new IllegalArgumentException("pvid = " + pvid + " not in range[1, 0xFFFFFF]");
			pvkey = eh.getString("key");
			tunnelKey = eh.getString("tunnelKey").getBytes();
			tunnelMac = eh.getString("tunnelMac", tunnelMac);
			viewManagerClassName = eh.getString("viewManagerClass");
			allowUseVariant = Enable.parse(eh.getString("useVariant"));
			allowUseScript = Enable.parse(eh.getString("useScript"));
			sessionTimeout = eh.getLong("sessionTimeout", 0);
			state.merge(ProviderClient.getDefaultState());
			String clsname = self.getAttribute("additionalStateClass");
			if (!clsname.isEmpty())
				state.merge((State) Class.forName(clsname).getMethod("getDefaultState").invoke(null));
		}

		@Override
		public int getProviderId() {
			return pvid;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getViewManagerClassName() {
			return viewManagerClassName;
		}

		@Override
		public String getAllowUseVariant() {
			return allowUseVariant.toString();
		}

		@Override
		public String getAllowUseScript() {
			return allowUseScript.toString();
		}

		@Override
		public long getSessionTimeout() {
			return sessionTimeout;
		}
	}

	private static class ProviderManagerConfigImpl implements ProviderManagerConfig {
		private final State providerstate;

		final List<ClientManagerConfigBuilder> cb = new ArrayList<>();
		final List<ServerManagerConfigBuilder> sb = new ArrayList<>();
		final ProviderData providerdata = new ProviderData();

		public ProviderManagerConfigImpl(State providerstate) {
			this.providerstate = providerstate;
		}

		void addClientManagerConfigBuilder(Element e) throws Exception {
			XmlConfigs.ClientManagerConfigXmlBuilder b = new XmlConfigs.ClientManagerConfigXmlBuilder();
			b.parse(e);
			cb.add(b.name("Provider " + providerdata.name + " client manager").defaultState(providerdata.state)
					.dispatcher(new Dispatcher(Engine.getProtocolExecutor())).autoReconnect(true));
		}

		void addServerManagerConfigBuilder(Element e) throws Exception {
			XmlConfigs.ServerManagerConfigXmlBuilder b = new XmlConfigs.ServerManagerConfigXmlBuilder();
			b.parse(e);
			sb.add(b.name("Provider " + providerdata.name + " server manager").defaultState(providerdata.state)
					.dispatcher(new Dispatcher(Engine.getProtocolExecutor())));
		}

		@Override
		public State getProviderState() {
			return providerstate;
		}

		@Override
		public int getProviderId() {
			return providerdata.pvid;
		}

		@Override
		public String getProviderKey() {
			return providerdata.pvkey;
		}

		@Override
		public Map<Integer, Integer> getProviderProtocolInfos() {
			return providerstate.getSizePolicy().entrySet().stream()
					.collect(Collectors.toMap(e -> e.getKey() | (providerdata.pvid << 8), e -> e.getValue()));
		}

		@Override
		public List<ManagerConfig> getManagerConfigs() {
			return Stream
					.concat(cb.stream().map(c -> c.dispatcher(new Dispatcher(Engine.getProtocolExecutor())).build()),
							sb.stream().map(c -> c.dispatcher(new Dispatcher(Engine.getProtocolExecutor())).build()))
					.collect(Collectors.toList());
		}

		@Override
		public String getName() {
			return providerdata.name;
		}

		@Override
		public String getTunnelMac() {
			return providerdata.tunnelMac;
		}

		@Override
		public byte[] getTunnelKey() {
			return providerdata.tunnelKey;
		}

		@Override
		public long getTunnelKeyExpireTime() {
			return providerdata.tunnelKeyExpireTime;
		}

		@Override
		public String getViewManagerClassName() {
			return providerdata.getViewManagerClassName();
		}

		@Override
		public Enable getAllowUseVariant() {
			return providerdata.allowUseVariant;
		}

		@Override
		public Enable getAllowUseScript() {
			return providerdata.allowUseScript;
		}

		@Override
		public long getSessionTimeout() {
			return providerdata.getSessionTimeout();
		}

	}

	public final static class ProviderDataCreator implements ConfigParserCreator {

		static {
			try {
				Class.forName("limax.provider.ProviderManagerImpl");
			} catch (ClassNotFoundException e) {
			}
		}

		@Override
		public ConfigParser createConfigParse(Element self) throws Exception {
			String clsname = self.getAttribute("className");
			ProviderListener listener;
			if (clsname.isEmpty()) {
				listener = new ProviderListener() {
					@Override
					public void onManagerInitialized(Manager manager, Config config) {
					}

					@Override
					public void onManagerUninitialized(Manager manager) {
					}

					@Override
					public void onTransportAdded(Transport transport) {
					}

					@Override
					public void onTransportRemoved(Transport transport) {
					}

					@Override
					public void onTransportDuplicate(Transport transport) throws Exception {
						transport.getManager().close(transport);
					}
				};
			} else {
				Class<?> cls = Class.forName(clsname);
				String singleton = self.getAttribute("classSingleton");
				listener = (ProviderListener) (singleton.isEmpty() ? cls.newInstance()
						: cls.getMethod(singleton).invoke(null));
			}

			ProviderManagerConfigImpl config = new ProviderManagerConfigImpl(
					XmlConfigs.ManagerConfigParserCreator.getDefaultState(self, listener));

			Service.addRunAfterEngineStartTask(() -> {
				try {
					Engine.add(config, listener);
					Trace.info("ProviderManager " + listener + " opened!");
				} catch (Exception e) {
					if (Trace.isErrorEnabled())
						Trace.error("Engine.getInstance().add " + listener, e);
					throw new RuntimeException(e);
				}
			});

			return new ConfigParser() {

				@Override
				public void parse(Element self) throws Exception {
					config.providerdata.parse(self);
					Service.JMXRegister(config.providerdata, "limax.provider:type=XmlConfig,name=" + config.getName());
					for (Element e : XMLUtils.getChildElements(self).stream()
							.filter(e -> e.getNodeName().equals("Manager"))
							.filter(e -> new ElementHelper(e).getBoolean("enable", true))
							.collect(Collectors.toList())) {
						String type = e.getAttribute("type");
						if (type.equalsIgnoreCase("client"))
							config.addClientManagerConfigBuilder(e);
						else if (type.equalsIgnoreCase("server"))
							config.addServerManagerConfigBuilder(e);
						else
							throw new IllegalArgumentException("Provider " + config.providerdata.name
									+ " manager's type must be 'server' or 'client'.");
					}
				}
			};
		}
	}

	public final static class StartZdb implements ConfigParser {

		@Override
		public void parse(Element self) throws Exception {
			limax.xmlgen.Zdb meta = limax.xmlgen.Zdb.loadFromClass();
			meta.initialize(self);
			for (Element e : XMLUtils.getChildElements(self)) {
				switch (e.getNodeName()) {
				case "Procedure":
					meta.getProcedure().initialize(e);
					break;
				case "Table":
					meta.getTable(e.getAttribute("name")).initialize(e);
					break;
				default:
					Trace.error("unknown tag Zdb." + e.getNodeName());
					break;
				}
			}
			Service.addRunBeforeEngineStartTask(() -> Zdb.getInstance().start(meta));
			Service.addRunAfterEngineStopTask(() -> Zdb.getInstance().stop());
		}
	}
}
