/*
 * Copyright 2019 ICON Foundation
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

package io.havah.test.vesting.score;

import foundation.icon.icx.Wallet;
import foundation.icon.icx.data.Address;
import foundation.icon.icx.data.TransactionResult;
import foundation.icon.icx.transport.jsonrpc.RpcObject;
import foundation.icon.icx.transport.jsonrpc.RpcValue;
import foundation.icon.test.ResultTimeoutException;
import foundation.icon.test.TransactionFailureException;
import foundation.icon.test.TransactionHandler;
import foundation.icon.test.score.Score;

import java.io.IOException;
import java.math.BigInteger;

import static foundation.icon.test.Env.LOG;

public class SampleTokenScore extends Score {

    public static SampleTokenScore mustDeploy(TransactionHandler txHandler, Wallet wallet,
                                              BigInteger decimals, BigInteger initialSupply)
            throws ResultTimeoutException, TransactionFailureException, IOException {
        LOG.infoEntering("deploy", "SampleToken");
        RpcObject params = new RpcObject.Builder()
                .put("_name", new RpcValue("MySampleToken"))
                .put("_symbol", new RpcValue("MST"))
                .put("_decimals", new RpcValue(decimals))
                .put("_initialSupply", new RpcValue(initialSupply))
                .build();
        Score score = txHandler.deploy(wallet, getFilePath("sample-token"), params);
        LOG.info("scoreAddr = " + score.getAddress());
        LOG.infoExiting();
        return new SampleTokenScore(score);
    }

    public SampleTokenScore(Score other) {
        super(other);
    }

    public BigInteger balanceOf(Address owner) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_owner", new RpcValue(owner))
                .build();
        return call("balanceOf", params).asInteger();
    }

    public TransactionResult transfer(Wallet wallet, Address to, BigInteger value)
            throws IOException, ResultTimeoutException {
        RpcObject.Builder builder = new RpcObject.Builder()
                .put("_to", new RpcValue(to))
                .put("_value", new RpcValue(value));
        return this.invokeAndWaitResult(wallet, "transfer", builder.build());
    }
}
