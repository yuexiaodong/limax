package limax.xmlgen.java;

import java.io.File;
import java.io.PrintStream;

import limax.xmlgen.Cbean;
import limax.xmlgen.FileOperation;

class ViewCbeanFormatter {
	private final Cbean bean;
	private final String name;

	public ViewCbeanFormatter(Cbean bean) {
		this.bean = bean;
		this.name = bean.getLastName();
	}

	public void make(File output) {
		output = new File(output, "cbean");
		try (PrintStream ps = FileOperation.fopen(output, name + ".java")) {
			ps.println("package cbean;");
			ps.println();
			ps.println(
					"public class " + bean.getName() + " implements limax.codec.Marshal, Comparable<" + name + "> {");
			printDefine(ps);
			ps.println("}");
			ps.println();
		}
	}

	public void printDefine(PrintStream ps) {
		Declare.make(bean.getEnums(), bean.getVariables(), Declare.Type.PUBLIC, ps, "	");
		Construct.make(bean, ps, "	");
		Hashcode.make(bean, ps, "	");
		Equals.make(bean, ps, "	");
		CompareTo.make(bean, ps, "	");
		Marshal.make(bean, ps, "	");
		Unmarshal.make(bean, ps, "	");
	}
}
