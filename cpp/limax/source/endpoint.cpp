#include "endpointinc.h"

#include "dh.h"
#include "xmlgeninc/_xmlgen_.hpp"
#include "erroroccured.h"
#include "variantdef.h"
#include "viewcontextimpl.h"
#include "scriptexchange.h"

namespace limax {

	EndpointListener::EndpointListener() {}
	EndpointListener::~EndpointListener() {}

	EndpointManager::EndpointManager() {}
	EndpointManager::~EndpointManager() {}

	Transport::Transport() {}
	Transport::~Transport() {}

	EndpointConfig::EndpointConfig() {}
	EndpointConfig::~EndpointConfig() {}

	EndpointConfigBuilder::EndpointConfigBuilder() {}
	EndpointConfigBuilder::~EndpointConfigBuilder() {}

	ServiceInfo::ServiceInfo() {}
	ServiceInfo::~ServiceInfo() {}

	namespace helper {

		struct EndpointConfigData
		{
			std::string serverIp;
			int serverPort;
			int dhGroup;
			std::string username;
			std::string token;
			std::string platflag;
			bool ispingonly;
			bool auanyService;
			std::shared_ptr<State> state;
			std::vector< std::shared_ptr<View::ViewCreatorManager> > vcms;
			std::vector<int32_t> vpvids;
			std::shared_ptr<ScriptEngineHandle> script;
			Executor executor;
		};

		struct EndpointConfigImpl : public EndpointConfig
		{
			EndpointConfigData	data;
			EndpointConfigImpl(const EndpointConfigData& _data) : data(_data){}
			virtual ~EndpointConfigImpl() {}
			virtual const std::string& getServerIP() const override
			{
				return data.serverIp;
			}
			virtual int getServerPort() const override
			{
				return data.serverPort;
			}
			virtual int getDHGroup() const override
			{
				return data.dhGroup;
			}
			virtual const std::string& getUserName() const override
			{
				return data.username;
			}
			virtual const std::string& getToken() const override
			{
				return data.token;
			}
			virtual const std::string& getPlatFlag()const  override
			{
				return data.platflag;
			}
			virtual bool isPingServerOnly() const override
			{
				return data.ispingonly;
			}
			virtual bool auanyService() const override
			{
				return data.auanyService;
			}
			virtual std::shared_ptr<State> getEndpointState() const override
			{
				return data.state;
			}
			virtual const std::vector< std::shared_ptr<View::ViewCreatorManager> >& getStaticViewCreatorManagers() const override
			{
				return data.vcms;
			}
			virtual const std::vector<int32_t>& getVariantProviderIds() const override
			{
				return data.vpvids;
			}
			virtual std::shared_ptr<ScriptEngineHandle> getScriptEngineHandle() const override
			{
				return data.script;
			}
			virtual Executor getExecutor() const override
			{
				return data.executor;
			}
		};

		struct EndpointConfigBuilderImpl : public EndpointConfigBuilder
		{
			EndpointConfigData data;
			std::weak_ptr<EndpointConfigBuilder> instance;
			EndpointConfigBuilderImpl(const std::string& serverip, int serverport, bool pingonly)
			{
				data.ispingonly = pingonly;
				data.auanyService = true;
				data.serverIp = serverip;
				data.serverPort = serverport;
				data.dhGroup = 2;
				data.executor = [](Runnable r){ r(); };
				data.state = limax::getEndpointStateEndpointClient(0);
			}
			virtual ~EndpointConfigBuilderImpl() { }
			virtual std::shared_ptr<EndpointConfigBuilder> endpointState(std::initializer_list<std::shared_ptr<State>> states) override
			{
				data.state = limax::getEndpointStateEndpointClient(0);
				for (auto& i : states)
					data.state->addStub(i);
				return instance.lock();
			}
			virtual std::shared_ptr<EndpointConfigBuilder> endpointState(std::vector<std::shared_ptr<State>> states) override
			{
				data.state = limax::getEndpointStateEndpointClient(0);
				for (auto& i : states)
					data.state->addStub(i);
				return instance.lock();
			}
			virtual std::shared_ptr<EndpointConfigBuilder> staticViewCreatorManagers(std::initializer_list<std::shared_ptr<View::ViewCreatorManager>> vcms) override
			{
				data.vcms.clear();
				data.vcms.reserve(vcms.size());
				data.vcms.insert(data.vcms.end(), vcms.begin(), vcms.end());
				return instance.lock();
			}
			virtual std::shared_ptr<EndpointConfigBuilder> staticViewCreatorManagers(std::vector<std::shared_ptr<View::ViewCreatorManager>> vcms) override
			{
				data.vcms.clear();
				data.vcms.reserve(vcms.size());
				data.vcms.insert(data.vcms.end(), vcms.begin(), vcms.end());
				return instance.lock();
			}
			virtual std::shared_ptr<EndpointConfigBuilder> variantProviderIds(std::initializer_list<int32_t> pvids) override
			{
				data.vpvids.clear();
				data.vpvids.reserve(pvids.size());
				data.vpvids.insert(data.vpvids.end(), pvids.begin(), pvids.end());
				return instance.lock();
			}
			virtual std::shared_ptr<EndpointConfigBuilder> variantProviderIds(std::vector<int32_t> pvids) override
			{
				data.vpvids.clear();
				data.vpvids.reserve(pvids.size());
				data.vpvids.insert(data.vpvids.end(), pvids.begin(), pvids.end());
				return instance.lock();
			}
			virtual std::shared_ptr<EndpointConfigBuilder> scriptEngineHandle(std::shared_ptr<ScriptEngineHandle> handle) override
			{
				data.script = handle;
				return instance.lock();
			}
			virtual std::shared_ptr<EndpointConfigBuilder> executor(Executor executor) override
			{
				data.executor = executor;
				return instance.lock();
			}
			virtual std::shared_ptr<EndpointConfigBuilder> auanyService(bool used) override
			{
				data.auanyService = used;
				return instance.lock();
			}
			virtual std::shared_ptr<EndpointConfig> build() const override
			{
				return std::shared_ptr<EndpointConfig>(new EndpointConfigImpl(data));
			}
		};

		struct ServiceInfoImpl : public ServiceInfo
		{
			std::vector<std::pair<std::string, int32_t>> switchers;
			int32_t appid;
			std::vector<int32_t> pvids;
			std::vector<int32_t> payids;
			std::vector<std::shared_ptr<JSON>> userjsons;
			std::string optional;
			bool running;
			ServiceInfoImpl(int32_t _appid, std::shared_ptr<JSON>json) : appid(_appid)
			{
				for (auto switcher : json->get("switchers")->toArray())
					switchers.push_back(std::make_pair(switcher->get("host")->toString(), switcher->get("port")->intValue()));
				for (auto i : json->get("pvids")->toArray())
					pvids.push_back(i->intValue());
				for (auto i : json->get("payids")->toArray())
					payids.push_back(i->intValue());
				for (auto i : json->get("userjsons")->toArray())
					userjsons.push_back(JSON::parse(i->toString()));
				running = json->get("running")->booleanValue();
				optional = json->get("optional")->toString();
			}
			std::pair<std::string, int32_t> randomSwitcherConfig()
			{
				std::random_shuffle(switchers.begin(), switchers.end());
				return switchers[0];
			}
			const std::vector<int32_t> getPvids() const { return pvids; }
			const std::vector<int32_t> getPayids() const { return payids; }
			const std::vector<std::shared_ptr<JSON>> getUserJSONs() const { return userjsons; }
			bool isRunning() const { return running; }
			const std::string getOptional() const { return optional; }
		};

		static inline void mapPvidsAppendValue(hashmap<int32_t, int8_t>& pvids, int32_t s, int nv)
		{
			pvids[s] = (int8_t)(nv | pvids[s]);
		}
		static inline void mapPvidsAppendValue(hashmap<int32_t, int8_t>& pvids, const hashset<int32_t>& set, int nv)
		{
			for (auto& it : set)
				mapPvidsAppendValue(pvids, it, nv);
		}
		static inline const hashset<int32_t> makeProtocolPvidSet(std::shared_ptr<EndpointConfig> config)
		{
			return config->getEndpointState()->getProviderIds();
		}

		static inline const hashset<int32_t> makeStaticPvidSet(std::shared_ptr<EndpointConfig> config)
		{
			hashset<int32_t> pvidset;
			const auto& vcms = config->getStaticViewCreatorManagers();
			for (auto& vcm : vcms)
				pvidset.insert(vcm->getProviderId());
			return pvidset;
		}

		static inline const hashset<int32_t> makeVariantPvidSet(std::shared_ptr<EndpointConfig> config)
		{
			hashset<int32_t> pvidset;
			const auto& pvids = config->getVariantProviderIds();
			pvidset.insert(pvids.begin(), pvids.end());
			return pvidset;
		}

		static inline const hashset<int32_t> makeScriptPvidSet(std::shared_ptr<EndpointConfig> config)
		{
			hashset<int32_t> pvidset;
			if (auto handle = config->getScriptEngineHandle())
				pvidset.insert(handle->getProviders().begin(), handle->getProviders().end());
			return pvidset;
		}

		static inline const hashset<int32_t> makeAuanyPvidSet()
		{
			hashset<int32_t> pvidset;
			pvidset.insert(limax::AuanyService::providerId);
			return pvidset;
		}

		struct MakeProviderMapException
		{
			MakeProviderMapException(int code) : errorcode(code){}
			MakeProviderMapException(int code, const std::string& msg) : errorcode(code), message(msg){}
			int errorcode;
			std::string message;
		};

		static inline void makeProviderMap(hashmap<int32_t, int8_t>& pvids, std::shared_ptr<EndpointConfig> config)
		{
			mapPvidsAppendValue(pvids, makeProtocolPvidSet(config), limax::defines::SessionType::ST_PROTOCOL);
			mapPvidsAppendValue(pvids, makeStaticPvidSet(config), limax::defines::SessionType::ST_STATIC);
			mapPvidsAppendValue(pvids, makeVariantPvidSet(config), limax::defines::SessionType::ST_VARIANT);
			mapPvidsAppendValue(pvids, makeScriptPvidSet(config), limax::defines::SessionType::ST_SCRIPT);
			if (config->auanyService())
				mapPvidsAppendValue(pvids, makeAuanyPvidSet(), limax::defines::SessionType::ST_STATIC);
		}

		struct ViewContextMap
		{
			struct ViewContextTypeHash
			{
				size_t operator()(ViewContext::Type t) const
				{
					return (size_t)t;
				}
			};
			struct ViewContextCollection
			{
				std::unordered_map<ViewContext::Type, std::shared_ptr<AbstractViewContext>, ViewContextTypeHash> map;
				void put(ViewContext::Type type, std::shared_ptr<AbstractViewContext> vc)
				{
					map.insert(std::make_pair(type, vc));
				}
				void onSyncViewToClients(limax::endpoint::providerendpoint::SyncViewToClients* protocol)
				{
					for (auto& it : map)
						it.second->onSyncViewToClients(protocol);
				}
				void clear()
				{
					for (auto& it : map)
						it.second->clear();
					map.clear();
				}
				std::shared_ptr<AbstractViewContext> getViewContext(ViewContext::Type type)
				{
					auto it = map.find(type);
					return it == map.end() ? nullptr : it->second;
				}
				int getSize() const
				{
					return (int)map.size();
				}
				std::shared_ptr<AbstractViewContext> getViewContext(ViewContext::Type type) const
				{
					auto it = map.find(type);
					return it == map.end() ? nullptr : it->second;
				}
			};
			hashmap<int32_t, ViewContextCollection> map;
			void put(ViewContext::Type type, int32_t pvid, std::shared_ptr<AbstractViewContext> vc)
			{
				ViewContextCollection& cc = map[pvid];
				cc.put(type, vc);
			}
			void onSyncViewToClients(limax::endpoint::providerendpoint::SyncViewToClients* protocol)
			{
				auto it = map.find(protocol->providerid);
				if (it != map.end())
					it->second.onSyncViewToClients(protocol);
			}
			void clear()
			{
				for (auto& it : map)
					it.second.clear();
				map.clear();
			}
			const std::shared_ptr<ViewContext> getViewContext(int32_t pvid, ViewContext::Type type) const
			{
				auto it = map.find(pvid);
				if (it == map.end())
					return nullptr;
				else
					return it->second.getViewContext(type);
			}
		};

		static std::atomic<EndpointManager*> g_defaultEndpointManager;

	} // namespace helper {

	std::shared_ptr<EndpointConfigBuilder> Endpoint::createLoginConfigBuilder(const std::string& serverip, int serverport, const std::string& username, const std::string& token, const std::string& platflag)
	{
		auto cb = std::shared_ptr<helper::EndpointConfigBuilderImpl>(new helper::EndpointConfigBuilderImpl(serverip, serverport, false));
		cb->instance = cb;
		cb->data.username = username;
		cb->data.token = token;
		cb->data.platflag = platflag;
		return cb;
	}
	std::shared_ptr<EndpointConfigBuilder> Endpoint::createLoginConfigBuilder(const std::shared_ptr<ServiceInfo>& service, const std::string& username, const std::string& token, const std::string& platflag)
	{
		const std::pair<std::string, int32_t> switcher = std::dynamic_pointer_cast<helper::ServiceInfoImpl>(service)->randomSwitcherConfig();
		return createLoginConfigBuilder(switcher.first, switcher.second, username, token, platflag);
	}
	std::shared_ptr<EndpointConfigBuilder> Endpoint::createPingOnlyConfigBuilder(const std::string& serverip, int serverport)
	{
		auto cb = std::shared_ptr<helper::EndpointConfigBuilderImpl>(new helper::EndpointConfigBuilderImpl(serverip, serverport, true));
		cb->instance = cb;
		cb->data.ispingonly = true;
		return cb;
	}
	std::vector<std::shared_ptr<ServiceInfo>> Endpoint::loadServiceInfos(const std::string& httpHost, int httpPort, int appid, long timeout, int maxsize, const std::string& cacheDir, bool staleEnable)
	{
		std::string jsonstring;
		{
			std::ostringstream oss;
			oss << "http://" << httpHost << ":" << httpPort << "/app?native=" << appid;
			jsonstring = limax::http::httpDownload(oss.str(), (int)timeout, maxsize, cacheDir, staleEnable);
		}
		std::vector<std::shared_ptr<ServiceInfo>> r;
		for (auto json : JSON::parse(jsonstring)->get("services")->toArray())
			r.push_back(std::make_shared<helper::ServiceInfoImpl>(appid, json));
		return r;
	}
	class EndpointImpl : public EndpointManager, public Transport, public NetSession::Listener
	{
		std::shared_ptr<EndpointConfig>  m_config;
		std::shared_ptr<EndpointListener> m_listener;
		NetSession*	m_session = nullptr;
		std::recursive_mutex	m_session_mutex;
		int64_t						m_sessionid = -1;
		int64_t						m_accountFlags = 0;
		IPEndPoint					m_localaddress;
		IPEndPoint					m_peeraddress;
		void*	m_object = nullptr;
		hashmap<int32_t, int8_t>	m_pvids;
		std::shared_ptr<limax::DHContext>	dhcontext;
		Dispatcher dispatcher;
		DelayedRunnable ping_timeout_runnable;
		DelayedRunnable ping_alive_delay_runnable;
		bool ping_cancelled = false;
		volatile bool timeout = false;
		helper::ViewContextMap	m_viewContextMap;
		helper::ScriptExchange	m_scriptexchange;
		volatile enum LoginStatus
		{
			LOGINING, LOGINED_NOTIFY, LOGINED_DONE,
		} loginstatus = LoginStatus::LOGINING;
	private:
		void startPingAndKeepAlive()
		{
			sendProtocol(limax::endpoint::switcherendpoint::PingAndKeepAlive(std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::steady_clock::now() - std::chrono::steady_clock::time_point::min()).count()));
			dispatcher([this](){
				Engine::execute(ping_timeout_runnable = DelayedRunnable([this](){
					timeout = true;
					close();
				}, 500, 10));
			});
		}
		void startConnect()
		{
			NetSession::create(this, this, m_config->getServerIP(), m_config->getServerPort());
		}
		void executeSessionTask(std::function<void(NetSession*&)> r)
		{
			std::lock_guard<std::recursive_mutex> l(m_session_mutex);
			if (m_session)
				r(m_session);
		}
		void createSessionTask(std::function<void(NetSession*&)> r)
		{
			std::lock_guard<std::recursive_mutex> l(m_session_mutex);
			r(m_session);
		}
		void onAddSession(NetSession* _session, const IPEndPoint& local, const IPEndPoint& peer) override
		{
			createSessionTask([=](NetSession*& session){
				session = _session;
				session->setState(getEndpointStateEndpointClient(0));
				session->setInputSecurity(false);
				session->setOutputSecurity(false);
				m_localaddress = local;
				m_peeraddress = peer;
				dispatcher([this](){m_listener->onSocketConnected(); });
				if (m_config->isPingServerOnly())
				{
					startPingAndKeepAlive();
				}
				else
				{
					const int dh_group = m_config->getDHGroup();
					dhcontext = createDHContext(dh_group);
					const std::vector<unsigned char>& data = dhcontext->generateDHResponse();
					session->send(limax::endpoint::switcherendpoint::CHandShake(dh_group, Octets(&data[0], data.size())));
				}
			});
		}
		void onAbortSession(NetSession* session) override
		{
			dispatcher([this]()
			{
				m_listener->onAbort(this);
				m_listener->onManagerUninitialized(this);
			});
			dispatcher.await();
			Engine::remove(this);
		}
		void onDelSession(NetSession* session) override
		{
			executeSessionTask([](NetSession*& session){ session = nullptr; });
			dispatcher([this](){
				ping_cancelled = true;
				ping_timeout_runnable.cancel();
				ping_alive_delay_runnable.cancel();
			});
			if (timeout)
				pushErrorOccured(SOURCE_ENDPOINT, ENDPOINT_PING_TIMEOUT, "ping server time out");
			dispatcher.await();
			dispatcher([this, session]()
			{
				if (LoginStatus::LOGINING == loginstatus)
				{
					if (!m_config->isPingServerOnly())
						m_listener->onAbort(this);
				}
				else
				{
					if (LoginStatus::LOGINED_DONE == loginstatus)
					{
						m_listener->onTransportRemoved(this);
					}
					if (LoginStatus::LOGINED_DONE == loginstatus || LoginStatus::LOGINED_NOTIFY == loginstatus) {
						EndpointManager* tmp = this;
						helper::g_defaultEndpointManager.compare_exchange_weak(tmp, nullptr);
						m_viewContextMap.clear();
						m_scriptexchange.onUnload();
					}
				}
				m_listener->onManagerUninitialized(this);
				Engine::execute([this](){
					dispatcher.await();
					ping_timeout_runnable.join();
					ping_alive_delay_runnable.join();
					Engine::remove(this);
				});
			});
		}
		void onProtocol(Protocol* p) override
		{
			if (Trace::isInfoEnabled())
			{
				auto rt = std::chrono::high_resolution_clock::now();
				dispatcher([p, rt](){
					auto dt = std::chrono::high_resolution_clock::now() - rt;
					auto elapsed = std::chrono::duration_cast<std::chrono::milliseconds>(dt).count();
					std::ostringstream oss;
					oss << "endpoint protocol process delay type = " << p->getType() << "  = " << elapsed;
					Trace::info(oss.str());
					p->process();
					p->destroy();
				});
			}
			else
			{
				dispatcher([p](){
					p->process();
					p->destroy();
				});
			}
		}
		void onCheckUnknownProtocol(NetSession* session, Protocol::Type type, int size) override
		{
			std::stringstream ss;
			ss << "type = " << type << " size = " << size;
			pushErrorOccured(SOURCE_ENDPOINT, SYSTEM_ENDPOINT_RECV_UNKNOWN_PROTOCOL, ss.str());
			throw Protocol::Exception();
		}
		void createStaticViewContexts()
		{
			auto pvid = AuanyService::providerId;
			auto vc = helper::StaticViewContextImpl::create(this, pvid, getAuanyviewsViewCreatorManager(pvid));
			m_viewContextMap.put(ViewContext::Static, pvid, vc);
			for (auto& vcm : m_config->getStaticViewCreatorManagers())
			{
				auto pvid = vcm->getProviderId();
				auto vc = helper::StaticViewContextImpl::create(this, pvid, vcm);
				m_viewContextMap.put(ViewContext::Static, pvid, vc);
			}
		}
		void createVariantViewContexts(const hashmap<int32_t, limax::defines::VariantDefines>& vdmap)
		{
			for (auto& it : vdmap)
			{
				auto pvid = it.first;
				const auto& vd = it.second;
				auto vc = helper::VariantViewContextImpl::create(this, pvid, vd);
				if (vc)
					m_viewContextMap.put(ViewContext::Variant, pvid, vc);
				else
					close();
			}
		}
		void createScriptViewContexts(const std::string& defines)
		{
			m_scriptexchange.onLoad(this, m_config->getScriptEngineHandle(), defines);
		}
	public:
		EndpointImpl(std::shared_ptr<EndpointConfig> config, const hashmap<int32_t, int8_t>& pvids, EndpointListener* listener)
			: m_config(config), m_listener(std::shared_ptr<EndpointListener>(listener, [](EndpointListener *listener){ listener->destroy(); }))
			, m_pvids(pvids), dispatcher(config->getExecutor())
		{
			Engine::add(this);
			dispatcher([this](){ m_listener->onManagerInitialized(this, m_config.get()); });
			if (std::shared_ptr<TunnelSupport> ts = std::dynamic_pointer_cast<TunnelSupport>(m_listener)){
				ts->registerTunnelSender([this](int providerid, int label, Octets data){
					sendProtocol(limax::endpoint::providerendpoint::Tunnel(providerid, 0, label, data));
				});
			}
			startConnect();
		}
		void close() override
		{
			executeSessionTask([](NetSession*& session) { session->close(); });
		}
		virtual int64_t getSessionId() const override{ return m_sessionid; }
		virtual int64_t getAccountFlags() const override{ return m_accountFlags; }
		virtual void sendProtocol(const limax::Protocol& p) override
		{
			executeSessionTask([&p](NetSession*& session) { session->send(&p); });
		}
		virtual ViewContext* getViewContext(int32_t pvid, ViewContext::Type type) const override
		{
			return m_viewContextMap.getViewContext(pvid, type).get();
		}
		void unmarshalViewException(int32_t pvid, int16_t classIndex, int32_t instanceIndex)
		{
			std::stringstream ss;
			ss << "providerId = " << pvid << " classIndex = " << classIndex << " instanceIndex = " << instanceIndex;
			fireErrorOccured(SOURCE_ENDPOINT, SYSTEM_VIEW_MARSHAL_EXCEPTION, ss.str());
			close();
		}
		void fireErrorOccured(int source, int code, const std::string& info)
		{
			m_listener->onErrorOccured(source, code, info);
		}
		void pushErrorOccured(int source, int code, const std::string& info)
		{
			dispatcher([=](){ fireErrorOccured(source, code, info); });
		}
		void onProtocolOnlineAnnounce(limax::endpoint::switcherendpoint::OnlineAnnounce* p)
		{
			if (SOURCE_LIMAX == p->errorSource && SUCCEED == p->errorCode)
			{
				m_accountFlags = p->flags;
				m_sessionid = p->sessionid;
				createStaticViewContexts();
				createVariantViewContexts(p->variantdefines);
				createScriptViewContexts(p->scriptdefines);
				loginstatus = LoginStatus::LOGINED_NOTIFY;
				EndpointManager*tmp = nullptr;
				helper::g_defaultEndpointManager.compare_exchange_weak(tmp, this);
				dispatcher([this](){m_listener->onTransportAdded(this); });
				loginstatus = LoginStatus::LOGINED_DONE;
				startPingAndKeepAlive();
			}
			else
			{
				fireErrorOccured(p->errorSource, p->errorCode, "switcherendpoint::OnlineAnnounce");
			}
		}
		void onProtocolSKeyExchange(limax::endpoint::switcherendpoint::SHandShake* p)
		{
			executeSessionTask([this, p](NetSession *&session) {
				const std::vector<unsigned char> material = dhcontext->computeDHKey((unsigned char*)p->dh_data.begin(), (int32_t)p->dh_data.size());
				socklen_t key_len;
				int8_t *key = (int8_t*)m_peeraddress.getAddress(key_len);
				int32_t half = (int32_t)material.size() / 2;
				{
					HmacMD5 hmac(key, 0, key_len);
					hmac.update((int8_t*)&material[0], 0, half);
					session->setOutputSecurity(p->c2sneedcompress, Octets(hmac.digest(), 16));
				}
				{
					HmacMD5 hmac(key, 0, key_len);
					hmac.update((int8_t*)&material[0], half, (int32_t)material.size() - half);
					session->setInputSecurity(p->s2cneedcompress, Octets(hmac.digest(), 16));
				}
				dispatcher([this](){m_listener->onKeyExchangeDone(); });
				dhcontext.reset();
				limax::endpoint::switcherendpoint::SessionLoginByToken protocol;
				protocol.username = m_config->getUserName();
				protocol.token = m_config->getToken();
				protocol.platflag = m_config->getPlatFlag();
				ScriptEngineHandlePtr seh = m_config->getScriptEngineHandle();
				if (seh)
				{
					std::string join;
					for (auto s : seh->getDictionaryKeys())
					{
						join.push_back(',');
						join.append(s);
					}
					if (join.size())
					{
						join[0] = ';';
						protocol.platflag.append(join);
					}
				}
				socklen_t localaddress_len;
				const void *localaddress = m_localaddress.getAddress(localaddress_len);
				protocol.report_ip.insert(protocol.report_ip.begin(), localaddress, localaddress_len);
				protocol.report_port = m_localaddress.getPort();
				protocol.pvids.insert(m_pvids.begin(), m_pvids.end());
				session->send(&protocol);
				session->setState(m_config->getEndpointState());
			});
		}
		void onProtocolPingAndKeepAlive(limax::endpoint::switcherendpoint::PingAndKeepAlive* p)
		{
			if (ping_cancelled)
				return;
			ping_timeout_runnable.cancel();
			dispatcher([this, p](){m_listener->onKeepAlived((int)(std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::steady_clock::now() - std::chrono::steady_clock::time_point::min()).count() - p->timestamp)); });
			if (!m_config->isPingServerOnly())
				Engine::execute(ping_alive_delay_runnable = DelayedRunnable([this](){startPingAndKeepAlive(); }, 500, 100));
		}
		void onProtocolSessionKick(limax::endpoint::switcherendpoint::SessionKick* p)
		{
			fireErrorOccured(SOURCE_LIMAX, p->error, "switcherendpoint::SessionKick");
			m_scriptexchange.onClose(p->error);
		}
		void onProtocolSyncViewToClients(limax::endpoint::providerendpoint::SyncViewToClients* protocol)
		{
			m_viewContextMap.onSyncViewToClients(protocol);
			m_scriptexchange.onSyncViewToClients(protocol);
		}
		void onProtocolTunnel(limax::endpoint::providerendpoint::Tunnel* protocol)
		{
			auto providerid = protocol->providerid;
			auto label = protocol->label;
			auto data = protocol->data;
			dispatcher([this, providerid, label, data](){
				if (std::shared_ptr<TunnelSupport> ts = std::dynamic_pointer_cast<TunnelSupport>(m_listener))
					ts->onTunnel(providerid, label, data);
			});
		}
		EndpointManager* getManager() override { return this; }
		const IPEndPoint& getPeerAddress() const override { return m_peeraddress; }
		const IPEndPoint& getLocalAddress() const override { return m_localaddress; }
		void* getSessionObject() override { return m_object; }
		void setSessionObject(void *obj) override { m_object = obj; }
		Transport* getTransport() override { return this; }
	};

	namespace erroroccured {

		void fireErrorOccured(EndpointManager* manager, int type, int code, const std::string& info)
		{
			if (auto impl = dynamic_cast<EndpointImpl*>(manager))
				impl->fireErrorOccured(type, code, info);
		}

	} // namespace erroroccured {

	void Endpoint::openEngine(){ Engine::open(); }

	void Endpoint::closeEngine(Runnable done){ Engine::close(done); }

	void Endpoint::start(std::shared_ptr<EndpointConfig> config, EndpointListener* handler)
	{
		Engine::execute([config, handler](){
			hashmap<int32_t, int8_t> pvids;
			if (!config->isPingServerOnly())
			{
				try
				{
					helper::makeProviderMap(pvids, config);
				}
				catch (helper::MakeProviderMapException& e)
				{
					config->getExecutor()([=](){handler->onErrorOccured(SOURCE_ENDPOINT, e.errorcode, e.message); });
					return;
				}
			}
			new EndpointImpl(config, pvids, handler);
		});
	}

	EndpointManager* Endpoint::getDefaultEndpointManager()
	{
		return helper::g_defaultEndpointManager.load();
	}

	namespace endpoint {
		namespace switcherendpoint {

			void SHandShake::process()
			{
				if (EndpointImpl* manager = dynamic_cast<EndpointImpl*>(getTransport()->getManager()))
					manager->onProtocolSKeyExchange(this);
			}

			void OnlineAnnounce::process()
			{
				if (EndpointImpl* manager = dynamic_cast<EndpointImpl*>(getTransport()->getManager()))
					manager->onProtocolOnlineAnnounce(this);
			}

			void PingAndKeepAlive::process()
			{
				if (EndpointImpl* manager = dynamic_cast<EndpointImpl*>(getTransport()->getManager()))
					manager->onProtocolPingAndKeepAlive(this);
			}

			void SessionKick::process()
			{
				if (EndpointImpl* manager = dynamic_cast<EndpointImpl*>(getTransport()->getManager()))
					manager->onProtocolSessionKick(this);
			}

			void PortForward::process() {}
			void CHandShake::process() {}
			void SessionLoginByToken::process() {}

		} // namespace switcherendpoint { 

		namespace providerendpoint {

			void SyncViewToClients::process()
			{
				auto impl = dynamic_cast<EndpointImpl*>(getTransport()->getManager());
				if (impl)
				{
					try
					{
						impl->onProtocolSyncViewToClients(this);
					}
					catch (MarshalException&)
					{
						impl->unmarshalViewException(providerid, classindex, instanceindex);
					}
				}
			}

			void Tunnel::process()
			{
				if (EndpointImpl* manager = dynamic_cast<EndpointImpl*>(getTransport()->getManager()))
					manager->onProtocolTunnel(this);
			}

			void SendControlToServer::process() {}

		} // namespace providerendpoint { 
	} // namespace endpoint {

	std::mutex Engine::mutex;
	std::mutex Engine::closeables_mutex;
	std::condition_variable_any Engine::cond;
	std::unordered_set<EndpointImpl*> Engine::set;
	std::unordered_set<std::shared_ptr<Closeable>> Engine::closeables;
	ThreadPool *Engine::pool;
	void Engine::open()
	{
		std::lock_guard<std::mutex> l(mutex);
		if (!pool)
		{
			helper::OsSystemInit::getInstance().Startup();
			pool = new ThreadPool(30000);
		}
	}

	namespace helper { void cleanupAuanyService(); }
	void Engine::close(Runnable done)
	{
		helper::cleanupAuanyService();
		std::thread([done](){
			std::lock_guard<std::mutex> l(mutex);
			if (pool)
			{
				while (!closeables.empty())
					remove(*closeables.begin());
				for (auto& e : std::vector<EndpointImpl*>(set.begin(), set.end()))
					e->close();
				while (set.size())
					cond.wait(mutex);
				delete pool;
				pool = nullptr;
				helper::OsSystemInit::getInstance().Cleanup();
			}
			if (done)
				done();
		}).detach();
	}

	void Engine::add(EndpointImpl* e)
	{
		std::lock_guard<std::mutex> l(mutex);
		set.insert(e);
	}

	void Engine::add(std::shared_ptr<Closeable> c)
	{
		std::lock_guard<std::mutex> l(closeables_mutex);
		closeables.insert(c);
	}

	void Engine::remove(EndpointImpl* e)
	{
		std::lock_guard<std::mutex> l(mutex);
		if (set.erase(e) == 0)
			return;
		if (set.empty())
			cond.notify_one();
		delete e;
	}

} // namespace limax {

namespace limax
{
	struct WebSocketConnector : public Closeable, public TcpClient::Listener {
		int const DHGroup = 2;
		enum CloseStatus { CONNECTION_FAIL, ACTIVE_CLOSED, PASSIVE_CLOSED };
		enum ReadyState { CONNECTING, OPEN, CLOSING, CLOSED };
		TcpClient* tcpclient;
		std::string query;
		ReadyState readyState;
		Octets buffer;
		OctetsUnmarshalStreamSource datasrc;
		std::weak_ptr<WebSocketConnector> self;
		std::recursive_mutex mutex;
		DelayedRunnable timer;
		ScriptEngineHandlePtr handle;
		int reportError;
		int stage;
		std::string sbhead;
		IPEndPoint peer;
		std::shared_ptr<limax::DHContext> dhcontext;
		std::shared_ptr<Codec> isec;
		std::shared_ptr<Codec> osec;
		Octets ibuf;
		Octets obuf;

		WebSocketConnector(const std::string& _query, ScriptEngineHandlePtr _handle)
			: tcpclient(nullptr), query(_query), readyState(CONNECTING), datasrc(buffer)
			, handle(_handle), reportError(0), stage(0)
		{
		}
		void process(int t, const std::string& p)
		{
			switch (handle->action(t, p))
			{
			case 2:
				Engine::execute(timer = DelayedRunnable([this](){ send(" "); }, 500, 100));
				break;
			case 3:
				close();
			}
		}

		void onopen()
		{
		}

		void onmessage(const std::string& message)
		{
			std::shared_ptr<WebSocketConnector> _this(self);
			if (isdigit(message[0]))
				reportError = atoi(message.c_str());
			else
				runOnUiThread([_this, message](){_this->process(1, message); });
		}

		void onclose(CloseStatus status)
		{
			std::stringstream ss;
			ss << reportError;
			std::string message(ss.str());
			std::shared_ptr<WebSocketConnector> _this(self);
			runOnUiThread([_this, status, message](){ _this->process(2, message); });
		}

		virtual void onCreate(TcpClient *tcpclient) override
		{
			this->tcpclient = tcpclient;
		}

		virtual void onOpen(const IPEndPoint& local, const IPEndPoint& peer) override
		{
			std::stringstream request;
			request << "GET / HTTP/1.1\r\nConnection: Upgrade\r\nUpgrade: WebSocket\r\nSec-WebSocket-Version: 13\r\nSec-WebSocket-Key: AQIDBAUGBwgJCgsMDQ4PEC==\r\nOrigin: null\r\n";
			dhcontext = createDHContext(DHGroup);
			const std::vector<unsigned char>& data = dhcontext->generateDHResponse();
			request << "X-Limax-Security: " << DHGroup << ';';
			Octets o = Base64Encode::transform(Octets(&data[0], data.size()));
			int8_t e = 0;
			o.insert(o.end(), &e, 1);
			request << (char *)o.begin() << "\r\n\r\n";
			auto r = request.str();
			tcpclient->send(r.c_str(), (int32_t)r.length());
			this->peer = peer;
		}

		virtual void onAbort(const IPEndPoint& sa) override
		{
			readyState = CLOSED;
			onclose(CloseStatus::CONNECTION_FAIL);
		}

		virtual void onRecv(const void *data, int32_t size) override
		{
			std::lock_guard<std::recursive_mutex> l(mutex);
			switch (readyState)
			{
			case CONNECTING:
				for (int32_t i = 0; i < size; i++)
				{
					char c = ((const char *)data)[i];
					sbhead.push_back(c);
					switch (stage)
					{
					case 0:
						stage = c == '\r' ? 1 : 0;
						break;
					case 1:
						stage = c == '\n' ? 2 : 0;
						break;
					case 2:
						stage = c == '\r' ? 3 : 0;
						break;
					case 3:
						if (c == '\n')
						{
							std::string security;
							for (std::string::size_type spos = 0, epos; security.empty() && (epos = sbhead.find_first_of('\n', spos)) != std::string::npos; spos = epos + 1)
							{
								std::string::size_type mpos = sbhead.find_last_of(':', epos);
								if (mpos != std::string::npos && mpos > spos)
								{
									spos = sbhead.find_first_not_of(" \t\r\n", spos);
									std::string key;
									for (auto c : sbhead.substr(spos, sbhead.find_last_not_of(" \t\r\n:", mpos) + 1 - spos))
										key.push_back(tolower(c));
									if (key.compare("x-limax-security") == 0)
									{
										spos = sbhead.find_first_not_of(" \t\r\n:", mpos);
										security = sbhead.substr(spos, sbhead.find_last_not_of(" \t\r\n", epos) + 1 - spos);
									}
								}
							}
							if (security.empty())
							{
								close();
								return;
							}
							Octets dh_data = Base64Decode::transform(Octets(&security[0], security.length()));
							const std::vector<unsigned char> material = dhcontext->computeDHKey((unsigned char*)dh_data.begin(), (int32_t)dh_data.size());
							socklen_t key_len;
							int8_t *key = (int8_t*)peer.getAddress(key_len);
							int32_t half = (int32_t)material.size() / 2;
							{
								HmacMD5 hmac(key, 0, key_len);
								hmac.update((int8_t*)&material[0], 0, half);
								osec = std::make_shared<RFC2118Encode>(std::make_shared<Encrypt>(std::make_shared<SinkOctets>(obuf), (int8_t*)hmac.digest(), 16));
							}
							{
								HmacMD5 hmac(key, 0, key_len);
								hmac.update((int8_t*)&material[0], half, (int32_t)material.size() - half);
								isec = std::make_shared<Decrypt>(std::make_shared<RFC2118Decode>(std::make_shared<SinkOctets>(ibuf)), (int8_t*)hmac.digest(), 16);
							}
							send(query.c_str(), query.size());
							readyState = OPEN;
							onopen();
							return;
						}
						else
							stage = 0;
					}
				}
				break;
			case OPEN:
				if (true)
				{
					isec->update((int8_t *)data, 0, size);
					isec->flush();
					buffer.insert(buffer.end(), ibuf.begin(), ibuf.size());
					ibuf.clear();
					while (true)
					{
						datasrc.transaction(datasrc.Begin);
						try
						{
							UnmarshalStream is(datasrc);
							int opcode = is.pop_byte_8();
							size_t len = is.pop_byte_8() & 0x7f;
							switch (len)
							{
							case 126:
								len = is.pop_byte_16();
								break;
							case 127:
								len = (size_t)is.pop_byte_64();
							}
							char *data = new char[len];
							is.pop_byte(data, len);
							if (opcode == 0x81)
								onmessage(std::string(data, len));
							delete[] data;
							datasrc.transaction(datasrc.Commit);
						}
						catch (MarshalException)
						{
							datasrc.transaction(datasrc.Rollback);
							break;
						}
					}
				}
			default:
				break;
			}
			limax::uiThreadSchedule();
		}

		virtual void onClose(int status) override
		{
			std::lock_guard<std::recursive_mutex> l(mutex);
			onclose(readyState == ReadyState::CLOSING ? CloseStatus::ACTIVE_CLOSED : CloseStatus::PASSIVE_CLOSED);
			readyState = ReadyState::CLOSED;
			limax::uiThreadSchedule();
		}

		void send(const char *p, size_t _len)
		{
			int64_t len = (int64_t)_len;
			osec->update((int8_t)0x81);
			if (len < 126)
				osec->update((int8_t)(len | 0x80));
			else if (len < 65536)
			{
				osec->update((int8_t)126);
				osec->update((int8_t)(len >> 8));
				osec->update((int8_t)len);
			}
			else
			{
				osec->update((int8_t)127);
				osec->update((int8_t)(len >> 56));
				osec->update((int8_t)(len >> 48));
				osec->update((int8_t)(len >> 40));
				osec->update((int8_t)(len >> 32));
				osec->update((int8_t)(len >> 24));
				osec->update((int8_t)(len >> 16));
				osec->update((int8_t)(len >> 8));
				osec->update((int8_t)len);
			}
			osec->update(0);
			osec->update(0);
			osec->update(0);
			osec->update(0);
			osec->update((int8_t*)p, 0, (int32_t)len);
			osec->flush();
			tcpclient->send(Octets(obuf));
			obuf.clear();
		}

		void send(const std::string& utf8message)
		{
			std::lock_guard<std::recursive_mutex> l(mutex);
			if (readyState != ReadyState::OPEN)
				return;
			send(utf8message.c_str(), utf8message.length());
		}

		void _close()
		{
			{
				std::lock_guard<std::recursive_mutex> l(mutex);
				if (readyState != ReadyState::OPEN && readyState != ReadyState::CONNECTING)
					return;
				readyState = ReadyState::CLOSING;
			}
			tcpclient->destroy();
			timer.cancel();
			timer.join();
		}

		virtual void close() override
		{
			std::shared_ptr<WebSocketConnector> _this(self);
			if (_this)
				Engine::remove(_this);
		}
	};

	void Engine::remove(std::shared_ptr<Closeable> c)
	{
		{
			std::lock_guard<std::mutex> l(closeables_mutex);
			if (!closeables.erase(c))
				return;
		}
		if (std::shared_ptr<WebSocketConnector> w = std::dynamic_pointer_cast<WebSocketConnector>(c))
			w->_close();
	}

	static std::shared_ptr<Closeable> createWebSocketConnector(const std::string& ip, short port, const std::string& username, const std::string& token, const std::string& platflag, ScriptEngineHandlePtr handle)
	{
		std::stringstream spvids;
		for (auto pvid : handle->getProviders())
			spvids << pvid << ',';
		std::string pvids(spvids.str());
		pvids.pop_back();
		std::stringstream skeys;
		skeys << ';';
		for (auto v : handle->getDictionaryKeys())
			skeys << v << ';';
		std::string keys(skeys.str());
		keys.pop_back();
		std::string query;
		query.append("/?username=").append(username).append("&token=").append(token).append("&platflag=").append(platflag).append(keys).append("&pvids=").append(pvids);
		std::shared_ptr<WebSocketConnector> ws = std::make_shared<WebSocketConnector>(query, handle);
		std::weak_ptr<WebSocketConnector> self = std::weak_ptr<WebSocketConnector>(ws);
		ws->self = self;
		handle->registerScriptSender([self](const std::string& data)
		{
			std::shared_ptr<WebSocketConnector> wsc(self);
			if (!wsc)
				return false;
			wsc->send(data);
			return true;
		});
		TcpClient::createAsync(TcpClient::createWeakListener(ws), ip, port);
		Engine::add(ws);
		return ws;
	}

	std::shared_ptr<Closeable> Endpoint::start(const std::string& host, short port, const std::string& username, const std::string& token, const std::string& platflag, ScriptEngineHandlePtr handle)
	{
		return createWebSocketConnector(host, port, username, token, platflag, handle);
	}
}
namespace limax {
	namespace helper {
		struct AuanyService
		{
			static std::atomic_int snGenerator;
			static std::unordered_map<int, std::shared_ptr<AuanyService>> map;
			static std::mutex lock;
			int sn;
			limax::AuanyService::Result result;
			DelayedRunnable future;

			static std::shared_ptr<AuanyService> removeService(int sn)
			{
				std::shared_ptr<AuanyService> service;
				{
					std::lock_guard<std::mutex> l(lock);
					auto it = map.find(sn);
					if (it != map.end())
					{
						(service = (*it).second)->future.cancel();
						map.erase(it);
					}
				}
				return service;
			}
			AuanyService(limax::AuanyService::Result _onresult, long timeout) :sn(snGenerator++), result(_onresult),
				future(DelayedRunnable([this](){
				std::shared_ptr<AuanyService> service = removeService(sn);
				if (service)
					service->result(SOURCE_ENDPOINT, ENDPOINT_AUANY_SERVICE_CLIENT_TIMEOUT, "");
			}, 500, (int)(timeout / 500)))
			{
				std::lock_guard<std::mutex> l(lock);
				Engine::execute(future);
				map.insert(std::make_pair(sn, std::shared_ptr<AuanyService>(this)));
			}
			static void onResultViewOpen(endpoint::auanyviews::ServiceResult *view)
			{
				view->registerListener([view](const ViewChangedEvent& e)
				{
					limax::auanyviews::Result *r = (limax::auanyviews::Result*) e.getValue();
					std::shared_ptr<AuanyService> service = removeService(r->sn);
					if (service)
						service->result(r->errorSource, r->errorCode, r->result);
				});
			}
			static void cleanup()
			{
				std::lock_guard<std::mutex> l(lock);
				for (auto& e : map)
				{
					e.second->future.cancel();
					if (e.second->result)
						e.second->result(SOURCE_ENDPOINT, ENDPOINT_AUANY_SERVICE_ENGINE_CLOSE, "");
				}
				map.clear();
			}
		};
		std::atomic_int AuanyService::snGenerator;
		std::unordered_map<int, std::shared_ptr<AuanyService>> AuanyService::map;
		std::mutex AuanyService::lock;
		void cleanupAuanyService()
		{
			AuanyService::cleanup();
		}
		class CredentialContext
		{
			typedef std::function<Runnable(const std::string&)> Action;

			struct SharedData
			{
				int errorSource = SOURCE_ENDPOINT;
				int errorCode = ENDPOINT_AUANY_SERVICE_CLIENT_TIMEOUT;
				std::string credential;
				bool closemanager = false;
				EndpointManager* manager = nullptr;
				Runnable action;
			};
			typedef std::shared_ptr<SharedData> SharedDataPtr;
			struct Listener : public EndpointListener {

				SharedDataPtr ptr;
				Listener(const SharedDataPtr& p)
					: ptr(p)
				{}
				virtual ~Listener() {}

				virtual void onManagerInitialized(EndpointManager* m, EndpointConfig*) override
				{
					ptr->manager = m;
				}
				virtual void onTransportAdded(Transport*) override
				{
					ptr->action();
				}
				virtual void onTransportRemoved(Transport*) override {}
				virtual void onAbort(Transport*) override
				{
					ptr->closemanager = true;
				}
				virtual void onManagerUninitialized(EndpointManager*) override
				{
					ptr->manager = nullptr;
				}
				virtual void onSocketConnected() override {}
				virtual void onKeyExchangeDone() override {}
				virtual void onKeepAlived(int ping) override {}
				virtual void onErrorOccured(int errorsource, int errorvalue, const std::string& info) override
				{
					ptr->errorSource = errorsource;
					ptr->errorCode = errorvalue;
					ptr->closemanager = true;
				}
				virtual void destroy() override
				{
					delete this;
				}
			};
			friend struct Listener;
			const std::string httpHost;
			const int httpPort;
			const int appid;
			const long timeout;
			limax::AuanyService::Result result;
			SharedDataPtr	shareddata;
		public:
			CredentialContext(const std::string& _httpHost, int _httpPort, int _appid, long _timeout)
				: httpHost(_httpHost), httpPort(_httpPort), appid(_appid), timeout(_timeout), shareddata(new SharedData())
			{
				auto data = shareddata;
				result = [data](int s, int c, const std::string& t)
				{
					data->errorSource = s;
					data->errorCode = c;
					data->credential = t;
					data->closemanager = true;
				};
			}
			~CredentialContext() {}
		public:
			void derive(const std::string& authcode, const limax::AuanyService::Result& r)
			{
				auto data = shareddata;
				execute([=](const std::string& code)
				{
					Runnable r = [=]()
					{
						limax::AuanyService::derive(code, authcode, timeout, result, data->manager);
					};
					return r;
				}, r);
			}
			void bind(const std::string& authcode, const std::string& username, const std::string& token, const std::string& platflag, const limax::AuanyService::Result& r)
			{
				auto data = shareddata;
				execute([=](const std::string& code)
				{
					Runnable r = [=]()
					{
						limax::AuanyService::bind(code, authcode, username, token, platflag, timeout, result, data->manager);
					};
					return r;
				}, r);
			}
			void temporary(const std::string& credential, const std::string& authcode, const std::string& authcode2, long millisecond, int8_t usage, const std::string& subid, const limax::AuanyService::Result& r)
			{
				auto data = shareddata;
				execute([=](const std::string& code)
				{
					Runnable r = [=]()
					{
						limax::AuanyService::temporary(credential, authcode, authcode2, millisecond, usage, subid, timeout, result, data->manager);
					};
					return r;
				}, r);
			}
			void temporary(const std::string& username, const std::string& token, const std::string& platflag, int appid, const std::string& authcode, long millisecond, int8_t usage, const std::string& subid, const limax::AuanyService::Result& r)
			{
				auto data = shareddata;
				execute([=](const std::string& code)
				{
					Runnable r = [=]()
					{
						limax::AuanyService::temporary(username, token, platflag, appid, authcode, millisecond, usage, subid, timeout, result, data->manager);
					};
					return r;
				}, r);
			}
			void transfer(const std::string& username, const std::string& token, const std::string& platflag, const std::string& authcode, const std::string& temp, const std::string& authtemp, const limax::AuanyService::Result& r)
			{
				auto data = shareddata;
				execute([=](const std::string& code)
				{
					Runnable r0 = [=]()
					{
						endpoint::auanyviews::Service::getInstance(data->manager)->Transfer((new helper::AuanyService(r, timeout))->sn, username, token, platflag, authcode, temp, authtemp);
					};
					return r0;
				}, r);
			}
		private:
			void execute(const Action& a, const limax::AuanyService::Result& r) {
				auto data = shareddata;
				try
				{
					auto starttime = std::chrono::high_resolution_clock::now();
					std::string jsonstring;
					{
						std::ostringstream oss;
						oss << "http://" << httpHost << ":" << httpPort << "/invite?native=" << appid;
						jsonstring = limax::http::httpDownload(oss.str(), (int)timeout, 4096, "", false);
					}
					auto json = JSON::parse(jsonstring);
					auto switcher = json->get("switcher");
					auto code = json->get("code")->toString();
					auto config = Endpoint::createLoginConfigBuilder(switcher->get("host")->toString(),
						switcher->get("port")->intValue(), code, "", "invite")->executor(limax::runOnUiThread)->build();
					long remain = timeout - (long)std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::high_resolution_clock::now() - starttime).count();
					if (remain > 0)
					{
						data->action = a(code);
						Endpoint::start(config, new Listener(data));
						while (!data->closemanager && (remain > 0))
						{
							limax::uiThreadSchedule();
							std::this_thread::sleep_for(std::chrono::milliseconds(1));
							remain = timeout - (long)std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::high_resolution_clock::now() - starttime).count();
						}
						if (nullptr != data->manager)
						{
							data->manager->close();
							while (nullptr != data->manager)
							{
								limax::uiThreadSchedule();
								std::this_thread::sleep_for(std::chrono::milliseconds(1));
							}
						}
					}
				}
				catch (...)
				{
				}
				r(data->errorSource, data->errorCode, data->credential);
			}
		};
	}
	void AuanyService::derive(const std::shared_ptr<ServiceInfo>& service, const std::string& authcode, long timeout, Result onresult)
	{
		std::shared_ptr<helper::ServiceInfoImpl> s = std::dynamic_pointer_cast<helper::ServiceInfoImpl>(service);
		std::pair<std::string, int32_t> p = s->randomSwitcherConfig();
		derive(p.first, p.second, s->appid, authcode, timeout, onresult);
	}
	void AuanyService::derive(const std::string& httpHost, int httpPort, int appid, const std::string& authcode, long timeout, Result onresult)
	{
		helper::CredentialContext(httpHost, httpPort, appid, timeout).derive(authcode, onresult);
	}
	void AuanyService::derive(const std::string& credential, const std::string& authcode, long timeout, Result onresult, EndpointManager* manager)
	{
		endpoint::auanyviews::Service::getInstance(manager)->Derive((new helper::AuanyService(onresult, timeout))->sn, credential, authcode);
	}
	void AuanyService::derive(const std::string& credential, const std::string& authcode, long timeout, Result onresult)
	{
		derive(credential, authcode, timeout, onresult, Endpoint::getDefaultEndpointManager());
	}
	void AuanyService::bind(const std::shared_ptr<ServiceInfo>& service, const std::string& authcode, const std::string& username, const std::string& token, const std::string& platflag, long timeout, Result onresult)
	{
		std::shared_ptr<helper::ServiceInfoImpl> s = std::dynamic_pointer_cast<helper::ServiceInfoImpl>(service);
		std::pair<std::string, int32_t> p = s->randomSwitcherConfig();
		bind(p.first, p.second, s->appid, authcode, username, token, platflag, timeout, onresult);
	}
	void AuanyService::bind(const std::string& httpHost, int httpPort, int appid, const std::string& authcode, const std::string& username, const std::string& token, const std::string& platflag, long timeout, Result onresult)
	{
		helper::CredentialContext(httpHost, httpPort, appid, timeout).bind(authcode, username, token, platflag, onresult);
	}
	void AuanyService::bind(const std::string& credential, const std::string& authcode, const std::string& username, const std::string& token, const std::string& platflag, long timeout, Result onresult, EndpointManager* manager)
	{
		endpoint::auanyviews::Service::getInstance(manager)->Bind((new helper::AuanyService(onresult, timeout))->sn, credential, authcode, username, token, platflag);
	}
	void AuanyService::bind(const std::string& credential, const std::string& authcode, const std::string& username, const std::string& token, const std::string& platflag, long timeout, Result onresult)
	{
		bind(credential, authcode, username, token, platflag, timeout, onresult, Endpoint::getDefaultEndpointManager());
	}
	void AuanyService::temporary(const std::shared_ptr<ServiceInfo>& service, const std::string& credential, const std::string& authcode, const std::string& authcode2, long millisecond, int8_t usage, const std::string& subid, long timeout, Result onresult)
	{
		std::shared_ptr<helper::ServiceInfoImpl> s = std::dynamic_pointer_cast<helper::ServiceInfoImpl>(service);
		std::pair<std::string, int32_t> p = s->randomSwitcherConfig();
		temporary(p.first, p.second, s->appid, credential, authcode, authcode2, millisecond, usage, subid, timeout, onresult);
	}
	void AuanyService::temporary(const std::string& httpHost, int httpPort, int appid, const std::string& credential, const std::string& authcode, const std::string& authcode2, long millisecond, int8_t usage, const std::string& subid, long timeout, Result onresult)
	{
		helper::CredentialContext(httpHost, httpPort, appid, timeout).temporary(credential, authcode, authcode2, millisecond, usage, subid, onresult);
	}
	void AuanyService::temporary(const std::string& credential, const std::string& authcode, const std::string& authcode2, long millisecond, int8_t usage, const std::string& subid, long timeout, Result onresult, EndpointManager* manager)
	{
		endpoint::auanyviews::Service::getInstance(manager)->TemporaryFromCredential((new helper::AuanyService(onresult, timeout))->sn, credential, authcode, authcode2, millisecond, usage, subid);
	}
	void AuanyService::temporary(const std::string& credential, const std::string& authcode, const std::string& authcode2, long millisecond, int8_t usage, const std::string& subid, long timeout, Result onresult)
	{
		temporary(credential, authcode, authcode2, millisecond, usage, subid, timeout, onresult, Endpoint::getDefaultEndpointManager());
	}
	void AuanyService::temporary(const std::shared_ptr<ServiceInfo>& service, const std::string& username, const std::string& token, const std::string& platflag, const std::string& authcode, long millisecond, int8_t usage, const std::string& subid, long timeout, Result onresult)
	{
		std::shared_ptr<helper::ServiceInfoImpl> s = std::dynamic_pointer_cast<helper::ServiceInfoImpl>(service);
		std::pair<std::string, int32_t> p = s->randomSwitcherConfig();
		temporary(p.first, p.second, s->appid, username, token, platflag, authcode, millisecond, usage, subid, timeout, onresult);
	}
	void AuanyService::temporary(const std::string& httpHost, int httpPort, int appid, const std::string& username, const std::string& token, const std::string& platflag, const std::string& authcode, long millisecond, int8_t usage, const std::string& subid, long timeout, Result onresult)
	{
		helper::CredentialContext(httpHost, httpPort, appid, timeout).temporary(username, token, platflag, appid, authcode, millisecond, usage, subid, onresult);
	}
	void AuanyService::temporary(const std::string& username, const std::string& token, const std::string& platflag, int appid, const std::string& authcode, long millisecond, int8_t usage, const std::string& subid, long timeout, Result onresult, EndpointManager* manager)
	{
		endpoint::auanyviews::Service::getInstance(manager)->TemporaryFromLogin((new helper::AuanyService(onresult, timeout))->sn, username, token, platflag, appid, authcode, millisecond, usage, subid);
	}
	void AuanyService::temporary(const std::string& username, const std::string& token, const std::string& platflag, int appid, const std::string& authcode, long millisecond, int8_t usage, const std::string& subid, long timeout, Result onresult)
	{
		temporary(username, token, platflag, appid, authcode, millisecond, usage, subid, timeout, onresult, Endpoint::getDefaultEndpointManager());
	}
	void AuanyService::transfer(const std::shared_ptr<ServiceInfo>& service, const std::string& username, const std::string& token, const std::string& platflag, const std::string& authcode, const std::string& temp, const std::string& authtemp, long timeout, Result onresult)
	{
		std::shared_ptr<helper::ServiceInfoImpl> s = std::dynamic_pointer_cast<helper::ServiceInfoImpl>(service);
		std::pair<std::string, int32_t> p = s->randomSwitcherConfig();
		transfer(p.first, p.second, s->appid, username, token, platflag, authcode, temp, authtemp, timeout, onresult);
	}
	void AuanyService::transfer(const std::string& httpHost, int httpPort, int appid, const std::string& username, const std::string& token, const std::string& platflag, const std::string& authcode, const std::string& temp, const std::string& authtemp, long timeout, Result onresult)
	{
		helper::CredentialContext(httpHost, httpPort, appid, timeout).transfer(username, token, platflag, authcode, temp, authtemp, onresult);
	}
	void AuanyService::pay(int gateway, int payid, int product, int price, int quantity, const std::string& receipt, long timeout, Result onresult, EndpointManager *manager)
	{
		endpoint::auanyviews::Service::getInstance(manager)->Pay((new helper::AuanyService(onresult, timeout))->sn, gateway, payid, product, price, quantity, receipt);
	}
	void AuanyService::pay(int gateway, int payid, int product, int price, int quantity, const std::string& receipt, long timeout, Result onresult)
	{
		pay(gateway, payid, product, price, quantity, receipt, timeout, onresult, Endpoint::getDefaultEndpointManager());
	}
}

namespace limax {
	namespace endpoint {
		namespace auanyviews {
			void ServiceResult::onOpen(const std::vector<int64_t>& sessionids)
			{
				limax::helper::AuanyService::onResultViewOpen(this);
			}
			void ServiceResult::onAttach(int64_t sessionid) {}
			void ServiceResult::onDetach(int64_t sessionid, int reason)
			{
				if (reason >= 0)
				{
					//Application Reason
				}
				else
				{
					//Connection abort Reason
				}
			}
			void ServiceResult::onClose() {}
		}
	}
}
