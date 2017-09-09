using System;
using System.Collections.Generic;
using limax.endpoint.providerendpoint;

namespace limax.endpoint.script
{
    public interface DictionaryCache
    {
        void put(string key, string value);
        string get(string key);
        ICollection<string> keys();
    }
    public interface ScriptSender
    {
        Exception send(string s);
    }
    public interface ScriptEngineHandle
    {
        ISet<int> getProviders();
        int action(int t, object p);
        void registerScriptSender(ScriptSender sender);
        ICollection<string> getDictionaryKeys();
    }
}
