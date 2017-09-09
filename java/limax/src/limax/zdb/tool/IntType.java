package limax.zdb.tool;

public enum IntType {
	BOOLEAN {
		public int size() {
			return 1;
		}
	},
	BYTE {
		public int size() {
			return 1;
		}
	},
	SHORT {
		public int size() {
			return 2;
		}
	},
	INT {
		public int size() {
			return 4;
		}
	},
	LONG {
		public int size() {
			return 8;
		}
	};

	public abstract int size();
}