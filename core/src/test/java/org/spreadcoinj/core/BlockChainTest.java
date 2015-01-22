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

import org.spreadcoinj.core.Wallet.BalanceType;
import org.spreadcoinj.params.MainNetParams;
import org.spreadcoinj.params.TestNetParams;
import org.spreadcoinj.params.UnitTestParams;
import org.spreadcoinj.store.BlockStore;
import org.spreadcoinj.store.MemoryBlockStore;
import org.spreadcoinj.testing.FakeTxBuilder;
import org.spreadcoinj.utils.BriefLogFormatter;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static org.spreadcoinj.core.Coin.*;
import static org.spreadcoinj.testing.FakeTxBuilder.createFakeBlock;
import static org.spreadcoinj.testing.FakeTxBuilder.createFakeTx;
import static org.junit.Assert.*;

// Handling of chain splits/reorgs are in ChainSplitTests.

public class BlockChainTest {
    private BlockChain testNetChain;

    private Wallet wallet;
    private BlockChain chain;
    private BlockStore blockStore;
    private Address coinbaseTo;
    private NetworkParameters unitTestParams;
    private final StoredBlock[] block = new StoredBlock[1];
    private Transaction coinbaseTransaction;

    private static class TweakableTestNet2Params extends TestNetParams {
        public void setMaxTarget(BigInteger limit) {
            maxTarget = limit;
        }
    }
    private static final TweakableTestNet2Params testNet = new TweakableTestNet2Params();

    private void resetBlockStore() {
        blockStore = new MemoryBlockStore(unitTestParams);
    }

    @Before
    public void setUp() throws Exception {
        BriefLogFormatter.initVerbose();
        testNetChain = new BlockChain(testNet, new Wallet(testNet), new MemoryBlockStore(testNet));
        Wallet.SendRequest.DEFAULT_FEE_PER_KB = Coin.ZERO;

        unitTestParams = UnitTestParams.get();
        wallet = new Wallet(unitTestParams) {
            @Override
            public void receiveFromBlock(Transaction tx, StoredBlock block, BlockChain.NewBlockType blockType,
                                         int relativityOffset) throws VerificationException {
                super.receiveFromBlock(tx, block, blockType, relativityOffset);
                BlockChainTest.this.block[0] = block;
                if (tx.isCoinBase()) {
                    BlockChainTest.this.coinbaseTransaction = tx;
                }
            }
        };
        wallet.freshReceiveKey();

        resetBlockStore();
        chain = new BlockChain(unitTestParams, wallet, blockStore);

        coinbaseTo = wallet.currentReceiveKey().toAddress(unitTestParams);
    }

    @After
    public void tearDown() {
        Wallet.SendRequest.DEFAULT_FEE_PER_KB = Transaction.REFERENCE_DEFAULT_MIN_TX_FEE;
    }

    @Test
    public void testBasicChaining() throws Exception {
        // Check that we can plug a few blocks together and the futures work.
        ListenableFuture<StoredBlock> future = testNetChain.getHeightFuture(2);
        // Block 1 from the testnet.
        Block b1 = getBlock1();
        assertTrue(testNetChain.add(b1));
        assertFalse(future.isDone());
        // Block 2 from the testnet.
        Block b2 = getBlock2();

        // Let's try adding an invalid block.
        long n = b2.getNonce();
        try {
            b2.setNonce(12345);
            testNetChain.add(b2);
            fail();
        } catch (VerificationException e) {
            b2.setNonce(n);
        }

        // Now it works because we reset the nonce.
        assertTrue(testNetChain.add(b2));
        assertTrue(future.isDone());
        assertEquals(2, future.get().getHeight());
    }

    @Test
    public void receiveCoins() throws Exception {
        // Quick check that we can actually receive coins.
        Transaction tx1 = createFakeTx(unitTestParams,
                                       COIN,
                                       wallet.currentReceiveKey().toAddress(unitTestParams));
        Block b1 = createFakeBlock(blockStore, tx1).block;
        chain.add(b1);
        assertTrue(wallet.getBalance().signum() > 0);
    }

    @Test
    public void merkleRoots() throws Exception {
        // Test that merkle root verification takes place when a relevant transaction is present and doesn't when
        // there isn't any such tx present (as an optimization).
        Transaction tx1 = createFakeTx(unitTestParams,
                                       COIN,
                                       wallet.currentReceiveKey().toAddress(unitTestParams));
        Block b1 = createFakeBlock(blockStore, tx1).block;
        chain.add(b1);
        resetBlockStore();
        Sha256Hash hash = b1.getMerkleRoot();
        b1.setMerkleRoot(Sha256Hash.ZERO_HASH);
        try {
            chain.add(b1);
            fail();
        } catch (VerificationException e) {
            // Expected.
            b1.setMerkleRoot(hash);
        }
        // Now add a second block with no relevant transactions and then break it.
        Transaction tx2 = createFakeTx(unitTestParams, COIN,
                                       new ECKey().toAddress(unitTestParams));
        Block b2 = createFakeBlock(blockStore, tx2).block;
        b2.getMerkleRoot();
        b2.setMerkleRoot(Sha256Hash.ZERO_HASH);
        b2.solve();
        chain.add(b2);  // Broken block is accepted because its contents don't matter to us.
    }

    @Test
    public void unconnectedBlocks() throws Exception {
        Block b1 = unitTestParams.getGenesisBlock().createNextBlock(coinbaseTo);
        Block b2 = b1.createNextBlock(coinbaseTo);
        Block b3 = b2.createNextBlock(coinbaseTo);
        // Connected.
        assertTrue(chain.add(b1));
        // Unconnected but stored. The head of the chain is still b1.
        assertFalse(chain.add(b3));
        assertEquals(chain.getChainHead().getHeader(), b1.cloneAsHeader());
        // Add in the middle block.
        assertTrue(chain.add(b2));
        assertEquals(chain.getChainHead().getHeader(), b3.cloneAsHeader());
    }

    @Test
    public void difficultyTransitions() throws Exception {
        // Add a bunch of blocks in a loop until we reach a difficulty transition point. The unit test params have an
        // artificially shortened period.
        Block prev = unitTestParams.getGenesisBlock();
        Utils.setMockClock(System.currentTimeMillis()/1000);
        for (int i = 0; i < unitTestParams.getInterval() - 1; i++) {
            Block newBlock = prev.createNextBlock(coinbaseTo, Utils.currentTimeSeconds());
            assertTrue(chain.add(newBlock));
            prev = newBlock;
            // The fake chain should seem to be "fast" for the purposes of difficulty calculations.
            Utils.rollMockClock(2);
        }
        // Now add another block that has no difficulty adjustment, it should be rejected.
        try {
            chain.add(prev.createNextBlock(coinbaseTo, Utils.currentTimeSeconds()));
            fail();
        } catch (VerificationException e) {
        }
        // Create a new block with the right difficulty target given our blistering speed relative to the huge amount
        // of time it's supposed to take (set in the unit test network parameters).
        Block b = prev.createNextBlock(coinbaseTo, Utils.currentTimeSeconds());
        b.setDifficultyTarget(0x201fFFFFL);
        b.solve();
        assertTrue(chain.add(b));
        // Successfully traversed a difficulty transition period.
    }

    @Test
    public void badDifficulty() throws Exception {
        assertTrue(testNetChain.add(getBlock1()));
        Block b2 = getBlock2();
        assertTrue(testNetChain.add(b2));
        Block bad = new Block(testNet);
        bad.setHeight(3);
        // Merkle root can be anything here, doesn't matter.
        bad.setMerkleRoot(new Sha256Hash("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"));
        // Nonce was just some number that made the hash < difficulty limit set below, it can be anything.
        bad.setNonce(140548933);
        bad.setTime(1279242649);
        bad.setPrevBlockHash(b2.getHash());
        // We're going to make this block so easy 50% of solutions will pass, and check it gets rejected for having a
        // bad difficulty target. Unfortunately the encoding mechanism means we cannot make one that accepts all
        // solutions.
        bad.setDifficultyTarget(Block.EASIEST_DIFFICULTY_TARGET);
        try {
            testNetChain.add(bad);
            // The difficulty target above should be rejected on the grounds of being easier than the networks
            // allowable difficulty.
            fail();
        } catch (VerificationException e) {
            assertTrue(e.getMessage(), e.getCause().getMessage().contains("Difficulty target is bad"));
        }

        // Accept any level of difficulty now.
        BigInteger oldVal = testNet.getMaxTarget();
        testNet.setMaxTarget(new BigInteger("00ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16));
        try {
            testNetChain.add(bad);
            // We should not get here as the difficulty target should not be changing at this point.
            fail();
        } catch (VerificationException e) {
            assertTrue(e.getMessage(), e.getCause().getMessage().contains("Unexpected change in difficulty"));
        }
        testNet.setMaxTarget(oldVal);

        // TODO: Test difficulty change is not out of range when a transition period becomes valid.
    }

    @Test
    public void duplicates() throws Exception {
        // Adding a block twice should not have any effect, in particular it should not send the block to the wallet.
        Block b1 = unitTestParams.getGenesisBlock().createNextBlock(coinbaseTo);
        Block b2 = b1.createNextBlock(coinbaseTo);
        Block b3 = b2.createNextBlock(coinbaseTo);
        assertTrue(chain.add(b1));
        assertEquals(b1, block[0].getHeader());
        assertTrue(chain.add(b2));
        assertEquals(b2, block[0].getHeader());
        assertTrue(chain.add(b3));
        assertEquals(b3, block[0].getHeader());
        assertEquals(b3, chain.getChainHead().getHeader());
        assertTrue(chain.add(b2));
        assertEquals(b3, chain.getChainHead().getHeader());
        // Wallet was NOT called with the new block because the duplicate add was spotted.
        assertEquals(b3, block[0].getHeader());
    }

    @Test
    public void intraBlockDependencies() throws Exception {
        // Covers issue 166 in which transactions that depend on each other inside a block were not always being
        // considered relevant.
        Address somebodyElse = new ECKey().toAddress(unitTestParams);
        Block b1 = unitTestParams.getGenesisBlock().createNextBlock(somebodyElse);
        ECKey key = wallet.freshReceiveKey();
        Address addr = key.toAddress(unitTestParams);
        // Create a tx that gives us some coins, and another that spends it to someone else in the same block.
        Transaction t1 = FakeTxBuilder.createFakeTx(unitTestParams, COIN, addr);
        Transaction t2 = new Transaction(unitTestParams);
        t2.addInput(t1.getOutputs().get(0));
        t2.addOutput(valueOf(2, 0), somebodyElse);
        b1.addTransaction(t1);
        b1.addTransaction(t2);
        b1.solve();
        chain.add(b1);
        assertEquals(Coin.ZERO, wallet.getBalance());
    }

    @Test
    public void coinbaseTransactionAvailability() throws Exception {
        // Check that a coinbase transaction is only available to spend after NetworkParameters.getSpendableCoinbaseDepth() blocks.

        // Create a second wallet to receive the coinbase spend.
        Wallet wallet2 = new Wallet(unitTestParams);
        ECKey receiveKey = wallet2.freshReceiveKey();
        chain.addWallet(wallet2);

        Address addressToSendTo = receiveKey.toAddress(unitTestParams);

        // Create a block, sending the coinbase to the coinbaseTo address (which is in the wallet).
        Block b1 = unitTestParams.getGenesisBlock().createNextBlockWithCoinbase(wallet.currentReceiveKey().getPubKey());
        chain.add(b1);

        // Check a transaction has been received.
        assertNotNull(coinbaseTransaction);

        // The coinbase tx is not yet available to spend.
        assertEquals(Coin.ZERO, wallet.getBalance());
        assertEquals(wallet.getBalance(BalanceType.ESTIMATED), FIFTY_COINS);
        assertTrue(!coinbaseTransaction.isMature());

        // Attempt to spend the coinbase - this should fail as the coinbase is not mature yet.
        try {
            wallet.createSend(addressToSendTo, valueOf(49, 0));
            fail();
        } catch (InsufficientMoneyException e) {
        }

        // Check that the coinbase is unavailable to spend for the next spendableCoinbaseDepth - 2 blocks.
        for (int i = 0; i < unitTestParams.getSpendableCoinbaseDepth() - 2; i++) {
            // Non relevant tx - just for fake block creation.
            Transaction tx2 = createFakeTx(unitTestParams, COIN,
                new ECKey().toAddress(unitTestParams));

            Block b2 = createFakeBlock(blockStore, tx2).block;
            chain.add(b2);

            // Wallet still does not have the coinbase transaction available for spend.
            assertEquals(Coin.ZERO, wallet.getBalance());
            assertEquals(wallet.getBalance(BalanceType.ESTIMATED), FIFTY_COINS);

            // The coinbase transaction is still not mature.
            assertTrue(!coinbaseTransaction.isMature());

            // Attempt to spend the coinbase - this should fail.
            try {
                wallet.createSend(addressToSendTo, valueOf(49, 0));
                fail();
            } catch (InsufficientMoneyException e) {
            }
        }

        // Give it one more block - should now be able to spend coinbase transaction. Non relevant tx.
        Transaction tx3 = createFakeTx(unitTestParams, COIN, new ECKey().toAddress(unitTestParams));
        Block b3 = createFakeBlock(blockStore, tx3).block;
        chain.add(b3);

        // Wallet now has the coinbase transaction available for spend.
        assertEquals(wallet.getBalance(), FIFTY_COINS);
        assertEquals(wallet.getBalance(BalanceType.ESTIMATED), FIFTY_COINS);
        assertTrue(coinbaseTransaction.isMature());

        // Create a spend with the coinbase BTC to the address in the second wallet - this should now succeed.
        Transaction coinbaseSend2 = wallet.createSend(addressToSendTo, valueOf(49, 0));
        assertNotNull(coinbaseSend2);

        // Commit the coinbaseSpend to the first wallet and check the balances decrement.
        wallet.commitTx(coinbaseSend2);
        assertEquals(wallet.getBalance(BalanceType.ESTIMATED), COIN);
        // Available balance is zero as change has not been received from a block yet.
        assertEquals(wallet.getBalance(BalanceType.AVAILABLE), ZERO);

        // Give it one more block - change from coinbaseSpend should now be available in the first wallet.
        Block b4 = createFakeBlock(blockStore, coinbaseSend2).block;
        chain.add(b4);
        assertEquals(wallet.getBalance(BalanceType.AVAILABLE), COIN);

        // Check the balances in the second wallet.
        assertEquals(wallet2.getBalance(BalanceType.ESTIMATED), valueOf(49, 0));
        assertEquals(wallet2.getBalance(BalanceType.AVAILABLE), valueOf(49, 0));
    }

    // Some blocks from the test net.
    private static Block getBlock2() throws Exception {
        Block b2 = new Block(testNet);
        b2.setMerkleRoot(new Sha256Hash("50338165798f3ca34078299a051fcfff6f385ffb73ea34f605d072e15bfc78da"));
        b2.setHeight(2);
        b2.setNonce(61473024);
        b2.setTime(1406624502L);
        b2.setPrevBlockHash(new Sha256Hash("b3e028fe396ebaa9115812404d2d3ff65dce68f17ecc431d43745cd7ccc150b8"));
        assertEquals("adf17f66d545b7bdb9fc9c51e3d8355c36d41661e2d780ff614e01e1bd2c0e97", b2.getHashAsString());
        b2.verifyHeader();
        return b2;
    }

    private static Block getBlock1() throws Exception {
        Block b1 = new Block(testNet);
        b1.setMerkleRoot(new Sha256Hash("ab9df16bbb50b4a49d0a07026044b35f0cf91d35c351065e0d4c7c10dfe3027d"));
        b1.setHeight(1);
        b1.setNonce(1101988352);
        b1.setTime(1406624476);
        b1.setPrevBlockHash(new Sha256Hash("14dcedae9369344e0542270805a0cef3213f2b17f693d9b2acec76c1b7ed7fa3"));
        assertEquals("b3e028fe396ebaa9115812404d2d3ff65dce68f17ecc431d43745cd7ccc150b8", b1.getHashAsString());
        b1.verifyHeader();
        return b1;
    }

    @Test
    public void estimatedBlockTime() throws Exception {
        NetworkParameters params = MainNetParams.get();
        BlockChain prod = new BlockChain(params, new MemoryBlockStore(params));
        Date d = prod.estimateBlockTime(200000);
        // The actual date of block 200,000 was   2018-05-18 07:06:40
        assertEquals(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).parse("2018-05-17T22:06:40.000-0700"), d);
    }

    @Test
    public void falsePositives() throws Exception {
        double decay = AbstractBlockChain.FP_ESTIMATOR_ALPHA;
        assertTrue(0 == chain.getFalsePositiveRate()); // Exactly
        chain.trackFalsePositives(55);
        assertEquals(decay * 55, chain.getFalsePositiveRate(), 1e-4);
        chain.trackFilteredTransactions(550);
        double rate1 = chain.getFalsePositiveRate();
        // Run this scenario a few more time for the filter to converge
        for (int i = 1 ; i < 10 ; i++) {
            chain.trackFalsePositives(55);
            chain.trackFilteredTransactions(550);
        }

        // Ensure we are within 10%
        assertEquals(0.1, chain.getFalsePositiveRate(), 0.01);

        // Check that we get repeatable results after a reset
        chain.resetFalsePositiveEstimate();
        assertTrue(0 == chain.getFalsePositiveRate()); // Exactly

        chain.trackFalsePositives(55);
        assertEquals(decay * 55, chain.getFalsePositiveRate(), 1e-4);
        chain.trackFilteredTransactions(550);
        assertEquals(rate1, chain.getFalsePositiveRate(), 1e-4);
    }

    @Test
    public void rollbackBlockStore() throws Exception {
        // This test simulates an issue on Android, that causes the VM to crash while receiving a block, so that the
        // block store is persisted but the wallet is not.
        Block b1 = unitTestParams.getGenesisBlock().createNextBlock(coinbaseTo);
        Block b2 = b1.createNextBlock(coinbaseTo);
        // Add block 1, no frills.
        assertTrue(chain.add(b1));
        assertEquals(b1.cloneAsHeader(), chain.getChainHead().getHeader());
        assertEquals(1, chain.getBestChainHeight());
        assertEquals(1, wallet.getLastBlockSeenHeight());
        // Add block 2 while wallet is disconnected, to simulate crash.
        chain.removeWallet(wallet);
        assertTrue(chain.add(b2));
        assertEquals(b2.cloneAsHeader(), chain.getChainHead().getHeader());
        assertEquals(2, chain.getBestChainHeight());
        assertEquals(1, wallet.getLastBlockSeenHeight());
        // Add wallet back. This will detect the height mismatch and repair the damage done.
        chain.addWallet(wallet);
        assertEquals(b1.cloneAsHeader(), chain.getChainHead().getHeader());
        assertEquals(1, chain.getBestChainHeight());
        assertEquals(1, wallet.getLastBlockSeenHeight());
        // Now add block 2 correctly.
        assertTrue(chain.add(b2));
        assertEquals(b2.cloneAsHeader(), chain.getChainHead().getHeader());
        assertEquals(2, chain.getBestChainHeight());
        assertEquals(2, wallet.getLastBlockSeenHeight());
    }
}
