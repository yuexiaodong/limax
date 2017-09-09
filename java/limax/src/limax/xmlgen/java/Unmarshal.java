package limax.xmlgen.java;

import java.io.PrintStream;
import java.util.Collection;

import limax.xmlgen.Bean;
import limax.xmlgen.Bind;
import limax.xmlgen.Cbean;
import limax.xmlgen.Main;
import limax.xmlgen.Type;
import limax.xmlgen.TypeAny;
import limax.xmlgen.TypeBinary;
import limax.xmlgen.TypeBoolean;
import limax.xmlgen.TypeByte;
import limax.xmlgen.TypeDouble;
import limax.xmlgen.TypeFloat;
import limax.xmlgen.TypeInt;
import limax.xmlgen.TypeList;
import limax.xmlgen.TypeLong;
import limax.xmlgen.TypeMap;
import limax.xmlgen.TypeSet;
import limax.xmlgen.TypeShort;
import limax.xmlgen.TypeString;
import limax.xmlgen.TypeVector;
import limax.xmlgen.Variable;
import limax.xmlgen.View;
import limax.xmlgen.Visitor;
import limax.xmlgen.Xbean;

public class Unmarshal implements Visitor {
	private final String varname;
	private final PrintStream ps;
	private final String prefix;

	public static void make(View view, PrintStream ps, String prefix) {
		ps.println(prefix + "@Override");
		ps.println(prefix
				+ "public limax.codec.OctetsStream unmarshal(limax.codec.OctetsStream _os_) throws limax.codec.MarshalException {");
		view.getVariables().forEach(var -> var.getType().accept(new Unmarshal(var.getName(), ps, prefix + "	")));
		view.getBinds().forEach(bind -> {
			if (bind.isFullBind())
				bind.getTable().getValueType().accept(new Unmarshal(bind.getName(), ps, prefix + "	"));
			else
				ps.println(prefix + "	_os_.unmarshal(" + bind.getName() + ");");
		});
		ps.println(prefix + "	return _os_;");
		ps.println(prefix + "}");
		ps.println();
	}

	public static void make(Collection<Variable> variables, PrintStream ps, String prefix) {
		ps.println(prefix + "@Override");
		ps.println(prefix
				+ "public limax.codec.OctetsStream unmarshal(limax.codec.OctetsStream _os_) throws limax.codec.MarshalException {");
		variables.forEach(var -> make(var, ps, prefix + "	"));
		ps.println(prefix + "	return _os_;");
		ps.println(prefix + "}");
		ps.println();
	}

	public static void make(Bean bean, PrintStream ps, String prefix) {
		make(bean.getVariables(), ps, prefix);
	}

	public static void make(Cbean bean, PrintStream ps, String prefix) {
		make(bean.getVariables(), ps, prefix);
	}

	public static void make(Xbean bean, PrintStream ps, String prefix) {
		make(bean.getVariables(), ps, prefix);
	}

	public static void make(Bind bind, PrintStream ps, String prefix) {
		make(bind.getVariables(), ps, prefix);
	}

	public static void make(Variable var, PrintStream ps, String prefix) {
		var.getType().accept(new Unmarshal("this." + var.getName(), ps, prefix));
	}

	public static void make(Type type, String varname, PrintStream ps, String prefix) {
		type.accept(new Unmarshal(varname, ps, prefix));
	}

	private Unmarshal(String varname, PrintStream ps, String prefix) {
		this.varname = varname;
		this.ps = ps;
		this.prefix = prefix;
	}

	@Override
	public void visit(TypeByte type) {
		ps.println(prefix + varname + " = _os_.unmarshal_byte();");
	}

	@Override
	public void visit(TypeFloat type) {
		ps.println(prefix + varname + " = _os_.unmarshal_float();");
	}

	@Override
	public void visit(TypeDouble type) {
		ps.println(prefix + varname + " = _os_.unmarshal_double();");
	}

	@Override
	public void visit(TypeInt type) {
		ps.println(prefix + varname + " = _os_.unmarshal_int();");
	}

	@Override
	public void visit(TypeShort type) {
		ps.println(prefix + varname + " = _os_.unmarshal_short();");
	}

	@Override
	public void visit(TypeLong type) {
		ps.println(prefix + varname + " = _os_.unmarshal_long();");
	}

	@Override
	public void visit(TypeBinary type) {
		if (Main.isMakingZdb)
			ps.println(prefix + varname + " = _os_.unmarshal_bytes();");
		else
			ps.println(prefix + varname + " = _os_.unmarshal_Octets();");
	}

	@Override
	public void visit(TypeString type) {
		ps.println(prefix + varname + " = _os_.unmarshal_String();");
	}

	private void unmarshalContainer(Type valueType) {
		ps.println(prefix + "for(int _i_ = _os_.unmarshal_size(); _i_ > 0; --_i_) {");
		ConstructWithUnmarshal.make(valueType, "_v_", ps, prefix + "	");
		ps.println(prefix + "	" + varname + ".add(_v_);");
		ps.println(prefix + "}");
	}

	@Override
	public void visit(TypeList type) {
		this.unmarshalContainer(type.getValueType());
	}

	@Override
	public void visit(TypeVector type) {
		this.unmarshalContainer(type.getValueType());
	}

	@Override
	public void visit(TypeSet type) {
		this.unmarshalContainer(type.getValueType());
	}

	private void unmarshalContainer(Type keytype, Type valuetype) {
		ps.println(prefix + "for(int _i_ = _os_.unmarshal_size(); _i_ > 0; --_i_) {");
		ConstructWithUnmarshal.make(keytype, "_k_", ps, prefix + "	");
		ConstructWithUnmarshal.make(valuetype, "_v_", ps, prefix + "	");
		ps.println(prefix + "	" + varname + ".put(_k_, _v_);");
		ps.println(prefix + "}");
	}

	@Override
	public void visit(TypeMap type) {
		unmarshalContainer(type.getKeyType(), type.getValueType());
	}

	@Override
	public void visit(Bean bean) {
		ps.println(prefix + varname + ".unmarshal(_os_);");
	}

	@Override
	public void visit(Cbean type) {
		ps.println(prefix + varname + ".unmarshal(_os_);");
	}

	@Override
	public void visit(Xbean type) {
		ps.println(prefix + varname + ".unmarshal(_os_);");
	}

	@Override
	public void visit(TypeBoolean type) {
		ps.println(prefix + varname + " = _os_.unmarshal_boolean();");
	}

	@Override
	public void visit(TypeAny type) {
		throw new UnsupportedOperationException();
	}
}
