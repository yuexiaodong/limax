package limax.codec;

import java.io.Serializable;
import java.net.IDN;

public final class RFC2822Address implements Serializable {
	private static final long serialVersionUID = -2688321952627089441L;
	private String displayname;
	private String username;
	private String domain;

	private static String unquote(String s) {
		return s.startsWith("\"") && s.endsWith("\"") ? s.substring(1, s.length() - 1) : s;
	}

	private static String quote(String s) {
		return s.length() > 0 ? "\"" + s + "\"" : s;
	}

	public final class Exception extends java.lang.Exception {
		private static final long serialVersionUID = 6319258451955519665L;
	}

	public int hashCode() {
		return username.toLowerCase().hashCode() ^ domain.hashCode();
	}

	public boolean equals(Object o) {
		if (o instanceof RFC2822Address) {
			RFC2822Address r = (RFC2822Address) o;
			return r.username.equalsIgnoreCase(username) && r.domain.equals(domain);
		}
		return false;
	}

	public RFC2822Address(String addr) throws Exception {
		int i = addr.indexOf("<");
		if (i != -1) {
			displayname = unquote(RFC2047Word.decode(addr.substring(0, i)));
			addr = addr.substring(i + 1);
			i = addr.indexOf(">");
			if (i == -1)
				throw new Exception();
			addr = addr.substring(0, i);
		} else
			displayname = "";
		i = addr.indexOf(":"); // RFC2821 strip off source route
		if (i != -1)
			addr = addr.substring(i + 1);
		i = addr.indexOf("@");
		if (i == -1)
			throw new Exception();
		username = addr.substring(0, i).trim();
		domain = IDN.toASCII(addr.substring(i + 1).trim().toLowerCase());
	}

	public RFC2822Address(String displayname, String username, String domain) {
		this.displayname = unquote(displayname);
		this.username = username;
		this.domain = IDN.toASCII(domain);
	}

	public RFC2822Address(String username, String domain) {
		this("", username, domain);
	}

	public String toRFC2821Address() {
		return "<" + username + "@" + domain + ">";
	}

	public String toString() {
		return quote(RFC2047Word.encode(displayname, 76)) + "<" + username + "@" + domain + ">";
	}

	public String getDisplayname() {
		return displayname;
	}

	public void setDisplayname(String displayname) {
		this.displayname = displayname;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getDomain() {
		return IDN.toUnicode(domain);
	}

	public void setDomain(String domain) {
		this.domain = IDN.toASCII(domain);
	}
}
