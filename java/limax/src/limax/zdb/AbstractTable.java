package limax.zdb;

abstract class AbstractTable implements Table {

	AbstractTable() {
	}

	abstract StorageInterface open(limax.xmlgen.Table meta, LoggerEngine logger);

	abstract void close();

}
