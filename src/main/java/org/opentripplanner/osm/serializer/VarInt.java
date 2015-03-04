package org.opentripplanner.osm.serializer;

// Protocol Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
// http://code.google.com/p/protobuf/
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
//     * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
//     * Redistributions in binary form must reproduce the above
// copyright notice, this list of conditions and the following disclaimer
// in the documentation and/or other materials provided with the
// distribution.
//     * Neither the name of Google Inc. nor the names of its
// contributors may be used to endorse or promote products derived from
// this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

import com.google.common.base.Charsets;
import org.opentripplanner.osm.Tagged;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Variable-width integer coding. Adapted from Google Protobuf library for MapDB Serializers.
 */
public class VarInt {

    // WRITING VARINTS

    /**
     * Encode a ZigZag-encoded 32-bit value.  ZigZag encodes signed integers
     * into values that can be efficiently encoded with varint.  (Otherwise,
     * negative values must be sign-extended to 64 bits to be varint encoded,
     * thus always taking 10 bytes on the wire.)
     *
     * @param n A signed 32-bit integer.
     * @return An unsigned 32-bit integer, stored in a signed int because
     *         Java has no explicit unsigned support.
     */
    public static int encodeZigZag32(final int n) {
        // Note:  the right-shift must be arithmetic
        return (n << 1) ^ (n >> 31);
    }

    /**
     * Encode a ZigZag-encoded 64-bit value.  ZigZag encodes signed integers
     * into values that can be efficiently encoded with varint.  (Otherwise,
     * negative values must be sign-extended to 64 bits to be varint encoded,
     * thus always taking 10 bytes on the wire.)
     *
     * @param n A signed 64-bit integer.
     * @return An unsigned 64-bit integer, stored in a signed int because
     *         Java has no explicit unsigned support.
     */
    public static long encodeZigZag64(final long n) {
        // Note:  the right-shift must be arithmetic
        return (n << 1) ^ (n >> 63);
    }

    /** Write an {@code sint32} field to the stream. */
    public static void writeSInt32NoTag(DataOutput out, final int value) throws IOException {
        writeRawVarint32(out, encodeZigZag32(value));
    }

    /** Write an {@code sint64} field to the stream. */
    public static void writeSInt64NoTag(DataOutput out, final long value) throws IOException {
        writeRawVarint64(out, encodeZigZag64(value));
    }

    /** Encode and write a varint. */
    public static void writeRawVarint64(DataOutput out, long value) throws IOException {
        while (true) {
            if ((value & ~0x7FL) == 0) {
                out.write((int) value);
                return;
            } else {
                out.write(((int) value & 0x7F) | 0x80);
                value >>>= 7;
            }
        }
    }

    /**
     * Encode and write a varint.  {@code value} is treated as
     * unsigned, so it won't be sign-extended if negative.
     */
    public static void writeRawVarint32(DataOutput out, int value) throws IOException {
        while (true) {
            if ((value & ~0x7F) == 0) {
                out.write(value);
                return;
            } else {
                out.write((value & 0x7F) | 0x80);
                value >>>= 7;
            }
        }
    }
    
    // READING VARINTS

    /** Read an {@code sint32} field value from the stream. */
    public static int readSInt32(DataInput in) throws IOException {
        return decodeZigZag32(readRawVarint32(in));
    }

    /** Read an {@code sint64} field value from the stream. */
    public static long readSInt64(DataInput in) throws IOException {
        return decodeZigZag64(readRawVarint64(in));
    }

    /**
     * Decode a ZigZag-encoded 32-bit value.  ZigZag encodes signed integers
     * into values that can be efficiently encoded with varint.  (Otherwise,
     * negative values must be sign-extended to 64 bits to be varint encoded,
     * thus always taking 10 bytes on the wire.)
     *
     * @param n An unsigned 32-bit integer, stored in a signed int because
     *          Java has no explicit unsigned support.
     * @return A signed 32-bit integer.
     */
    public static int decodeZigZag32(final int n) {
        return (n >>> 1) ^ -(n & 1);
    }

    /**
     * Decode a ZigZag-encoded 64-bit value.  ZigZag encodes signed integers
     * into values that can be efficiently encoded with varint.  (Otherwise,
     * negative values must be sign-extended to 64 bits to be varint encoded,
     * thus always taking 10 bytes on the wire.)
     *
     * @param n An unsigned 64-bit integer, stored in a signed int because
     *          Java has no explicit unsigned support.
     * @return A signed 64-bit integer.
     */
    public static long decodeZigZag64(final long n) {
        return (n >>> 1) ^ -(n & 1);
    }

    /**
     * Read a raw Varint from the stream.  If larger than 32 bits, discard the
     * upper bits.
     */
    public static int readRawVarint32(DataInput in) throws IOException {
        byte tmp = in.readByte();
        if (tmp >= 0) {
            return tmp;
        }
        int result = tmp & 0x7f;
        if ((tmp = in.readByte()) >= 0) {
            result |= tmp << 7;
        } else {
            result |= (tmp & 0x7f) << 7;
            if ((tmp = in.readByte()) >= 0) {
                result |= tmp << 14;
            } else {
                result |= (tmp & 0x7f) << 14;
                if ((tmp = in.readByte()) >= 0) {
                    result |= tmp << 21;
                } else {
                    result |= (tmp & 0x7f) << 21;
                    result |= (tmp = in.readByte()) << 28;
                    if (tmp < 0) {
                        // Discard upper 32 bits.
                        for (int i = 0; i < 5; i++) {
                            if (in.readByte() >= 0) {
                                return result;
                            }
                        }
                        throw new RuntimeException("Malformed VarInt");
                    }
                }
            }
        }
        return result;
    }
    
    /** Read a raw Varint from the stream. */
    public static long readRawVarint64(DataInput in) throws IOException {
        int shift = 0;
        long result = 0;
        while (shift < 64) {
            final byte b = in.readByte();
            result |= (long)(b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return result;
            }
            shift += 7;
        }
        throw new RuntimeException("Malformed VarInt");
    }

    // WORKING WITH OSM TAGS
    // TODO KV serializer using common tags file from VEX

    // For strings less that 128 characters in length, this will use only one byte more than the string itself
    public static void writeString(DataOutput out, String string) throws IOException {
        byte[] bytes = string.getBytes(Charsets.UTF_8);
        writeRawVarint32(out, bytes.length);
        out.write(bytes);
    }

    public static String readString(DataInput in) throws IOException {
        int length = readRawVarint32(in);
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, Charsets.UTF_8);
    }

    public static void writeTags(DataOutput out, Tagged tagged) throws IOException {
        if (tagged.hasNoTags()) {
            writeRawVarint32(out, 0);
            return;
        }
        for (Tagged.Tag tag : tagged.tags) {
            writeString(out, tag.key);
            writeString(out, tag.value);
        }
    }

    public static void readTags(DataInput in, Tagged tagged) throws IOException {
        int nTags = readRawVarint32(in);
        for (int i = 0; i < nTags; i++) {
            tagged.addTag(readString(in), readString(in));
        }
    }



}
