/*
 * Copyright 2013 Google Inc.
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
import org.spreadcoinj.core.Sha256Hash;
import org.spreadcoinj.core.Utils;

import static com.google.common.base.Preconditions.checkState;

/**
 * Parameters for the main production network on which people trade goods and services.
 */
public class MainNetParams extends NetworkParameters {
    public MainNetParams() {
        super();
        interval = INTERVAL;
        targetTimespan = TARGET_TIMESPAN;
        maxTarget = Utils.decodeCompactBits(0x1e0fffffL);
        dumpedPrivateKeyHeader = 191;
        addressHeader = 63;
        p2shHeader = 5;
        acceptableAddressCodes = new int[] { addressHeader, p2shHeader };
        port = 41678;
        packetMagic = 0x4f3c5cbbL;
        genesisBlock.setDifficultyTarget(0x1e0fffffL);
        genesisBlock.setTime(1406620000L);
        genesisBlock.setNonce(0);
        genesisBlock.setHeight(0);
        id = ID_MAINNET;
        subsidyDecreaseBlockCount = 2000000;
        spendableCoinbaseDepth = 100;
        String genesisHash = genesisBlock.getHashAsString();
        checkState(genesisHash.equals("14dcedae9369344e0542270805a0cef3213f2b17f693d9b2acec76c1b7ed7fa3"),
                genesisHash);

        // This contains (at a minimum) the blocks which are not BIP30 compliant. BIP30 changed how duplicate
        // transactions are handled. Duplicated transactions could occur in the case where a coinbase had the same
        // extraNonce and the same outputs but appeared at different heights, and greatly complicated re-org handling.
        // Having these here simplifies block connection logic considerably.
//        checkpoints.put(91722, new Sha256Hash("00000000000271a2dc26e7667f8419f2e15416dc6955e5a6c6cdf3f2574dd08e"));

        dnsSeeds = new String[] {
                "dnsseed.spreadcoin.net",
        };
        addrSeeds = new int[] {
                0x203aedcf, 0x95b35a4e, 0x7a0b0732, 0xc63a6f3e, 0x22f4fbac, 0x60a5c0b2, 0x41546f76, 0x2f3d6853,
                0x14d8b751, 0xa0845a45, 0x3caa4a4c, 0x72f68368, 0xfd351fb0, 0x75845a45, 0x3e6f3ac6, 0xcbe5ac45,
                0xa3babc4e, 0x3baa4a4c, 0x43e49850, 0x7ab969b7, 0x2a32e52e, 0x31ffff54, 0xdc04c5d9, 0x16e6e6b4,
                0x8cd0a52e, 0x0bcc39ae, 0x6d6b6f12, 0x1a7f6f12, 0x776d6f12, 0x9a02c162, 0xf13014d4, 0x16d40151,
                0x60096f12, 0x36056112, 0xcca7bc4e, 0x63e2205a, 0x70d01ad8, 0x88bed75b, 0x17132b1f, 0x8d466659,
                0xb4e5e352, 0x47f4cd48, 0x22f7185e, 0xe6537057, 0x7ab86344, 0xe5591518, 0xe8a9c418, 0x6db09d1b,
        };

        firstHardforkBlock = 2200;
        secondHardforkBlock = 43000;
        thirdHardforkBlock = 999999;
    }

    private static MainNetParams instance;
    public static synchronized MainNetParams get() {
        if (instance == null) {
            instance = new MainNetParams();
        }
        return instance;
    }

    @Override
    public String getPaymentProtocolId() {
        return PAYMENT_PROTOCOL_ID_MAINNET;
    }
}
