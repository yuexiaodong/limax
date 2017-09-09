package limax.provider;

abstract class AutoView extends View {
	private boolean ticking;
	private boolean trigger;
	private long tick;

	protected AutoView(CreateParameter param, String[] prefix, byte[][] collectors, int cycle) {
		super(param, prefix, collectors, cycle);
		setTick(param.getViewStub().getTick());
	}

	public void setTick(long millisecond) {
		this.tick = millisecond < 0 ? 0 : millisecond;
	}

	public long getTick() {
		return this.tick;
	}

	void schedule() {
		trigger = true;
		if (!ticking && !isClosed()) {
			ticking = true;
			getViewContext().delaySchedule(tick, () -> hashSchedule(() -> {
				trigger = false;
				flush();
				ticking = false;
				if (trigger)
					schedule();
			}));
		}
	}

	abstract void flush();
}
