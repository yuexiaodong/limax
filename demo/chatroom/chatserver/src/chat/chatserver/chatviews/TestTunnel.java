
package chat.chatserver.chatviews;

import limax.endpoint.providerendpoint.Tunnel;

// {{{ XMLGEN_IMPORT_BEGIN
// {{{ DO NOT EDIT THIS
/** 测试隧道协议 **/
// DO NOT EDIT THIS }}}
// XMLGEN_IMPORT_END }}}

public class TestTunnel extends limax.net.Protocol {
	@Override
	public void process() {
		// protocol handle
		System.out.println(toString());
	
		
	}

	// {{{ XMLGEN_DEFINE_BEGIN
	// {{{ DO NOT EDIT THIS
	public static int TYPE;

	public int getType() {
		return TYPE;
	}

    public long tid; 
    public String tname; 
    public limax.codec.Octets tdata; 

	public TestTunnel() {
		tname = "";
		tdata = new limax.codec.Octets();
	}

	public TestTunnel(long _tid_, String _tname_, limax.codec.Octets _tdata_) {
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
		this.tdata = _os_.unmarshal_Octets();
		return _os_;
	}

	@Override
	public String toString() {
		StringBuilder _sb_ = new StringBuilder(super.toString());
		_sb_.append("=(");
		_sb_.append(this.tid).append(",");
		_sb_.append("T").append(this.tname.length()).append(",");
		_sb_.append("B").append(this.tdata.size()).append(",");
		_sb_.append(")");
		return _sb_.toString();
	}

	// DO NOT EDIT THIS }}}
	// XMLGEN_DEFINE_END }}}

}

