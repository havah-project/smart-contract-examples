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

package foundation.icon.test.score;

import foundation.icon.icx.Wallet;
import foundation.icon.icx.data.Address;
import foundation.icon.icx.data.IconAmount;
import foundation.icon.icx.data.TransactionResult;
import foundation.icon.icx.transport.jsonrpc.RpcItem;
import foundation.icon.icx.transport.jsonrpc.RpcObject;
import foundation.icon.icx.transport.jsonrpc.RpcValue;
import foundation.icon.test.Constants;
import foundation.icon.test.ResultTimeoutException;
import foundation.icon.test.TransactionFailureException;
import foundation.icon.test.TransactionHandler;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static foundation.icon.test.Env.LOG;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MultiSigWalletScore extends Score {

    public MultiSigWalletScore(Score other) {
        super(other);
    }

    public static MultiSigWalletScore mustDeploy(TransactionHandler txHandler, Wallet wallet,
                                                 Address[] walletOwners, int required)
            throws IOException, TransactionFailureException, ResultTimeoutException {
        LOG.infoEntering("deploy", "MultiSigWallet");
        String initialOwners = Arrays.stream(walletOwners)
                .map(Address::toString)
                .collect(Collectors.joining(","));
        RpcObject params = new RpcObject.Builder()
                .put("_walletOwners", new RpcValue(initialOwners))
                .put("_required", new RpcValue(BigInteger.valueOf(required)))
                .build();
        Score score = txHandler.deploy(wallet, getFilePath("multisig-wallet"), params);
        LOG.info("scoreAddr = " + score.getAddress());
        LOG.infoExiting();
        return new MultiSigWalletScore(score);
    }

    public TransactionResult submitIcxTransaction(Wallet fromWallet, Address dest, BigInteger value, String description)
            throws IOException, ResultTimeoutException {
        RpcObject params = new RpcObject.Builder()
                .put("_destination", new RpcValue(dest))
                .put("_value", new RpcValue(value))
                .put("_description", new RpcValue(description))
                .build();
        return invokeAndWaitResult(fromWallet, "submitTransaction", params);
    }

    public TransactionResult confirmTransaction(Wallet fromWallet, BigInteger txId)
            throws IOException, ResultTimeoutException {
        RpcObject params = new RpcObject.Builder()
                .put("_transactionId", new RpcValue(txId))
                .build();
        TransactionResult result = invokeAndWaitResult(fromWallet, "confirmTransaction", params);
        ensureConfirmation(result, fromWallet.getAddress(), txId);
        return result;
    }

    public TransactionResult revokeTransaction(Wallet fromWallet, BigInteger txId)
            throws IOException, ResultTimeoutException {
        RpcObject params = new RpcObject.Builder()
                .put("_transactionId", new RpcValue(txId))
                .build();
        return invokeAndWaitResult(fromWallet, "revokeTransaction", params);
    }

    public TransactionResult addWalletOwner(Wallet fromWallet, Address newOwner, String description)
            throws IOException, ResultTimeoutException {
        String methodParams = String.format("[{\"name\": \"_walletOwner\", \"type\": \"Address\", \"value\": \"%s\"}]", newOwner);
        RpcObject params = new RpcObject.Builder()
                .put("_destination", new RpcValue(getAddress()))
                .put("_method", new RpcValue("addWalletOwner"))
                .put("_params", new RpcValue(methodParams))
                .put("_description", new RpcValue(description))
                .build();
        return invokeAndWaitResult(fromWallet, "submitTransaction", params);
    }

    public TransactionResult removeWalletOwner(Wallet fromWallet, Address owner, String description)
            throws IOException, ResultTimeoutException {
        String methodParams = String.format("[{\"name\": \"_walletOwner\", \"type\": \"Address\", \"value\": \"%s\"}]", owner);
        RpcObject params = new RpcObject.Builder()
                .put("_destination", new RpcValue(getAddress()))
                .put("_method", new RpcValue("removeWalletOwner"))
                .put("_params", new RpcValue(methodParams))
                .put("_description", new RpcValue(description))
                .build();
        return invokeAndWaitResult(fromWallet, "submitTransaction", params);
    }

    public TransactionResult replaceWalletOwner(Wallet fromWallet, Address oldOwner, Address newOwner, String description)
            throws IOException, ResultTimeoutException {
        String methodParams = String.format(
                "[{\"name\": \"_walletOwner\", \"type\": \"Address\", \"value\": \"%s\"},"
                + "{\"name\": \"_newWalletOwner\", \"type\": \"Address\", \"value\": \"%s\"}]", oldOwner, newOwner);
        RpcObject params = new RpcObject.Builder()
                .put("_destination", new RpcValue(getAddress()))
                .put("_method", new RpcValue("replaceWalletOwner"))
                .put("_params", new RpcValue(methodParams))
                .put("_description", new RpcValue(description))
                .build();
        return invokeAndWaitResult(fromWallet, "submitTransaction", params);
    }

    public TransactionResult changeRequirement(Wallet fromWallet, int required, String description)
            throws IOException, ResultTimeoutException {
        String methodParams = String.format("[{\"name\": \"_required\", \"type\": \"int\", \"value\": \"%d\"}]", required);
        RpcObject params = new RpcObject.Builder()
                .put("_destination", new RpcValue(getAddress()))
                .put("_method", new RpcValue("changeRequirement"))
                .put("_params", new RpcValue(methodParams))
                .put("_description", new RpcValue(description))
                .build();
        return invokeAndWaitResult(fromWallet, "submitTransaction", params);
    }

    public BigInteger getTransactionId(TransactionResult result) throws IOException {
        TransactionResult.EventLog event = findEventLog(result, getAddress(), "Submission(int)");
        if (event != null) {
            return event.getIndexed().get(1).asInteger();
        }
        throw new IOException("Failed to get transactionId.");
    }

    public void ensureConfirmation(TransactionResult result, Address sender, BigInteger txId) throws IOException {
        TransactionResult.EventLog event = findEventLog(result, getAddress(), "Confirmation(Address,int)");
        if (event != null) {
            Address _sender = event.getIndexed().get(1).asAddress();
            BigInteger _txId = event.getIndexed().get(2).asInteger();
            if (sender.equals(_sender) && txId.equals(_txId)) {
                return; // ensured
            }
        }
        throw new IOException("Failed to get Confirmation.");
    }

    public void ensureRevocation(TransactionResult result, Address sender, BigInteger txId) throws IOException {
        TransactionResult.EventLog event = findEventLog(result, getAddress(), "Revocation(Address,int)");
        if (event != null) {
            Address _sender = event.getIndexed().get(1).asAddress();
            BigInteger _txId = event.getIndexed().get(2).asInteger();
            if (sender.equals(_sender) && txId.equals(_txId)) {
                return; // ensured
            }
        }
        throw new IOException("Failed to get Revocation.");
    }

    public void ensureIcxTransfer(TransactionResult result, Address from, Address to, long value) throws IOException {
        TransactionResult.EventLog event = findEventLog(result, Constants.SYSTEM_ADDRESS, "Transfer(Address,Address,int)");
        if (event != null) {
            BigInteger icxValue = IconAmount.of(BigInteger.valueOf(value), IconAmount.Unit.ICX).toLoop();
            Address _from = event.getIndexed().get(1).asAddress();
            Address _to = event.getIndexed().get(2).asAddress();
            BigInteger _value = event.getData().get(0).asInteger();
            if (from.equals(_from) && to.equals(_to) && icxValue.equals(_value)) {
                return; // ensured
            }
        }
        throw new IOException("Failed to get Transfer.");
    }

    public void ensureExecution(TransactionResult result, BigInteger txId) throws IOException {
        TransactionResult.EventLog event = findEventLog(result, getAddress(), "Execution(int)");
        if (event != null) {
            BigInteger _txId = event.getIndexed().get(1).asInteger();
            if (txId.equals(_txId)) {
                return; // ensured
            }
        }
        throw new IOException("Failed to get Execution.");
    }

    public void ensureWalletOwnerAddition(TransactionResult result, Address address) throws IOException {
        TransactionResult.EventLog event = findEventLog(result, getAddress(), "WalletOwnerAddition(Address)");
        if (event != null) {
            Address _address = event.getIndexed().get(1).asAddress();
            if (address.equals(_address)) {
                return; // ensured
            }
        }
        throw new IOException("Failed to get WalletOwnerAddition.");
    }

    public void ensureWalletOwnerRemoval(TransactionResult result, Address address) throws IOException {
        TransactionResult.EventLog event = findEventLog(result, getAddress(), "WalletOwnerRemoval(Address)");
        if (event != null) {
            Address _address = event.getIndexed().get(1).asAddress();
            if (address.equals(_address)) {
                return; // ensured
            }
        }
        throw new IOException("Failed to get WalletOwnerRemoval.");
    }

    public void ensureRequirementChange(TransactionResult result, Integer required) throws IOException {
        TransactionResult.EventLog event = findEventLog(result, getAddress(), "RequirementChange(int)");
        if (event != null) {
            BigInteger _required = event.getData().get(0).asInteger();
            if (required.equals(_required.intValue())) {
                return; // ensured
            }
        }
        throw new IOException("Failed to get RequirementChange.");
    }

    public void ensureOwners(Address... expected) throws IOException {
        List<RpcItem> items = getWalletOwners().asArray().asList();
        assertAddressEquals(items, expected);
    }

    private RpcItem getWalletOwners() throws IOException {
        return this.call("getWalletOwners", null);
    }

    public void ensureConfirmationCount(BigInteger txId, int count) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_transactionId", new RpcValue(txId))
                .build();
        assertEquals(count, this.call("getConfirmationCount", params).asInteger().intValue());
    }

    public void getConfirmationsAndCheck(BigInteger txId, Address... expected) throws IOException {
        List<RpcItem> items = getConfirmations(txId).asArray().asList();
        assertAddressEquals(items, expected);
    }

    private RpcItem getConfirmations(BigInteger txId) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_transactionId", new RpcValue(txId))
                .build();
        return this.call("getConfirmations", params);
    }

    public void ensureTransactionCount(int pending, int executed) throws IOException {
        assertEquals(pending, getTransactionCount(true, false));
        assertEquals(executed, getTransactionCount(false, true));
        assertEquals(pending + executed, getTransactionCount(true, true));
    }

    private int getTransactionCount(boolean pending, boolean executed) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_pending", new RpcValue(pending))
                .put("_executed", new RpcValue(executed))
                .build();
        return this.call("getTransactionCount", params).asInteger().intValue();
    }

    public void ensurePendingTransactionIds(int offset, int count, BigInteger... expected) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_offset", new RpcValue(BigInteger.valueOf(offset)))
                .put("_count", new RpcValue(BigInteger.valueOf(count)))
                .put("_pending", new RpcValue(true))
                .put("_executed", new RpcValue(false))
                .build();
        List<RpcItem> items = this.call("getTransactionList", params).asArray().asList();
        assertEquals(expected.length, items.size());
        BigInteger[] actual = new BigInteger[items.size()];
        for (int i = 0; i < actual.length; i++) {
            actual[i] = items.get(i).asObject().getItem("_transactionId").asInteger();
        }
        assertArrayEquals(expected, actual);
    }

    private void assertAddressEquals(List<RpcItem> items, Address[] expected) {
        assertEquals(expected.length, items.size());
        Address[] actual = new Address[items.size()];
        for (int i = 0; i < actual.length; i++) {
            actual[i] = items.get(i).asAddress();
        }
        assertArrayEquals(expected, actual);
    }
}
