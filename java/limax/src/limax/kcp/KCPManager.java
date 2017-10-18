package limax.kcp;

import limax.util.Trace;

public class KCPManager {

	private static final KCPManager instance = new KCPManager();
	public static KCPManager getInstance() {
		return instance;
	}
	private KCPManager() {}
	
	static int KCP_PORT = 9999;
	static int KCP_THREAD_NUM = Runtime.getRuntime().availableProcessors() * 2;
	static int KCP_MTU_SIZE = 512;
	
	private static KCPServer s;
	
	public void init(){
		s = new KCPServer(KCP_PORT, KCP_THREAD_NUM);
		s.noDelay(1, 10, 2, 1);
		s.setMinRto(10);
		s.wndSize(64, 64);
		s.setTimeout(10 * 1000);
		s.setMtu(KCP_MTU_SIZE);
		s.setStream(false);
		s.start();
	}
	
	public void stop(){
		try {
			s.close();
		} catch (Exception e) {
			e.printStackTrace();
		}finally {
			Trace.info("kcp stop!");
		}
	}
	
}
