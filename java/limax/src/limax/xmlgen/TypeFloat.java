package limax.xmlgen;

public final class TypeFloat extends Type {

	TypeFloat() {
		super("float");
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
