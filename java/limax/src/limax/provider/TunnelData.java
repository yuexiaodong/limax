package limax.provider;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import limax.codec.Marshal;
import limax.codec.MarshalException;
import limax.codec.Octets;
import limax.codec.OctetsStream;
import limax.provider.TunnelException.Type;

class TunnelData implements Marshal {
	private String algo;
	private long expire;
	private Octets data;
	private Octets hash;

	private Octets computeMac(byte[] key, int label) throws Exception {
		Mac mac = Mac.getInstance(algo);
		mac.init(new SecretKeySpec(key, algo));
		mac.update(new OctetsStream().marshal(expire).marshal(label).getBytes());
		mac.update(data.array(), 0, data.size());
		return Octets.wrap(mac.doFinal());
	}

	TunnelData(String algo, byte[] key, long expire, int label, Octets data) throws Exception {
		this.algo = algo;
		this.expire = expire;
		this.data = data;
		this.hash = computeMac(key, label);
	}

	TunnelData(byte[] key, int label, Octets data) throws TunnelException {
		try {
			unmarshal(OctetsStream.wrap(data));
		} catch (Exception e) {
			throw new TunnelException(Type.MARSHAL);
		}
		if (expire < System.currentTimeMillis())
			throw new TunnelException(Type.EXPIRE, this.data);
		try {
			if (!hash.equals(computeMac(key, label)))
				throw new TunnelException(Type.SIGNATURE, this.data);
		} catch (Exception e) {
			throw new TunnelException(Type.SIGNATURE, this.data);
		}
	}

	Octets get() {
		return data;
	}

	@Override
	public OctetsStream marshal(OctetsStream os) {
		return os.marshal(algo).marshal(expire).marshal(data).marshal(hash);
	}

	@Override
	public OctetsStream unmarshal(OctetsStream os) throws MarshalException {
		this.algo = os.unmarshal_String();
		this.expire = os.unmarshal_long();
		this.data = os.unmarshal_Octets();
		this.hash = os.unmarshal_Octets();
		return os;
	}
}
