package limax.zdb;

import java.util.concurrent.atomic.AtomicLong;

import limax.codec.Marshal;

public abstract class XBean implements Marshal {
	private static final AtomicLong objid = new AtomicLong();

	final Long _objid_ = objid.incrementAndGet();
	private XBean _parent_;
	private String _varname_;

	protected XBean(XBean parent, String varname) {
		_parent_ = parent;
		_varname_ = varname;
	}

	final void link(XBean parent, String varname, boolean log) {
		if (null != parent) {
			if (null != _parent_) // parent != null && _parent_ != null
				throw new XManagedError("ambiguously managed");
			if (parent == this)
				throw new XManagedError("loop managed");
		} else {
			if (null == _parent_) // parent == null && _parent_ == null
				throw new XManagedError("not managed");
		}
		if (log)
			Transaction.currentSavepoint().addIfAbsent(new LogKey(this, "_parent_"), new Log() {
				private final XBean parent = _parent_;
				private final String varname = _varname_;

				@Override
				public void commit() {
				}

				@Override
				public void rollback() {
					_parent_ = parent;
					_varname_ = varname;
				}
			});
		_parent_ = parent;
		_varname_ = varname;
	}

	public final XBean parent() {
		return _parent_;
	}

	void notify(LogNotify notify) {
		if (null != _parent_)
			_parent_.notify(notify.push(new LogKey(_parent_, _varname_)));
	}

	private TRecord<?, ?> getRecord() {
		XBean self = this;
		do {
			if (self instanceof TRecord<?, ?>)
				return (TRecord<?, ?>) self;
			self = self._parent_;
		} while (self != null);
		return null;
	}

	private final static Runnable donothing = () -> {
	};

	protected final Runnable verifyStandaloneOrLockHeld(String methodName, boolean readonly) {
		if (!Zdb.meta().isZdbVerify())
			return donothing;
		if (Transaction.current() == null)
			return donothing;
		if (Zdb.tables().isFlushWriteLockHeldByCurrentThread())
			return donothing;
		TRecord<?, ?> record = getRecord();
		if (record == null)
			return donothing;
		Lockey lockey = record.getLockey();
		if (lockey.isWriteLockedByCurrentThread())
			return donothing;
		if (readonly && lockey.isReadLockedByCurrentThread())
			return () -> {
				throw new XLockLackedError(getClass().getName() + "." + methodName);
			};
		throw new XLockLackedError(getClass().getName() + "." + methodName);
	}

	public static class XLockLackedError extends XError {
		private static final long serialVersionUID = -2377572699783238493L;

		public XLockLackedError(String message) {
			super(message);
		}
	}

	public static class XManagedError extends XError {
		static final long serialVersionUID = 7269011645942640931L;

		XManagedError(String message) {
			super(message);
		}
	}

	@Override
	public String toString() {
		return getClass().getName();
	}
}
