#pragma once

namespace limax {

	enum
	{
		SOURCE_LIMAX = 0,
		SOURCE_PLAT = 1,
		SOURCE_ENDPOINT = 2,

		SUCCEED = 0,
		SWITCHER_AUANY_UNREADY = 11,
		SWITCHER_AUANY_TIMEOUT = 12,
		SWITCHER_SEND_DISPATCH_EXCEPTION = 13,
		SWITCHER_SEND_TO_ENDPOINT_EXCEPTION = 14,
		SWITCHER_DHGROUP_NOTSUPPRTED = 15,
		SWITCHER_LOST_PROVIDER = 16,
		SWITCHER_PROVIDER_UNBIND = 17,
		SWITCHER_WRONG_PROVIDER = 18,
		AUANY_UNKNOWN_PLAT = 1001,
		AUANY_BAD_TOKEN = 1002,
		AUANY_AUTHENTICATE_TIMEOUT = 1003,
		AUANY_AUTHENTICATE_FAIL = 1004,
		AUANY_CALL_PROCEDURE_FAILED = 1005,
		AUANY_CHECK_LOGIN_IP_FAILED = 1006,
		AUANY_CHECK_PROVIDER_KEY_UNKNOWN_PVID = 1007,
		AUANY_CHECK_PROVIDER_KEY_BAD_KEY = 1008,
		AUANY_SERVICE_BAD_ARGS = 1021,
		AUANY_SERVICE_BIND_HAS_BEEN_BOUND = 1022,
		AUANY_SERVICE_BIND_ACCOUNT_HAS_BEEN_USED = 1023,
		AUANY_SERVICE_PAY_NOT_ENABLED = 1026,
		AUANY_SERVICE_PAY_GATEWAY_NOT_DEFINED = 1027,
		AUANY_SERVICE_PAY_GATEWAY_FAIL = 1028,
		AUANY_SERVICE_INVALID_INVITE = 1030,
		AUANY_SERVICE_INVALID_CREDENTIAL = 1031,
		AUANY_SERVICE_CREDENTIAL_NOT_MATCH = 1032,
		AUANY_SERVICE_ACCOUNT_TOO_MANY_SUBORDINATES = 1033,
		AUANY_SERVICE_TRANSFER_APPID_COLLISION = 1034,
		ENDPOINT_PING_TIMEOUT = 2001,
		ENDPOINT_AUANY_SERVICE_CLIENT_TIMEOUT = 2002,
		ENDPOINT_AUANY_SERVICE_ENGINE_CLOSE = 2003,
		PROVIDER_DUPLICATE_ID = 3002,
		PROVIDER_UNSUPPORTED_VARINAT = 3004,
		PROVIDER_NOT_ALLOW_VARINAT = 3005,
		PROVIDER_UNSUPPORTED_SCRIPT = 3006,
		PROVIDER_NOT_ALLOW_SCRIPT = 3007,
		PROVIDER_KICK_SESSION = 3008,
		PROVIDER_SESSION_LOGINED = 3011,
		PROVIDER_ADD_TRANSPORT_EXCEPTION = 3012,

		SYSTEM_SUCCEED = SUCCEED,
		SYSTEM_ENDPOINT_RECV_UNKNOWN_PROTOCOL = 3,
		SYSTEM_VIEW_MARSHAL_EXCEPTION = 4,
		SYSTEM_VIEW_LOST_INSTANCE = 5,
		SYSTEM_VIEW_LOST_FIELD = 6,
		SYSTEM_VIEW_BAD_PROTOCOL_DATA = 7,
		SYSTEM_PARSE_VARIANT_DEFINES_EXCEPTION = 8,

		FLAG_ACCOUNT_BOUND = 1,
		FLAG_TEMPORARY_LOGIN = 2,
		FLAG_CAN_FLOW_CONTROL = 4,

		TEMPORARY_CREDENTIAL_USAGE_LOGIN = 1,
		TEMPORARY_CREDENTIAL_USAGE_TRANSFER = 2,
	};

	struct LIMAX_DLL_EXPORT_API EndpointConfig;
	struct LIMAX_DLL_EXPORT_API EndpointManager
	{
		EndpointManager();
		virtual ~EndpointManager();
		virtual Transport* getTransport() = 0;
		virtual void close() = 0;
		virtual int64_t getSessionId() const = 0;
		virtual int64_t getAccountFlags() const = 0;
		virtual ViewContext* getViewContext(int32_t pvid, ViewContext::Type type) const = 0;
	};

	struct LIMAX_DLL_EXPORT_API Transport
	{
		virtual void sendProtocol(const limax::Protocol&) = 0;
		Transport();
		virtual ~Transport();
		virtual EndpointManager* getManager() = 0;
		virtual const IPEndPoint& getPeerAddress() const = 0;
		virtual const IPEndPoint& getLocalAddress() const = 0;
		virtual void* getSessionObject() = 0;
		virtual void setSessionObject(void *) = 0;
	};

	struct LIMAX_DLL_EXPORT_API EndpointConfig
	{
		EndpointConfig();
		virtual ~EndpointConfig();
		virtual int getDHGroup() const = 0;
		virtual const std::string& getServerIP() const = 0;
		virtual int getServerPort() const = 0;
		virtual const std::string& getUserName() const = 0;
		virtual const std::string& getToken() const = 0;
		virtual const std::string& getPlatFlag() const = 0;
		virtual bool isPingServerOnly() const = 0;
		virtual bool auanyService() const = 0;
		virtual std::shared_ptr<State> getEndpointState() const = 0;
		virtual Executor getExecutor() const = 0;
		virtual const std::vector<std::shared_ptr<View::ViewCreatorManager>>& getStaticViewCreatorManagers() const = 0;
		virtual const std::vector<int32_t>& getVariantProviderIds() const = 0;
		virtual std::shared_ptr<ScriptEngineHandle> getScriptEngineHandle() const = 0;
	};

	struct LIMAX_DLL_EXPORT_API EndpointListener
	{
		EndpointListener();
		virtual ~EndpointListener();
		virtual void onManagerInitialized(EndpointManager*, EndpointConfig*) = 0;
		virtual void onTransportAdded(Transport*) = 0;
		virtual void onTransportRemoved(Transport*) = 0;
		virtual void onAbort(Transport*) = 0;
		virtual void onManagerUninitialized(EndpointManager*) = 0;
		virtual void onSocketConnected() = 0;
		virtual void onKeyExchangeDone() = 0;
		virtual void onKeepAlived(int ping) = 0;
		virtual void onErrorOccured(int errorsource, int errorvalue, const std::string& info) = 0;
		virtual void destroy() = 0;
	};

	typedef std::function<void(int32_t, int32_t, Octets)> TunnelSender;

	struct LIMAX_DLL_EXPORT_API TunnelSupport
	{
		virtual void onTunnel(int32_t providerid, int32_t label, Octets data) = 0;
		virtual void registerTunnelSender(TunnelSender sender) = 0;
	};

	struct LIMAX_DLL_EXPORT_API EndpointConfigBuilder
	{
		EndpointConfigBuilder();
		virtual ~EndpointConfigBuilder();
		virtual std::shared_ptr<EndpointConfigBuilder> endpointState(std::initializer_list<std::shared_ptr<State>>) = 0;
		virtual std::shared_ptr<EndpointConfigBuilder> endpointState(std::vector<std::shared_ptr<State>>) = 0;
		virtual std::shared_ptr<EndpointConfigBuilder> staticViewCreatorManagers(std::initializer_list<std::shared_ptr<View::ViewCreatorManager>>) = 0;
		virtual std::shared_ptr<EndpointConfigBuilder> staticViewCreatorManagers(std::vector<std::shared_ptr<View::ViewCreatorManager>>) = 0;
		virtual std::shared_ptr<EndpointConfigBuilder> variantProviderIds(std::initializer_list<int32_t>) = 0;
		virtual std::shared_ptr<EndpointConfigBuilder> variantProviderIds(std::vector<int32_t>) = 0;
		virtual std::shared_ptr<EndpointConfigBuilder> scriptEngineHandle(std::shared_ptr<ScriptEngineHandle>) = 0;
		virtual std::shared_ptr<EndpointConfigBuilder> executor(Executor) = 0;
		virtual std::shared_ptr<EndpointConfigBuilder> auanyService(bool) = 0;
		virtual std::shared_ptr<EndpointConfig> build() const = 0;
	};
	struct LIMAX_DLL_EXPORT_API ServiceInfo
	{
		ServiceInfo();
		virtual ~ServiceInfo();
		virtual const std::vector<int32_t> getPvids() const = 0;
		virtual const std::vector<int32_t> getPayids() const = 0;
		virtual const std::vector<std::shared_ptr<JSON>> getUserJSONs() const = 0;
		virtual bool isRunning() const = 0;
		virtual const std::string getOptional() const = 0;
	};
	struct LIMAX_DLL_EXPORT_API Closeable
	{
		virtual void close() = 0;
	};
	struct LIMAX_DLL_EXPORT_API Endpoint
	{
		static void openEngine();
		static void closeEngine(Runnable);
		static std::shared_ptr<EndpointConfigBuilder> createLoginConfigBuilder(const std::string& serverip, int serverport, const std::string& username, const std::string& token, const std::string& platflag);
		static std::shared_ptr<EndpointConfigBuilder> createLoginConfigBuilder(const std::shared_ptr<ServiceInfo>& service, const std::string& username, const std::string& token, const std::string& platflag);
		static std::shared_ptr<EndpointConfigBuilder> createPingOnlyConfigBuilder(const std::string& serverip, int serverport);
		static std::vector<std::shared_ptr<ServiceInfo>> loadServiceInfos(const std::string& httpHost, int httpPort, int appid, long timeout, int maxsize, const std::string& cacheDir, bool staleEnable);
		static void start(std::shared_ptr<EndpointConfig>, EndpointListener* handler);
		static std::shared_ptr<Closeable> start(const std::string& ip, short port, const std::string& username, const std::string& token, const std::string& platflag, ScriptEngineHandlePtr handle);
		static EndpointManager* getDefaultEndpointManager();
	};
	struct LIMAX_DLL_EXPORT_API AuanyService
	{
		enum : int32_t { providerId = 1 };
		typedef std::function<void(int, int, const std::string&)> Result;
		static void derive(const std::shared_ptr<ServiceInfo>& service, const std::string& authcode, long timeout, Result onresult);
		static void derive(const std::string& httpHost, int httpPort, int appid, const std::string& authcode, long timeout, Result onresult);
		static void derive(const std::string& credential, const std::string& authcode, long timeout, Result onresult, EndpointManager* manager);
		static void derive(const std::string& credential, const std::string& authcode, long timeout, Result onresult);
		static void bind(const std::shared_ptr<ServiceInfo>& service, const std::string& authcode, const std::string& username, const std::string& token, const std::string& platflag, long timeout, Result onresult);
		static void bind(const std::string& httpHost, int httpPort, int appid, const std::string& authcode, const std::string& username, const std::string& token, const std::string& platflag, long timeout, Result onresult);
		static void bind(const std::string& credential, const std::string& authcode, const std::string& username, const std::string& token, const std::string& platflag, long timeout, Result onresult, EndpointManager* manager);
		static void bind(const std::string& credential, const std::string& authcode, const std::string& username, const std::string& token, const std::string& platflag, long timeout, Result onresult);
		static void temporary(const std::shared_ptr<ServiceInfo>& service, const std::string& credential, const std::string& authcode, const std::string& authcode2, long millisecond, int8_t usage, const std::string& subid, long timeout, Result onresult);
		static void temporary(const std::string& httpHost, int httpPort, int appid, const std::string& credential, const std::string& authcode, const std::string& authcode2, long millisecond, int8_t usage, const std::string& subid, long timeout, Result onresult);
		static void temporary(const std::string& credential, const std::string& authcode, const std::string& authcode2, long millisecond, int8_t usage, const std::string& subid, long timeout, Result onresult, EndpointManager* manager);
		static void temporary(const std::string& credential, const std::string& authcode, const std::string& authcode2, long millisecond, int8_t usage, const std::string& subid, long timeout, Result onresult);
		static void temporary(const std::shared_ptr<ServiceInfo>& service, const std::string& username, const std::string& token, const std::string& platflag, const std::string& authcode, long millisecond, int8_t usage, const std::string& subid, long timeout, Result onresult);
		static void temporary(const std::string& httpHost, int httpPort, int appid, const std::string& username, const std::string& token, const std::string& platflag, const std::string& authcode, long millisecond, int8_t usage, const std::string& subid, long timeout, Result onresult);
		static void temporary(const std::string& username, const std::string& token, const std::string& platflag, int appid, const std::string& authcode, long millisecond, int8_t usage, const std::string& subid, long timeout, Result onresult, EndpointManager* manager);
		static void temporary(const std::string& username, const std::string& token, const std::string& platflag, int appid, const std::string& authcode, long millisecond, int8_t usage, const std::string& subid, long timeout, Result onresult);
		static void transfer(const std::shared_ptr<ServiceInfo>& service, const std::string& username, const std::string& token, const std::string& platflag, const std::string& authcode, const std::string& temp, const std::string& authtemp, long timeout, Result onresult);
		static void transfer(const std::string& httpHost, int httpPort, int appid, const std::string& username, const std::string& token, const std::string& platflag, const std::string& authcode, const std::string& temp, const std::string& authtemp, long timeout, Result onresult);
		static void pay(int gateway, int payid, int product, int price, int count, const std::string& invoice, long timeout, Result onresult, EndpointManager *manager);
		static void pay(int gateway, int payid, int product, int price, int count, const std::string& invoice, long timeout, Result onresult);
	};
} // namespace limax {
