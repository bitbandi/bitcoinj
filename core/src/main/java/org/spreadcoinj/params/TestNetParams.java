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
        maxTarget = Utils.decodeCompactBits(0x1e0fffffL);
        port = 51678;
        addressHeader = 111;
        p2shHeader = 196;
        acceptableAddressCodes = new int[] { addressHeader, p2shHeader };
        dumpedPrivateKeyHeader = 239;
        genesisBlock.setTime(1406620000L); // real: 1406620000L
        genesisBlock.setDifficultyTarget(0x1e0fffffL);
        genesisBlock.setNonce(0);
        genesisBlock.setHeight(0);
        spendableCoinbaseDepth = 100;
        subsidyDecreaseBlockCount = 2000000;
        String genesisHash = genesisBlock.getHashAsString();
        checkState(genesisHash.equals("14dcedae9369344e0542270805a0cef3213f2b17f693d9b2acec76c1b7ed7fa3"));
        alertSigningKey = Utils.HEX.decode("03f5cee48df4990af166d539f1cc42367034558d62e765a30ed3228ec418cc46bb");

        dnsSeeds = new String[] {
        };

        firstHardforkBlock = 0;
        secondHardforkBlock = 0;
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
