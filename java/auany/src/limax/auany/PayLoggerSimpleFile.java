package limax.auany;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.w3c.dom.Element;

import limax.util.ElementHelper;

public final class PayLoggerSimpleFile implements PayLogger {
	private final static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	private Date getDate() throws IOException {
		Calendar cal = Calendar.getInstance();
		if (last == null) {
			ps = new PrintStream(new FileOutputStream(path.resolve("pay.log").toFile(), true), false, "UTF-8");
		} else if (last.get(Calendar.DAY_OF_YEAR) != cal.get(Calendar.DAY_OF_YEAR)) {
			ps.close();
			File dest = path.resolve("pay." + new SimpleDateFormat("yyyy.MM.dd").format(last.getTime()) + ".log")
					.toFile();
			File file = path.resolve("pay.log").toFile();
			ps = new PrintStream(new FileOutputStream(file, !file.renameTo(dest)), false, "UTF-8");
		}
		last = cal;
		return cal.getTime();
	}

	private PrintStream ps;
	private Path path;
	private Calendar last;

	@Override
	public void initialize(Element e) {
		ElementHelper eh = new ElementHelper(e);
		try {
			Files.createDirectories(path = Paths.get(eh.getString("payLoggerSimpleFileHome", "paylogs")));
		} catch (IOException e1) {
		}
	}

	private String format(Date date, String op, PayOrder o) {
		return dateFormat.format(date) + "," + op + "," + o.getOrder() + "," + o.getGateway() + "," + o.getSessionId()
				+ "," + o.getPayId() + "," + o.getProduct() + "," + o.getPrice() + "," + o.getQuantity() + ","
				+ o.getPrice() * o.getQuantity() + "," + o.getElapsed();
	}

	@Override
	public synchronized void logCreate(PayOrder order) throws IOException {
		Date date = getDate();
		ps.println(format(date, "CREATE", order));
	}

	@Override
	public synchronized void logFake(String orderid, int gateway, int expect) throws IOException {
		Date date = getDate();
		ps.println(dateFormat.format(date) + ",FAKE," + orderid + "," + gateway + "," + expect);
	}

	@Override
	public synchronized void logExpire(PayOrder order) throws IOException {
		Date date = getDate();
		ps.println(format(date, "EXPIRE", order));
	}

	@Override
	public synchronized void logOk(PayOrder order) throws IOException {
		Date date = getDate();
		ps.println(format(date, "OK", order));
	}

	@Override
	public synchronized void logFail(PayOrder order, String gatewayMessage) throws IOException {
		Date date = getDate();
		ps.println(format(date, "FAIL", order) + "," + gatewayMessage);
	}

	@Override
	public synchronized void logDead(PayDelivery pd) throws IOException {
		Date date = getDate();
		ps.println(dateFormat.format(date) + ",DEAD," + pd.getOrder() + "," + pd.getSessionId() + "," + pd.getPayId()
				+ "," + pd.getProduct() + "," + pd.getPrice() + "," + pd.getQuantity() + ","
				+ pd.getPrice() * pd.getQuantity() + "," + pd.getElapsed());
	}

	@Override
	public synchronized void log(String s) throws IOException {
		Date date = getDate();
		ps.println(dateFormat.format(date) + "," + s);
	}

	@Override
	public synchronized void close() throws Exception {
		ps.close();
	}

}
