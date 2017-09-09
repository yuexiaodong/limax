package limax.zdb.tool;

public interface Schema {
	Data create();

	ConvertType diff(Schema target, boolean asKey);
}
