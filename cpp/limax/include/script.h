#pragma once

namespace limax {

	typedef std::function<void(int, int, const std::string&)> ScriptErrorCollector;
	typedef std::function<bool(const std::string&)> ScriptSender;

	class LIMAX_DLL_EXPORT_API ScriptEngineHandle
	{
	public:
		ScriptEngineHandle();
		virtual ~ScriptEngineHandle();
	public:
		virtual const hashset<int32_t>& getProviders() = 0;
		virtual int action(int, const std::string&) = 0;
		virtual void registerScriptSender(ScriptSender) = 0;
		virtual std::vector<std::string> getDictionaryKeys() = 0;
	};

	typedef std::shared_ptr<ScriptEngineHandle> ScriptEngineHandlePtr;

} // namespace limax {
