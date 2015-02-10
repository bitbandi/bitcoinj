/*
 * Copyright 2013 Google Inc.
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

package org.spreadcoinj.params;

import org.spreadcoinj.core.NetworkParameters;
import org.spreadcoinj.core.Utils;

import static com.google.common.base.Preconditions.checkState;

/**
 * Parameters for the testnet, a separate public instance of Bitcoin that has relaxed rules suitable for development
 * and testing of applications and new Bitcoin versions.
 */
public class TestNetParams extends NetworkParameters {
    public TestNetParams() {
        super();
        id = ID_TESTNET;
        // Genesis hash is ?
        packetMagic = 0xc2e3cbfa;
        interval = INTERVAL;
        targetTimespan = TARGET_TIMESPAN;
        maxTarget = Utils.decodeCompactBits(0x1f3fffffL);
        port = 51678;
        addressHeader = 111;
        p2shHeader = 196;
        acceptableAddressCodes = new int[] { addressHeader, p2shHeader };
        dumpedPrivateKeyHeader = 239;
        genesisBlock.setTime(1423400000L);
        genesisBlock.setDifficultyTarget(0x1f3fffffL);
        genesisBlock.setNonce(0);
        genesisBlock.setHeight(0);
        spendableCoinbaseDepth = 100;
        subsidyDecreaseBlockCount = 2000000;
        String genesisHash = genesisBlock.getHashAsString();
        checkState(genesisHash.equals("192c73a268208dab77debebc64844506a70b08c14c032c6681a164e5cb0180f7"));
        alertSigningKey = Utils.HEX.decode("03f5cee48df4990af166d539f1cc42367034558d62e765a30ed3228ec418cc46bb");

        dnsSeeds = new String[] {
        };
        addrSeeds = new int[] {
                0x7e532468, 0x721afb60, 0xf75dc451, 0xb999404f, 0x2b033649, 0x64f17eb0, 0x72f68368, 0x6c59b8ad,
        };

        firstHardforkBlock = 0;
        secondHardforkBlock = 0;
        thirdHardforkBlock = 700;
    }

    private static TestNetParams instance;
    public static synchronized TestNetParams get() {
        if (instance == null) {
            instance = new TestNetParams();
        }
        return instance;
    }

    @Override
    public String getPaymentProtocolId() {
        return PAYMENT_PROTOCOL_ID_TESTNET;
    }
}
