package limax.auany;

import java.io.IOException;

import org.w3c.dom.Element;

public interface PayLogger extends AutoCloseable {
	void initialize(Element e);

	void logCreate(PayOrder order) throws IOException;

	void logFake(String orderid, int gateway, int expect) throws IOException;

	void logExpire(PayOrder order) throws IOException;

	void logOk(PayOrder order) throws IOException;

	void logFail(PayOrder order, String gatewayMessage) throws IOException;

	void logDead(PayDelivery pd) throws IOException;

	void log(String s) throws IOException;
}
