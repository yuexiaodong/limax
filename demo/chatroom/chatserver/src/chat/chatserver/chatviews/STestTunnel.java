
package chat.chatserver.chatviews;

// {{{ XMLGEN_IMPORT_BEGIN
// {{{ DO NOT EDIT THIS

// DO NOT EDIT THIS }}}
// XMLGEN_IMPORT_END }}}

public class STestTunnel extends limax.net.Protocol {
	@Override
	public void process() {
		// protocol handle
	}

	// {{{ XMLGEN_DEFINE_BEGIN
	// {{{ DO NOT EDIT THIS
	public static int TYPE;

	public int getType() {
		return TYPE;
	}

    public long tid; 
    public String tname; 
    public chat.chatviews.ChatMessage tdata; 

	public STestTunnel() {
		tname = "";
		tdata = new chat.chatviews.ChatMessage();
	}

	public STestTunnel(long _tid_, String _tname_, chat.chatviews.ChatMessage _tdata_) {
		this.tid = _tid_;
		this.tname = _tname_;
		this.tdata = _tdata_;
	}

	@Override
	public limax.codec.OctetsStream marshal(limax.codec.OctetsStream _os_) {
		_os_.marshal(this.tid);
		_os_.marshal(this.tname);
		_os_.marshal(this.tdata);
		return _os_;
	}

	@Override
	public limax.codec.OctetsStream unmarshal(limax.codec.OctetsStream _os_) throws limax.codec.MarshalException {
		this.tid = _os_.unmarshal_long();
		this.tname = _os_.unmarshal_String();
		this.tdata.unmarshal(_os_);
		return _os_;
	}

	@Override
	public String toString() {
		StringBuilder _sb_ = new StringBuilder(super.toString());
		_sb_.append("=(");
		_sb_.append(this.tid).append(",");
		_sb_.append("T").append(this.tname.length()).append(",");
		_sb_.append(this.tdata).append(",");
		_sb_.append(")");
		return _sb_.toString();
	}

	// DO NOT EDIT THIS }}}
	// XMLGEN_DEFINE_END }}}

}

