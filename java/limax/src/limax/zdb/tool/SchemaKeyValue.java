package limax.zdb.tool;

import java.util.LinkedHashMap;
import java.util.Map;

import limax.xmlgen.Type;

class SchemaKeyValue implements Schema {
	private Schema key;
	private Schema value;

	public SchemaKeyValue(Type key, Type value) {
		this.key = Schemas.of(key);
		this.value = Schemas.of(value);
	}

	public Schema key() {
		return key;
	}

	public Schema value() {
		return value;
	}

	public Map<String, Schema> entries() {
		Map<String, Schema> map = new LinkedHashMap<>();
		map.put("key", key);
		map.put("value", value);
		return map;
	}

	@Override
	public DataKeyValue create() {
		return new DataKeyValue(this);
	}

	@Override
	public ConvertType diff(Schema t, boolean asKey) {
		if (t instanceof SchemaKeyValue) {
			SchemaKeyValue target = (SchemaKeyValue) t;
			ConvertType keyDiff = key.diff(target.key, true);
			ConvertType valueDiff = value.diff(target.value, asKey);

			if (keyDiff == ConvertType.MANUAL || valueDiff == ConvertType.MANUAL) {
				return ConvertType.MANUAL;
			} else if (keyDiff == ConvertType.MAYBE_AUTO || valueDiff == ConvertType.MAYBE_AUTO) {
				return ConvertType.MAYBE_AUTO;
			} else if (keyDiff == ConvertType.AUTO || valueDiff == ConvertType.AUTO) {
				return ConvertType.AUTO;
			} else {
				return ConvertType.SAME;
			}
		} else {
			return ConvertType.MANUAL;
		}
	}

	@Override
	public String toString() {
		return Schemas.str(entries(), "");
	}
}