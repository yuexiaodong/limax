package limax.xmlgen;

import java.util.Set;

public final class TypeList extends Type {
	private final Type valueType;
	private final TypeVector vectorType;

	TypeList(Naming parent, Type valueType) {
		super(parent);
		this.valueType = valueType;
		this.vectorType = new TypeVector(parent, valueType);
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	@Override
	public boolean isConstType() {
		return false;
	}

	@Override
	public boolean isAny() {
		return valueType.isAny();
	}

	@Override
	public boolean isJSONSerializable() {
		return valueType.isJSONSerializable();
	}

	@Override
	public void depends(Set<Type> incls) {
		incls.add(this);
		valueType.depends(incls);
	}

	public Type getValueType() {
		return valueType;
	}

	public TypeVector asTypeVector() {
		return vectorType;
	}
}
