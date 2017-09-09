package limax.auany;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;

import com.sun.net.httpserver.HttpHandler;

import limax.net.Engine;
import limax.util.ElementHelper;
import limax.util.Pair;
import limax.util.XMLUtils;
import limax.xmlconfig.Service;

final class Firewall {
	private static volatile Map<Integer, Set<InetAddress>> permitmap = Collections.emptyMap();

	private Firewall() {
	}

	private static void load(Path path) {
		try (InputStream is = new FileInputStream(path.toFile())) {
			permitmap = XMLUtils
					.getChildElements(
							DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is).getDocumentElement())
					.stream().filter(e -> e.getTagName().equals("permit")).map(e -> {
						try {
							return new Pair<>(Integer.parseInt(e.getAttribute("pvid")),
									InetAddress.getByName(e.getAttribute("host")));
						} catch (Exception e1) {
							return null;
						}
					}).filter(Objects::nonNull).collect(Collectors.groupingBy(p -> p.getKey(),
							Collectors.mapping(p -> p.getValue(), Collectors.toSet())));
		} catch (Exception e) {
			permitmap = Collections.emptyMap();
		}
	}

	static boolean checkPermit(SocketAddress peer, Set<Integer> pvids) {
		try {
			Map<Integer, Set<InetAddress>> map = permitmap;
			if (map.isEmpty())
				return true;
			Set<Integer> set = new HashSet<>(pvids);
			set.retainAll(map.keySet());
			if (set.isEmpty())
				return true;
			InetAddress ia = ((InetSocketAddress) peer).getAddress();
			for (int pvid : set)
				if (map.get(pvid).contains(ia))
					return true;
			return false;
		} catch (Exception e) {
		}
		return true;
	}

	public static void initialize(Element self, Map<String, HttpHandler> httphandlers) throws Exception {
		ElementHelper eh = new ElementHelper(self);
		Path path = Service.getConfigParentFile().toPath().resolve(eh.getString("firewallConfig", "firewall.xml"));
		long period = eh.getLong("firewallConfigCheckPeriod", 30000l);
		Service.addRunAfterEngineStartTask(() -> Engine.getProtocolScheduler().scheduleAtFixedRate(() -> load(path), 0,
				period, TimeUnit.MILLISECONDS));
	}
}
