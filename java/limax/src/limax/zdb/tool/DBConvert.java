package limax.zdb.tool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import limax.util.StringUtils;
import limax.xmlgen.Cbean;
import limax.xmlgen.Main;
import limax.xmlgen.Table;
import limax.xmlgen.Type;
import limax.xmlgen.Xbean;
import limax.xmlgen.Zdb;
import limax.xmlgen.java.Construct;
import limax.xmlgen.java.ConstructWithUnmarshal;
import limax.xmlgen.java.Declare;
import limax.xmlgen.java.Define;
import limax.xmlgen.java.Marshal;
import limax.xmlgen.java.Unmarshal;
import limax.zdb.DBC;

class DBConvert {
	public static void convert(String sourcePath, String targetPath, boolean autoConvertWhenMaybeAuto,
			boolean generateSolver, PrintStream out) throws Exception {
		Zdb targetMeta = Zdb.loadFromClass();
		Zdb sourceMeta = Zdb.loadFromDb(sourcePath);
		targetMeta.setDbHome(targetPath);

		List<String> needGenerateConverters = new ArrayList<>();
		List<String> needGenerateSolvers = new ArrayList<>();

		Convert.diff(sourceMeta, targetMeta, true, null).forEach((tableName, type) -> {
			out.println(tableName + " " + type);
			needGenerateSolvers.add(tableName);
			switch (type) {
			case SAME:
				break;
			case AUTO:
				break;
			case MAYBE_AUTO:
				if (!autoConvertWhenMaybeAuto) {
					needGenerateConverters.add(tableName);
				}
				break;
			case MANUAL:
				needGenerateConverters.add(tableName);
				break;
			}
		});

		if (needGenerateConverters.isEmpty() && !generateSolver) {
			out.println("-----no need generate!, auto convert start-----");
			DBC.start();
			Convert.convert(DBC.open(sourceMeta), DBC.open(targetMeta), null, null, autoConvertWhenMaybeAuto, out);
			DBC.stop();
			out.println("-----auto convert end-----");
		} else {
			try {
				Method method = Class.forName("COV").getMethod("convert", DBC.class, DBC.class);
				out.println("-----COV.class found, manual convert start-----");
				try {
					DBC.start();
					DBC sourceDBC = DBC.open(sourceMeta);
					DBC targetDBC = DBC.open(targetMeta);
					method.invoke(null, sourceDBC, targetDBC);
					DBC.stop();
				} catch (Exception e) {
					e.printStackTrace();
				}
				out.println("-----manual convert end-----");
				return;
			} catch (Exception e) {
				out.println("-----COV.class not found, generate-----");
			}
			Path dir = Paths.get("cov");
			if (dir.toFile().mkdir()) {
				out.println("make dir cov");
			}

			for (String tableName : needGenerateConverters) {
				String clazz = "_" + StringUtils.upper1(tableName);
				File file = dir.resolve(clazz + ".java").toFile();
				if (file.exists()) {
					out.println("skip exist file " + file);
				} else {
					out.println("generating " + file);
					try (PrintStream ps = new PrintStream(new FileOutputStream(file), true, "UTF-8")) {
						new DBConvert(sourceMeta, targetMeta, tableName, clazz, ps).generateConverter();
					} catch (Exception e) {
						e.printStackTrace(out);
					}
				}
			}

			{
				File file = dir.resolve("COV.java").toFile();
				if (file.exists()) {
					out.println("skip exist file " + file);
				} else {
					out.println("generating " + file);
					try (PrintStream ps = new PrintStream(new FileOutputStream(file), true, "UTF-8")) {
						ps.println("import limax.zdb.DBC;");
						ps.println("import limax.zdb.tool.Converter;");
						ps.println("import limax.zdb.tool.Convert;");
						ps.println("import limax.zdb.tool.ConflictSolver;");
						ps.println();
						ps.println("import java.util.Map;");
						ps.println("import java.util.LinkedHashMap;");
						ps.println();
						ps.println("public class COV {");
						ps.println();
						ps.println("    public static void convert(DBC sourceDBC, DBC targetDBC) {");
						ps.println("        Map<String, Converter> converterMap = new LinkedHashMap<>();");
						for (String tableName : needGenerateConverters) {
							ps.println("        converterMap.put(\"" + tableName + "\", _"
									+ StringUtils.upper1(tableName) + ".INSTANCE);");
						}

						ps.println("        Map<String, ConflictSolver> solverMap = new LinkedHashMap<>();");
						if (generateSolver) {
							for (String tableName : needGenerateSolvers) {
								ps.println("        solverMap.put(\"" + tableName + "\", solver._"
										+ StringUtils.upper1(tableName) + ".INSTANCE);");
							}
						}

						ps.println("        Convert.convert(sourceDBC, targetDBC, converterMap, solverMap, "
								+ autoConvertWhenMaybeAuto + ", System.out);");
						ps.println("    }");
						ps.println("}");

					} catch (Exception e) {
						e.printStackTrace(out);
					}
				}
			}

			if (generateSolver) {
				Path solverDir = dir.resolve("solver");
				if (solverDir.toFile().mkdir()) {
					out.println("make dir [cov/solver]");
				}

				for (String tableName : needGenerateSolvers) {
					String clazz = "_" + StringUtils.upper1(tableName);
					File file = solverDir.resolve(clazz + ".java").toFile();
					if (file.exists()) {
						out.println("skip exist file " + file);
					} else {
						out.println("generating " + file);
						try (PrintStream ps = new PrintStream(new FileOutputStream(file), true, "UTF-8")) {
							new DBConvert(sourceMeta, targetMeta, tableName, clazz, ps).generateSolver();
						} catch (Exception e) {
							e.printStackTrace(out);
						}
					}
				}
			}
		}
	}

	private Zdb sourceZdbMeta;
	private Zdb targetZdbMeta;
	private Table sourceMeta;
	private Table targetMeta;

	private String clazz;
	private PrintStream ps;

	private SchemaKeyValue sourceSchema;
	private SchemaKeyValue targetSchema;

	private Map<String, SchemaBean> sourceBeans = new HashMap<>();
	private Map<String, SchemaBean> targetBeans = new HashMap<>();
	private Map<String, SchemaBean> allTargetBeans = new HashMap<>();
	private Map<String, SchemaBean> sameBeans = new HashMap<>();

	private DBConvert(Zdb sourceZdbMeta, Zdb targetZdbMeta, String tableName, String clazz, PrintStream ps) {
		this.sourceZdbMeta = sourceZdbMeta;
		this.targetZdbMeta = targetZdbMeta;
		this.clazz = clazz;
		this.ps = ps;

		sourceMeta = sourceZdbMeta.getTable(tableName);
		targetMeta = targetZdbMeta.getTable(tableName);
		sourceSchema = Schemas.of(sourceMeta);
		targetSchema = Schemas.of(targetMeta);

		findAllChildBean(sourceSchema, sourceBeans);
		findAllChildBean(targetSchema, targetBeans);
		allTargetBeans.putAll(targetBeans);

		sourceBeans.forEach((beanName, sourceBean) -> {
			SchemaBean targetBean = targetBeans.get(beanName);
			if (targetBean != null && sourceBean.diff(targetBean, true) == ConvertType.SAME)
				sameBeans.put(beanName, sourceBean);
		});
		sameBeans.forEach((beanName, s) -> {
			sourceBeans.remove(beanName);
			targetBeans.remove(beanName);
		});
	}

	private void generateSolver() {
		ps.println("package solver;");
		ps.println();

		ps.println("import limax.codec.Octets;");
		ps.println("import limax.codec.OctetsStream;");
		ps.println("import limax.codec.MarshalException;");
		ps.println();

		ps.println("public enum " + clazz + " implements limax.zdb.tool.ConflictSolver {");
		ps.println();
		ps.println("    INSTANCE;");
		ps.println();

		for (String name : allTargetBeans.keySet()) {
			formatBean(targetZdbMeta, name, "    ", false);
		}

		ps.println();
		ps.println("    @Override");
		ps.println(
				"    public Octets solve(Octets sourceValue, Octets targetValue, Octets key) throws MarshalException {");
		ps.println("        OctetsStream _os_;");
		formatDefineAndUnmarshal(targetMeta.getValueType(), "sourceValue", "s");
		formatDefineAndUnmarshal(targetMeta.getValueType(), "targetValue", "t");
		formatDefineAndUnmarshal(targetMeta.getKeyType(), "key", "k");
		Define.make(targetMeta.getValueType(), "res", ps, "        ");
		ps.println("        //TODO ");
		ps.println("        _os_ = new OctetsStream();");
		Marshal.make(targetMeta.getValueType(), "res", ps, "        ");
		ps.println("        return _os_;");
		ps.println("    }");
		ps.println();

		ps.println("}");
	}

	private void formatDefineAndUnmarshal(Type type, String osName, String varName) {
		ps.println("        _os_ = OctetsStream.wrap(" + osName + ");");
		ConstructWithUnmarshal.make(type, varName, ps, "        ");
	}

	private void generateConverter() {
		ps.println("import limax.codec.OctetsStream;");
		ps.println("import limax.codec.MarshalException;");
		ps.println();

		ps.println("public enum " + clazz + " implements limax.zdb.tool.Converter {");
		ps.println();
		ps.println("    INSTANCE;");
		ps.println();

		sameBeans.keySet().forEach(name -> formatBean(sourceZdbMeta, name, "    ", false));
		ps.println();

		ps.println("    static class SS {");
		ps.println();
		sourceBeans.keySet().forEach(name -> formatBean(sourceZdbMeta, name, "        ", false));
		ps.println("    }");
		ps.println();

		ps.println("    static class TT {");
		ps.println();
		targetBeans.keySet().forEach(name -> formatBean(targetZdbMeta, name, "        ", true));
		ps.println("    }");
		ps.println();
		ps.println();
		ps.println();

		ps.println("    @Override");
		ps.println("    public OctetsStream convertKey(OctetsStream key) throws MarshalException {");
		formatConvert(true);
		ps.println("    }");
		ps.println();

		ps.println("    @Override");
		ps.println("    public OctetsStream convertValue(OctetsStream value) throws MarshalException {");
		formatConvert(false);
		ps.println("    }");
		ps.println();

		ps.println("}");
	}

	private void formatConvert(boolean isKey) {
		Schema sourcePart = isKey ? sourceSchema.key() : sourceSchema.value();
		Schema targetPart = isKey ? targetSchema.key() : targetSchema.value();
		String param = isKey ? "key" : "value";
		if (sourcePart.diff(targetPart, true) == ConvertType.SAME) {
			ps.println("        return " + param + ";");
		} else {
			String sourceBeanName = null;
			if (sourcePart instanceof SchemaBean) {
				SchemaBean partBean = (SchemaBean) sourcePart;
				sourceBeanName = partBean.getType();
				String clz = clazzName(partBean, true);
				ps.println("        " + clz + " s = new " + clz + "();");
				ps.println("        s.unmarshal(" + param + ");");
			} else {
				Type type = isKey ? sourceMeta.getKeyType() : sourceMeta.getValueType();
				ps.println("        OctetsStream _os_ = " + param + ";");
				ConstructWithUnmarshal.make(type, "s", ps, "        ");
			}

			if (targetPart instanceof SchemaBean) {
				SchemaBean partBean = (SchemaBean) targetPart;
				boolean hasConvertFrom = partBean.getType().equals(sourceBeanName);
				String clz = clazzName(partBean, false);
				ps.println("        " + clz + " t = new " + clz + "();");
				if (hasConvertFrom)
					ps.println("        t.convertFrom(s);");
				else
					ps.println("        // TODO");
				ps.println("        return new OctetsStream().marshal(t);");
			} else {
				Type type = isKey ? targetMeta.getKeyType() : targetMeta.getValueType();
				Define.make(type, "t", ps, "        ");
				ps.println("        // TODO");

				if (sourceBeanName == null) {
					ps.println("        OctetsStream _os_ = new OctetsStream();");
				} else {
					ps.println("        _os_ = new OctetsStream();");
				}
				Marshal.make(type, "t", ps, "        ");
				ps.println("        return _os_;");

			}
		}
	}

	private void formatBean(Zdb zdbMeta, String name, String prefix, boolean tryConvertFrom) {
		Main.isMakingConverter = true;
		ps.println(prefix + "static class " + name + " implements limax.codec.Marshal {");
		Xbean xb = zdbMeta.getXbean(name);
		if (xb != null) {
			Declare.make(xb.getEnums(), xb.getVariables(), Declare.Type.PUBLIC, ps, prefix + "    ");
			Construct.make(xb, ps, prefix + "    ");
			Marshal.make(xb, ps, prefix + "    ");
			Unmarshal.make(xb, ps, prefix + "    ");
		} else {
			Cbean cb = zdbMeta.getCbean(name);
			Declare.make(cb.getEnums(), cb.getVariables(), Declare.Type.PUBLIC, ps, prefix + "    ");
			Construct.make(cb, ps, prefix + "    ");
			Marshal.make(cb, ps, prefix + "    ");
			Unmarshal.make(cb, ps, prefix + "    ");
		}

		if (tryConvertFrom) {
			formatBeanConvertFrom(name, prefix);
		}

		ps.println(prefix + "}");
		ps.println();
	}

	private void formatBeanConvertFrom(String name, String prefix) {
		SchemaBean sourceBean = sourceBeans.get(name);
		SchemaBean targetBean = targetBeans.get(name);
		if (sourceBean == null || targetBean == null)
			return;

		ps.println(prefix + "    public void convertFrom(SS." + name + " s) {");
		boolean hasTodo = false;
		for (Map.Entry<String, Schema> entry : targetBean.entries().entrySet()) {
			String fn = entry.getKey();
			Schema tb = entry.getValue();
			Schema sb = sourceBean.entries().get(fn);
			if (sb == null) {
				ps.println(prefix + "        // TODO " + fn + " = ");
				hasTodo = true;
			} else {
				if (sameBeans.containsKey(fn) || sb.diff(tb, true) == ConvertType.SAME) {
					ps.println(prefix + "        " + fn + " = s." + fn + ";");
				} else {
					if (tb instanceof SchemaBean && sb instanceof SchemaBean) {
						ps.println(prefix + "        " + fn + ".convertFrom(s." + fn + ");");
					} else {
						ps.println(prefix + "        // TODO " + fn + " = s." + fn + ";");
						hasTodo = true;
					}
				}
			}
		}
		if (!hasTodo) {
			ps.println(prefix + "        // TODO");
		}
		ps.println(prefix + "    }");
		ps.println();
	}

	private String clazzName(SchemaBean bean, boolean source) {
		String name = bean.getType();
		if (sameBeans.containsKey(name)) {
			return name;
		} else if (source) {
			return "SS." + name;
		} else {
			return "TT." + name;
		}
	}

	private static void findAllChildBean(Schema schema, Map<String, SchemaBean> collector) {
		if (schema instanceof SchemaBean) {
			SchemaBean sb = (SchemaBean) schema;
			collector.put(sb.getType(), sb);
			sb.entries().forEach((f, s) -> findAllChildBean(s, collector));
		} else if (schema instanceof SchemaKeyValue) {
			SchemaKeyValue skv = (SchemaKeyValue) schema;
			findAllChildBean(skv.key(), collector);
			findAllChildBean(skv.value(), collector);
		} else if (schema instanceof SchemaCollection) {
			SchemaCollection sc = (SchemaCollection) schema;
			findAllChildBean(sc.element(), collector);
		}
	}
}
