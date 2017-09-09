#include "common.h"

extern const char* g_limax_js_code_string;
namespace limax
{
	JsCreator::JsCreator() {}
	JsCreator::~JsCreator() {}

	class JsEngineHandle : public ScriptEngineHandle
	{
		struct DictionaryCachePtrGCWrapper
		{
			DictionaryCachePtr cache;
			DictionaryCachePtrGCWrapper(DictionaryCachePtr _cache) : cache(_cache) { }
		};

		struct ScriptSenderGCWrapper
		{
			ScriptSender sender;
			ScriptSenderGCWrapper(ScriptSender _sender) : sender(_sender) { }
			bool send(const std::string& p)
			{
				return sender(p);
			}
		};

		static void finalize_cacheholder(JSFreeOp* fop, JSObject* obj)
		{
			delete (DictionaryCachePtrGCWrapper *)JS_GetPrivate(obj);
		}

		static void finalize_senderholder(JSFreeOp* fop, JSObject* obj)
		{
			delete (ScriptSenderGCWrapper *)JS_GetPrivate(obj);
		}

		static JSClass cacheholder_class;
		static JSClass senderholder_class;

		JsEnginePtr engine;
		hashset<int> providers;
		DictionaryCachePtr cache;
	public:
		JsEngineHandle(const JsEnginePtr& _engine, const char * initscript, const std::vector<int>& _providers, const DictionaryCachePtr& _cache)
			: engine(_engine), providers(_providers.begin(), _providers.end()), cache(_cache)
		{
			engine->wait([initscript, this](JSRuntime*, JSContext* cx, JS::HandleObject global){
				JS::RootedValue rval(cx);
				JS::CompileOptions opts(cx);
				opts.setFile("limax.js");
				JS::Evaluate(cx, opts, g_limax_js_code_string, strlen(g_limax_js_code_string), &rval);
				if (cache)
				{
					JS::RootedObject objcache(cx, JS_NewPlainObject(cx));
					JS_SetProperty(cx, global, "cache", JS::RootedValue(cx, JS::ObjectValue(*objcache)));
					JS_SetPrivate(JS_DefineObject(cx, objcache, "data", &cacheholder_class), new DictionaryCachePtrGCWrapper(cache));
					JS_DefineFunction(cx, objcache, "get", [](JSContext *cx, unsigned argc, JS::Value *vp){
						JS::CallArgs args = JS::CallArgsFromVp(argc, vp);
						if (argc == 1)
						{
							JS::RootedObject thisv(cx, &args.thisv().toObject());
							JS::RootedValue data(cx);
							JS_GetProperty(cx, thisv, "data", &data);
							DictionaryCachePtrGCWrapper* p = (DictionaryCachePtrGCWrapper*)JS_GetPrivate(&data.toObject());
							JS::RootedString objkey(cx, JS::ToString(cx, args[0]));
							char *pkey = JS_EncodeStringToUTF8(cx, objkey);
							std::string val = p->cache->get(pkey);
							JS_free(cx, pkey);
							args.rval().setString(JS_NewStringCopyZ(cx, val.c_str()));
						}
						else
							args.rval().setUndefined();
						return true;
					}, 0, 0);
					JS_DefineFunction(cx, objcache, "put", [](JSContext *cx, unsigned argc, JS::Value *vp){
						JS::CallArgs args = JS::CallArgsFromVp(argc, vp);
						if (argc == 2)
						{
							JS::RootedString objkey(cx, JS::ToString(cx, args[0]));
							JS::RootedString objval(cx, JS::ToString(cx, args[1]));
							JS::RootedObject thisv(cx, &args.thisv().toObject());
							JS::RootedValue data(cx);
							JS_GetProperty(cx, thisv, "data", &data);
							DictionaryCachePtrGCWrapper* p = (DictionaryCachePtrGCWrapper*)JS_GetPrivate(&data.toObject());
							char *pkey = JS_EncodeStringToUTF8(cx, objkey);
							char *pval = JS_EncodeStringToUTF8(cx, objval);
							p->cache->put(pkey, pval);
							JS_free(cx, pkey);
							JS_free(cx, pval);
						}
						args.rval().setUndefined();
						return true;
					}, 0, 0);
					JS_DefineFunction(cx, objcache, "keys", [](JSContext *cx, unsigned argc, JS::Value *vp){
						JS::CallArgs args = JS::CallArgsFromVp(argc, vp);
						JS::RootedObject thisv(cx, &args.thisv().toObject());
						JS::RootedValue data(cx);
						JS_GetProperty(cx, thisv, "data", &data);
						DictionaryCachePtrGCWrapper* p = (DictionaryCachePtrGCWrapper*)JS_GetPrivate(&data.toObject());
						JS::AutoValueVector v(cx);
						for (auto& key : p->cache->keys())
							v.append(JS::StringValue(JS_NewStringFromUTF8(cx, key.c_str(), key.length())));
						args.rval().setObject(*JS_NewArrayObject(cx, v));
						return true;
					}, 0, 0);
				}
				else
				{
					JS_SetProperty(cx, global, "cache", JS::RootedValue(cx));
				}
				opts.setFile(initscript).setUTF8(true);
				JS::Evaluate(cx, opts, initscript, &rval);
				if (providers.empty())
				{
					JS_GetProperty(cx, global, "providers", &rval);
					JS::RootedValue lenval(cx);
					JS::RootedObject array(cx, &rval.toObject());
					JS_GetProperty(cx, array, "length", &lenval);
					uint32_t len = lenval.toPrivateUint32();
					for (uint32_t i = 0; i < len; i++)
					{
						JS_GetElement(cx, array, i, &rval);
						providers.insert(rval.toInt32());
					}
				}
			});
		}

		virtual const hashset<int32_t>& getProviders() override
		{
			return providers;
		}

		virtual int action(int t, const std::string& p) override
		{
			int r;
			engine->wait([t, p, &r, this](JSRuntime*, JSContext* cx, JS::HandleObject global){
				JS::RootedValue rval(cx);
				JS::AutoValueArray<2> args(cx);
				args[0].setInt32(t);
				args[1].setString(JS_NewStringFromUTF8(cx, p.c_str(), p.length()));
				JS_CallFunctionName(cx, global, "limax", args, &rval);
				r = rval.toInt32();
			});
			return r;
		}

		virtual void registerScriptSender(ScriptSender sender) override
		{
			engine->wait([sender, this](JSRuntime*, JSContext* cx, JS::HandleObject global){
				JS::RootedValue rval(cx);
				JS::AutoValueArray<2> args(cx);
				JS::RootedObject funcobj(cx, JS_GetFunctionObject(JS_NewFunction(cx, [](JSContext *cx, unsigned argc, JS::Value *vp)
				{
					JS::CallArgs args = JS::CallArgsFromVp(argc, vp);
					JS::RootedObject callee(cx, &args.callee());
					JS::RootedValue data(cx);
					JS_GetProperty(cx, callee, "data", &data);
					ScriptSenderGCWrapper* p = (ScriptSenderGCWrapper*)JS_GetPrivate(&data.toObject());
					char *msg = JS_EncodeStringToUTF8(cx, JS::RootedString(cx, JS::ToString(cx, args[0])));
					if (p->send(msg))
						args.rval().setUndefined();
					else
						args.rval().setString(JS_NewStringCopyZ(cx, "send fail"));
					JS_free(cx, msg);
					return true;
				}, 0, 0, nullptr)));
				JS_SetPrivate(JS_DefineObject(cx, funcobj, "data", &senderholder_class), new ScriptSenderGCWrapper(sender));
				args[0].setInt32(0);
				args[1].setObject(*funcobj);
				JS_CallFunctionName(cx, global, "limax", args, &rval);
			});
		}

		virtual std::vector<std::string> getDictionaryKeys() override
		{
			return cache ? cache->keys() : std::vector<std::string>();
		}
	};

	JSClass JsEngineHandle::cacheholder_class = { "cacheholder_class", JSCLASS_HAS_PRIVATE, nullptr, nullptr, nullptr, nullptr, nullptr, nullptr, nullptr, finalize_cacheholder, nullptr, nullptr, nullptr, JS_GlobalObjectTraceHook };
	JSClass JsEngineHandle::senderholder_class = { "senderholder_class", JSCLASS_HAS_PRIVATE, nullptr, nullptr, nullptr, nullptr, nullptr, nullptr, nullptr, finalize_senderholder, nullptr, nullptr, nullptr, JS_GlobalObjectTraceHook };

	ScriptEngineHandlePtr JsCreator::createScriptEngineHandle(JsEnginePtr engine, const char *initscript, std::vector<int> providers, DictionaryCachePtr cache)
	{
		return std::make_shared<JsEngineHandle>(engine, initscript, providers, cache);
	}

	ScriptEngineHandlePtr JsCreator::createScriptEngineHandle(JsEnginePtr engine, const char *initscript)
	{
		return createScriptEngineHandle(engine, initscript, std::vector<int>(), DictionaryCachePtr());
	}

	ScriptEngineHandlePtr JsCreator::createScriptEngineHandle(JsEnginePtr engine, const char *initscript, std::vector<int> providers)
	{
		return createScriptEngineHandle(engine, initscript, providers, DictionaryCachePtr());
	}

	ScriptEngineHandlePtr JsCreator::createScriptEngineHandle(JsEnginePtr engine, const char *initscript, DictionaryCachePtr cache)
	{
		return createScriptEngineHandle(engine, initscript, std::vector<int>(), cache);
	}
}