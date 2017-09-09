package limax.pkix.tool;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.w3c.dom.Element;

import limax.pkix.CAService;
import limax.util.ConcurrentEnvironment;
import limax.util.ElementHelper;
import limax.util.Helper;
import limax.util.Trace;
import limax.util.XMLUtils;

public class CAServer {
	private static final ScheduledThreadPoolExecutor scheduler = ConcurrentEnvironment.getInstance()
			.newScheduledThreadPool("CAServer Scheduler", 5, true);
	private final OcspServer ocspServer;
	private final CertServer certServer;
	private final CertUpdateServer certUpdateServer;

	private CAServer(Element root) throws Exception {
		ElementHelper eh = new ElementHelper((Element) root.getElementsByTagName("Trace").item(0));
		Trace.Config config = new Trace.Config();
		config.setOutDir(eh.getString("outDir", "./trace"));
		config.setConsole(eh.getBoolean("console", true));
		config.setRotateHourOfDay(eh.getInt("rotateHourOfDay", 6));
		config.setRotateMinute(eh.getInt("rotateMinute", 0));
		config.setRotatePeriod(eh.getLong("rotatePeriod", 86400000l));
		config.setLevel(eh.getString("level", "warn").toUpperCase());
		Trace.openNew(config);
		eh = new ElementHelper(root);
		String _authCode = eh.getString("authCode", null);
		char[] authCode;
		if (_authCode != null) {
			if (Trace.isWarnEnabled())
				Trace.warn("CAServer authCode SHOULD NOT contains in config file, except for test.");
			authCode = _authCode.toCharArray();
		} else {
			authCode = System.console().readPassword("authCode:");
			if (!Arrays.equals(authCode, System.console().readPassword("Confirm authCode:")))
				throw new RuntimeException("Differ authCode Input.");
		}
		Archive archive = new Archive(eh.getString("archive"));
		String domain = eh.getString("domain");
		CAService ca = null;
		for (Element e : XMLUtils.getChildElements(root)) {
			if (e.getTagName().equals("CAService")) {
				eh = new ElementHelper(e);
				URI location = URI.create(eh.getString("location"));
				if (Trace.isInfoEnabled())
					Trace.info("CAService " + location + " loading");
				String passphrase = eh.getString("passphrase", null);
				if (passphrase != null && Trace.isWarnEnabled())
					Trace.warn("CAService " + location
							+ " passphrase SHOULD NOT contains in config file, except for test.");
				CAService tmp = CAService.create(location, passphrase == null
						? prompt -> System.console().readPassword(prompt) : prompt -> passphrase.toCharArray());
				ca = ca == null ? tmp : ca.combine(tmp);
			}
		}
		eh = new ElementHelper((Element) root.getElementsByTagName("OcspServer").item(0));
		this.ocspServer = new OcspServer(
				new OcspSignerConfig(ca, eh.getInt("nextUpdateDelay"), eh.getString("certificateAlgorithm"),
						eh.getInt("certificateLifetime"), eh.getInt("signatureBits"), scheduler),
				eh.getInt("port"), domain, Paths.get(eh.getString("ocspStore")), eh.getInt("responseCacheCapacity"));
		Element e = (Element) root.getElementsByTagName("CertServer").item(0);
		eh = new ElementHelper(e);
		this.certServer = new CertServer(ca, eh.getInt("port"), ocspServer, e,
				AuthCode.create(Helper.makeRandValues(32), authCode), archive);
		eh = new ElementHelper((Element) root.getElementsByTagName("CertUpdateServer").item(0));
		this.certUpdateServer = new CertUpdateServer(ca, domain, eh.getInt("port"), eh.getInt("renewLifespanPercent"),
				eh.getString("certificateAlgorithm"), eh.getInt("certificateLifetime"), ocspServer, archive, scheduler);
	}

	private void start() throws Exception {
		ocspServer.start();
		certServer.start();
		certUpdateServer.start();
	}

	public static void main(String[] args) throws Exception {
		Path path = Paths.get(args.length == 0 ? "caserver.xml" : args[0]);
		byte[] data = Files.readAllBytes(path);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		XMLUtils.prettySave(XMLUtils.getRootElement(new ByteArrayInputStream(data)).getOwnerDocument(), os);
		if (!Arrays.equals(os.toByteArray(), data))
			Files.write(path, os.toByteArray());
		new CAServer(XMLUtils.getRootElement(new ByteArrayInputStream(data))).start();
	}
}
