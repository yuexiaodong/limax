package limax.zdb.tool;

import limax.xmlgen.Type;

class SchemaCollection implements Schema {
	private Schema element;
	String type;
	private boolean isSet;

	public SchemaCollection(String type, Type element, boolean isSet) {
		this.type = type;
		this.element = Schemas.of(element);
		this.isSet = isSet;
	}

	public SchemaCollection(String type, Type key, Type value) {
		this.type = type;
		this.element = new SchemaKeyValue(key, value);
	}

	public Schema element() {
		return element;
	}

	@Override
	public DataCollection create() {
		return new DataCollection(this);
	}

	@Override
	public ConvertType diff(Schema t, boolean asKey) {
		if (t instanceof SchemaCollection) {
			return element.diff(((SchemaCollection) t).element, isSet || asKey);
		}
		return ConvertType.MANUAL;
	}
}