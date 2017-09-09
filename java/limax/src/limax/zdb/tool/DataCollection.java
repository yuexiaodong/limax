package limax.zdb.tool;

import java.util.ArrayList;
import java.util.Collection;

import limax.codec.MarshalException;
import limax.codec.OctetsStream;

public class DataCollection implements Data {
	private Schema elementSchema;
	private Collection<Data> value = new ArrayList<>();

	DataCollection(SchemaCollection schema) {
		elementSchema = schema.element();
	}

	public Collection<Data> getValue() {
		return value;
	}

	public Schema elementSchema() {
		return elementSchema;
	}

	@Override
	public void convertTo(Data t) {
		DataCollection target = (DataCollection) t;
		for (Data d : value) {
			Data td = target.elementSchema.create();
			d.convertTo(td);
			target.value.add(td);
		}
	}

	@Override
	public OctetsStream marshal(OctetsStream os) {
		os.marshal_size(value.size());
		for (Data d : value)
			d.marshal(os);
		return os;
	}

	@Override
	public OctetsStream unmarshal(OctetsStream os) throws MarshalException {
		int size = os.unmarshal_size();
		for (int i = 0; i < size; i++) {
			Data d = elementSchema.create();
			d.unmarshal(os);
			value.add(d);
		}
		return os;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (Data d : value) {
			sb.append(d).append(", ");
		}
		sb.append("]");
		return sb.toString();
	}

}
