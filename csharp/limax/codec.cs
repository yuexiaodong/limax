﻿using System;
using System.Collections;
using System.Collections.Generic;
using System.IO;
using System.Reflection;
using System.Security.Cryptography;
using System.Text;
using limax.util;

namespace limax.codec
{
    public interface Appendable
    {
        Appendable Append(char c);
        Appendable Append(string s);
    }
    public sealed class Base64Decode : Codec
    {
        private static readonly byte[] DECODE = { (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0x3e, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0x3f, (byte)0x34, (byte)0x35, (byte)0x36, (byte)0x37, (byte)0x38, (byte)0x39, (byte)0x3a, (byte)0x3b, (byte)0x3c, (byte)0x3d, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0x7f, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0x00, (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04, (byte)0x05, (byte)0x06, (byte)0x07, (byte)0x08, (byte)0x09, (byte)0x0a, (byte)0x0b, (byte)0x0c, (byte)0x0d, (byte)0x0e, (byte)0x0f, (byte)0x10, (byte)0x11, (byte)0x12, (byte)0x13, (byte)0x14, (byte)0x15, (byte)0x16, (byte)0x17, (byte)0x18, (byte)0x19, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0x1a, (byte)0x1b, (byte)0x1c, (byte)0x1d, (byte)0x1e, (byte)0x1f, (byte)0x20, (byte)0x21, (byte)0x22, (byte)0x23, (byte)0x24, (byte)0x25, (byte)0x26, (byte)0x27, (byte)0x28, (byte)0x29, (byte)0x2a, (byte)0x2b, (byte)0x2c, (byte)0x2d, (byte)0x2e, (byte)0x2f, (byte)0x30, (byte)0x31, (byte)0x32, (byte)0x33, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0x00 };
        private static readonly byte B64PAD = (byte)'=';
        private readonly Codec sink;
        private int b0;
        private int b1;
        private int b2;
        private int b3;
        private int n;
        public Base64Decode(Codec sink)
        {
            this.sink = sink;
        }
        private int update0(byte[] r, int j, byte[] data, int off, int len)
        {
            for (n = len; n > 7; n -= 4)
            {
                b0 = DECODE[data[off++] & 0xff];
                b1 = DECODE[data[off++] & 0xff];
                b2 = DECODE[data[off++] & 0xff];
                b3 = DECODE[data[off++] & 0xff];
                if (b0 == 0xff || b1 == 0xff || b2 == 0xff || b3 == 0xff)
                    throw new CodecException("bad base64 char");
                r[j++] = (byte)(b0 << 2 & 0xfc | b1 >> 4 & 0x3);
                r[j++] = (byte)(b1 << 4 & 0xf0 | b2 >> 2 & 0xf);
                r[j++] = (byte)(b2 << 6 & 0xc0 | b3 & 0x3f);
            }
            if (n > 3)
            {
                n -= 4;
                b0 = DECODE[data[off++] & 0xff];
                b1 = DECODE[data[off++] & 0xff];
                b2 = DECODE[data[off++] & 0xff];
                b3 = DECODE[data[off++] & 0xff];
                if (b0 == 0xff || b1 == 0xff || b2 == 0xff || b3 == 0xff)
                    throw new CodecException("bad base64 char");
                if (b2 == 0x7f)
                {
                    r[j++] = (byte)(b0 << 2 & 0xfc | b1 >> 4 & 0x3);
                    return j;
                }
                else if (b3 == 0x7f)
                {
                    r[j++] = (byte)(b0 << 2 & 0xfc | b1 >> 4 & 0x3);
                    r[j++] = (byte)(b1 << 4 & 0xf0 | b2 >> 2 & 0xf);
                    return j;
                }
                r[j++] = (byte)(b0 << 2 & 0xfc | b1 >> 4 & 0x3);
                r[j++] = (byte)(b1 << 4 & 0xf0 | b2 >> 2 & 0xf);
                r[j++] = (byte)(b2 << 6 & 0xc0 | b3 & 0x3f);
            }
            if (n == 1)
            {
                b0 = DECODE[data[off] & 0xff];
            }
            else if (n == 2)
            {
                b0 = DECODE[data[off] & 0xff];
                b1 = DECODE[data[off + 1] & 0xff];
            }
            else if (n == 3)
            {
                b0 = DECODE[data[off] & 0xff];
                b1 = DECODE[data[off + 1] & 0xff];
                b2 = DECODE[data[off + 2] & 0xff];
            }
            return j;
        }
        private int update1(byte[] r, byte[] data, int off, int len)
        {
            switch (len)
            {
                case 0:
                    return 0;
                case 1:
                    b1 = DECODE[data[off] & 0xff];
                    n = 2;
                    return 0;
                case 2:
                    b1 = DECODE[data[off] & 0xff];
                    b2 = DECODE[data[off + 1] & 0xff];
                    n = 3;
                    return 0;
            }
            b1 = DECODE[data[off] & 0xff];
            b2 = DECODE[data[off + 1] & 0xff];
            b3 = DECODE[data[off + 2] & 0xff];
            if (b0 == 0xff || b1 == 0xff || b2 == 0xff || b3 == 0xff)
                throw new CodecException("bad base64 char");
            if (b2 == 0x7f)
            {
                r[0] = (byte)(b0 << 2 & 0xfc | b1 >> 4 & 0x3);
                return 1;
            }
            else if (b3 == 0x7f)
            {
                r[0] = (byte)(b0 << 2 & 0xfc | b1 >> 4 & 0x3);
                r[1] = (byte)(b1 << 4 & 0xf0 | b2 >> 2 & 0xf);
                return 2;
            }
            r[0] = (byte)(b0 << 2 & 0xfc | b1 >> 4 & 0x3);
            r[1] = (byte)(b1 << 4 & 0xf0 | b2 >> 2 & 0xf);
            r[2] = (byte)(b2 << 6 & 0xc0 | b3 & 0x3f);
            return update0(r, 3, data, off + 3, len - 3);
        }
        private int update2(byte[] r, byte[] data, int off, int len)
        {
            switch (len)
            {
                case 0:
                    return 0;
                case 1:
                    b2 = DECODE[data[off] & 0xff];
                    n = 3;
                    return 0;
            }
            b2 = DECODE[data[off] & 0xff];
            b3 = DECODE[data[off + 1] & 0xff];
            if (b0 == 0xff || b1 == 0xff || b2 == 0xff || b3 == 0xff)
                throw new CodecException("bad base64 char");
            if (b2 == 0x7f)
            {
                r[0] = (byte)(b0 << 2 & 0xfc | b1 >> 4 & 0x3);
                return 1;
            }
            else if (b3 == 0x7f)
            {
                r[0] = (byte)(b0 << 2 & 0xfc | b1 >> 4 & 0x3);
                r[1] = (byte)(b1 << 4 & 0xf0 | b2 >> 2 & 0xf);
                return 2;
            }
            r[0] = (byte)(b0 << 2 & 0xfc | b1 >> 4 & 0x3);
            r[1] = (byte)(b1 << 4 & 0xf0 | b2 >> 2 & 0xf);
            r[2] = (byte)(b2 << 6 & 0xc0 | b3 & 0x3f);
            return update0(r, 3, data, off + 2, len - 2);
        }
        private int update3(byte[] r, byte[] data, int off, int len)
        {
            if (len == 0)
                return 0;
            b3 = DECODE[data[off] & 0xff];
            if (b0 == 0xff || b1 == 0xff || b2 == 0xff || b3 == 0xff)
                throw new CodecException("bad base64 char");
            if (b2 == 0x7f)
            {
                r[0] = (byte)(b0 << 2 & 0xfc | b1 >> 4 & 0x3);
                return 1;
            }
            else if (b3 == 0x7f)
            {
                r[0] = (byte)(b0 << 2 & 0xfc | b1 >> 4 & 0x3);
                r[1] = (byte)(b1 << 4 & 0xf0 | b2 >> 2 & 0xf);
                return 2;
            }
            r[0] = (byte)(b0 << 2 & 0xfc | b1 >> 4 & 0x3);
            r[1] = (byte)(b1 << 4 & 0xf0 | b2 >> 2 & 0xf);
            r[2] = (byte)(b2 << 6 & 0xc0 | b3 & 0x3f);
            return update0(r, 3, data, off + 1, len - 1);
        }
        private int update(byte[] r, byte[] data, int off, int len)
        {
            switch (n)
            {
                case 0:
                    return update0(r, 0, data, off, len);
                case 1:
                    return update1(r, data, off, len);
                case 2:
                    return update2(r, data, off, len);
            }
            return update3(r, data, off, len);
        }
        public void update(byte[] data, int off, int len)
        {
            int length = (n + len) / 4 * 3;
            byte[] r = new byte[length];
            sink.update(r, 0, Math.Min(update(r, data, off, len), length));
        }
        public void update(byte c)
        {
            update(new byte[] { c }, 0, 1);
        }
        public void flush()
        {
            sink.flush();
            n = 0;
        }
        public void Dispose() { }
        public static byte[] transform(byte[] data)
        {
            int len = data.Length / 4 * 3;
            if (data[data.Length - 1] == B64PAD)
                len--;
            if (data[data.Length - 2] == B64PAD)
                len--;
            byte[] r = new byte[len];
            new Base64Decode(null).update(r, data, 0, data.Length);
            return r;
        }
    }
    public sealed class Base64Encode : Codec
    {
        private static readonly byte[] ENCODE = { (byte)'A', (byte)'B', (byte)'C', (byte)'D', (byte)'E', (byte)'F', (byte)'G', (byte)'H', (byte)'I', (byte)'J', (byte)'K', (byte)'L', (byte)'M', (byte)'N', (byte)'O', (byte)'P', (byte)'Q', (byte)'R', (byte)'S', (byte)'T', (byte)'U', (byte)'V', (byte)'W', (byte)'X', (byte)'Y', (byte)'Z', (byte)'a', (byte)'b', (byte)'c', (byte)'d', (byte)'e', (byte)'f', (byte)'g', (byte)'h', (byte)'i', (byte)'j', (byte)'k', (byte)'l', (byte)'m', (byte)'n', (byte)'o', (byte)'p', (byte)'q', (byte)'r', (byte)'s', (byte)'t', (byte)'u', (byte)'v', (byte)'w', (byte)'x', (byte)'y', (byte)'z', (byte)'0', (byte)'1', (byte)'2', (byte)'3', (byte)'4', (byte)'5', (byte)'6', (byte)'7', (byte)'8', (byte)'9', (byte)'+', (byte)'/' };
        private static readonly byte B64PAD = (byte)'=';
        private readonly Codec sink;
        private byte b0;
        private byte b1;
        private byte b2;
        private int n;
        public Base64Encode(Codec sink)
        {
            this.sink = sink;
        }
        private void update0(byte[] r, int j, byte[] data, int off, int len)
        {
            for (n = len; n > 2; n -= 3)
            {
                b0 = data[off++];
                b1 = data[off++];
                b2 = data[off++];
                int c = ((b0 & 0xff) << 16) | ((b1 & 0xff) << 8) | (b2 & 0xff);
                r[j++] = ENCODE[c >> 18];
                r[j++] = ENCODE[(c >> 12) & 0x3f];
                r[j++] = ENCODE[(c >> 6) & 0x3f];
                r[j++] = ENCODE[c & 0x3f];
            }
            if (n == 1)
            {
                b0 = data[off];
            }
            else if (n == 2)
            {
                b0 = data[off];
                b1 = data[off + 1];
            }
        }
        private void update1(byte[] r, byte[] data, int off, int len)
        {
            switch (len)
            {
                case 0:
                    return;
                case 1:
                    b1 = data[off];
                    n = 2;
                    return;
            }
            b1 = data[off];
            b2 = data[off + 1];
            int c = ((b0 & 0xff) << 16) | ((b1 & 0xff) << 8) | (b2 & 0xff);
            r[0] = ENCODE[c >> 18];
            r[1] = ENCODE[(c >> 12) & 0x3f];
            r[2] = ENCODE[(c >> 6) & 0x3f];
            r[3] = ENCODE[c & 0x3f];
            update0(r, 4, data, off + 2, len - 2);
        }
        private void update2(byte[] r, byte[] data, int off, int len)
        {
            if (len == 0)
                return;
            b2 = data[off];
            int c = ((b0 & 0xff) << 16) | ((b1 & 0xff) << 8) | (b2 & 0xff);
            r[0] = ENCODE[c >> 18];
            r[1] = ENCODE[(c >> 12) & 0x3f];
            r[2] = ENCODE[(c >> 6) & 0x3f];
            r[3] = ENCODE[c & 0x3f];
            update0(r, 4, data, off + 1, len - 1);
        }
        private void update(byte[] r, byte[] data, int off, int len)
        {
            switch (n)
            {
                case 0:
                    update0(r, 0, data, off, len);
                    return;
                case 1:
                    update1(r, data, off, len);
                    return;
            }
            update2(r, data, off, len);
        }
        public void update(byte[] data, int off, int len)
        {
            byte[] r = new byte[(len + n) / 3 * 4];
            update(r, data, off, len);
            sink.update(r, 0, r.Length);
        }
        public void update(byte c)
        {
            update(new byte[] { c }, 0, 1);
        }
        public void flush()
        {
            int c;
            switch (n)
            {
                case 1:
                    c = b0 & 0xff;
                    byte[] r1 = { ENCODE[c >> 2], ENCODE[(c << 4) & 0x3f], B64PAD, B64PAD };
                    sink.update(r1, 0, r1.Length);
                    break;
                case 2:
                    c = ((b0 & 0xff) << 8) | (b1 & 0xff);
                    byte[] r2 = { ENCODE[c >> 10], ENCODE[(c >> 4) & 0x3f], ENCODE[(c << 2) & 0x3f], B64PAD };
                    sink.update(r2, 0, r2.Length);
                    break;
            }
            sink.flush();
            n = 0;
        }
        public void Dispose() { }
        public static byte[] transform(byte[] data)
        {
            byte[] r = new byte[data.Length / 3 * 4 + (data.Length % 3 == 0 ? 0 : 4)];
            Base64Encode e = new Base64Encode(null);
            e.update(r, data, 0, data.Length);
            if (e.n == 1)
            {
                int c = e.b0 & 0xff;
                r[r.Length - 4] = ENCODE[c >> 2];
                r[r.Length - 3] = ENCODE[(c << 4) & 0x3f];
                r[r.Length - 2] = B64PAD;
                r[r.Length - 1] = B64PAD;
            }
            else if (e.n == 2)
            {
                int c = ((e.b0 & 0xff) << 8) | (e.b1 & 0xff);
                r[r.Length - 4] = ENCODE[c >> 10];
                r[r.Length - 3] = ENCODE[(c >> 4) & 0x3f];
                r[r.Length - 2] = ENCODE[(c << 2) & 0x3f];
                r[r.Length - 1] = B64PAD;
            }
            return r;
        }
    }
    public sealed class BufferedSink : Codec
    {
        private readonly Codec sink;
        private readonly byte[] buffer = new byte[8192];
        private int pos = 0;
        private void flushInternal()
        {
            if (pos > 0)
            {
                sink.update(buffer, 0, pos);
                pos = 0;
            }
        }
        public BufferedSink(Codec sink)
        {
            this.sink = sink;
        }
        public void update(byte c)
        {
            if (buffer.Length == pos)
            {
                flushInternal();
            }
            buffer[pos++] = c;
        }
        public void update(byte[] data, int off, int len)
        {
            if (len >= buffer.Length)
            {
                flushInternal();
                sink.update(data, off, len);
                return;
            }
            if (len > buffer.Length - pos)
            {
                flushInternal();
            }
            Buffer.BlockCopy(data, off, buffer, pos, len);
            pos += len;
        }
        public void flush()
        {
            flushInternal();
            sink.flush();
        }
        public void Dispose()
        {
            sink.Dispose();
        }
    }
    public sealed class BoundCheck : Codec
    {
        private long bytes;
        private readonly long deadline;
        private readonly Codec sink;
        public BoundCheck(long bytes, long millisecond, Codec sink)
        {
            this.bytes = bytes;
            long s = millisecond * TimeSpan.TicksPerMillisecond + DateTime.Now.Ticks;
            this.deadline = s < 0 ? long.MaxValue : s;
            this.sink = sink;
        }
        private void check(int len)
        {
            if ((bytes -= len) < 0)
                throw new CodecException("BoundCheck overflow");
            if (DateTime.Now.Ticks > deadline)
                throw new CodecException("BoundCheck deadline");
        }
        public void update(byte c)
        {
            check(1);
            sink.update(c);
        }
        public void update(byte[] data, int off, int len)
        {
            check(len);
            sink.update(data, off, len);
        }
        public void flush()
        {
            check(0);
            sink.flush();
        }
        public void Dispose() { }
    }
    public interface CharConsumer
    {
        void accept(char c);
    }
    public sealed class CharSink : Codec
    {
        private Decoder cd;
        private readonly CharConsumer consumer;
        private readonly byte[] bb = new byte[16];
        private readonly char[] cb = new char[1024];
        private int bblen = 0;
        private int cblen = 0;
        public CharSink(Encoding charset, CharConsumer consumer)
        {
            this.cd = charset.GetDecoder();
            this.consumer = consumer;
        }
        public CharSink(CharConsumer consumer)
        {
            this.consumer = consumer;
        }
        public void setCharset(Encoding charset)
        {
            cd = charset.GetDecoder();
        }
        public void update(byte c)
        {
            update(new byte[] { c }, 0, 1);
        }
        public void update(byte[] data, int off, int len)
        {
            int bbused, cbused;
            bool completed;
            try
            {
                while (bblen > 0 && len > 0)
                {
                    bb[bblen++] = data[off++];
                    len--;
                    cd.Convert(bb, 0, bblen, cb, 0, cb.Length, false, out bbused, out cbused, out completed);
                    if (bblen == bbused)
                    {
                        bblen = 0;
                        cblen += cbused;
                    }
                }
                while (true)
                {
                    cd.Convert(data, off, len, cb, cblen, cb.Length - cblen, false, out bbused, out cbused, out completed);
                    cblen += cbused;
                    off += bbused;
                    len -= bbused;
                    if (cblen == 0)
                    {
                        if (len != 0)
                            Buffer.BlockCopy(data, off, bb, 0, bblen = len);
                        break;
                    }
                    update();
                }
            }
            catch (Exception e)
            {
                throw new CodecException(e);
            }
        }
        public void flush()
        {
            int bbused, cbused;
            bool completed;
            try
            {
                cd.Convert(bb, 0, bblen, cb, 0, cb.Length, true, out bbused, out cbused, out completed);
                cblen += cbused;
                if (!completed)
                    throw new Exception("insufficent data");
                update();
                if (consumer is JSONDecoder)
                    (consumer as JSONDecoder).flush();
            }
            catch (Exception e)
            {
                throw new CodecException(e);
            }
        }
        private void update()
        {
            for (int i = 0; i < cblen; i++)
                consumer.accept(cb[i]);
            cblen = 0;
        }

        public void Dispose() { }
    }
    public interface Codec : IDisposable
    {
        void update(byte c);
        void update(byte[] data, int off, int len);
        void flush();
    }
    public interface Source
    {
        void flush();
    }
    public sealed class CodecCollection : Codec
    {
        private readonly ICollection<Codec> sinks;
        public CodecCollection(params Codec[] sinks)
        {
            this.sinks = sinks;
        }
        public void update(byte c)
        {
            foreach (Codec sink in sinks)
                sink.update(c);
        }
        public void update(byte[] data, int off, int len)
        {
            foreach (Codec sink in sinks)
                sink.update(data, off, len);
        }
        public void flush()
        {
            foreach (Codec sink in sinks)
                sink.flush();
        }
        public void Dispose() { }
    }
    public sealed class ExceptionJail : Codec
    {
        private readonly Codec sink;
        private volatile Exception exception;
        public ExceptionJail(Codec sink)
        {
            this.sink = sink;
        }
        public void update(byte c)
        {
            try
            {
                if (exception == null)
                    sink.update(c);
            }
            catch (Exception e) { exception = e; }
        }
        public void update(byte[] data, int off, int len)
        {
            try
            {
                if (exception == null)
                    sink.update(data, off, len);
            }
            catch (Exception e) { exception = e; }
        }
        public void flush()
        {
            try
            {
                if (exception == null)
                    sink.flush();
            }
            catch (Exception e) { exception = e; }
        }
        public Exception get() { return exception; }
        public void Dispose() { }
    }
    public abstract class MD : Codec
    {
        private readonly Codec sink;
        private byte[] oneByte = new byte[1];
        private readonly HashAlgorithm md;
        public MD(Codec sink, HashAlgorithm md)
        {
            this.sink = sink;
            this.md = md;
            md.Initialize();
        }
        public void update(byte c)
        {
            oneByte[0] = c;
            md.TransformBlock(oneByte, 0, 1, oneByte, 0);
            sink.update(c);
        }
        public void update(byte[] data, int off, int len)
        {
            md.TransformBlock(data, off, len, data, off);
            sink.update(data, off, len);
        }
        public void flush()
        {
            sink.flush();
        }
        public byte[] digest()
        {
            md.TransformFinalBlock(new byte[0], 0, 0);
            return md.Hash;
        }
        public void Dispose()
        {
            md.Dispose();
            sink.Dispose();
        }
    }
    public sealed class MD5 : MD
    {
        public MD5(Codec sink) : base(sink, System.Security.Cryptography.MD5.Create()) { }
        public MD5() : base(NullCodec.getInstance(), System.Security.Cryptography.MD5.Create()) { }
        public static byte[] digest(byte[] message)
        {
            using (var md = System.Security.Cryptography.MD5.Create())
                return md.ComputeHash(message);
        }
    }
    public sealed class SHA1 : MD
    {
        public SHA1(Codec sink) : base(sink, System.Security.Cryptography.SHA1.Create()) { }
        public SHA1() : base(NullCodec.getInstance(), System.Security.Cryptography.SHA1.Create()) { }
        public static byte[] digest(byte[] message)
        {
            using (var md = System.Security.Cryptography.SHA1.Create())
                return md.ComputeHash(message);
        }
    }
    public sealed class HmacMD5 : MD
    {
        public HmacMD5(Codec sink, byte[] key, int off, int len) : base(sink, new HMACMD5(Helper.copyOfRange(key, off, off + len))) { }
        public HmacMD5(byte[] key, int off, int len) : base(NullCodec.getInstance(), new HMACMD5(Helper.copyOfRange(key, off, off + len))) { }
    }
    public sealed class HmacSHA1 : MD
    {
        public HmacSHA1(Codec sink, byte[] key, int off, int len) : base(sink, new HMACSHA1(Helper.copyOfRange(key, off, off + len))) { }
        public HmacSHA1(byte[] key, int off, int len) : base(NullCodec.getInstance(), new HMACSHA1(Helper.copyOfRange(key, off, off + len))) { }
    }
    public sealed class NullCodec : Codec
    {
        private readonly static Codec codec = new NullCodec();
        private NullCodec() { }
        public void update(byte c) { }
        public void update(byte[] data, int off, int len) { }
        public void flush() { }
        public void Dispose() { }
        public static Codec getInstance() { return codec; }
    }
    public sealed class Encrypt : Codec
    {
        private readonly Codec sink;
        private readonly ICryptoTransform cipher;
        private readonly byte[] _iv;
        private readonly byte[] _in = new byte[16];
        private readonly byte[] _out = new byte[16];
        private int count = 0;
        public Encrypt(Codec sink, byte[] key)
        {
            this.sink = sink;
            _iv = MD5.digest(key);
            AesManaged aes = new AesManaged();
            aes.Mode = CipherMode.ECB;
            cipher = aes.CreateEncryptor(_iv, _iv);
        }
        private void succeed()
        {
            cipher.TransformBlock(_iv, 0, 16, _iv, 0);
        }
        public void update(byte c)
        {
            if (count < 0)
            {
                sink.update(_iv[count++ + 16] ^= c);
                return;
            }
            _in[count++] = c;
            if (count < 16)
                return;
            succeed();
            for (int i = 0; i < 16; i++)
                _iv[i] ^= _in[i];
            sink.update(_iv, 0, 16);
            count = 0;
        }
        public void update(byte[] data, int off, int len)
        {
            int i = off;
            len += off;
            if (count < 0)
            {
                for (; i < len && count < 0; i++, count++)
                    sink.update(_iv[count + 16] ^= data[i]);
            }
            else if (count > 0)
            {
                for (; i < len && count < 16; i++, count++)
                    _in[count] = data[i];
                if (count < 16)
                    return;
                succeed();
                for (int j = 0; j < 16; j++)
                    _iv[j] ^= _in[j];
                sink.update(_iv, 0, 16);
                count = 0;
            }
            int nblocks = (len - i) >> 4;
            for (int j = 0; j < nblocks; j++)
            {
                succeed();
                for (int k = 0; k < 16; k++)
                    _iv[k] ^= data[i + j * 16 + k];
                sink.update(_iv, 0, 16);
            }
            for (i += nblocks << 4; i < len; i++)
                _in[count++] = data[i];
        }
        public void flush()
        {
            if (count > 0)
            {
                succeed();
                for (int i = 0; i < count; i++)
                    sink.update(_iv[i] ^= _in[i]);
                count -= 16;
            }
            sink.flush();
        }
        public void Dispose()
        {
            cipher.Dispose();
            sink.Dispose();
        }
    }
    public sealed class Decrypt : Codec
    {
        private readonly Codec sink;
        private readonly ICryptoTransform cipher;
        private readonly byte[] _iv;
        private readonly byte[] _in = new byte[16];
        private readonly byte[] _out = new byte[16];
        private int count = 0;
        public Decrypt(Codec sink, byte[] key)
        {
            this.sink = sink;
            _iv = MD5.digest(key);
            AesManaged aes = new AesManaged();
            aes.Mode = CipherMode.ECB;
            cipher = aes.CreateEncryptor(_iv, _iv);
        }
        private void succeed()
        {
            cipher.TransformBlock(_iv, 0, 16, _iv, 0);
        }
        public void update(byte c)
        {
            if (count < 0)
            {
                sink.update((byte)(_iv[count + 16] ^ c));
                _iv[count++ + 16] = c;
                return;
            }
            _in[count++] = c;
            if (count < 16)
                return;
            succeed();
            for (int i = 0; i < 16; i++)
            {
                _out[i] = (byte)(_iv[i] ^ _in[i]);
                _iv[i] = _in[i];
            }
            sink.update(_out, 0, 16);
            count = 0;
        }
        public void update(byte[] data, int off, int len)
        {
            int i = off;
            len += off;
            if (count < 0)
            {
                for (; i < len && count < 0; i++, count++)
                {
                    sink.update((byte)(_iv[count + 16] ^ data[i]));
                    _iv[count + 16] = data[i];
                }
            }
            else if (count > 0)
            {
                for (; i < len && count < 16; i++, count++)
                    _in[count] = data[i];
                if (count < 16)
                    return;
                succeed();
                for (int j = 0; j < 16; j++)
                {
                    _out[j] = (byte)(_iv[j] ^ _in[j]);
                    _iv[j] = _in[j];
                }
                sink.update(_out, 0, 16);
                count = 0;
            }
            int nblocks = (len - i) >> 4;
            for (int j = 0; j < nblocks; j++)
            {
                succeed();
                for (int k = 0; k < 16; k++)
                {
                    byte c = data[i + j * 16 + k];
                    _out[k] = (byte)(_iv[k] ^ c);
                    _iv[k] = c;
                }
                sink.update(_out, 0, 16);
            }
            for (i += nblocks << 4; i < len; i++)
                _in[count++] = data[i];
        }
        public void flush()
        {
            if (count > 0)
            {
                succeed();
                for (int i = 0; i < count; i++)
                {
                    sink.update((byte)(_iv[i] ^ _in[i]));
                    _iv[i] = _in[i];
                }
                count -= 16;
            }
            sink.flush();
        }
        public void Dispose()
        {
            cipher.Dispose();
            sink.Dispose();
        }
    }
    public sealed class RFC2118Encode : Codec
    {
        private readonly Codec sink;
        private int pos = 0;
        private int rem = 0;
        private byte[] dict = new byte[8192];
        private short[] hash = new short[65536];
        private int idx = 0;
        private int match_idx;
        private int match_off = -1;
        private int match_len;
        private bool flushed = true;
        public RFC2118Encode(Codec sink)
        {
            this.sink = sink;
            for (int i = 0; i < hash.Length; i++)
                hash[i] = -1;
        }
        private void putBits(int val, int nbits)
        {
            pos += nbits;
            rem |= val << (32 - pos);
            while (pos > 7)
            {
                sink.update((byte)(rem >> 24));
                pos -= 8;
                rem <<= 8;
            }
        }
        private void putLiteral(byte c)
        {
            if ((c & 0x80) == 0)
                putBits(c, 8);
            else
                putBits(c & 0x7f | 0x100, 9);
        }
        private void putTuple(int off, int len)
        {
            if (off < 64)
                putBits(0x3c0 | off, 10);
            else if (off < 320)
                putBits(0xe00 | (off - 64), 12);
            else
                putBits(0xc000 | (off - 320), 16);
            if (len < 4)
                putBits(0, 1);
            else if (len < 8)
                putBits(0x08 | (len & 0x03), 4);
            else if (len < 16)
                putBits(0x30 | (len & 0x07), 6);
            else if (len < 32)
                putBits(0xe0 | (len & 0x0f), 8);
            else if (len < 64)
                putBits(0x3c0 | (len & 0x1f), 10);
            else if (len < 128)
                putBits(0xf80 | (len & 0x3f), 12);
            else if (len < 256)
                putBits(0x3f00 | (len & 0x7f), 14);
            else if (len < 512)
                putBits(0xfe00 | (len & 0xff), 16);
            else if (len < 1024)
                putBits(0x3fc00 | (len & 0x1ff), 18);
            else if (len < 2048)
                putBits(0xff800 | (len & 0x3ff), 20);
            else if (len < 4096)
                putBits(0x3ff000 | (len & 0x7ff), 22);
            else if (len < 8192)
                putBits(0xffe000 | (len & 0xfff), 24);
        }
        private void _flush()
        {
            if (match_off > 0)
            {
                if (match_len == 2)
                {
                    putLiteral(dict[match_idx - 2]);
                    putLiteral(dict[match_idx - 1]);
                }
                else
                    putTuple(match_off, match_len);
                match_off = -1;
            }
            else
                putLiteral(dict[idx - 1]);
            flushed = true;
        }
        public void update(byte c)
        {
            if (idx == dict.Length)
            {
                if (!flushed)
                    _flush();
                for (int i = 0; i < hash.Length; i++)
                    hash[i] = -1;
                idx = 0;
            }
            dict[idx++] = c;
            if (flushed)
            {
                flushed = false;
                return;
            }
            int key = ((c & 0xff) << 8) | dict[idx - 2] & 0xff;
            int tmp = hash[key];
            hash[key] = (short)idx;
            if (match_off > 0)
            {
                if (dict[match_idx] == c)
                {
                    match_idx++;
                    match_len++;
                }
                else
                {
                    if (match_len == 2)
                    {
                        putLiteral(dict[match_idx - 2]);
                        putLiteral(dict[match_idx - 1]);
                    }
                    else
                        putTuple(match_off, match_len);
                    match_off = -1;
                }
            }
            else
            {
                if (tmp != -1)
                {
                    match_idx = tmp;
                    match_off = idx - tmp;
                    match_len = 2;
                }
                else
                    putLiteral(dict[idx - 2]);
            }
        }
        public void update(byte[] data, int off, int len)
        {
            for (int i = off; i < len; i++)
                update(data[i]);
        }
        public void flush()
        {
            if (!flushed)
            {
                _flush();
                if (pos > 0)
                    putBits(0x3c0, 10);
            }
            sink.flush();
        }
        public void Dispose()
        {
            sink.Dispose();
        }
    }
    public sealed class RFC2118Decode : Codec
    {
        private readonly Codec sink;
        private int rem = 0;
        private int pos = 0;
        private int off = -1;
        private int len;
        private byte[] hist = new byte[8192 * 3];
        private int hpos = 0;
        public sealed class UncompressException : Exception { }
        public RFC2118Decode(Codec sink)
        {
            this.sink = sink;
        }
        private void drain()
        {
            if (hpos >= 8192 * 2)
            {
                Buffer.BlockCopy(hist, hpos - 8192, hist, 0, 8192);
                hpos = 8192;
            }
        }
        private void copy(int dstPos, int srcPos, int length)
        {
            for (int i = 0; i < length; i++)
                hist[dstPos++] = hist[srcPos++];
        }
        private void output(byte c)
        {
            sink.update(hist[hpos++] = c);
            drain();
        }
        private void output(int off, int len)
        {
            if (hpos < off)
                throw new UncompressException();
            copy(hpos, hpos - off, len);
            sink.update(hist, hpos, len);
            hpos += len;
            drain();
        }
        private int bitCompute()
        {
            long val = (rem << (32 - pos)) & 0xffffffffL;
            if (off < 0)
            {
                if (val < 0x80000000L)
                    return 8;
                else if (val < 0xc0000000L)
                    return 9;
                else if (val < 0xe0000000L)
                    return 16;
                else if (val < 0xf0000000L)
                    return 12;
                else
                    return 10;
            }
            else
            {
                if (val < 0x80000000L)
                    return 1;
                else if (val < 0xc0000000L)
                    return 4;
                else if (val < 0xe0000000L)
                    return 6;
                else if (val < 0xf0000000L)
                    return 8;
                else if (val < 0xf8000000L)
                    return 10;
                else if (val < 0xfc000000L)
                    return 12;
                else if (val < 0xfe000000L)
                    return 14;
                else if (val < 0xff000000L)
                    return 16;
                else if (val < 0xff800000L)
                    return 18;
                else if (val < 0xffc00000L)
                    return 20;
                else if (val < 0xffe00000L)
                    return 22;
                else if (val < 0xfff00000L)
                    return 24;
                else
                    return 32;
            }
        }
        private void process()
        {
            long val = (rem << (32 - pos)) & 0xffffffffL;
            if (off < 0)
            {
                if (val < 0x80000000L)
                {
                    output((byte)(val >> 24));
                    pos -= 8;
                }
                else if (val < 0xc0000000L)
                {
                    output((byte)((val >> 23) | 0x80));
                    pos -= 9;
                }
                else if (val < 0xe0000000L)
                {
                    off = (int)(((val >> 16) & 0x1fff) + 320);
                    pos -= 16;
                }
                else if (val < 0xf0000000L)
                {
                    off = (int)(((val >> 20) & 0xff) + 64);
                    pos -= 12;
                }
                else
                {
                    off = (int)((val >> 22) & 0x3f);
                    pos -= 10;
                    if (off == 0)
                        off = -1;
                }
            }
            else
            {
                if (val < 0x80000000L)
                {
                    len = 3;
                    pos -= 1;
                }
                else if (val < 0xc0000000L)
                {
                    len = (int)(4 | ((val >> 28) & 3));
                    pos -= 4;
                }
                else if (val < 0xe0000000L)
                {
                    len = (int)(8 | ((val >> 26) & 7));
                    pos -= 6;
                }
                else if (val < 0xf0000000L)
                {
                    len = (int)(16 | ((val >> 24) & 15));
                    pos -= 8;
                }
                else if (val < 0xf8000000L)
                {
                    len = (int)(32 | ((val >> 22) & 31));
                    pos -= 10;
                }
                else if (val < 0xfc000000L)
                {
                    len = (int)(64 | ((val >> 20) & 63));
                    pos -= 12;
                }
                else if (val < 0xfe000000L)
                {
                    len = (int)(128 | ((val >> 18) & 127));
                    pos -= 14;
                }
                else if (val < 0xff000000L)
                {
                    len = (int)(256 | ((val >> 16) & 255));
                    pos -= 16;
                }
                else if (val < 0xff800000L)
                {
                    len = (int)(512 | ((val >> 14) & 511));
                    pos -= 18;
                }
                else if (val < 0xffc00000L)
                {
                    len = (int)(1024 | ((val >> 12) & 1023));
                    pos -= 20;
                }
                else if (val < 0xffe00000L)
                {
                    len = (int)(2048 | ((val >> 10) & 2047));
                    pos -= 22;
                }
                else if (val < 0xfff00000L)
                {
                    len = (int)(4096 | ((val >> 8) & 4095));
                    pos -= 24;
                }
                else
                    throw new UncompressException();
                output(off, len);
                off = -1;
            }
        }
        public void update(byte c)
        {
            pos += 8;
            rem = (rem << 8) | (c & 0xff);
            while (pos > 24)
                process();
        }
        public void update(byte[] data, int off, int len)
        {
            for (int i = off; i < len; i++)
                update(data[i]);
        }
        public void flush()
        {
            while (pos >= bitCompute())
                process();
            sink.flush();
        }
        public void Dispose()
        {
            sink.Dispose();
        }
    }
    public sealed class SinkStream : Codec
    {
        private readonly Stream stream;
        public SinkStream(Stream stream)
        {
            this.stream = stream;
        }
        public void update(byte c)
        {
            stream.WriteByte(c);
        }
        public void update(byte[] data, int off, int len)
        {
            stream.Write(data, off, len);
        }
        public void flush()
        {
            stream.Flush();
        }
        public void Dispose()
        {
            stream.Dispose();
        }
    }
    public sealed class SinkOctets : Codec
    {
        private readonly Octets o;
        public SinkOctets(Octets o)
        {
            this.o = o;
        }
        public void update(byte c)
        {
            o.push_byte(c);
        }
        public void update(byte[] data, int off, int len)
        {
            o.insert(o.size(), data, off, len);
        }
        public void flush()
        {
        }
        public void Dispose()
        {
        }
    }
    public sealed class StreamSource : Source
    {
        private readonly Stream stream;
        private readonly Codec sink;
        public StreamSource(Stream stream, Codec sink)
        {
            this.stream = stream;
            this.sink = sink;
        }
        public void flush()
        {
            byte[] buffer = new byte[4096];
            try
            {
                for (int nread; (nread = stream.Read(buffer, 0, buffer.Length)) > 0; )
                    sink.update(buffer, 0, nread);
            }
            catch (Exception e)
            {
                throw new CodecException(e);
            }
            sink.flush();
        }
    }
    public sealed class MarshalException : Exception { }

    public class CodecException : Exception
    {
        public CodecException(Exception e) : base("", e) { }
        public CodecException(string message) : base(message) { }
        public CodecException(string message, Exception e) : base(message, e) { }
    }
    public interface Marshal
    {
        OctetsStream marshal(OctetsStream os);
        OctetsStream unmarshal(OctetsStream os);
    }
    public class Octets : IComparable<Octets>
    {
        private const int DEFAULT_SIZE = 128;
        internal protected byte[] buffer = null;
        private int count = 0;
        private byte[] roundup(int size)
        {
            int capacity = DEFAULT_SIZE;
            while (size > capacity)
                capacity <<= 1;
            return new byte[capacity];
        }
        public void reserve(int size)
        {
            if (buffer == null)
            {
                buffer = roundup(size);
            }
            else if (size > buffer.Length)
            {
                byte[] tmp = roundup(size);
                Buffer.BlockCopy(buffer, 0, tmp, 0, count);
                buffer = tmp;
            }
        }
        public Octets replace(byte[] data, int pos, int size)
        {
            reserve(size);
            Buffer.BlockCopy(data, pos, buffer, 0, size);
            count = size;
            return this;
        }
        public Octets replace(Octets data, int pos, int size)
        {
            return replace(data.buffer, pos, size);
        }
        public Octets replace(byte[] data)
        {
            return replace(data, 0, data.Length);
        }
        public Octets replace(Octets data)
        {
            return replace(data.buffer, 0, data.count);
        }
        public Octets()
        {
            reserve(DEFAULT_SIZE);
        }
        public Octets(int size)
        {
            reserve(size);
        }
        public Octets(Octets rhs)
        {
            replace(rhs);
        }
        public Octets(byte[] rhs)
        {
            replace(rhs);
        }
        internal Octets(byte[] bytes, int length)
        {
            this.buffer = bytes;
            this.count = length;
        }
        public static Octets wrap(byte[] bytes, int length)
        {
            return new Octets(bytes, length);
        }
        public static Octets wrap(byte[] bytes)
        {
            return wrap(bytes, bytes.Length);
        }
        public Octets(byte[] rhs, int pos, int size)
        {
            replace(rhs, pos, size);
        }
        public Octets(Octets rhs, int pos, int size)
        {
            replace(rhs, pos, size);
        }
        public Octets resize(int size)
        {
            reserve(size);
            count = size;
            return this;
        }
        public int size()
        {
            return count;
        }
        public int capacity()
        {
            return buffer.Length;
        }
        public Octets clear()
        {
            count = 0;
            return this;
        }
        public Octets swap(Octets rhs)
        {
            int size = count;
            count = rhs.count;
            rhs.count = size;
            byte[] tmp = rhs.buffer;
            rhs.buffer = buffer;
            buffer = tmp;
            return this;
        }
        public Octets push_byte(byte data)
        {
            reserve(count + 1);
            buffer[count++] = data;
            return this;
        }
        public Octets erase(int from, int to)
        {
            Buffer.BlockCopy(buffer, to, buffer, from, count - to);
            count -= to - from;
            return this;
        }
        public Octets insert(int from, byte[] data, int pos, int size)
        {
            reserve(count + size);
            Buffer.BlockCopy(buffer, from, buffer, from + size, count - from);
            Buffer.BlockCopy(data, pos, buffer, from, size);
            count += size;
            return this;
        }
        public Octets insert(int from, Octets data, int pos, int size)
        {
            return insert(from, data.buffer, pos, size);
        }
        public Octets insert(int from, byte[] data)
        {
            return insert(from, data, 0, data.Length);
        }
        public Octets insert(int from, Octets data)
        {
            return insert(from, data.buffer, 0, data.size());
        }
        public byte[] getBytes()
        {
            byte[] tmp = new byte[count];
            Buffer.BlockCopy(buffer, 0, tmp, 0, count);
            return tmp;
        }
        public byte[] array()
        {
            return buffer;
        }
        public byte getByte(int pos)
        {
            return buffer[pos];
        }
        public void setByte(int pos, byte b)
        {
            buffer[pos] = b;
        }
        public int CompareTo(Octets rhs)
        {
            int c = count - rhs.count;
            if (c != 0)
                return c;
            byte[] v1 = buffer;
            byte[] v2 = rhs.buffer;
            for (int i = 0; i < count; i++)
            {
                if (v1[i] > v2[i])
                    return 1;
                else if (v1[i] < v2[i])
                    return -1;
            }
            return 0;
        }
        public override bool Equals(object obj)
        {
            if (object.ReferenceEquals(this, obj))
                return true;
            Octets o = obj as Octets;
            return o != null ? 0 == CompareTo(o) : false;
        }
        public override int GetHashCode()
        {
            int result = 1;
            for (int i = 0; i < count; i++)
                result = 31 * result + buffer[i];
            return result;
        }
    }
    public sealed class OctetsStream : Octets
    {
        private const int MAXSPARE = 8192;
        private int pos = 0;
        private int tranpos = 0;
        public OctetsStream()
        {
        }
        public OctetsStream(int size)
            : base(size)
        {
        }
        public OctetsStream(Octets o)
            : base(o)
        {
        }
        private OctetsStream(byte[] bytes, int length)
            : base(bytes, length)
        {
        }
        public static OctetsStream wrap(Octets o)
        {
            return new OctetsStream(o.array(), o.size());
        }
        public bool eos()
        {
            return pos == size();
        }
        public OctetsStream position(int pos)
        {
            this.pos = pos;
            return this;
        }
        public int position()
        {
            return pos;
        }
        public int remain()
        {
            return size() - pos;
        }
        public OctetsStream begin()
        {
            tranpos = pos;
            return this;
        }
        public OctetsStream rollback()
        {
            pos = tranpos;
            return this;
        }
        public OctetsStream commit()
        {
            if (pos >= MAXSPARE)
            {
                erase(0, pos);
                pos = 0;
            }
            return this;
        }
        private OctetsStream push_bytes(byte[] data)
        {
            insert(size(), data);
            return this;
        }
        public OctetsStream marshal(byte x)
        {
            push_byte(x);
            return this;
        }
        public OctetsStream marshal(bool b)
        {
            push_byte((byte)(b ? 1 : 0));
            return this;
        }
        public OctetsStream marshal(short x)
        {
            return marshal((byte)(x >> 8)).marshal((byte)(x));
        }
        public OctetsStream marshal(char x)
        {
            return marshal((byte)(x >> 8)).marshal((byte)(x));
        }
        public OctetsStream marshal(int x)
        {
            return marshal((byte)(x >> 24)).marshal((byte)(x >> 16)).marshal((byte)(x >> 8)).marshal((byte)(x));
        }
        public OctetsStream marshal(long x)
        {
            return marshal((byte)(x >> 56)).marshal((byte)(x >> 48)).marshal((byte)(x >> 40)).marshal((byte)(x >> 32)).marshal((byte)(x >> 24)).marshal((byte)(x >> 16)).marshal((byte)(x >> 8)).marshal((byte)(x));
        }
        public OctetsStream marshal(float x)
        {
            byte[] bytes = BitConverter.GetBytes(x);
            if (BitConverter.IsLittleEndian)
                Array.Reverse(bytes);
            return push_bytes(bytes);
        }
        public OctetsStream marshal(double x)
        {
            byte[] bytes = BitConverter.GetBytes(x);
            if (BitConverter.IsLittleEndian)
                Array.Reverse(bytes);
            return push_bytes(bytes);
        }
        public OctetsStream marshal(Marshal m)
        {
            return m.marshal(this);
        }
        public OctetsStream marshal_size(int x)
        {
            if (x >= 0)
            {
                if (x < 0x80) // 0xxxxxxx
                    return marshal((byte)x);
                if (x < 0x4000) // 10xxxxxx xxxxxxxx
                    return marshal((byte)((x >> 8) | 0x80)).marshal((byte)x);
                if (x < 0x200000) // 110xxxxx xxxxxxxx xxxxxxxx
                    return marshal((byte)((x >> 16) | 0xc0)).marshal(
                            (byte)(x >> 8)).marshal((byte)x);
                if (x < 0x10000000) // 1110xxxx xxxxxxxx xxxxxxxx xxxxxxxx
                    return marshal((byte)((x >> 24) | 0xe0))
                            .marshal((byte)(x >> 16)).marshal((byte)(x >> 8))
                            .marshal((byte)x);
            }
            return marshal((byte)0xf0).marshal(x);
        }
        public OctetsStream marshal(Octets o)
        {
            this.marshal_size(o.size());
            insert(size(), o);
            return this;
        }
        public OctetsStream marshal(byte[] bytes)
        {
            this.marshal_size(bytes.Length);
            insert(size(), bytes);
            return this;
        }
        public OctetsStream marshal(string str)
        {
            return marshal(Encoding.UTF8.GetBytes(str));
        }
        public byte unmarshal_byte()
        {
            if (pos + 1 > size())
                throw new MarshalException();
            return getByte(pos++);
        }
        public bool unmarshal_bool()
        {
            return unmarshal_byte() == 1;
        }
        public short unmarshal_short()
        {
            if (pos + 2 > size())
                throw new MarshalException();
            byte b0 = getByte(pos++);
            byte b1 = getByte(pos++);
            return (short)((b0 << 8) | (b1 & 0xff));
        }
        public char unmarshal_char()
        {
            if (pos + 2 > size())
                throw new MarshalException();
            byte b0 = getByte(pos++);
            byte b1 = getByte(pos++);
            return (char)((b0 << 8) | (b1 & 0xff));
        }
        public int unmarshal_int()
        {
            if (pos + 4 > size())
                throw new MarshalException();
            byte b0 = getByte(pos++);
            byte b1 = getByte(pos++);
            byte b2 = getByte(pos++);
            byte b3 = getByte(pos++);
            return ((b0 & 0xff) << 24) | ((b1 & 0xff) << 16) | ((b2 & 0xff) << 8) | (b3 & 0xff);
        }
        public long unmarshal_long()
        {
            if (pos + 8 > size())
                throw new MarshalException();
            byte b0 = getByte(pos++);
            byte b1 = getByte(pos++);
            byte b2 = getByte(pos++);
            byte b3 = getByte(pos++);
            byte b4 = getByte(pos++);
            byte b5 = getByte(pos++);
            byte b6 = getByte(pos++);
            byte b7 = getByte(pos++);
            return (((long)b0 & 0xff) << 56) | (((long)b1 & 0xff) << 48) | (((long)b2 & 0xff) << 40) | (((long)b3 & 0xff) << 32) | (((long)b4 & 0xff) << 24) | (((long)b5 & 0xff) << 16) | (((long)b6 & 0xff) << 8) | ((long)b7 & 0xff);
        }
        public float unmarshal_float()
        {
            if (pos + 4 > size())
                throw new MarshalException();
            float v;
            if (BitConverter.IsLittleEndian)
            {
                byte[] data = new byte[] { buffer[pos + 3], buffer[pos + 2], buffer[pos + 1], buffer[pos] };
                v = BitConverter.ToSingle(data, 0);
            }
            else
            {
                v = BitConverter.ToSingle(buffer, pos);
            }
            pos += 4;
            return v;
        }
        public double unmarshal_double()
        {
            if (pos + 8 > size())
                throw new MarshalException();
            double v;
            if (BitConverter.IsLittleEndian)
            {
                byte[] data = new byte[] { buffer[pos + 7], buffer[pos + 6], buffer[pos + 5], buffer[pos + 4], buffer[pos + 3], buffer[pos + 2], buffer[pos + 1], buffer[pos] };
                v = BitConverter.ToDouble(data, 0);
            }
            else
            {
                v = BitConverter.ToDouble(buffer, pos);
            }
            pos += 8;
            return v;
        }
        public int unmarshal_size()
        {
            byte b0 = unmarshal_byte();
            if ((b0 & 0x80) == 0)
                return b0;
            if ((b0 & 0x40) == 0)
            {
                byte b1 = unmarshal_byte();
                return ((b0 & 0x3f) << 8) | (b1 & 0xff);
            }
            if ((b0 & 0x20) == 0)
            {
                byte b1 = unmarshal_byte();
                byte b2 = unmarshal_byte();
                return ((b0 & 0x1f) << 16) | ((b1 & 0xff) << 8) | (b2 & 0xff);
            }
            if ((b0 & 0x10) == 0)
            {
                byte b1 = unmarshal_byte();
                byte b2 = unmarshal_byte();
                byte b3 = unmarshal_byte();
                return ((b0 & 0x0f) << 24) | ((b1 & 0xff) << 16)
                        | ((b2 & 0xff) << 8) | (b3 & 0xff);
            }
            return unmarshal_int();
        }
        public Octets unmarshal_Octets()
        {
            int _size = this.unmarshal_size();
            if (pos + _size > size())
                throw new MarshalException();
            Octets o = new Octets(this, pos, _size);
            pos += _size;
            return o;
        }
        public byte[] unmarshal_bytes()
        {
            int _size = this.unmarshal_size();
            if (pos + _size > size())
                throw new MarshalException();
            byte[] copy = new byte[_size];
            Buffer.BlockCopy(buffer, pos, copy, 0, _size);
            pos += _size;
            return copy;
        }
        public string unmarshal_string()
        {
            int _size = this.unmarshal_size();
            if (pos + _size > size())
                throw new MarshalException();
            string v = Encoding.UTF8.GetString(array(), pos, _size);
            pos += _size;
            return v;
        }
        public OctetsStream unmarshal(Marshal m)
        {
            return m.unmarshal(this);
        }
    }
    #region JSON
    public class JSON
    {
        internal readonly object data;
        internal class NullConst
        {
            override public string ToString()
            {
                return "null";
            }
        }
        internal static readonly object Null = new NullConst();
        internal class TrueConst
        {
            override public string ToString()
            {
                return "true";
            }
        }
        internal static readonly object True = new TrueConst();
        internal class FalseConst
        {
            override public string ToString()
            {
                return "false";
            }
        }
        internal static readonly object False = new FalseConst();
        internal JSON(object data)
        {
            this.data = data;
        }
        public JSON get(string key)
        {
            return new JSON(((IDictionary<string, object>)data)[key]);
        }
        public ICollection<string> keySet()
        {
            return ((IDictionary<string, object>)data).Keys;
        }
        public JSON get(int index)
        {
            return new JSON(index < 0 || index >= ((IList<object>)data).Count ? null : ((IList<object>)data)[index]);
        }
        public JSON[] ToArray()
        {
            List<JSON> list = new List<JSON>();
            foreach (object o in (IList<object>)data)
                list.Add(new JSON(o));
            return list.ToArray();
        }
        public override string ToString()
        {
            return data == null ? "undefined" : data.ToString();
        }
        public bool booleanValue()
        {
            if (data == True)
                return true;
            if (data == False || data == Null || data == null)
                return false;
            if (data is long)
                return ((long)data) != 0L;
            if (data is double)
                return ((double)data) != 0.0;
            if (data is string)
                return (data as string).Length != 0;
            return true;
        }
        public int intValue()
        {
            if (data == True)
                return 1;
            if (data == False || data == Null)
                return 0;
            if (data is long)
                return (int)(long)data;
            if (data is double)
                return (int)(double)data;
            if (data is string)
                try { return Int32.Parse(data as string); }
                catch (Exception) { }
            throw new JSONException(data + " cast to int");
        }
        public long longValue()
        {
            if (data == True)
                return 1L;
            if (data == False || data == Null)
                return 0L;
            if (data is long)
                return (long)data;
            if (data is double)
                return (long)(double)data;
            if (data is string)
                try { return Int64.Parse(data as string); }
                catch (Exception) { }
            throw new JSONException(data + " cast to long");
        }
        public double doubleValue()
        {
            if (data == True)
                return 1;
            if (data == False || data == Null)
                return 0;
            if (data is long)
                return (double)(long)data;
            if (data is double)
                return (double)data;
            if (data is string)
                try { return Double.Parse(data as string); }
                catch (Exception) { }
            throw new JSONException(data + " cast to double");
        }
        public bool isUndefined()
        {
            return data == null;
        }
        public bool isNull()
        {
            return data == Null;
        }
        public bool isBoolean()
        {
            return data == True || data == False;
        }
        public bool isString()
        {
            return data is string;
        }
        public bool isNumber()
        {
            return data is long || data is double;
        }
        public bool isObject()
        {
            return data is Dictionary<string, object>;
        }
        public bool isArray()
        {
            return data is List<object>;
        }
        public static JSON parse(string text)
        {
            try
            {
                JSONDecoder decoder = new JSONDecoder();
                foreach (char c in text)
                    decoder.accept(c);
                decoder.flush();
                return decoder.get();
            }
            catch (JSONException e) { throw e; }
            catch (Exception e) { throw new JSONException(e); }
        }
        private class StringBuilderAdaptor : Appendable
        {
            private readonly StringBuilder sb = new StringBuilder();
            public Appendable Append(char c)
            {
                sb.Append(c);
                return this;
            }
            public Appendable Append(string s)
            {
                sb.Append(s);
                return this;
            }
            public override string ToString() { return sb.ToString(); }
        }
        public static string stringify(object obj)
        {
            StringBuilderAdaptor sb = new StringBuilderAdaptor();
            JSONEncoder.encode(obj, sb);
            return sb.ToString();
        }
    }
    #endregion
    public sealed class JSONBuilder
    {
        private sealed class _StringBuilder : Appendable
        {
            private readonly StringBuilder sb = new StringBuilder();
            public Appendable Append(string v)
            {
                sb.Append(v);
                return this;
            }
            public Appendable Append(char v)
            {
                sb.Append(v);
                return this;
            }
            internal void end()
            {
                int last = sb.Length - 1;
                if (sb[last] == ',')
                    sb[last] = '}';
                else
                    sb.Append('}');
            }
            public override string ToString()
            {
                return sb.ToString();
            }
        }
        private readonly _StringBuilder sb = new _StringBuilder();
        private readonly ISet<object> l;
        internal JSONBuilder(ISet<object> l)
        {
            this.l = l;
        }
        public JSONBuilder Append(object v)
        {
            JSONEncoder.encode(l, v, sb);
            return this;
        }
        public JSONBuilder Append(JSONMarshal v)
        {
            if (!l.Add(v))
                throw new JSONException("JSONBuilder loop detected. object = " + v + ", type = " + v.GetType().Name);
            v.marshal(this);
            l.Remove(v);
            return this;
        }
        public JSONBuilder begin()
        {
            sb.Append('{');
            return this;
        }
        public JSONBuilder end()
        {
            sb.end();
            return this;
        }
        public JSONBuilder comma()
        {
            sb.Append(',');
            return this;
        }
        public JSONBuilder colon()
        {
            sb.Append(':');
            return this;
        }
        public override string ToString()
        {
            return sb.ToString();
        }
    }
    public delegate void JSONConsumer(JSON json);
    #region JSONDecoder
    public class JSONDecoder : CharConsumer
    {
        private readonly JSONConsumer consumer;
        private readonly JSONRoot root;
        private JSONValue current;
        private JSON json;
        public JSONDecoder(JSONConsumer consumer)
        {
            this.consumer = consumer;
            this.current = this.root = new JSONRoot(this);
        }
        public JSONDecoder() : this(null) { }
        private interface JSONValue
        {
            bool accept(char c);
            void reduce(object v);
        }
        private class JSONRoot : JSONValue
        {
            private readonly JSONDecoder decoder;
            internal JSONRoot(JSONDecoder decoder) { this.decoder = decoder; }
            public bool accept(char c)
            {
                if (Char.IsWhiteSpace(c))
                    return true;
                if (decoder.json != null)
                    throw new JSONException("value has been parsed.");
                return false;
            }
            public void reduce(object v)
            {
                if (decoder.consumer != null)
                    decoder.consumer(new JSON(v));
                else
                    decoder.json = new JSON(v);
            }
        }
        private class JSONObject : JSONValue
        {
            private readonly JSONDecoder decoder;
            private readonly JSONValue parent;
            private readonly Dictionary<string, object> map = new Dictionary<string, object>();
            private string key;
            private int stage = 0;
            public JSONObject(JSONDecoder decoder)
            {
                this.decoder = decoder;
                this.parent = decoder.current;
            }
            public bool accept(char c)
            {
                switch (stage)
                {
                    case 0:
                        stage = 1;
                        return true;
                    case 1:
                        if (Char.IsWhiteSpace(c))
                            return true;
                        if (c == '}')
                        {
                            (decoder.current = parent).reduce(map);
                            return true;
                        }
                        return false;
                    case 2:
                        if (Char.IsWhiteSpace(c))
                            return true;
                        if (c == ':' || c == '=')
                        {
                            stage = 3;
                            return true;
                        }
                        throw new JSONException("object expect [:=] but encounter <" + c + ">");
                    case 4:
                        if (Char.IsWhiteSpace(c))
                            return true;
                        if (c == ',' || c == ';')
                        {
                            stage = 1;
                            return true;
                        }
                        if (c == '}')
                        {
                            (decoder.current = parent).reduce(map);
                            return true;
                        }
                        throw new JSONException("object expect [,;}] but encounter <" + c + ">");
                }
                return Char.IsWhiteSpace(c);
            }
            public void reduce(object v)
            {
                if (stage == 1)
                {
                    key = (string)v;
                    stage = 2;
                }
                else
                {
                    map[key] = v;
                    stage = 4;
                }
            }
        }
        private class JSONArray : JSONValue
        {
            private readonly JSONDecoder decoder;
            private readonly JSONValue parent;
            private readonly List<object> list = new List<object>();
            private int stage = 0;
            public JSONArray(JSONDecoder decoder)
            {
                this.decoder = decoder;
                this.parent = decoder.current;
            }
            public bool accept(char c)
            {
                switch (stage)
                {
                    case 0:
                        stage = 1;
                        return true;
                    case 1:
                        if (Char.IsWhiteSpace(c))
                            return true;
                        if (c == ']')
                        {
                            (decoder.current = parent).reduce(list);
                            return true;
                        }
                        return false;
                    default:
                        if (Char.IsWhiteSpace(c))
                            return true;
                        if (c == ',' || c == ';')
                        {
                            stage = 1;
                            return true;
                        }
                        if (c == ']')
                        {
                            (decoder.current = parent).reduce(list);
                            return true;
                        }
                        throw new JSONException("List expect [,;]] but encounter <" + c + ">");
                }
            }
            public void reduce(object v)
            {
                list.Add(v);
                stage = 2;
            }
        }
        private class JSONString : JSONValue
        {
            private readonly JSONDecoder decoder;
            private readonly JSONValue parent;
            private readonly StringBuilder sb = new StringBuilder();
            private int stage;
            public JSONString(JSONDecoder decoder)
            {
                this.decoder = decoder;
                this.parent = decoder.current;
            }
            private static int hex(char c)
            {
                if (c >= '0' && c <= '9')
                    return c - '0';
                if (c >= 'A' && c <= 'F')
                    return c - 'A' + 10;
                if (c >= 'a' && c <= 'f')
                    return c - 'a' + 10;
                throw new JSONException("bad hex char [" + c + "]");
            }
            public bool accept(char c)
            {
                if (stage < 0)
                {
                    stage = (stage << 4) | hex(c);
                    if ((stage & 0xffff0000) == 0xfff00000)
                    {
                        sb.Append((char)stage);
                        stage = 0x40000000;
                    }
                }
                else if ((stage & 0x20000000) != 0)
                {
                    switch (c)
                    {
                        case '"':
                            sb.Append('"');
                            break;
                        case '\\':
                            sb.Append('\\');
                            break;
                        case 'b':
                            sb.Append('\b');
                            break;
                        case 'f':
                            sb.Append('\f');
                            break;
                        case 'n':
                            sb.Append('\n');
                            break;
                        case 'r':
                            sb.Append('\r');
                            break;
                        case 't':
                            sb.Append('\t');
                            break;
                        case 'u':
                            stage = -16;
                            break;
                    }
                    stage &= ~0x20000000;
                }
                else if (c == '"')
                {
                    if ((stage & 0x40000000) != 0)
                        (decoder.current = parent).reduce(sb.ToString());
                    stage |= 0x40000000;
                }
                else if (c == '\\')
                    stage |= 0x20000000;
                else
                    sb.Append(c);
                return true;
            }
            public void reduce(object v) { }
        }
        private class JSONNumber : JSONValue
        {
            private readonly JSONDecoder decoder;
            private readonly JSONValue parent;
            private readonly StringBuilder sb = new StringBuilder();
            private readonly static string chars = "+-0123456789Ee.";
            private bool isDouble = false;
            public JSONNumber(JSONDecoder decoder)
            {
                this.decoder = decoder;
                this.parent = decoder.current;
            }
            public bool accept(char c)
            {
                if (chars.IndexOf(c) == -1)
                {
                    decoder.current = parent;
                    if (isDouble)
                        parent.reduce(Double.Parse(sb.ToString()));
                    else
                        parent.reduce(Int64.Parse(sb.ToString()));
                    return parent.accept(c);
                }
                if (c == '.')
                    isDouble = true;
                sb.Append(c);
                return true;
            }
            public void reduce(object v) { }
        }
        private class JSONConst : JSONValue
        {
            private readonly JSONDecoder decoder;
            private readonly JSONValue parent;
            private readonly string match;
            private readonly object value;
            private int stage = 0;
            public JSONConst(JSONDecoder decoder, string match, object value)
            {
                this.decoder = decoder;
                this.parent = decoder.current;
                this.match = match;
                this.value = value;
            }
            public bool accept(char c)
            {
                if (Char.ToLower(c) != match[stage++])
                    throw new JSONException("for const <" + match + "> encounter unexpected <" + c + ">");
                if (stage == match.Length)
                    (decoder.current = parent).reduce(value);
                return true;
            }
            public void reduce(object v) { }
        }
        public void accept(char c)
        {
            while (!current.accept(c))
                switch (c)
                {
                    case '{':
                        current = new JSONObject(this);
                        break;
                    case '[':
                        current = new JSONArray(this);
                        break;
                    case '"':
                        current = new JSONString(this);
                        break;
                    case '-':
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9':
                        current = new JSONNumber(this);
                        break;
                    case 't':
                    case 'T':
                        current = new JSONConst(this, "true", JSON.True);
                        break;
                    case 'f':
                    case 'F':
                        current = new JSONConst(this, "false", JSON.False);
                        break;
                    case 'n':
                    case 'N':
                        current = new JSONConst(this, "null", JSON.Null);
                        break;
                    default:
                        throw new JSONException("unknown character <" + c + "> current = " + current);
                }
        }
        internal void flush() { accept(' '); }
        public JSON get() { return json; }
    }
    #endregion
    #region JSONEncoder
    public sealed class JSONEncoder
    {
        private JSONEncoder() { }
        private readonly static IDictionary<Type, Action<ISet<object>, object, Appendable>> actions = new Dictionary<Type, Action<ISet<object>, object, Appendable>>();
        private readonly static Action<ISet<object>, object, Appendable> stringAction = (l, v, a) =>
        {
            a.Append('"');
            foreach (char c in v.ToString())
                switch (c)
                {
                    case '"':
                        a.Append("\\\"");
                        break;
                    case '\\':
                        a.Append("\\\\");
                        break;
                    case '\b':
                        a.Append("\\b");
                        break;
                    case '\f':
                        a.Append("\\f");
                        break;
                    case '\n':
                        a.Append("\\n");
                        break;
                    case '\r':
                        a.Append("\\r");
                        break;
                    case '\t':
                        a.Append("\\t");
                        break;
                    default:
                        a.Append(c);
                        break;
                }
            a.Append('"');
        };
        private readonly static Action<ISet<object>, object, Appendable> collectionAction = (l, v, a) =>
        {
            string comma = "";
            a.Append('[');
            foreach (object i in (v as IEnumerable))
            {
                a.Append(comma);
                encode(l, i, a);
                comma = ",";
            }
            a.Append(']');
        };
        private readonly static Action<ISet<object>, object, Appendable> mapAction = (l, v, a) =>
        {
            string comma = "";
            a.Append('{');
            foreach (DictionaryEntry e in (v as IDictionary))
            {
                a.Append(comma);
                stringAction(l, e.Key, a);
                a.Append(":");
                encode(l, e.Value, a);
                comma = ",";
            }
            a.Append('}');
        };
        static JSONEncoder()
        {
            Action<ISet<object>, object, Appendable> numberAction = (l, v, a) => a.Append(v.ToString());
            Action<ISet<object>, object, Appendable> booleanAction = (l, v, a) => a.Append((bool)v ? "true" : "false");
            lock (actions)
            {
                actions.Add(typeof(byte), numberAction);
                actions.Add(typeof(sbyte), numberAction);
                actions.Add(typeof(short), numberAction);
                actions.Add(typeof(ushort), numberAction);
                actions.Add(typeof(int), numberAction);
                actions.Add(typeof(uint), numberAction);
                actions.Add(typeof(long), numberAction);
                actions.Add(typeof(ulong), numberAction);
                actions.Add(typeof(float), numberAction);
                actions.Add(typeof(double), numberAction);
                actions.Add(typeof(bool), booleanAction);
                actions.Add(typeof(char), stringAction);
                actions.Add(typeof(string), stringAction);
                actions.Add(typeof(JSON), (l, v, a) =>
                {
                    object o = (v as JSON).data;
                    if (o == null)
                        throw new JSONException("JSONEncoder encounter undefined JSON Object.");
                    encode(l, o, a);
                });
                actions.Add(typeof(JSON.NullConst), (l, v, a) => encode(l, null, a));
                actions.Add(typeof(JSON.TrueConst), (l, v, a) => encode(l, true, a));
                actions.Add(typeof(JSON.FalseConst), (l, v, a) => encode(l, false, a));
            }
        }
        private static Action<ISet<object>, object, Appendable> makeFieldAction(FieldInfo field)
        {
            return (l, v, a) =>
            {
                stringAction(l, field.Name, a);
                a.Append(':');
                encode(l, field.GetValue(v), a);
            };
        }
        private static Action<ISet<object>, object, Appendable> packFieldActions(List<Action<ISet<object>, object, Appendable>> actions)
        {
            return (l, v, a) =>
            {
                string comma = "";
                a.Append('{');
                foreach (var action in actions)
                {
                    a.Append(comma);
                    action(l, v, a);
                    comma = ",";
                }
                a.Append('}');
            };
        }
        internal static void encode(ISet<object> l, object v, Appendable a)
        {
            if (v is JSONSerializable)
            {
                Type c = v.GetType();
                if (!l.Add(v))
                    throw new JSONException("JSONEncoder loop detected. object = " + v + ", type = " + c.Name);
                if (v is JSONMarshal)
                {
                    JSONBuilder jb = new JSONBuilder(l);
                    (v as JSONMarshal).marshal(jb);
                    a.Append(jb.ToString());
                }
                else
                {
                    Action<ISet<object>, object, Appendable> action;
                    lock (actions)
                    {
                        if (!actions.TryGetValue(c, out action))
                        {
                            var fieldActions = new List<Action<ISet<object>, object, Appendable>>();
                            foreach (var field in c.GetFields(BindingFlags.DeclaredOnly | BindingFlags.Instance | BindingFlags.Public | BindingFlags.NonPublic))
                                fieldActions.Add(makeFieldAction(field));
                            actions.Add(c, action = packFieldActions(fieldActions));
                        }
                    }
                    action(l, v, a);
                }
                l.Remove(v);
            }
            else if (v != null)
            {
                Type c = v.GetType();
                Action<ISet<object>, object, Appendable> action;
                lock (actions)
                {
                    if (!actions.TryGetValue(c, out action))
                    {
                        if (v is IDictionary)
                            action = mapAction;
                        else if (v is IEnumerable)
                            action = collectionAction;
                        else
                            throw new JSONException("JSONEncoder encounter unrecognized type = " + c.Name);
                    }
                }
                action(l, v, a);
            }
            else
                a.Append("null");
        }
        public static void encode(object obj, Appendable a)
        {
            try
            {
                encode(new HashSet<object>(), obj, a);
                if (a is Source)
                    (a as Source).flush();
            }
            catch (JSONException e) { throw e; }
            catch (Exception e) { throw new JSONException(e); }
        }
    }
    #endregion
    public class JSONException : CodecException
    {
        public JSONException(string message) : base(message) { }
        public JSONException(Exception e) : base(e) { }
    }
    public interface JSONSerializable { }
    public interface JSONMarshal : JSONSerializable
    {
        JSONBuilder marshal(JSONBuilder jb);
    }
}