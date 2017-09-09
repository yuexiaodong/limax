package limax.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import limax.codec.BoundCheck;
import limax.codec.CharSink;
import limax.codec.StreamSource;

public class HttpClientService {
	private final int defaultMaxContentLength;
	private final int defaultMaxOutstanding;
	private final int defaultMaxQueueCapacity;
	private final int defaultTimeout;
	private final String name;
	private final ThreadPoolExecutor executor;
	private final AtomicBoolean stopped = new AtomicBoolean();
	private final Map<String, BoundedExecutor> classifyExecutor = new ConcurrentHashMap<>();

	public class Request {
		private final URL url;
		private final String post;
		private final long deadline;
		private final int maxContentLength;

		private Request(URL url, String post, int maxContentLength, int timeout) {
			this.url = url;
			if (!url.getProtocol().equals("http") && !url.getProtocol().equals("https"))
				throw new IllegalArgumentException("Http[s] protocol only, but url = " + url.toString());
			this.post = post;
			this.maxContentLength = maxContentLength;
			this.deadline = System.currentTimeMillis() + timeout;
		}

		private long updateTimeout() throws SocketTimeoutException {
			long timeout = deadline - System.currentTimeMillis();
			if (timeout < 0)
				throw new SocketTimeoutException();
			return timeout;
		}

		private Response execute() throws Exception {
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			int timeout = (int) updateTimeout();
			conn.setConnectTimeout(timeout);
			conn.setReadTimeout(timeout);
			if (!post.isEmpty()) {
				conn.setDoOutput(true);
				conn.setAllowUserInteraction(false);
				conn.setRequestMethod("POST");
				try (OutputStream os = conn.getOutputStream()) {
					os.write(post.getBytes(StandardCharsets.UTF_8));
				}
			} else
				conn.setRequestMethod("GET");
			int code = conn.getResponseCode();
			Charset charset;
			try {
				charset = Charset.forName(conn.getContentEncoding());
			} catch (Exception e) {
				charset = StandardCharsets.UTF_8;
			}
			conn.setReadTimeout((int) updateTimeout());
			StringBuilder sb = new StringBuilder();
			try (InputStream is = conn.getInputStream()) {
				new StreamSource(is,
						new BoundCheck(maxContentLength, updateTimeout(), new CharSink(charset, c -> sb.append(c))))
								.flush();
			}
			return new Response(code, sb.toString());
		}

		private String getClassify() {
			return url.getHost();
		}

		public Future<Response> submit() {
			return HttpClientService.this.submit(this);
		}
	}

	public static class Response {
		private final int code;
		private final String content;

		public int getCode() {
			return code;
		}

		public String getContent() {
			return content;
		}

		public Response(int code, String value) {
			this.code = code;
			this.content = value;
		}
	}

	public HttpClientService(int corePoolSize, int defaultMaxOutstanding, int defaultMaxQueueCapacity,
			int defaultMaxContentLength, int defaultTimeout) {
		this.name = "HttpClientSerivce-" + System.currentTimeMillis();
		this.executor = ConcurrentEnvironment.getInstance().newThreadPool(name, corePoolSize);
		this.defaultMaxOutstanding = defaultMaxOutstanding;
		this.defaultMaxQueueCapacity = defaultMaxQueueCapacity;
		this.defaultMaxContentLength = defaultMaxContentLength;
		this.defaultTimeout = defaultTimeout;
	}

	public void shutdown() {
		if (stopped.compareAndSet(false, true))
			ConcurrentEnvironment.getInstance().shutdown(name);
	}

	private Future<Response> submit(Request req) {
		FutureTask<Response> task = new FutureTask<>(() -> req.execute());
		classifyExecutor
				.computeIfAbsent(req.getClassify(),
						k -> new BoundedExecutor(executor, defaultMaxOutstanding, defaultMaxQueueCapacity))
				.execute(task);
		return task;
	}

	public void initHost(String host, int maxOutstanding, int maxQueueCapacity) {
		classifyExecutor.put(host, new BoundedExecutor(executor, maxOutstanding, maxQueueCapacity));
	}

	public Request makeGetRequest(URL url, int maxContentLength, int timeout) {
		return new Request(url, "", maxContentLength, timeout);
	}

	public Request makeGetRequest(URL url) {
		return makeGetRequest(url, defaultMaxContentLength, defaultTimeout);
	}

	public Request makePostRequest(URL url, String data, int maxContentLength, int timeout) {
		if (data.isEmpty())
			throw new IllegalArgumentException("empty post data");
		return new Request(url, data, maxContentLength, timeout);
	}

	public Request makePostRequest(URL url, String data) {
		return makePostRequest(url, data, defaultMaxContentLength, defaultTimeout);
	}
}
