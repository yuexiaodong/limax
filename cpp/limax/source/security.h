#pragma once

namespace limax {

	class MD5 : public Codec {
		std::shared_ptr<Codec> sink;
		struct CTX {
			uint32_t state[4];
			uint64_t count;
			uint32_t remain;
			int8_t buffer[64];
		} ctx;
		int8_t result[16];
		void transform(int8_t block[64]);
		void fill(int8_t *data, int32_t len);
		void reset();
		friend class HmacMD5;
	public:
		MD5(std::shared_ptr<Codec> _sink);
		MD5();
		void update(int8_t c);
		void update(int8_t data[], int32_t off, int32_t len);
		void flush();
		const int8_t* digest();
	};

	class SHA1 : public Codec {
		std::shared_ptr<Codec> sink;
		struct CTX {
			uint32_t state[5];
			uint64_t count;
			uint32_t remain;
			int8_t buffer[64];
		} ctx;
		int8_t result[20];
		void transform(int8_t block[64]);
		void fill(int8_t *data, int32_t len);
		void reset();
		friend class HmacSHA1;
	public:
		SHA1(std::shared_ptr<Codec> _sink);
		SHA1();
		void update(int8_t c);
		void update(int8_t data[], int32_t off, int32_t len);
		void flush();
		const int8_t* digest();
	};

	class HmacMD5 : public Codec {
		std::shared_ptr<Codec> sink;
		MD5 md, out;
		int8_t k_opad[64];
		void reset(int8_t key[], int32_t off, int32_t len);
	public:
		HmacMD5(std::shared_ptr<Codec> _sink, int8_t key[], int32_t off, int32_t len);
		HmacMD5(int8_t key[], int32_t off, int32_t len);
		void update(int8_t c);
		void update(int8_t data[], int32_t off, int32_t len);
		void flush();
		const int8_t* digest();
	};

	class HmacSHA1 : public Codec {
		std::shared_ptr<Codec> sink;
		SHA1 md, out;
		int8_t k_opad[64];
		void reset(int8_t key[], int32_t off, int32_t len);
	public:
		HmacSHA1(std::shared_ptr<Codec> _sink, int8_t key[], int32_t off, int32_t len);
		HmacSHA1(int8_t key[], int32_t off, int32_t len);
		void update(int8_t c);
		void update(int8_t data[], int32_t off, int32_t len);
		void flush();
		const int8_t* digest();
	};

	class CRC32 : public Codec {
		std::shared_ptr<Codec> sink;
		uint32_t crc;
	public:
		CRC32(std::shared_ptr<Codec> _sink);
		void update(int8_t c);
		void update(int8_t data[], int32_t off, int32_t len);
		void flush();
		int64_t getValue();
	};

	class Encrypt : public Codec {
		LIMAX_ALIGN(16) uint32_t	ctx[48];
		LIMAX_ALIGN(16) uint32_t iv[4];
		std::shared_ptr<Codec> sink;
		int8_t in[16];
		int32_t count;
	public:
		Encrypt(std::shared_ptr<Codec> _sink, int8_t key[], int32_t len);
		void update(int8_t c);
		void update(int8_t data[], int32_t off, int32_t len);
		void flush();
	};

	class Decrypt : public Codec {
		std::shared_ptr<Codec> sink;
		LIMAX_ALIGN(16) uint32_t ctx[44];
		LIMAX_ALIGN(16) uint32_t iv[4];
		int8_t in[16];
		int32_t count;
	public:
		Decrypt(std::shared_ptr<Codec> _sink, int8_t key[], int32_t len);
		void update(int8_t c);
		void update(int8_t data[], int32_t off, int32_t len);
		void flush();
	};

} // namespace limax {
