/*
 * Copyright 2020 ICON Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.havah.test.score;

import foundation.icon.icx.Wallet;
import foundation.icon.icx.data.Address;
import foundation.icon.icx.data.Bytes;
import foundation.icon.icx.transport.jsonrpc.RpcObject;
import foundation.icon.icx.transport.jsonrpc.RpcValue;
import foundation.icon.test.ResultTimeoutException;
import foundation.icon.test.TransactionFailureException;
import foundation.icon.test.TransactionHandler;
import foundation.icon.test.score.Score;

import java.io.IOException;
import java.math.BigInteger;

import static foundation.icon.test.Env.LOG;

public class HSP721TokenScore extends Score {
    public HSP721TokenScore(Score other) {
        super(other);
    }

    public static HSP721TokenScore mustDeploy(TransactionHandler txHandler, Wallet owner)
            throws ResultTimeoutException, TransactionFailureException, IOException {
        LOG.infoEntering("deploy", "HSP721Token");
        RpcObject params = new RpcObject.Builder()
                .put("_name", new RpcValue("HSP721Token"))
                .put("_symbol", new RpcValue("NFT"))
                .build();
        Score score = txHandler.deploy(owner, getFilePath("hsp721-token"), params);
        LOG.info("scoreAddr = " + score.getAddress());
        LOG.infoExiting();
        return new HSP721TokenScore(score);
    }

    public Address ownerOf(BigInteger tokenId) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_tokenId", new RpcValue(tokenId))
                .build();
        return call("ownerOf", params).asAddress();
    }

    public Address getApproved(BigInteger tokenId) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_tokenId", new RpcValue(tokenId))
                .build();
        return call("getApproved", params).asAddress();
    }

    public BigInteger balanceOf(Address owner) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_owner", new RpcValue(owner))
                .build();
        return call("balanceOf", params).asInteger();
    }

    public BigInteger totalSupply() throws IOException {
        return call("totalSupply", null).asInteger();
    }

    public BigInteger tokenByIndex(int index) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_index", new RpcValue(BigInteger.valueOf(index)))
                .build();
        return call("tokenByIndex", params).asInteger();
    }

    public BigInteger tokenOfOwnerByIndex(Address owner, int index) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_owner", new RpcValue(owner))
                .put("_index", new RpcValue(BigInteger.valueOf(index)))
                .build();
        return call("tokenOfOwnerByIndex", params).asInteger();
    }

    public Bytes mint(Wallet wallet, BigInteger tokenId) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_tokenId", new RpcValue(tokenId))
                .build();
        return invoke(wallet, "mint", params);
    }

    public Bytes burn(Wallet wallet, BigInteger tokenId) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_tokenId", new RpcValue(tokenId))
                .build();
        return invoke(wallet, "burn", params);
    }

    public Bytes approve(Wallet wallet, Address to, BigInteger tokenId) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_approved", new RpcValue(to))
                .put("_tokenId", new RpcValue(tokenId))
                .build();
        return invoke(wallet, "approve", params);
    }

    public Bytes safeTransferFrom(Wallet wallet, Address from, Address to, BigInteger tokenId, byte[] data) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_from", new RpcValue(from))
                .put("_to", new RpcValue(to))
                .put("_tokenId", new RpcValue(tokenId))
                .put("_data", new RpcValue(data))
                .build();
        return invoke(wallet, "safeTransferFrom", params);
    }

    public Bytes transferFrom(Wallet wallet, Address from, Address to, BigInteger tokenId) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_from", new RpcValue(from))
                .put("_to", new RpcValue(to))
                .put("_tokenId", new RpcValue(tokenId))
                .build();
        return invoke(wallet, "transferFrom", params);
    }
}
