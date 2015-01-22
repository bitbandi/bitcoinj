package org.spreadcoinj.core;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Created by Elbandi on 2015.01.16..
 */
public class MinerSignature implements Serializable, Comparable<MinerSignature> {
    private byte[] bytes;
    public static final MinerSignature ZERO_SIGNATURE = new MinerSignature(new byte[65]);

    /**
     * Creates a Sha256Hash by wrapping the given byte array. It must be 32 bytes long.
     */
    public MinerSignature(byte[]rawSignBytes) {
        checkArgument(rawSignBytes.length == 65);
        this.bytes = rawSignBytes;

    }

    /**
     * Creates a Sha256Hash by decoding the given hex string. It must be 64 characters long.
     */
    public MinerSignature(String hexString) {
        checkArgument(hexString.length() == 130);
        this.bytes = Utils.HEX.decode(hexString);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MinerSignature other = (MinerSignature) o;
        return Arrays.equals(bytes, other.bytes);
    }

    /**
     * Hash code of the byte array as calculated by {@link Arrays#hashCode()}. Note the difference between a SHA256
     * secure bytes and the type of quick/dirty bytes used by the Java hashCode method which is designed for use in
     * bytes tables.
     */
    @Override
    public int hashCode() {
        // Use the last 4 bytes, not the first 4 which are often zeros in Bitcoin.
        return (bytes[31] & 0xFF) | ((bytes[30] & 0xFF) << 8) | ((bytes[29] & 0xFF) << 16) | ((bytes[28] & 0xFF) << 24);
    }

    @Override
    public String toString() {
        return Utils.HEX.encode(bytes);
    }

    /**
     * Returns the bytes interpreted as a positive integer.
     */
    public BigInteger toBigInteger() {
        return new BigInteger(1, bytes);
    }

    public byte[] getBytes() {
        return bytes;
    }

    public MinerSignature duplicate() {
        return new MinerSignature(bytes);
    }

    @Override
    public int compareTo(MinerSignature o) {
        int thisCode = this.hashCode();
        int oCode = ((MinerSignature)o).hashCode();
        return thisCode > oCode ? 1 : (thisCode == oCode ? 0 : -1);
    }
}
