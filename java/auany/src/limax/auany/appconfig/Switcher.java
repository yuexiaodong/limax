package limax.auany.appconfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import limax.auany.json.SwitcherInfo;
import limax.util.ElementHelper;
import limax.util.XMLUtils;

class Switcher {
	private final int id;
	private final String key;
	private final ServiceType type;
	private final SwitcherInfo info;
	private final List<App> apps = new ArrayList<>();
	private volatile String host;

	private Switcher(Element self, Context ctx) {
		this.id = Integer.parseInt(self.getAttribute("id"));
		if (this.id <= 0)
			ctx.updateErrorMessage("Switcher id must > 0, but " + this.id);
		this.key = self.getAttribute("key");
		this.info = new SwitcherInfo(self.getAttribute("host"), Integer.parseInt(self.getAttribute("port")));
		this.type = ServiceType.valueOf(self.getAttribute("type").toUpperCase());
	}

	private Element createElement(Document doc) {
		Element e = doc.createElement("switcher");
		ElementHelper eh = new ElementHelper(e);
		eh.set("id", this.id);
		eh.set("key", this.key);
		eh.set("host", this.info.getHost());
		eh.set("port", this.info.getPort());
		eh.set("type", this.type.toString().toLowerCase());
		return e;
	}

	String update(String host, List<Runnable> log, Set<App> infectapps) {
		if (host == null) {
			if (this.host == null)
				return "";
		} else {
			if (this.host != null)
				return host.equals(this.host) ? "" : this.host;
		}
		infectapps.addAll(this.apps);
		log.add(() -> this.host = host);
		return "";
	}

	int getId() {
		return id;
	}

	boolean verifyKey(String key) {
		return key.equals(this.key);
	}

	boolean isRunning() {
		return host != null;
	}

	ServiceType getType() {
		return type;
	}

	SwitcherInfo getInfo() {
		return info;
	}

	List<App> getApps() {
		return apps;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Switcher))
			return false;
		Switcher s = (Switcher) o;
		return s.id == this.id && s.info.equals(this.info) && s.key.equals(this.key) && s.type.equals(this.type);
	}

	static void load(Element self, Context ctx) {
		List<Integer> conflict = XMLUtils.getChildElements(self).stream().filter(e -> e.getTagName().equals("switcher"))
				.map(e -> new Switcher(e, ctx)).map(s -> ctx.switcherMap.putIfAbsent(s.id, s)).filter(Objects::nonNull)
				.map(Switcher::getId).collect(Collectors.toList());
		if (!conflict.isEmpty())
			ctx.updateErrorMessage("Switcher id conflict " + conflict);
	}

	static Switcher get(Context ctx, int id) {
		return ctx.switcherMap.get(id);
	}

	static List<Element> createElements(Document doc, Context ctx) {
		return ctx.switcherMap.values().stream().sorted(Comparator.comparingInt(Switcher::getId))
				.map(s -> s.createElement(doc)).collect(Collectors.toList());
	}
}