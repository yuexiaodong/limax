package limax.endpoint.script;

import java.util.Collection;
import java.util.Set;

public interface ScriptEngineHandle {
	Set<Integer> getProviders();

	int action(int t, Object p) throws Exception;

	void registerScriptSender(ScriptSender sender) throws Exception;

	Collection<String> getDictionaryKeys();
}
