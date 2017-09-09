package limax.endpoint;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import limax.codec.CharSink;
import limax.codec.JSON;
import limax.codec.JSONDecoder;
import limax.defines.ErrorCodes;
import limax.defines.ErrorSource;
import limax.endpoint.auanyviews.Service;
import limax.endpoint.auanyviews.ServiceResult;
import limax.net.Config;
import limax.net.Engine;
import limax.net.Manager;
import limax.net.Transport;
import limax.util.HttpClient;

public final class AuanyService {
	public final static int providerId = 1;

	public interface Result {
		void apply(int errorSource, int errorCode, String result);
	}

	private final static AtomicInteger snGenerator = new AtomicInteger();
	private final static Map<Integer, AuanyService> map = new HashMap<Integer, AuanyService>();

	private final int sn;
	private final Executor executor;
	private final Result onresult;
	private final Future<?> future;

	private static AuanyService removeService(int sn) {
		synchronized (map) {
			AuanyService service = map.remove(sn);
			if (service != null)
				service.future.cancel(false);
			return service;
		}
	}

	private AuanyService(Result onresult, long timeout, Executor executor) {
		this.sn = snGenerator.getAndIncrement();
		this.executor = executor;
		this.onresult = onresult;
		synchronized (map) {
			this.future = Engine.getProtocolScheduler().schedule(new Runnable() {
				@Override
				public void run() {
					AuanyService service = removeService(sn);
					if (service != null)
						service.onError(ErrorCodes.ENDPOINT_AUANY_SERVICE_CLIENT_TIMEOUT);
				}
			}, timeout, TimeUnit.MILLISECONDS);
			map.put(this.sn, this);
		}
	}

	private void onError(int errorCode) {
		onResult(ErrorSource.ENDPOINT, errorCode, null);
	}

	private void onResult(final int errorSource, final int errorCode, final String result) {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				onresult.apply(errorSource, errorCode, result);
			}
		});
	}

	static void onResultViewOpen(ServiceResult view) {
		view.registerListener(new ViewChangedListener() {
			@Override
			public void onViewChanged(ViewChangedEvent e) {
				limax.auanyviews.Result r = (limax.auanyviews.Result) e.getValue();
				AuanyService service = removeService(r.sn);
				if (service != null)
					service.onResult(r.errorSource, r.errorCode, r.result);
			}
		});
	}

	static void cleanup() {
		synchronized (map) {
			for (AuanyService service : map.values()) {
				service.future.cancel(false);
				service.onError(ErrorCodes.ENDPOINT_AUANY_SERVICE_ENGINE_CLOSE);
			}
			map.clear();
		}
	}

	private static Executor createExecutor(final EndpointManager manager) {
		return new Executor() {
			@Override
			public void execute(Runnable command) {
				((EndpointManagerImpl) manager).dispatch(command, manager.getTransport());
			}
		};
	}

	private static class CredentialContext {
		private static class Listener implements EndpointListener {
			private final CredentialContext ctx;
			private final Callable<Void> action;

			Listener(CredentialContext ctx, Callable<Void> action) {
				this.ctx = ctx;
				this.action = action;
			}

			@Override
			public void onAbort(Transport transport) throws Exception {
				synchronized (ctx) {
					ctx.notify();
				}
			}

			@Override
			public void onManagerInitialized(Manager manager, Config config) {
				ctx.manager = (EndpointManager) manager;
			}

			@Override
			public void onManagerUninitialized(Manager manager) {
			}

			@Override
			public void onTransportAdded(Transport transport) throws Exception {
				action.call();
			}

			@Override
			public void onTransportRemoved(Transport transport) throws Exception {
			}

			@Override
			public void onSocketConnected() {
			}

			@Override
			public void onKeyExchangeDone() {
			}

			@Override
			public void onKeepAlived(int ms) {
			}

			@Override
			public void onErrorOccured(int source, int code, Throwable exception) {
				synchronized (ctx) {
					ctx.errorSource = source;
					ctx.errorCode = code;
					ctx.notify();
				}
			}
		}

		private int errorSource = ErrorSource.ENDPOINT;
		private int errorCode = ErrorCodes.ENDPOINT_AUANY_SERVICE_CLIENT_TIMEOUT;
		private String credential;
		private EndpointManager manager;
		private final String httpHost;
		private final int httpPort;
		private final int appid;
		private final long timeout;
		private final Result result = new Result() {
			@Override
			public void apply(int errorSource, int errorCode, String credential) {
				synchronized (CredentialContext.this) {
					CredentialContext.this.errorSource = errorSource;
					CredentialContext.this.errorCode = errorCode;
					CredentialContext.this.credential = credential;
					CredentialContext.this.notify();
				}
			}
		};

		private CredentialContext(String httpHost, int httpPort, int appid, long timeout) {
			this.httpHost = httpHost;
			this.httpPort = httpPort;
			this.appid = appid;
			this.timeout = timeout;
		}

		private interface Action {
			Callable<Void> apply(String code);
		}

		private void execute(Action action, Result r) {
			try {
				long starttime = System.currentTimeMillis();
				JSONDecoder decoder = new JSONDecoder();
				new HttpClient("http://" + httpHost + ":" + httpPort + "/invite?native=" + appid, timeout / 2, 4096,
						null, false).transfer(new CharSink(decoder));
				JSON json = decoder.get();
				JSON switcher = json.get("switcher");
				final String code = json.get("code").toString();
				final EndpointConfig config = Endpoint.createLoginConfigBuilder(switcher.get("host").toString(),
						switcher.get("port").intValue(), code, "", "invite").build();
				long remain = timeout - (System.currentTimeMillis() - starttime);
				if (remain > 0) {
					synchronized (this) {
						Endpoint.start(config, new Listener(this, action.apply(code)));
						wait(remain);
					}
					manager.close();
				}
			} catch (Exception e) {
			}
			r.apply(errorSource, errorCode, credential);
		}

		public void derive(final String authcode, Result r) {
			execute(new Action() {
				@Override
				public Callable<Void> apply(final String code) {
					return new Callable<Void>() {
						@Override
						public Void call() throws Exception {
							AuanyService.derive(code, authcode, timeout, result, manager);
							return null;
						}
					};
				}
			}, r);
		}

		public void bind(final String authcode, final String username, final String token, final String platflag,
				Result r) {
			execute(new Action() {
				@Override
				public Callable<Void> apply(final String code) {
					return new Callable<Void>() {
						@Override
						public Void call() throws Exception {
							AuanyService.bind(code, authcode, username, token, platflag, timeout, result, manager);
							return null;
						}
					};
				}
			}, r);
		}

		public void temporary(final String credential, final String authcode, final String authcode2,
				final long millisecond, final byte usage, final String subid, Result r) {
			execute(new Action() {
				@Override
				public Callable<Void> apply(final String code) {
					return new Callable<Void>() {
						@Override
						public Void call() throws Exception {
							AuanyService.temporary(credential, authcode, authcode2, millisecond, usage, subid, timeout,
									result, manager);
							return null;
						}
					};
				}
			}, r);
		}

		public void temporary(final String username, final String token, final String platflag, final String authcode,
				final long millisecond, final byte usage, final String subid, Result r) {
			execute(new Action() {
				@Override
				public Callable<Void> apply(final String code) {
					return new Callable<Void>() {
						@Override
						public Void call() throws Exception {
							AuanyService.temporary(username, token, platflag, appid, authcode, millisecond, usage,
									subid, timeout, result, manager);
							return null;
						}
					};
				}
			}, r);
		}

		public void transfer(final String username, final String token, final String platflag, final String authcode,
				final String temp, final String authtemp, Result r) {
			execute(new Action() {
				@Override
				public Callable<Void> apply(final String code) {
					return new Callable<Void>() {
						@Override
						public Void call() throws Exception {
							AuanyService.transfer(username, token, platflag, authcode, temp, authtemp, timeout, result,
									manager);
							return null;
						}
					};
				}
			}, r);
		}

	}

	public static void derive(ServiceInfo service, String authcode, long timeout, Result onresult) {
		ServiceInfo.SwitcherConfig switcher = service.randomSwitcherConfig();
		derive(switcher.host, switcher.port, service.appid, authcode, timeout, onresult);
	}

	public static void derive(String httpHost, int httpPort, int appid, String authcode, long timeout,
			Result onresult) {
		new CredentialContext(httpHost, httpPort, appid, timeout).derive(authcode, onresult);
	}

	public static void derive(String credential, String authcode, long timeout, Result onresult,
			EndpointManager manager) throws Exception {
		Service.getInstance(manager).Derive(new AuanyService(onresult, timeout, createExecutor(manager)).sn, credential,
				authcode);
	}

	public static void derive(String credential, String authcode, long timeout, Result onresult) throws Exception {
		derive(credential, authcode, timeout, onresult, Endpoint.getDefaultEndpointManager());
	}

	public static void bind(ServiceInfo service, String authcode, String username, String token, String platflag,
			long timeout, Result onresult) {
		ServiceInfo.SwitcherConfig switcher = service.randomSwitcherConfig();
		bind(switcher.host, switcher.port, service.appid, authcode, username, token, platflag, timeout, onresult);
	}

	public static void bind(String httpHost, int httpPort, int appid, String authcode, String username, String token,
			String platflag, long timeout, Result onresult) {
		new CredentialContext(httpHost, httpPort, appid, timeout).bind(authcode, username, token, platflag, onresult);
	}

	public static void bind(String credential, String authcode, String username, String token, String platflag,
			long timeout, Result onresult, EndpointManager manager) throws Exception {
		Service.getInstance(manager).Bind(new AuanyService(onresult, timeout, createExecutor(manager)).sn, credential,
				authcode, username, token, platflag);
	}

	public static void bind(String credential, String authcode, String username, String token, String platflag,
			long timeout, Result onresult) throws Exception {
		bind(credential, authcode, username, token, platflag, timeout, onresult, Endpoint.getDefaultEndpointManager());
	}

	public static void temporary(ServiceInfo service, String credential, String authcode, String authcode2,
			long millisecond, byte usage, String subid, long timeout, Result onresult) {
		ServiceInfo.SwitcherConfig switcher = service.randomSwitcherConfig();
		temporary(switcher.host, switcher.port, service.appid, credential, authcode, authcode2, millisecond, usage,
				subid, timeout, onresult);
	}

	public static void temporary(String httpHost, int httpPort, int appid, String credential, String authcode,
			String authcode2, long millisecond, byte usage, String subid, long timeout, Result onresult) {
		new CredentialContext(httpHost, httpPort, appid, timeout).temporary(credential, authcode, authcode2,
				millisecond, usage, subid, onresult);
	}

	public static void temporary(String credential, String authcode, String authcode2, long millisecond, byte usage,
			String subid, long timeout, Result onresult, EndpointManager manager) throws Exception {
		Service.getInstance(manager).TemporaryFromCredential(
				new AuanyService(onresult, timeout, createExecutor(manager)).sn, credential, authcode, authcode2,
				millisecond, usage, subid);
	}

	public static void temporary(String credential, String authcode, String authcode2, long millisecond, byte usage,
			String subid, long timeout, Result onresult) throws Exception {
		temporary(credential, authcode, authcode2, millisecond, usage, subid, timeout, onresult,
				Endpoint.getDefaultEndpointManager());
	}

	public static void temporary(ServiceInfo service, String username, String token, String platflag, String authcode,
			long millisecond, byte usage, String subid, long timeout, Result onresult) {
		ServiceInfo.SwitcherConfig switcher = service.randomSwitcherConfig();
		temporary(switcher.host, switcher.port, service.appid, username, token, platflag, authcode, millisecond, usage,
				subid, timeout, onresult);
	}

	public static void temporary(String httpHost, int httpPort, int appid, String username, String token,
			String platflag, String authcode, long millisecond, byte usage, String subid, long timeout,
			Result onresult) {
		new CredentialContext(httpHost, httpPort, appid, timeout).temporary(username, token, platflag, authcode,
				millisecond, usage, subid, onresult);
	}

	public static void temporary(String username, String token, String platflag, int appid, String authcode,
			long millisecond, byte usage, String subid, long timeout, Result onresult, EndpointManager manager)
					throws Exception {
		Service.getInstance(manager).TemporaryFromLogin(new AuanyService(onresult, timeout, createExecutor(manager)).sn,
				username, token, platflag, appid, authcode, millisecond, usage, subid);
	}

	public static void temporary(String username, String token, String platflag, int appid, String authcode,
			long millisecond, byte usage, String subid, long timeout, Result onresult) throws Exception {
		temporary(username, token, platflag, appid, authcode, millisecond, usage, subid, timeout, onresult,
				Endpoint.getDefaultEndpointManager());
	}

	public static void transfer(ServiceInfo service, String username, String token, String platflag, String authcode,
			String temp, String authtemp, long timeout, Result onresult) {
		ServiceInfo.SwitcherConfig switcher = service.randomSwitcherConfig();
		transfer(switcher.host, switcher.port, service.appid, username, token, platflag, authcode, temp, authtemp,
				timeout, onresult);
	}

	public static void transfer(String httpHost, int httpPort, int appid, String username, String token,
			String platflag, String authcode, String temp, String authtemp, long timeout, Result onresult) {
		new CredentialContext(httpHost, httpPort, appid, timeout).transfer(username, token, platflag, authcode, temp,
				authtemp, onresult);
	}

	private static void transfer(String username, String token, String platflag, String authcode, String temp,
			String authtemp, long timeout, Result onresult, EndpointManager manager) throws Exception {
		Service.getInstance(manager).Transfer(new AuanyService(onresult, timeout, createExecutor(manager)).sn, username,
				token, platflag, authcode, temp, authtemp);
	}

	public static void pay(int gateway, int payid, int product, int price, int quantity, String receipt, long timeout,
			Result onresult, EndpointManager manager) throws Exception {
		Service.getInstance(manager).Pay(new AuanyService(onresult, timeout, createExecutor(manager)).sn, gateway,
				payid, product, price, quantity, receipt);
	}

	public static void pay(int gateway, int payid, int product, int price, int quantity, String receipt, long timeout,
			Result onresult) throws Exception {
		pay(gateway, payid, product, price, quantity, receipt, timeout, onresult, Endpoint.getDefaultEndpointManager());
	}
}
