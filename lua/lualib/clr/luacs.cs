using System;
using System.Collections;
using System.Collections.Generic;

using limax.script;

namespace limax.endpoint.script
{
    public class LuaScriptHandle : ScriptEngineHandle
    {
        private readonly DictionaryCache cache;
        private readonly ISet<int> providers = new HashSet<int>();
        private readonly LuaFunction instance;
        public LuaScriptHandle(Lua lua, string init, ICollection<int> providers, DictionaryCache cache)
        {
            IDictionary r = (IDictionary)lua.name("initscript").eval(init);
            if (providers == null || providers.Count == 0)
                foreach (object pvid in ((IDictionary)r["pvids"]).Values)
                    this.providers.Add((int)(long)pvid);
            else
                foreach (int pvid in providers)
                    this.providers.Add(pvid);
            this.instance = (LuaFunction)lua.name("").eval("return <0>(<1>,<2>)", lua.name("limax.lua").eval(limax.script.codes.lua.limax), r["callback"], this.cache = cache);
        }
        public LuaScriptHandle(Lua lua, string init, DictionaryCache cache)
            : this(lua, init, null, cache)
        {
        }
        public LuaScriptHandle(Lua lua, string init, ICollection<int> providers)
            : this(lua, init, providers, null)
        {
        }
        public LuaScriptHandle(Lua lua, string init)
            : this(lua, init, null, null)
        {
        }
        public ISet<int> getProviders()
        {
            return providers;
        }
        public int action(int t, object p)
        {
            return (int)(long)instance(t, p);
        }
        private delegate Exception Send(string s);
        public void registerScriptSender(ScriptSender sender)
        {
            instance(0, (Send)sender.send);
        }
        public ICollection<string> getDictionaryKeys()
        {
            return cache != null ? cache.keys() : new List<string>();
        }
    }
}
