package limax.zdb.tool;

import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import limax.codec.MarshalException;
import limax.codec.Octets;
import limax.codec.OctetsStream;
import limax.xmlgen.Table;
import limax.xmlgen.Zdb;
import limax.zdb.DBC;

public class Convert {

	private Convert() {
	}

	public static Map<String, ConvertType> diff(Zdb source, Zdb target, boolean includeSame, Set<String> unused) {
		Map<String, ConvertType> res = new LinkedHashMap<>();
		for (Table st : source.getTables()) {
			String tableName = st.getName();
			Table tt = target.getTable(tableName);
			if (tt != null) {
				ConvertType ct = Schemas.of(st).diff(Schemas.of(tt), false);
				if (includeSame || ct != ConvertType.SAME)
					res.put(tableName, ct);
			} else if (unused != null) {
				unused.add(tableName);
			}
		}
		return res;
	}

	private static enum CType {
		SAME("copying..."), AUTO("auto converting..."), CONVERT("converting...");

		String status;

		CType(String s) {
			status = s;
		}
	}

	public static void convert(DBC sourceDBC, DBC targetDBC, Map<String, Converter> converterMap,
			Map<String, ConflictSolver> solverMap, boolean autoConvertWhenMaybeAuto, PrintStream trace) {

		if (trace == null)
			throw new NullPointerException();

		Map<String, CType> tables = new LinkedHashMap<>();
		diff(sourceDBC.meta(), targetDBC.meta(), true, null).forEach((tableName, convertType) -> {
			System.out.println(tableName + " " + convertType);
			Converter converter = (converterMap != null ? converterMap.get(tableName) : null);
			if (converter == null) {
				switch (convertType) {
				case SAME:
					tables.put(tableName, CType.SAME);
					break;
				case AUTO:
					tables.put(tableName, CType.AUTO);
					break;
				case MAYBE_AUTO:
					if (autoConvertWhenMaybeAuto) {
						tables.put(tableName, CType.AUTO);
					} else {
						throw new RuntimeException(tableName + " MAYBE_AUTO, need converter");
					}
					break;
				case MANUAL:
					throw new RuntimeException(tableName + " MANUAL, need converter");
				}
			} else {
				tables.put(tableName, CType.CONVERT);
			}
		});

		ExecutorService executor = Executors.newSingleThreadExecutor();

		tables.forEach((tableName, ct) -> {
			trace.println(ct.status + " " + tableName);

			DBC.Table sourceTable = sourceDBC.openTable(tableName);
			DBC.Table targetTable = targetDBC.openTable(tableName);
			SchemaKeyValue sourceSchema = Schemas.of(sourceTable.meta());
			SchemaKeyValue targetSchema = Schemas.of(targetTable.meta());

			ConflictSolver conflictSolver = (solverMap != null ? solverMap.get(tableName) : null);

			sourceTable.walk((key, data) -> {
				executor.execute(() -> {
					try {
						OctetsStream keyOs = OctetsStream.wrap(Octets.wrap(key));
						OctetsStream sourceValueOs = OctetsStream.wrap(Octets.wrap(data));
						Data keyData = null;
						if (ct == CType.AUTO) {
							DataKeyValue sourceData = sourceSchema.create();
							sourceData.unmarshal(OctetsStream.wrap(new Octets(key).append(data)));

							DataKeyValue convertedData = targetSchema.create();
							sourceData.convertTo(convertedData);

							keyOs = new OctetsStream().marshal(convertedData.getKey());
							sourceValueOs = new OctetsStream().marshal(convertedData.getValue());

							keyData = convertedData.getKey();
						} else if (ct == CType.CONVERT) {
							Converter converter = converterMap.get(tableName);

							keyOs = converter.convertKey(keyOs);
							sourceValueOs = converter.convertValue(sourceValueOs);
						}
						if (!targetTable.insert(keyOs, sourceValueOs)) {
							if (keyData == null) { // SAME, MANUAL
								keyData = targetSchema.key().create();
								keyData.unmarshal(keyOs);
								keyOs.position(0);
							}
							if (conflictSolver != null) {
								Octets targetValueOs = targetTable.find(keyOs);
								trace.println("    conflict key resolving... table=" + tableName + ", key=" + keyData);
								Octets solvedValueOs = conflictSolver.solve(sourceValueOs, targetValueOs, keyOs);
								targetTable.replace(keyOs, solvedValueOs);
							} else {
								throw new RuntimeException("insert failed! table=" + tableName + ", key=" + keyData);
							}
						}
					} catch (MarshalException e) {
						throw new RuntimeException(e);
					}
				}); // end executor
				return true;
			}); // end walk

			sourceTable.close();
			targetTable.close();
		}); // end tables

		for (executor.shutdown(); true;) {
			try {
				if (executor.awaitTermination(1, TimeUnit.SECONDS))
					break;
			} catch (InterruptedException ignore) {
			}
		}
	}

}
