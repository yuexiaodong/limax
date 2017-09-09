package limax.zdb.tool;

import java.util.LinkedHashMap;
import java.util.Map;

import limax.codec.MarshalException;
import limax.codec.OctetsStream;

public class DataBean implements Data {
	private Map<String, Data> value = new LinkedHashMap<>();

	DataBean(SchemaBean schema) {
		for (Map.Entry<String, Schema> t : schema.entries().entrySet()) {
			value.put(t.getKey(), t.getValue().create());
		}
	}

	public Map<String, Data> entries() {
		return value;
	}

	@Override
	public void convertTo(Data t) {
		DataBean target = (DataBean) t;
		target.value.forEach((name, targetData) -> {
			Data data = value.get(name);
			if (data != null)
				data.convertTo(targetData);
		});
	}

	@Override
	public OctetsStream marshal(OctetsStream os) {
		for (Data d : value.values())
			d.marshal(os);
		return os;
	}

	@Override
	public OctetsStream unmarshal(OctetsStream os) throws MarshalException {
		for (Data d : value.values())
			d.unmarshal(os);
		return os;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		value.forEach((name, data) -> {
			sb.append(name).append(":").append(data).append(", ");
		});
		sb.append("}");
		return sb.toString();
	}

}
