package limax.endpoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import limax.codec.CodecException;
import limax.codec.Marshal;
import limax.codec.MarshalException;
import limax.codec.Octets;
import limax.codec.OctetsStream;
import limax.endpoint.providerendpoint.SendControlToServer;
import limax.net.SizePolicyException;

public abstract class View {

	private final class ViewChangedListenerContainer {
		private final Map<String, Collection<ViewChangedListener>> listeners = new HashMap<String, Collection<ViewChangedListener>>();

		Runnable addListener(String fieldname, final ViewChangedListener listener) {
			Collection<ViewChangedListener> ls = listeners.get(fieldname);
			if (null == ls) {
				ls = new ArrayList<ViewChangedListener>();
				listeners.put(fieldname, ls);
			}
			if (ls.add(listener)) {
				final Collection<ViewChangedListener> rmvls = ls;
				return new Runnable() {
					@Override
					public void run() {
						synchronized (View.this) {
							rmvls.remove(listener);
						}
					}
				};
			} else
				throw new IllegalArgumentException(
						"addListener more than once [" + " fieldname = " + fieldname + " listener = " + listener + "]");
		}

		Collection<ViewChangedListener> getListeners(String fieldname) {
			final Collection<ViewChangedListener> ls = listeners.get(fieldname);
			if (ls == null)
				return Collections.emptyList();
			else
				return ls;
		}

		void clear() {
			for (Collection<ViewChangedListener> c : listeners.values())
				c.clear();
			listeners.clear();
		}
	}

	private final AbstractViewContext viewContext;

	protected View(ViewContext vc) {
		viewContext = (AbstractViewContext) vc;
	}

	public final ViewContext getViewContext() {
		return viewContext;
	}

	public void sendMessage(String msg)
			throws InstantiationException, ClassCastException, SizePolicyException, CodecException {
		viewContext.sendMessage(this, msg);
	}

	protected abstract short getClassIndex();

	protected abstract void onData(long sessionid, byte index, byte field, Octets data, Octets dataremoved)
			throws MarshalException;

	@Override
	public String toString() {
		return "[class = " + getClass().getName() + " ProviderId = " + viewContext.getProviderId() + " classindex = "
				+ getClassIndex() + "]";
	}

	public abstract Set<String> getFieldNames();

	private final ViewChangedListenerContainer listenerContainer = new ViewChangedListenerContainer();

	public final synchronized Runnable registerListener(ViewChangedListener listener) {
		final Collection<Runnable> all = new ArrayList<Runnable>();
		for (final String name : getFieldNames())
			all.add(listenerContainer.addListener(name, listener));
		return new Runnable() {
			@Override
			public void run() {
				for (Runnable r : all)
					r.run();
			}
		};
	}

	public final synchronized Runnable registerListener(String fieldname, ViewChangedListener listener) {
		if (!getFieldNames().contains(fieldname))
			throw new IllegalArgumentException(
					"View " + getClass().getName() + " has no field named \"" + fieldname + "\"");
		return listenerContainer.addListener(fieldname, listener);
	}

	protected final void onViewChanged(final long sessionid, final String fieldname, final Object value,
			final ViewChangedType type) {
		ViewChangedEvent e = new ViewChangedEvent() {
			@Override
			public View getView() {
				return View.this;
			}

			@Override
			public long getSessionId() {
				return sessionid;
			}

			@Override
			public String getFieldName() {
				return fieldname;
			}

			@Override
			public Object getValue() {
				return value;
			}

			@Override
			public ViewChangedType getType() {
				return type;
			}

			@Override
			public String toString() {
				return View.this + " " + sessionid + " " + fieldname + " " + value + " " + type;
			}
		};
		for (ViewChangedListener l : listenerContainer.getListeners(fieldname))
			l.onViewChanged(e);
	}

	synchronized void doClose() {
		listenerContainer.clear();
	}

	public abstract class Control implements Marshal {
		protected Control() {
		}

		public abstract byte getControlIndex();

		public final void send() throws InstantiationException, SizePolicyException, CodecException {
			final SendControlToServer p = new SendControlToServer();
			p.providerid = View.this.viewContext.getProviderId();
			p.classindex = View.this.getClassIndex();
			p.instanceindex = View.this instanceof TemporaryView ? ((TemporaryView) View.this).getInstanceIndex() : 0;
			p.controlindex = this.getControlIndex();
			p.controlparameter = new OctetsStream().marshal(this);
			p.send(View.this.viewContext.getEndpointManager().getTransport());
		}
	}

	public interface StaticManager {
		int getProviderId();

		Map<Short, Class<? extends View>> getClasses();
	}
}
