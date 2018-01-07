package limax.kcp;


import io.netty.buffer.ByteBuf;
import limax.codec.Octets;
import limax.net.kcp.KcpOnUdp;
import limax.net.kcp.KcpServer;

public class KCPServer extends KcpServer {

	public KCPServer(int port, int workerSize) {
		super(port, workerSize);
	}

	@Override
	public void handleReceive(ByteBuf bb, KcpOnUdp kcp) {
		//bb的格式：
		long sid = bb.readLongLE();
		long label = bb.readIntLE();
		Octets oct  = Octets.wrap(bb.array());
		
		 
//	    String content = bb.toString(java.nio.charset.Charset.forName("utf-8"));
//	    System.out.println("msg:" + content + " kcp--> " + kcp);
	    send(bb, kcp);
	}

	@Override
	public void handleException(Throwable ex, KcpOnUdp kcp) {
		System.out.println(ex);
	}

	@Override
	public void handleClose(KcpOnUdp kcp) {
		System.out.println("客户端离开:" + kcp);
	    System.out.println("waitSnd:" + kcp.getKcp().waitSnd());
	}

}
