package limax.provider;

import limax.codec.Octets;

public final class TunnelException extends Exception {
	public enum Type {
		MARSHAL, EXPIRE, SIGNATURE
	};

	private final Type type;
	private final Octets suspect;

	private static final long serialVersionUID = -976611466007860042L;

	TunnelException(Type type, Octets suspect) {
		this.type = type;
		this.suspect = suspect;
	}

	TunnelException(Type type) {
		this(type, null);
	}

	public Type getType() {
		return type;
	}

	public Octets getSuspect() {
		return suspect;
	}
}
