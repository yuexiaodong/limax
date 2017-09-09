package limax.zdb.tool;

import java.util.Map;

import limax.xmlgen.Bean;
import limax.xmlgen.Cbean;
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
import limax.xmlgen.Xbean;

class Schemas {

	static SchemaKeyValue of(limax.xmlgen.Table table) {
		return new SchemaKeyValue(table.getKeyType(), table.getValueType());
	}

	static Schema of(Type type) {
		TypeVisitor v = new TypeVisitor();
		type.accept(v);
		return v.dt;
	}

	static final Schema sboolean = new SchemaInt(IntType.BOOLEAN);
	static final Schema sbyte = new SchemaInt(IntType.BYTE);
	static final Schema sshort = new SchemaInt(IntType.SHORT);
	static final Schema sint = new SchemaInt(IntType.INT);
	static final Schema slong = new SchemaInt(IntType.LONG);

	static final Schema sdouble = new SchemaFloat(true);
	static final Schema sfloat = new SchemaFloat(false);

	static final Schema sbinary = new Schema() {

		@Override
		public DataBinary create() {
			return new DataBinary();
		}

		@Override
		public ConvertType diff(Schema t, boolean asKey) {
			return this == t ? ConvertType.SAME : ConvertType.MANUAL;
		}

		@Override
		public String toString() {
			return "byte[]";
		}
	};

	static final Schema sstring = new Schema() {

		@Override
		public DataString create() {
			return new DataString();
		}

		@Override
		public ConvertType diff(Schema t, boolean asKey) {
			return this == t ? ConvertType.SAME : ConvertType.MANUAL;
		}

		@Override
		public String toString() {
			return "String";
		}
	};

	static String type(Schema schema) {
		if (schema instanceof SchemaCollection) {
			SchemaCollection s = ((SchemaCollection) schema);
			return s.type + "<" + type(s.element()) + ">";
		} else if (schema instanceof SchemaBean) {
			return ((SchemaBean) schema).getType();
		} else if (schema instanceof SchemaKeyValue) {
			SchemaKeyValue kv = (SchemaKeyValue) schema;
			return type(kv.key()) + ", " + type(kv.value());
		} else {
			return schema.toString();
		}
	}

	static String str(Map<String, Schema> map, String prefix) {
		StringBuilder sb = new StringBuilder();
		map.forEach((name, s) -> {
			sb.append(prefix).append(name).append(":\t").append(Schemas.type(s));
			sb.append(System.lineSeparator());

			if (s instanceof SchemaCollection)
				s = ((SchemaCollection) s).element();

			if (s instanceof SchemaBean)
				sb.append(str(((SchemaBean) s).entries(), "\t" + prefix));
			else if (s instanceof SchemaKeyValue)
				sb.append(str(((SchemaKeyValue) s).entries(), "\t" + prefix));
		});
		return sb.toString();
	}

	private static class TypeVisitor implements limax.xmlgen.Visitor {

		private Schema dt;

		@Override
		public void visit(TypeBoolean type) {
			dt = sboolean;
		}

		@Override
		public void visit(TypeByte type) {
			dt = sbyte;
		}

		@Override
		public void visit(TypeShort type) {
			dt = sshort;
		}

		@Override
		public void visit(TypeInt type) {
			dt = sint;
		}

		@Override
		public void visit(TypeLong type) {
			dt = slong;
		}

		@Override
		public void visit(TypeFloat type) {
			dt = sfloat;
		}

		@Override
		public void visit(TypeDouble type) {
			dt = sdouble;
		}

		@Override
		public void visit(TypeBinary type) {
			dt = sbinary;
		}

		@Override
		public void visit(TypeString type) {
			dt = sstring;
		}

		@Override
		public void visit(TypeList type) {
			dt = new SchemaCollection("list", type.getValueType(), false);
		}

		@Override
		public void visit(TypeSet type) {
			dt = new SchemaCollection("set", type.getValueType(), true);
		}

		@Override
		public void visit(TypeVector type) {
			dt = new SchemaCollection("vector", type.getValueType(), false);
		}

		@Override
		public void visit(TypeMap type) {
			dt = new SchemaCollection("map", type.getKeyType(), type.getValueType());
		}

		@Override
		public void visit(Bean type) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit(Cbean type) {
			dt = new SchemaBean(type);
		}

		@Override
		public void visit(Xbean type) {
			dt = new SchemaBean(type);
		}

		@Override
		public void visit(TypeAny type) {
			throw new UnsupportedOperationException();
		}
	}
}
