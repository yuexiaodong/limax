package limax.endpoint.script;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.script.Invocable;
import javax.script.ScriptEngine;

public class JavaScriptHandle implements ScriptEngineHandle {
	private final DictionaryCache cache;
	private final Invocable invoker;
	private final Set<Integer> providers = new HashSet<Integer>();

	@SuppressWarnings("unchecked")
	public JavaScriptHandle(ScriptEngine engine, Reader init, Collection<Integer> providers, DictionaryCache cache)
			throws Exception {
		try (Reader reader = new InputStreamReader(getClass().getResourceAsStream("map.js"))) {
			engine.eval(reader);
		}
		try (Reader reader = new InputStreamReader(getClass().getResourceAsStream("limax.js"))) {
			engine.eval(reader);
		}
		engine.eval(
				"function sender(obj){ limax(0, function(s) { var r = obj.send(s); return r ? r : undefined; }); }");
		engine.put("cache", this.cache = cache);
		engine.eval(init);
		this.invoker = (Invocable) engine;
		if (providers == null || providers.isEmpty()) {
			Object o = engine.get("providers");
			if (!(o instanceof Map))
				throw new RuntimeException("init script must set var providers = [pvid0,..];");
			for (Object v : ((Map<Integer, Object>) o).values())
				this.providers.add((Integer) v);
		} else {
			for (Integer v : providers)
				this.providers.add(v);
		}
	}

	public JavaScriptHandle(ScriptEngine engine, Reader init, DictionaryCache cache) throws Exception {
		this(engine, init, null, cache);
	}

	public JavaScriptHandle(ScriptEngine engine, Reader init, Collection<Integer> providers) throws Exception {
		this(engine, init, providers, null);
	}

	public JavaScriptHandle(ScriptEngine engine, Reader init) throws Exception {
		this(engine, init, null, null);
	}

	@Override
	public Set<Integer> getProviders() {
		return providers;
	}

	@Override
	public int action(int t, Object p) throws Exception {
		return (int) invoker.invokeFunction("limax", t, p);
	}

	@Override
	public void registerScriptSender(ScriptSender sender) throws Exception {
		invoker.invokeFunction("sender", sender);
	}

	@Override
	public Collection<String> getDictionaryKeys() {
		return cache == null ? Collections.emptyList() : cache.keys();
	}
}
