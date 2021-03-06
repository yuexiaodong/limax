package limax.xmlgen;

public final class TypeString extends Type {

	TypeString() {
		super("string");
	}

	@Override
	public boolean resolve() {
		return true;
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	@Override
	public boolean isConstType() {
		return true;
	}

	@Override
	public boolean isAny() {
		return false;
	}

	@Override
	public boolean isJSONSerializable() {
		return true;
	}
}
