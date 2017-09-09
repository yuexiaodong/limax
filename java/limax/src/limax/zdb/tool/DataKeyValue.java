package limax.zdb.tool;

import limax.codec.MarshalException;
import limax.codec.OctetsStream;

public class DataKeyValue implements Data {
	private Data key;
	private Data value;

	DataKeyValue(SchemaKeyValue schema) {
		key = schema.key().create();
		value = schema.value().create();
	}

	public Data getKey() {
		return key;
	}

	public Data getValue() {
		return value;
	}

	public void setKey(Data key) {
		this.key = key;
	}

	public void setValue(Data value) {
		this.value = value;
	}

	@Override
	public void convertTo(Data t) {
		DataKeyValue target = (DataKeyValue) t;
		key.convertTo(target.key);
		value.convertTo(target.value);
	}

	@Override
	public OctetsStream marshal(OctetsStream os) {
		return os.marshal(key).marshal(value);
	}

	@Override
	public OctetsStream unmarshal(OctetsStream os) throws MarshalException {
		return os.unmarshal(key).unmarshal(value);
	}

	@Override
	public String toString() {
		return key + ": " + value;
	}
}
