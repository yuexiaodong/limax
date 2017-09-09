package limax.xmlgen.csharp;

import java.io.File;
import java.io.PrintStream;

import limax.xmlgen.Cbean;
import limax.xmlgen.FileOperation;

public class CbeanFormatter {
	private final Cbean bean;

	public CbeanFormatter(Cbean bean) {
		this.bean = bean;
	}

	public void make(File output) {
		try (final PrintStream ps = FileOperation.fopen(output, bean.getFullName() + ".cs")) {
			BeanFormatter.printCommonInclude(ps);
			ps.println();

			ps.println("namespace " + bean.getFirstName());
			ps.println("{");
			String baseclass = BeanFormatter.baseClassName;
			if (bean.isConstType())
				baseclass += ", IComparable<" + bean.getName() + ">";
			if (bean.isJSONEnabled())
				baseclass += ", JSONMarshal";
			ps.println("	public sealed class " + bean.getName() + " : " + baseclass);
			ps.println("	{");
			printDefine(ps);
			ps.println("	}");
			ps.println("}");
			ps.println();
		}
	}

	private void printDefine(PrintStream ps) {
		BeanFormatter.declareEnums(ps, bean.getEnums());
		Define.make(bean, ps, "		");
		Construct.make(bean, ps, "		");
		ConstructWithParam.make(bean, ps, "		");
		Marshal.make(bean, ps, "		");
		Unmarshal.make(bean, ps, "		");
		if (bean.isConstType())
			CompareTo.make(bean, ps, "		");
		if (bean.isJSONEnabled())
			JSONMarshal.make(bean, ps, "		");
		Equals.make(bean, ps, "		");
		Hashcode.make(bean, ps, "		");
	}
}
