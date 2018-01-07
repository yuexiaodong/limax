package limax.kcp;

import io.netty.buffer.ByteBuf;
import limax.net.kcp.KcpClient;
import limax.net.kcp.KcpOnUdp;

public class KCPClient extends KcpClient {

	@Override
	public void handleReceive(ByteBuf bb, KcpOnUdp kcp) {

	}

	@Override
	public void handleException(Throwable ex, KcpOnUdp kcp) {

	}

}
