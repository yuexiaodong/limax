package limax.xmlgen;

import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;

import limax.util.ElementHelper;

public class Xbean extends Type {

	private String comment;
	private boolean json = false;

	public Xbean(Zdb parent, Element self) throws Exception {
		super(parent, self);
		Variable.verifyName(this);
		comment = Bean.extractComment(self);
		ElementHelper eh = new ElementHelper(self);
		json = eh.getBoolean("json", false);
		eh.warnUnused("name", "xml:base", "xmlns:xi");
	}

	public Xbean(Zdb parent, String name) {
		super(parent, name);
	}

	@Override
	boolean resolve() {
		if (!super.resolve())
			return false;
		if (json && !isJSONSerializable())
			throw new RuntimeException("Xbean " + getFullName() + " is not JSONSerializable.");
		return true;
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	@Override
	public void depends(Set<Type> incls) {
		if (incls.add(this))
			getVariables().forEach(var -> var.getType().depends(incls));
	}

	public String getComment() {
		return comment;
	}

	public boolean isJSONEnabled() {
		return json;
	}

	@Override
	public boolean isJSONSerializable() {
		return json && !getVariables().stream()
				.filter(var -> var.isJSONEnabled() && !var.getType().isJSONSerializable()).findFirst().isPresent();
	}

	public List<Variable> getVariables() {
		return getChildren(Variable.class);
	}

	public Variable getVariable(String varname) {
		return getVariables().stream().filter(var -> var.getName().equals(varname)).findAny().orElse(null);
	}

	public List<Enum> getEnums() {
		return getChildren(Enum.class);
	}

	public String getLastName() {
		return getName();
	}

	public String getFirstName() {
		return "xbean";
	}

	public String getFullName() {
		return getFirstName() + "." + getName();
	}

	@Override
	public boolean isConstType() {
		return false;
	}
}
