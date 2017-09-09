package limax.provider;

import limax.codec.Octets;
import limax.codec.OctetsStream;
import limax.codec.StringStream;
import limax.provider.ViewDataCollector.CollectorBundle;
import limax.provider.ViewDataCollector.Data;
import limax.provider.providerendpoint.SyncViewToClients;
import limax.util.Resource;
import limax.util.Trace;

public abstract class View {
	interface CreateParameter {
		ViewContext getViewContext();

		ViewStub getViewStub();

		Resource getResource();
	}

	private final ViewContextImpl context;
	private final ViewStub stub;
	private final Resource resource;
	private boolean closed = false;
	final ViewDataCollector vdc;
	final CollectorBundle cb;

	protected abstract void processControl(byte controlIndex, OctetsStream controlparameter, long sessionid);

	protected abstract void onMessage(String message, long sessionid);

	protected abstract void onClose();

	abstract void hashSchedule(Runnable task);

	abstract void onUpdate(byte varindex, Data data);

	public void schedule(Runnable task) {
		hashSchedule(task);
	}

	protected final void update(byte varindex, Data data) {
		cb.add(varindex, data);
		onUpdate(varindex, data);
	}

	View(CreateParameter param, String[] prefix, byte[][] collectors, int cycle) {
		this.context = (ViewContextImpl) param.getViewContext();
		this.stub = param.getViewStub();
		this.resource = param.getResource();
		this.vdc = new ViewDataCollector(prefix);
		this.cb = vdc.createCollectorBundle(collectors, cycle);
	}

	int getProviderId() {
		return context.getProviderId();
	}

	short getClassIndex() {
		return stub.getClassIndex();
	}

	int getInstanceIndex() {
		return 0;
	}

	SyncViewToClients protocol(int synctype) {
		SyncViewToClients p = new SyncViewToClients();
		p.providerid = getProviderId();
		p.classindex = getClassIndex();
		p.instanceindex = getInstanceIndex();
		p.synctype = (byte) synctype;
		return p;
	}

	protected boolean isScriptEnabled() {
		return context.isScriptEnabled();
	}

	final ViewContextImpl getViewContext() {
		return context;
	}

	final void syncViewToClients(SyncViewToClients p) {
		try {
			context.syncViewToClients(p);
		} catch (Throwable t) {
			if (Trace.isErrorEnabled())
				Trace.error("syncViewToClient view = " + this + " protocol = " + p, t);
		}
	}

	void close() {
		closed = true;
		resource.close();
		onClose();
		context.onClosed(this);
	}

	boolean isClosed() {
		return closed;
	}

	StringStream prepareStringHeader(SyncViewToClients p) {
		return new StringStream(context.getDataDictionary()).marshal(p.providerid).marshal(p.classindex)
				.marshal(p.instanceindex).marshal(p.synctype).append("P");
	}

	void scheduleProcessControl(byte controlIndex, OctetsStream controlparameter, long sessionid) {
		hashSchedule(() -> {
			if (!closed)
				processControl(controlIndex, controlparameter, sessionid);
		});
	}

	void scheduleOnMessage(String message, long sessionid) {
		hashSchedule(() -> {
			if (!closed)
				onMessage(message, sessionid);
		});
	}

	@Override
	public String toString() {
		return "[class = " + getClass().getName() + " ProviderId = " + getProviderId() + " classindex = "
				+ getClassIndex() + "]";
	}

	public final void tunnel(long sessionid, int label, Octets data) {
		getViewContext().tunnel(sessionid, label, data);
	}
}
