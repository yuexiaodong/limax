package limax.zdb;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import limax.util.Elapse;
import limax.util.MBeans;
import limax.util.Trace;

final class Checkpoint implements Runnable, CheckpointMBean {
	private final static long SCHED_PERIOD = Integer.getInteger("limax.zdb.Checkpoint.SCHED_PERIOD", 100);
	private final ScheduledThreadPoolExecutor scheduler;
	private final Tables tables;

	private volatile long marshalNCount = 0;
	private volatile long marshal0Count = 0;
	private volatile long snapshotCount = 0;
	private volatile long flushCount = 0;
	private volatile int checkpointCount = 0;

	private volatile long marshalNTotalTime = 0;
	private volatile long snapshotTotalTime = 0;
	private volatile long flushTotalTime = 0;
	private volatile long checkpointTotalTime = 0;

	private volatile long nextMarshalTime;
	private volatile long nextCheckpointTime;

	private volatile boolean checkpointNow = false;

	@Override
	public void checkpoint() {
		this.checkpointNow = true;
		scheduler.execute(this);
	}

	Checkpoint(ScheduledThreadPoolExecutor scheduler, Tables tables) {
		this.scheduler = scheduler;
		this.tables = tables;
		long now = System.currentTimeMillis();
		nextMarshalTime = now + Zdb.meta().getMarshalPeriod();
		nextCheckpointTime = now + Zdb.meta().getCheckpointPeriod();
		MBeans.register(Zdb.mbeans(), this, "limax.zdb:type=Zdb,name=Checkpoint");
		scheduler.scheduleAtFixedRate(this, 0, SCHED_PERIOD, TimeUnit.MILLISECONDS);
	}

	private synchronized void checkpoint(final long now, limax.xmlgen.Zdb meta) {
		try {
			if (meta.getMarshalPeriod() >= 0 && nextMarshalTime <= now) {
				nextMarshalTime = now + meta.getMarshalPeriod();
				long start = System.nanoTime();
				int countMarshalN = tables.getStorages().stream().mapToInt(StorageInterface::marshalN).sum();
				this.marshalNCount += countMarshalN;
				this.marshalNTotalTime += System.nanoTime() - start;
				if (Trace.isDebugEnabled())
					Trace.debug("marshalN=*/" + countMarshalN);
			}
			final int checkpointPeriod = meta.getCheckpointPeriod();
			if (checkpointPeriod >= 0 && (this.checkpointNow || nextCheckpointTime <= now)) {
				nextCheckpointTime = now + checkpointPeriod;
				checkpointNow = false;
				checkpoint(meta);
			}
		} catch (Throwable e) {
			Trace.fatal("halt program", e);
			Runtime.getRuntime().halt(-1);
		}
	}

	private final Elapse elapse = new Elapse();

	synchronized void checkpoint(limax.xmlgen.Zdb meta) {
		if (Trace.isDebugEnabled())
			Trace.debug("---------------- begin ----------------");
		final List<StorageInterface> storages = this.tables.getStorages();

		if (meta.getMarshalN() < 1)
			if (Trace.isWarnEnabled())
				Trace.warn("marshalN disabled");

		elapse.reset();
		for (int i = 1; i <= meta.getMarshalN(); ++i) {
			int countMarshalN = storages.stream().mapToInt(StorageInterface::marshalN).sum();
			this.marshalNCount += countMarshalN;
			if (Trace.isDebugEnabled())
				Trace.debug("marshalN=" + i + "/" + countMarshalN);
		}
		this.marshalNTotalTime += elapse.elapsed();
		Runnable cleanupDuration;
		{
			int countSnapshot;
			int countMarshal0;
			Lock lock = tables.flushWriteLock();
			lock.lock();
			elapse.reset();
			try {
				countMarshal0 = storages.stream().mapToInt(StorageInterface::marshal0).sum();
				countSnapshot = storages.stream().mapToInt(StorageInterface::snapshot).sum();
				cleanupDuration = Zdb.checkpointDuration();
			} finally {
				lock.unlock();
			}
			long snapshotTime = elapse.elapsedAndReset();
			if (snapshotTime / 1000000 > meta.getSnapshotFatalTime())
				Trace.fatal(
						"snapshot time=" + snapshotTime + " snapshot=" + countSnapshot + " marshal0=" + countMarshal0);
			this.marshal0Count += countMarshal0;
			this.snapshotTotalTime += snapshotTime;
			this.snapshotCount += countSnapshot;
			if (Trace.isDebugEnabled())
				Trace.debug("snapshot=" + countSnapshot + " marshal0=" + countMarshal0);
		}
		int countFlush = storages.stream().mapToInt(StorageInterface::flush).sum();
		this.flushTotalTime += elapse.elapsedAndReset();
		if (Trace.isDebugEnabled())
			Trace.debug("flush=" + countFlush);
		if (countFlush > 0)
			this.tables.getLogger().checkpoint();
		if (countFlush > 0) {
			this.flushCount += countFlush;
			this.checkpointTotalTime += elapse.elapsedAndReset();
			if (Trace.isDebugEnabled())
				Trace.debug("checkpoint");
		}
		++checkpointCount;
		if (cleanupDuration != null)
			cleanupDuration.run();
		if (Trace.isDebugEnabled())
			Trace.debug("----------------- end -----------------");
	}

	@Override
	public void run() {
		checkpoint(System.currentTimeMillis(), Zdb.meta());
	}

	void cleanup() {
		Trace.fatal("final checkpoint begin");
		checkpoint(Zdb.meta());
		Trace.fatal("final checkpoint end");
	}

	@Override
	public long getCountMarshalN() {
		return marshalNCount;
	}

	@Override
	public long getCountMarshal0() {
		return marshal0Count;
	}

	@Override
	public long getCountFlush() {
		return this.flushCount;
	}

	@Override
	public long getTotalTimeCheckpoint() {
		return this.checkpointTotalTime;
	}

	@Override
	public long getTotalTimeFlush() {
		return this.flushTotalTime;
	}

	@Override
	public long getTotalTimeSnapshot() {
		return snapshotTotalTime;
	}

	@Override
	public int getCountCheckpoint() {
		return checkpointCount;
	}

	@Override
	public long getTotalTimeMarshalN() {
		return marshalNTotalTime;
	}

	public long getNextFlushTime() {
		return nextMarshalTime;
	}

	public long getNextCheckpointTime() {
		return nextCheckpointTime;
	}

	private final static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	@Override
	public String getTimeOfNextCheckpoint() {
		synchronized (dateFormat) {
			return dateFormat.format(this.getNextCheckpointTime());
		}
	}

	@Override
	public String getTimeOfNextFlush() {
		synchronized (dateFormat) {
			return dateFormat.format(this.getNextFlushTime());
		}
	}

	@Override
	public int getPeriodCheckpoint() {
		return Zdb.meta().getCheckpointPeriod();
	}

	@Override
	public long getCountSnapshot() {
		return this.snapshotCount;
	}
}
