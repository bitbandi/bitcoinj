/**
 * Copyright 2011 Noa Resare
 * Copyright 2014 Andreas Schildbach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.spreadcoinj.core;

import org.spreadcoinj.params.MainNetParams;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.spreadcoinj.core.Utils.HEX;
import static org.junit.Assert.*;

public class BitcoinSerializerTest {
    private final byte[] addrMessage = HEX.decode("4f3c5cbb6164647200000000000000001f000000" +
            "ed52399b01e215104d010000000000000000000000000000000000ffff0a000001208d");

    private final byte[] txMessage = HEX.withSeparator(" ", 2).decode(
            "4f 3c 5c bb 74 78 00 00  00 00 00 00 00 00 00 00" +
            "b4 00 00 00 24 82 ff ae  01 00 00 00 01 e7 90 6c" +
            "5e 12 02 0b b1 6e 4c 12  d9 5c c3 e5 7d 55 d7 ea" +
            "4a 1f 1b 4a 9b ce 7a 1b  80 2a 0b 54 4f 00 00 00" +
            "00 43 42 1c 67 57 37 70  26 98 4e 10 67 dd 1a 4b" +
            "de 32 6d 1f f7 f8 d8 0a  5d 7a 93 75 85 e5 c6 23" +
            "82 a3 af 97 ac 6c 2c 5d  77 d7 0c fd d7 0c df 39" +
            "ff ab e2 bb 36 7a 83 00  2b 0a f2 2d be b8 bf 2a" +
            "9d 6d 3f 3d 01 ff ff ff  ff 02 80 09 8d 25 00 00" +
            "00 00 16 14 0b e0 06 a3  cc b4 19 f7 22 e5 59 06" +
            "56 25 f9 8e 5a 80 03 e5  ac 58 5a 34 00 00 00 00" +
            "00 16 14 ef 72 e0 62 bf  0d ed cc a5 a6 68 b0 fb" +
            "1a 84 3e 5c fc a9 9e ac  00 00 00 00");

    @Test
    public void testAddr() throws Exception {
        BitcoinSerializer bs = new BitcoinSerializer(MainNetParams.get());
        // the actual data from https://en.bitcoin.it/wiki/Protocol_specification#addr
        AddressMessage a = (AddressMessage)bs.deserialize(ByteBuffer.wrap(addrMessage));
        assertEquals(1, a.getAddresses().size());
        PeerAddress pa = a.getAddresses().get(0);
        assertEquals(8333, pa.getPort());
        assertEquals("10.0.0.1", pa.getAddr().getHostAddress());
        ByteArrayOutputStream bos = new ByteArrayOutputStream(addrMessage.length);
        bs.serialize(a, bos);

        //this wont be true due to dynamic timestamps.
        //assertTrue(LazyParseByteCacheTest.arrayContains(bos.toByteArray(), addrMessage));
    }

    @Test
    public void testLazyParsing()  throws Exception {
        BitcoinSerializer bs = new BitcoinSerializer(MainNetParams.get(), true, false);

    	Transaction tx = (Transaction)bs.deserialize(ByteBuffer.wrap(txMessage));
        assertNotNull(tx);
        assertEquals(false, tx.isParsed());
        assertEquals(true, tx.isCached());
        tx.getInputs();
        assertEquals(true, tx.isParsed());

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bs.serialize(tx, bos);
        assertEquals(true, Arrays.equals(txMessage, bos.toByteArray()));
    }

    @Test
    public void testCachedParsing()  throws Exception {
        testCachedParsing(true);
        testCachedParsing(false);
    }

    private void testCachedParsing(boolean lazy)  throws Exception {
        BitcoinSerializer bs = new BitcoinSerializer(MainNetParams.get(), lazy, true);
        
        //first try writing to a fields to ensure uncaching and children are not affected
        Transaction tx = (Transaction)bs.deserialize(ByteBuffer.wrap(txMessage));
        assertNotNull(tx);
        assertEquals(!lazy, tx.isParsed());
        assertEquals(true, tx.isCached());

        tx.setLockTime(1);
        //parent should have been uncached
        assertEquals(false, tx.isCached());
        //child should remain cached.
        assertEquals(true, tx.getInputs().get(0).isCached());

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bs.serialize(tx, bos);
        assertEquals(true, !Arrays.equals(txMessage, bos.toByteArray()));

      //now try writing to a child to ensure uncaching is propagated up to parent but not to siblings
        tx = (Transaction)bs.deserialize(ByteBuffer.wrap(txMessage));
        assertNotNull(tx);
        assertEquals(!lazy, tx.isParsed());
        assertEquals(true, tx.isCached());

        tx.getInputs().get(0).setSequenceNumber(1);
        //parent should have been uncached
        assertEquals(false, tx.isCached());
        //so should child
        assertEquals(false, tx.getInputs().get(0).isCached());

        bos = new ByteArrayOutputStream();
        bs.serialize(tx, bos);
        assertEquals(true, !Arrays.equals(txMessage, bos.toByteArray()));

      //deserialize/reserialize to check for equals.
        tx = (Transaction)bs.deserialize(ByteBuffer.wrap(txMessage));
        assertNotNull(tx);
        assertEquals(!lazy, tx.isParsed());
        assertEquals(true, tx.isCached());
        bos = new ByteArrayOutputStream();
        bs.serialize(tx, bos);
        assertEquals(true, Arrays.equals(txMessage, bos.toByteArray()));

      //deserialize/reserialize to check for equals.  Set a field to it's existing value to trigger uncache
        tx = (Transaction)bs.deserialize(ByteBuffer.wrap(txMessage));
        assertNotNull(tx);
        assertEquals(!lazy, tx.isParsed());
        assertEquals(true, tx.isCached());

        tx.getInputs().get(0).setSequenceNumber(tx.getInputs().get(0).getSequenceNumber());

        bos = new ByteArrayOutputStream();
        bs.serialize(tx, bos);
        assertEquals(true, Arrays.equals(txMessage, bos.toByteArray()));

    }


    /**
     * Get 1 header of the block number 1 (the first one is 0) in the chain
     */
    @Test
    public void testHeaders1() throws Exception {
        BitcoinSerializer bs = new BitcoinSerializer(MainNetParams.get());

        HeadersMessage hm = (HeadersMessage) bs.deserialize(ByteBuffer.wrap(HEX.decode("4f3c5cbb686561" +
                "6465727300000000005a000000ade9db8e0102000000a37fedb7c176ecacb2d993f6172" +
                "b3f21f3cea005082742054e346993aeeddc147d02e3df107c4c0d5e0651c3351df90c5f" +
                "b3446002070a9da4b450bb6bf19dabdc62d75300000000ffff0f1e010000000002af4100")));

        // The first block after the genesis
        // http://spreadcoin.net/explorer/1
        Block block = hm.getBlockHeaders().get(0);
        String hash = block.getHashAsString();
        assertEquals(hash, "b3e028fe396ebaa9115812404d2d3ff65dce68f17ecc431d43745cd7ccc150b8");

        assertNull(block.transactions);

        assertEquals(Utils.HEX.encode(block.getMerkleRoot().getBytes()),
                "ab9df16bbb50b4a49d0a07026044b35f0cf91d35c351065e0d4c7c10dfe3027d");
    }


    @Test
    /**
     * Get 6 headers of blocks 1-6 in the chain
     */
    public void testHeaders2() throws Exception {
        BitcoinSerializer bs = new BitcoinSerializer(MainNetParams.get());

        HeadersMessage hm = (HeadersMessage) bs.deserialize(ByteBuffer.wrap(HEX.decode("4f3c5cbb6865616465" +
                "7273000000000017020000f83dc5cc06" +
                "02000000a37fedb7c176ecacb2d993f6172b3f21f3cea005082742054e346993aeeddc147d02e3df107c4c0d5e0651c3351df90c5fb3446002070a9da4b450bb6bf19dabdc62d75300000000ffff0f1e010000000002af4100" +
                "02000000b850c1ccd75c74431d43cc7ef168ce5df63f2d4d40125811a9ba6e39fe28e0b3da78fc5be172d005f634ea73fb5f386fffcf1f059a297840a33c8f7965813350f662d75300000000ffff0f1e020000000001aa0300" +
                "02000000970e2cbde1014e61ff80d7e26116d4365c35d8e3519cfcb9bdb745d5667ff1adad91a2c047aa4c1665c750b90a40782e84ca3262e347b0685e8c07450d2eaab90263d75300000000ffff0f1e030000000007d53900" +
                "02000000b7ae960d20a01e42bd87d5c35e3d580263055aac4849e2a7f7234b64f975774f772fc7ee6983e21169dedaac863c57ab5ca04609ead94672612e59d76555bc750c63d75300000000ffff0f1e0400000000047e3300" +
                "02000000cb7ed21fa3af9ec13149407a74ca35d3680d3111c8b99e8d4315a2afc841179a39c99e84efee9caa99813ff7e72bcd4959d9a2655715b3f12745e1aad039b5f20363d75300000000ffff0f1e050000000004fc9900" +
                "020000008fd0b80a71325d8531dd0d312495c2c52af43a2cb0582a5cec8c2172a9a23986a15d9b9172d69def47cca3c0a52f3946ca84fbebb9d0436737dede02e31e0f5c1d63d75300000000ffff0f1e0600000000014b6f00")));

        int nBlocks = hm.getBlockHeaders().size();
        assertEquals(nBlocks, 6);

        // index 0 block is the number 1 block in the block chain
        // http://spreadcoin.net/explorer/1
        Block zeroBlock = hm.getBlockHeaders().get(0);
        String zeroBlockHash = zeroBlock.getHashAsString();

        assertEquals("b3e028fe396ebaa9115812404d2d3ff65dce68f17ecc431d43745cd7ccc150b8",
                zeroBlockHash);
        assertEquals(zeroBlock.getNonce(), 1101988352L);


        Block thirdBlock = hm.getBlockHeaders().get(3);
        String thirdBlockHash = thirdBlock.getHashAsString();

        // index 3 block is the number 4 block in the block chain
        // http://spreadcoin.net/explorer/4
        assertEquals("9a1741c8afa215438d9eb9c811310d68d335ca747a404931c19eafa31fd27ecb",
                thirdBlockHash);
        assertEquals(thirdBlock.getNonce(), 863896576L);
    }

    @Test
    public void testBitcoinPacketHeader() {
        try {
            new BitcoinSerializer.BitcoinPacketHeader(ByteBuffer.wrap(new byte[]{0}));
            fail();
        } catch (BufferUnderflowException e) {
        }

        // Message with a Message size which is 1 too big, in little endian format.
        byte[] wrongMessageLength = HEX.decode("000000000000000000000000010000020000000000");
        try {
            new BitcoinSerializer.BitcoinPacketHeader(ByteBuffer.wrap(wrongMessageLength));
            fail();
        } catch (ProtocolException e) {
            // expected
        }
    }

    @Test
    public void testSeekPastMagicBytes() {
        // Fail in another way, there is data in the stream but no magic bytes.
        byte[] brokenMessage = HEX.decode("000000");
        try {
            new BitcoinSerializer(MainNetParams.get()).seekPastMagicBytes(ByteBuffer.wrap(brokenMessage));
            fail();
        } catch (BufferUnderflowException e) {
            // expected
        }
    }

    @Test
    /**
     * Tests serialization of an unknown message.
     */
    public void testSerializeUnknownMessage() {
        BitcoinSerializer bs = new BitcoinSerializer(MainNetParams.get());

        UnknownMessage a = new UnknownMessage();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(addrMessage.length);
        try {
            bs.serialize(a, bos);
            fail();
        } catch (Throwable e) {
        }
    }

    /**
     * Unknown message for testSerializeUnknownMessage.
     */
    class UnknownMessage extends Message {
        @Override
        void parse() throws ProtocolException {
        }

        @Override
        protected void parseLite() throws ProtocolException {
        }
    }

}
