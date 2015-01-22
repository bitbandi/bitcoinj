/**
 * Copyright 2011 Google Inc.
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

import org.spreadcoinj.params.TestNetParams;
import org.spreadcoinj.params.UnitTestParams;
import org.spreadcoinj.script.ScriptOpCodes;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.Arrays;

import static org.spreadcoinj.core.Utils.HEX;
import static org.junit.Assert.*;

public class BlockTest {
    static final NetworkParameters params = TestNetParams.get();

    public static final byte[] blockBytes;

    static {
        // Block 874f738f74ed1305ae79a817d9eb7dabdd382f8b173b845dbf293cf4ac26e171
        // One with lots of transactions in, so a good test of the merkle tree hashing.
        blockBytes = HEX.decode("0200000029fc49241d294664bb56fe608100087daeda209e72ee40cfac229a329908b71e41f11957f8188d0fa607fc3146144c5ec664ac6beb4fc5a896d81d561f7e3c931ce8a15400000000a2ee061ca60e030006f600000894a40e43a71966dac50de0f962821e0fa6c68e4d181fa5bbad4592aea653901b915c2925d1ace70600368fd2060f9a27d70a57426fa8ce7adab176e2d12359acc40873cccd86dbda5130fa1e3a12dc6d000c81f5ff3f877b94e271ab1b126a240401000000010000000000000000000000000000000000000000000000000000000000000000ffffffff020104ffffffff01e9fdbe25000000001614282502d4a3de089ad7a7ba763dbdd5a7b931e53fac000000000100000001e7906c5e12020bb16e4c12d95cc3e57d55d7ea4a1f1b4a9bce7a1b802a0b544f0000000043421c6757377026984e1067dd1a4bde326d1ff7f8d80a5d7a937585e5c62382a3af97ac6c2c5d77d70cfdd70cdf39ffabe2bb367a83002b0af22dbeb8bf2a9d6d3f3d01ffffffff0280098d250000000016140be006a3ccb419f722e559065625f98e5a8003e5ac585a3400000000001614ef72e062bf0dedcca5a668b0fb1a843e5cfca99eac000000000100000004312922b0281272b994d9ae4a3fef076e0fb96c5fe81fb843623899346115a4000000000043421c41c7f1643976f8a5ee5c66871206081551da5fbf6800962df17b74fd2804cd960e913f6b7ea36a6441d88c721c7eefbea20f2958b6796f191587cf2a7055bb7701ffffffffe1579f86385cae40ad9ab8e9991be2feb836ad55bb79947d31b7eb52268aedba0000000043421c9cdd056741f5e97380157e65bce24fe97806e0805d89f8dcfc29a8da278d9840ab9461d510b062dde9026e3fa3aeb7d1ab69379cd5e2a3a3be2dd1ddf990834701ffffffff8e22c477fd48f68e65a3893591e2f2ca153228e0bac90334e840db9eca5262250000000043421c30e1730a7977ab38b438fcfeac47be9b2833ddf7448917324d0c33d2192c240aff4d4aead17ac68697d5141184cbea9d2c9684211217adcf3d224fd31dc11ac701ffffffffa80a64fd9fc91e1f8c81df86456e8fa528fe5c99618e44ffc72aec98044b26e90000000043421c53fde149633091ddb4595d591291a9b5160d9854687b75200d3d1e8d9081050b1c706a48ccda58d46d71b2a523b3399a03081c43d8e47468741a6e6f6ee6ee4101ffffffff020462350000000000161484de6f94e76d4dc8cc1e3d92b370f1044990ac6eac80bccc960000000016140be006a3ccb419f722e559065625f98e5a8003e5ac0000000001000000044b1f918dab489a83da5449720c62174c04cd34f3ec42f81245765fe108565f100100000043421b9d6658f4dda6d2848e142c09e8332d7045133eae6b7608dc639a89c4f22c4c47088b3b59faee66b409ad2cd4b4e786108d8c9473cff808202c346825b9faae1001ffffffff0858a852d0ea55055fa2b330214bf697a23498a76bec7354128f7ba2d7333a760000000043421b27aae0cbf2e5a108a3be82f7b64219cc7a3a05388d5ad4d7a9921eda61c72cdde559481b5e742472bd9d5bf6359ad8ff327275b682b31b9b0b18d3f9dc841f6e01ffffffff7c9518d50cd55998d19802cc96b7ef0a8e4287db0bdbd3e48507ee6f26b83b980000000043421c40634992b62754e0f0778e59711fd0b0f9585460c390cf974b3ae53dcff5b383cabe81b7d93bd17379b61268fdb689a8b6cb2c921d49c6bfedd58ce6739b289201ffffffff45d2e90f1f401d5a5e1d69563f1d43b64fc9f867a59fc23de7842ea4415b23660000000043421c474821a74560a731182c0185dfaa10d4c43f456892396390da0293532648d1275eaaeaa9a4f57ee32dac155c86c4b8dfc5080a0ec14a253f054fe674c4a145eb01ffffffff0280a9b24b0000000016140be006a3ccb419f722e559065625f98e5a8003e5acc80b20000000000016149b614013f64949ef9963873ade7804dead45eccdac00000000");
    }

    @Test
    public void testWork() throws Exception {
        BigInteger work = params.getGenesisBlock().getWork();
        // This number is printed by the official client at startup as the calculated value of chainWork on testnet:
        //
        // SetBestChain: new best=874f738f74ed1305ae7  height=0  work=16777472
        assertEquals(BigInteger.valueOf(16777472L), work);
    }

    @Test
    public void testBlockVerification() throws Exception {
        Block block = new Block(params, blockBytes);
        block.verify();
        assertEquals("874f738f74ed1305ae79a817d9eb7dabdd382f8b173b845dbf293cf4ac26e171", block.getHashAsString());
    }
    
    @SuppressWarnings("deprecation")
    @Test
    public void testDate() throws Exception {
        Block block = new Block(params, blockBytes);
        assertEquals("29 Dec 2014 23:47:40 GMT", block.getTime().toGMTString());
    }

    @Test
    public void testProofOfWork() throws Exception {
        // This params accepts any difficulty target.
        NetworkParameters params = UnitTestParams.get();
        Block block = new Block(params, blockBytes);
        block.setNonce(12346);
        try {
            block.verify();
            fail();
        } catch (VerificationException e) {
            // Expected.
        }
        // Blocks contain their own difficulty target. The BlockChain verification mechanism is what stops real blocks
        // from containing artificially weak difficulties.
        block.setDifficultyTarget(Block.EASIEST_DIFFICULTY_TARGET);
        // Now it should pass.
        block.verify();
        // Break the nonce again at the lower difficulty level so we can try solving for it.
        block.setNonce(1);
        try {
            block.verify();
            fail();
        } catch (VerificationException e) {
            // Expected to fail as the nonce is no longer correct.
        }
        // Should find an acceptable nonce.
        block.solve();
        block.verify();
        assertEquals(block.getNonce(), 2);
    }

    @Test
    public void testBadTransactions() throws Exception {
        Block block = new Block(params, blockBytes);
        // Re-arrange so the coinbase transaction is not first.
        Transaction tx1 = block.transactions.get(0);
        Transaction tx2 = block.transactions.get(1);
        block.transactions.set(0, tx2);
        block.transactions.set(1, tx1);
        try {
            block.verify();
            fail();
        } catch (VerificationException e) {
            // We should get here.
        }
    }

    @Test
    public void testHeaderParse() throws Exception {
        Block block = new Block(params, blockBytes);
        Block header = block.cloneAsHeader();
        Block reparsed = new Block(params, header.bitcoinSerialize());
        assertEquals(reparsed, header);
    }

    @Test
    public void testBitCoinSerialization() throws Exception {
        // We have to be able to reserialize everything exactly as we found it for hashing to work. This test also
        // proves that transaction serialization works, along with all its subobjects like scripts and in/outpoints.
        //
        // NB: This tests the BITCOIN proprietary serialization protocol. A different test checks Java serialization
        // of transactions.
        Block block = new Block(params, blockBytes);
        assertTrue(Arrays.equals(blockBytes, block.bitcoinSerialize()));
    }

    @Test
    public void testJavaSerialiazation() throws Exception {
        Block block = new Block(params, blockBytes);
        Transaction tx = block.transactions.get(1);

        // Serialize using Java.
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(tx);
        oos.close();
        byte[] javaBits = bos.toByteArray();
        // Deserialize again.
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(javaBits));
        Transaction tx2 = (Transaction) ois.readObject();
        ois.close();

        // Note that this will actually check the transactions are equal by doing bitcoin serialization and checking
        // the bytestreams are the same! A true "deep equals" is not implemented for Transaction. The primary purpose
        // of this test is to ensure no errors occur during the Java serialization/deserialization process.
        assertEquals(tx, tx2);
    }
    
    @Test
    public void testUpdateLength() {
        NetworkParameters params = UnitTestParams.get();
        Block block = params.getGenesisBlock().createNextBlockWithCoinbase(new ECKey().getPubKey());
        assertEquals(block.bitcoinSerialize().length, block.length);
        final int origBlockLen = block.length;
        Transaction tx = new Transaction(params);
        // this is broken until the transaction has > 1 input + output (which is required anyway...)
        //assertTrue(tx.length == tx.bitcoinSerialize().length && tx.length == 8);
        byte[] outputScript = new byte[10];
        Arrays.fill(outputScript, (byte) ScriptOpCodes.OP_FALSE);
        tx.addOutput(new TransactionOutput(params, null, Coin.SATOSHI, outputScript));
        tx.addInput(new TransactionInput(params, null, new byte[] {(byte) ScriptOpCodes.OP_FALSE},
                new TransactionOutPoint(params, 0, Sha256Hash.create(new byte[] {1}))));
        int origTxLength = 8 + 2 + 8 + 1 + 10 + 40 + 1 + 1;
        assertEquals(tx.bitcoinSerialize().length, tx.length);
        assertEquals(origTxLength, tx.length);
        block.addTransaction(tx);
        assertEquals(block.bitcoinSerialize().length, block.length);
        assertEquals(origBlockLen + tx.length, block.length);
        block.getTransactions().get(1).getInputs().get(0).setScriptBytes(new byte[] {(byte) ScriptOpCodes.OP_FALSE, (byte) ScriptOpCodes.OP_FALSE});
        assertEquals(block.length, origBlockLen + tx.length);
        assertEquals(tx.length, origTxLength + 1);
        block.getTransactions().get(1).getInputs().get(0).setScriptBytes(new byte[] {});
        assertEquals(block.length, block.bitcoinSerialize().length);
        assertEquals(block.length, origBlockLen + tx.length);
        assertEquals(tx.length, origTxLength - 1);
        block.getTransactions().get(1).addInput(new TransactionInput(params, null, new byte[] {(byte) ScriptOpCodes.OP_FALSE},
                new TransactionOutPoint(params, 0, Sha256Hash.create(new byte[] {1}))));
        assertEquals(block.length, origBlockLen + tx.length);
        assertEquals(tx.length, origTxLength + 41); // - 1 + 40 + 1 + 1
    }
}
