using System;
using System.Collections;
using System.Collections.Generic;
using System.Threading;

using limax.script;
using limax.util;

namespace limax.endpoint.script
{
    public class JsContext
    {
        public delegate void JsConsumer(Js js);
        private readonly SingleThreadExecutor executor = new SingleThreadExecutor("js");
        private Js js;
        public JsContext(uint maxbytes)
        {
            sync(_ => js = new Js(maxbytes));
        }
        public JsContext()
        {
            sync(_ => js = new Js());
        }
        public void sync(JsConsumer jsc)
        {
            executor.wait(() => jsc(js));
        }
        public void async(JsConsumer jsc)
        {
            executor.execute(() => jsc(js));
        }
        public void shutdown()
        {
            executor.shutdown();
        }
    }
    public class JavaScriptHandle : ScriptEngineHandle
    {
        private readonly JsContext jsc;
        private readonly DictionaryCache cache;
        private readonly ISet<int> providers = new HashSet<int>();
        private JsFunction instance;
        public JavaScriptHandle(JsContext jsc, string init, ICollection<int> providers, DictionaryCache cache)
        {
            this.jsc = jsc;
            this.cache = cache;
            jsc.sync(js =>
            {
                js.name("limax.js").eval(limax.script.codes.js.limax);
                js.name("cache").eval("var cache=<0>", cache);
                js.name("initscript").eval(init);
                if (providers == null || providers.Count == 0)
                    foreach (object pvid in (JsArray)js.eval("providers"))
                        this.providers.Add((int)pvid);
                else
                    foreach (var pvid in providers)
                        this.providers.Add(pvid);
                this.instance = (JsFunction)js.name("").eval("limax");
            });
        }
        public JavaScriptHandle(JsContext jsc, string init, DictionaryCache cache)
            : this(jsc, init, null, cache)
        {
        }
        public JavaScriptHandle(JsContext jsc, string init, ICollection<int> providers)
            : this(jsc, init, providers, null)
        {
        }
        public JavaScriptHandle(JsContext jsc, string init)
            : this(jsc, init, null, null)
        {
        }
        public ISet<int> getProviders()
        {
            return providers;
        }
        public int action(int t, object p)
        {
            int r = 0;
            jsc.sync(_ => r = (int)instance(t, p));
            return r;
        }
        private delegate object Send(string s);
        public void registerScriptSender(ScriptSender sender)
        {
            Send send = s =>
            {
                object r = sender.send(s);
                return r == null ? DBNull.Value : r;
            };
            jsc.async(_ => instance(0, send));
        }
        public ICollection<string> getDictionaryKeys()
        {
            return cache != null ? cache.keys() : new List<string>();
        }
    }
}