package io.havah.test.vesting.cases;

import foundation.icon.icx.IconService;
import foundation.icon.icx.KeyWallet;
import foundation.icon.icx.Wallet;
import foundation.icon.icx.data.Address;
import foundation.icon.icx.data.Block;
import foundation.icon.icx.data.Bytes;
import foundation.icon.icx.data.TransactionResult;
import foundation.icon.icx.transport.http.HttpProvider;
import foundation.icon.test.Env;
import foundation.icon.test.ResultTimeoutException;
import foundation.icon.test.TestBase;
import foundation.icon.test.TransactionHandler;
import io.havah.test.vesting.score.SampleTokenScore;
import io.havah.test.vesting.score.VestingScore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static foundation.icon.test.Env.LOG;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class VestingTest extends TestBase {
    private static final Address ZERO_ADDRESS = new Address("hx0000000000000000000000000000000000000000");
    private static final BigInteger DAYILY = BigInteger.valueOf(3);
    private static final BigInteger WEEKLY = BigInteger.valueOf(4);
    private static final BigInteger MONTHLY = BigInteger.valueOf(5);
    private static final BigInteger YEARLY = BigInteger.valueOf(6);
    static final long ONE_DAY = 86400_000_000L;
    private static IconService iconService;
    private static TransactionHandler txHandler;

    private static Wallet govWallet;
    private static Wallet[] owners = new KeyWallet[5];
    private static VestingScore vesting;
    private static SampleTokenScore hsp20token;

    @BeforeAll
    static void setup() throws Exception {
        Env.Chain chain = Env.getDefaultChain();
        iconService = new IconService(new HttpProvider(chain.getEndpointURL(3)));
        txHandler = new TransactionHandler(iconService, chain);
        govWallet = txHandler.getChain().godWallet;

        BigInteger amount = ICX.multiply(BigInteger.valueOf(300));
        for (int i = 0; i < owners.length; i++) {
            owners[i] = KeyWallet.create();
            Bytes txHash = txHandler.transfer(owners[i].getAddress(), amount);
            assertSuccess(txHandler.getResult(txHash));
        }

        LOG.info("deploy Vesting");
        vesting = VestingScore.mustDeploy(txHandler, govWallet);
        Bytes txHash = txHandler.transfer(vesting.getAddress(), amount);
        assertSuccess(txHandler.getResult(txHash));

        hsp20token = SampleTokenScore.mustDeploy(txHandler, govWallet, BigInteger.valueOf(18), amount);
        assertSuccess(hsp20token.transfer(govWallet, vesting.getAddress(), amount));
        LOG.info("airdrop balanceOf : " + hsp20token.balanceOf(vesting.getAddress()));
    }

    protected static BigInteger _getTimestamp() throws IOException {
        Block lastBlk = iconService.getLastBlock().execute();
        return lastBlk.getTimestamp();
    }

    protected void _waitUtilTime(BigInteger timestamp) throws Exception {
        var now = _getTimestamp();
        while (now.compareTo(timestamp) < 0) {
            LOG.info("now(" + now + ") wait(" + timestamp + ")");
            Thread.sleep(1200);
            now = _getTimestamp();
        }
    }

    protected void _registerOnetimeVesting(VestingScore score, Wallet wallet, Address token, BigInteger startTime,
                                           List accounts, boolean success) throws IOException, ResultTimeoutException {
        TransactionResult result = score.registerOnetimeVesting(wallet, token, startTime, accounts);
        if(success) {
            assertSuccess(result);
        } else {
            assertFailure(result);
        }
    }

    protected void _registerLinearVesting(VestingScore score, Wallet wallet, Address token, BigInteger startTime,
            BigInteger endTime, List accounts, boolean success) throws IOException, ResultTimeoutException {
        TransactionResult result = score.registerLinearVesting(wallet, token, startTime, endTime, accounts);
        if(success) {
            assertSuccess(result);
        } else {
            assertFailure(result);
        }
    }

    protected void _registerPeriodicVesting(VestingScore score, Wallet wallet, Address token, BigInteger startTime,
                                          BigInteger endTime, BigInteger intervalTime, List accounts, boolean success)
            throws IOException, ResultTimeoutException {
        TransactionResult result = score.registerPeriodicVesting(wallet, token, startTime, endTime, intervalTime, accounts);
        if(success) {
            assertSuccess(result);
        } else {
            assertFailure(result);
        }
    }

    protected void _registerConditionalVesting(VestingScore score, Wallet wallet, BigInteger type,
                                               BigInteger[] month, BigInteger[] day, BigInteger[] weekday, BigInteger[] hour,
                                               Address token, BigInteger startTime, BigInteger endTime, BigInteger intervalTime,
                                               List accounts, boolean success) throws IOException, ResultTimeoutException {
        TransactionResult result = score.registerConditionalVesting(wallet, type, token, startTime, endTime, intervalTime, accounts, month, day, weekday, hour);
        if(success) {
            assertSuccess(result);
        } else {
            assertFailure(result);
        }
    }

    protected void _claim(VestingScore score, Wallet wallet, BigInteger id, boolean success) throws IOException, ResultTimeoutException {
        TransactionResult result = score.claim(wallet, id);
        if(success) {
            assertSuccess(result);
        } else {
            assertFailure(result);
        }
    }

    protected void _addVestingAccounts(VestingScore score, Wallet wallet, BigInteger id, List accounts, boolean success) throws IOException, ResultTimeoutException {
        TransactionResult result = score.addVestingAccounts(wallet, id, accounts);
        if(success) {
            assertSuccess(result);
        } else {
            assertFailure(result);
        }
    }

    protected void _removeVestingAccounts(VestingScore score, Wallet wallet, BigInteger id, Address[] accounts, boolean success) throws IOException, ResultTimeoutException {
        TransactionResult result = score.removeVestingAccounts(wallet, id, accounts);
        if(success) {
            assertSuccess(result);
        } else {
            assertFailure(result);
        }
    }

    @Test
    void registerOnetimeVestingTest() throws Exception {
        LOG.infoEntering("vesting", "registerOnetimeVestingTest");

        BigInteger time = _getTimestamp();
        BigInteger startTime = time.add(BigInteger.valueOf(10 * 1_000_000L));

        LOG.info(">>> registerOnetimeVesting");
        BigInteger amount = ICX.multiply(BigInteger.valueOf(2));
        List account = new ArrayList();
        account.add(Map.of(
                "address", owners[0].getAddress(),
                "eachAmount", BigInteger.ZERO,
                "totalAmount", amount
        ));
        _registerOnetimeVesting(vesting, govWallet, ZERO_ADDRESS, startTime, account, true);
        account.add(Map.of(
                "address", owners[1].getAddress(),
                "eachAmount", amount,
                "totalAmount", BigInteger.ZERO
        ));
        _registerOnetimeVesting(vesting, govWallet, ZERO_ADDRESS, startTime, account, false);

        BigInteger id = vesting.lastId();

        LOG.info("info : " + vesting.info(id));
        BigInteger claimable = vesting.claimableAmount(id, owners[0].getAddress());
        LOG.info("claimableAmount : " + claimable);
        assertEquals(BigInteger.ZERO, claimable);

        _waitUtilTime(startTime);

        claimable = vesting.claimableAmount(id, owners[0].getAddress());
        LOG.info("claimableAmount : " + claimable);
        assertEquals(amount, claimable);

        _claim(vesting, owners[0], id, true);

        LOG.infoExiting();
    }

    @Test
    void registerLinearVestingTest() throws Exception {
        LOG.infoEntering("vesting", "registerLinearVestingTest");

        BigInteger time = _getTimestamp();
        BigInteger startTime = time;
        BigInteger endTime = time.add(BigInteger.valueOf(20 * 1_000_000L));
        BigInteger amount = ICX.multiply(BigInteger.valueOf(2));

        List account = new ArrayList();
        account.add(Map.of(
                "address", owners[0].getAddress(),
                "eachAmount", BigInteger.ZERO,
                "totalAmount", amount
        ));
        _registerLinearVesting(vesting, govWallet, hsp20token.getAddress(), startTime, endTime, account, true);

        BigInteger id = vesting.lastId();

        LOG.info("claimableAmount : " + vesting.claimableAmount(id, owners[0].getAddress()));
        _waitUtilTime(time.add(BigInteger.valueOf(10 * 1_000_000L)));
        LOG.info("claimableAmount : " + vesting.claimableAmount(id, owners[0].getAddress()));

        _claim(vesting, owners[0], id, true);

        _waitUtilTime(endTime.add(BigInteger.valueOf(1_000_000L)));
        LOG.info("claimableAmount : " + vesting.claimableAmount(id, owners[0].getAddress()));

        _claim(vesting, owners[0], id, true);
        LOG.info("claimableAmount : " + vesting.claimableAmount(id, owners[0].getAddress()));

        LOG.infoExiting();
    }

    @Test
    void registerPeriodicVestingTest() throws Exception {
        LOG.infoEntering("vesting", "registerPeriodicVestingTest");

        BigInteger time = _getTimestamp();
        BigInteger startTime = _getTimestamp();
        BigInteger endTime = time.add(BigInteger.valueOf(20 * 1_000_000L));
        BigInteger interval = BigInteger.valueOf(5 * 1_000_000L);
        BigInteger amount = ICX.multiply(BigInteger.valueOf(2));

        List account = new ArrayList();
        account.add(Map.of(
                "address", owners[0].getAddress(),
                "eachAmount", BigInteger.ZERO,
                "totalAmount", amount
        ));
        account.add(Map.of(
                "address", owners[1].getAddress(),
                "eachAmount", amount,
                "totalAmount", BigInteger.ZERO
        ));
        _registerPeriodicVesting(vesting, govWallet, ZERO_ADDRESS, startTime, endTime, interval, account, true);

        BigInteger id = vesting.lastId();

        LOG.info("claimableAmount[0] : " + vesting.claimableAmount(id, owners[0].getAddress()));
        LOG.info("claimableAmount[1] : " + vesting.claimableAmount(id, owners[1].getAddress()));
        _waitUtilTime(time.add(BigInteger.valueOf(10 * 1_000_000L)));
        LOG.info("claimableAmount[0] : " + vesting.claimableAmount(id, owners[0].getAddress()));
        LOG.info("claimableAmount[1] : " + vesting.claimableAmount(id, owners[1].getAddress()));

        _claim(vesting, owners[0], id, true);
        _claim(vesting, owners[1], id, true);

        _waitUtilTime(endTime.add(BigInteger.valueOf(1_000_000L)));
        LOG.info("claimableAmount[0] : " + vesting.claimableAmount(id, owners[0].getAddress()));
        LOG.info("claimableAmount[1] : " + vesting.claimableAmount(id, owners[1].getAddress()));

        _claim(vesting, owners[0], id, true);
        _claim(vesting, owners[1], id, true);
        LOG.info("claimableAmount[0] : " + vesting.claimableAmount(id, owners[0].getAddress()));
        LOG.info("claimableAmount[1] : " + vesting.claimableAmount(id, owners[1].getAddress()));

        LOG.infoExiting();
    }

    @Test
    void registerDailyConditionalVestingTest() throws Exception {
        LOG.infoEntering("vesting", "registerDailyConditionalVestingTest");

        BigInteger time = _getTimestamp();
        BigInteger startTime = _getTimestamp();
        BigInteger endTime = time.add(BigInteger.valueOf(ONE_DAY * 1));
        BigInteger interval = BigInteger.ZERO;
        BigInteger amount = ICX.multiply(BigInteger.valueOf(2));

        List account = new ArrayList();
        account.add(Map.of(
                "address", owners[0].getAddress(),
                "eachAmount", BigInteger.ZERO,
                "totalAmount", amount
        ));
        account.add(Map.of(
                "address", owners[1].getAddress(),
                "eachAmount", amount,
                "totalAmount", BigInteger.ZERO
        ));

        BigInteger[] hour = new BigInteger[] { BigInteger.valueOf(24) };
        _registerConditionalVesting(vesting, govWallet, DAYILY, null, null, null, hour, ZERO_ADDRESS, startTime, endTime, interval, account, false);

        hour = new BigInteger[] { BigInteger.valueOf(13) };
        _registerConditionalVesting(vesting, govWallet, DAYILY, null, null, null, hour, ZERO_ADDRESS, startTime, endTime, interval, account, true);

        BigInteger id = vesting.lastId();

        LOG.info("info : " + vesting.info(id));
        LOG.info("vestingTimes" + vesting.vestingTimes(id));

        LOG.infoExiting();
    }

    @Test
    void registerWeeklyConditionalVestingTest() throws Exception {
        LOG.infoEntering("vesting", "registerWeeklyConditionalVestingTest");

        BigInteger time = _getTimestamp();
        BigInteger startTime = _getTimestamp();
        BigInteger endTime = time.add(BigInteger.valueOf(ONE_DAY * 10));
        BigInteger interval = BigInteger.ZERO;
        BigInteger amount = ICX.multiply(BigInteger.valueOf(2));

        List account = new ArrayList();
        account.add(Map.of(
                "address", owners[0].getAddress(),
                "eachAmount", BigInteger.ZERO,
                "totalAmount", amount
        ));
        account.add(Map.of(
                "address", owners[1].getAddress(),
                "eachAmount", amount,
                "totalAmount", BigInteger.ZERO
        ));

        BigInteger[] hour = new BigInteger[] { BigInteger.valueOf(9), BigInteger.valueOf(16) };
        BigInteger[] weekday = new BigInteger[] { BigInteger.valueOf(6) };

        _registerConditionalVesting(vesting, govWallet, WEEKLY, null, null, weekday, hour, ZERO_ADDRESS, startTime, endTime, interval, account, true);
        BigInteger id = vesting.lastId();
        LOG.info("info : " + vesting.info(id));
        LOG.info("vestedTimes" + vesting.vestingTimes(id));

        LOG.infoExiting();
    }

    @Test
    void registerMonthlyConditionalVestingTest() throws Exception {
        LOG.infoEntering("vesting", "registerMonthlyConditionalVestingTest");

        BigInteger time = _getTimestamp();
        BigInteger startTime = _getTimestamp();
        BigInteger endTime = time.add(BigInteger.valueOf(ONE_DAY * 35 * 7));
        BigInteger interval = BigInteger.ZERO;
        BigInteger amount = ICX.multiply(BigInteger.valueOf(2));

        List account = new ArrayList();
        account.add(Map.of(
                "address", owners[0].getAddress(),
                "eachAmount", BigInteger.ZERO,
                "totalAmount", amount
        ));
        account.add(Map.of(
                "address", owners[1].getAddress(),
                "eachAmount", amount,
                "totalAmount", BigInteger.ZERO
        ));

        BigInteger[] hour = new BigInteger[] { BigInteger.valueOf(9) };
        BigInteger[] day = new BigInteger[] { BigInteger.valueOf(36) };
        _registerConditionalVesting(vesting, govWallet, MONTHLY, null, day, null, hour, ZERO_ADDRESS, startTime, endTime, interval, account, false);

        day = new BigInteger[] { BigInteger.valueOf(1) };
        _registerConditionalVesting(vesting, govWallet, MONTHLY, null, day, null, hour, ZERO_ADDRESS, startTime, endTime, interval, account, true);

        BigInteger id = vesting.lastId();

        LOG.info("info : " + vesting.info(id));
        LOG.info("vestedTimes" + vesting.vestingTimes(id));

        LOG.infoExiting();
    }

    @Test
    void registerYearlyConditionalVestingTest() throws Exception {
        LOG.infoEntering("vesting", "registerYearlyConditionalVestingTest");

        BigInteger time = _getTimestamp();
        BigInteger startTime = _getTimestamp();
        BigInteger endTime = time.add(BigInteger.valueOf(ONE_DAY * 368));
        BigInteger interval = BigInteger.ZERO;
        BigInteger amount = ICX.multiply(BigInteger.valueOf(2));

        List account = new ArrayList();
        account.add(Map.of(
                "address", owners[0].getAddress(),
                "eachAmount", BigInteger.ZERO,
                "totalAmount", amount
        ));
        account.add(Map.of(
                "address", owners[1].getAddress(),
                "eachAmount", amount,
                "totalAmount", BigInteger.ZERO
        ));

        BigInteger[] hour = new BigInteger[] { BigInteger.valueOf(9)/*, BigInteger.valueOf(16)*/ };
        BigInteger[] day = new BigInteger[] { BigInteger.valueOf(1)/*, BigInteger.valueOf(-1), BigInteger.valueOf(0)*/ };
        BigInteger[] month = new BigInteger[] { BigInteger.valueOf(2), BigInteger.valueOf(8), BigInteger.valueOf(13) };

        _registerConditionalVesting(vesting, govWallet, YEARLY, month, day, null, hour, ZERO_ADDRESS, startTime, endTime, interval, account, false);

        month = new BigInteger[] { BigInteger.valueOf(2), BigInteger.valueOf(8), BigInteger.valueOf(12) };
        _registerConditionalVesting(vesting, govWallet, YEARLY, month, day, null, hour, ZERO_ADDRESS, startTime, endTime, interval, account, true);

        BigInteger id = vesting.lastId();

        LOG.info("info : " + vesting.info(id));
        LOG.info("vestedTimes" + vesting.vestingTimes(id));

        LOG.infoExiting();
    }

    @Test
    void addRemoveAccountsTest() throws Exception {
        LOG.infoEntering("vesting", "removeAccountsTest");

        BigInteger startTime = _getTimestamp();
        BigInteger endTime = startTime.add(BigInteger.valueOf(60 * 1_000_000L));
        BigInteger interval = BigInteger.valueOf(10 * 1_000_000L);
        BigInteger amount = ICX.multiply(BigInteger.valueOf(3));

        List account = new ArrayList();
        account.add(Map.of(
                "address", owners[0].getAddress(),
                "eachAmount", BigInteger.ZERO,
                "totalAmount", amount
        ));
        account.add(Map.of(
                "address", owners[1].getAddress(),
                "eachAmount", amount,
                "totalAmount", BigInteger.ZERO
        ));
        _registerPeriodicVesting(vesting, govWallet, ZERO_ADDRESS, startTime, endTime, interval, account, true);

        BigInteger id = vesting.lastId();

        LOG.info("claimableAmount[0] : " + vesting.claimableAmount(id, owners[0].getAddress()));
        LOG.info("claimableAmount[1] : " + vesting.claimableAmount(id, owners[1].getAddress()));
        _waitUtilTime(startTime.add(BigInteger.valueOf(11 * 1_000_000L)));
        LOG.info("claimableAmount[0] : " + vesting.claimableAmount(id, owners[0].getAddress()));
        LOG.info("claimableAmount[1] : " + vesting.claimableAmount(id, owners[1].getAddress()));

        LOG.info(">>> claim");
        _claim(vesting, owners[0], id, true);
        _claim(vesting, owners[1], id, true);

        _waitUtilTime(startTime.add(BigInteger.valueOf(30 * 1_000_000L)));
        LOG.info("claimableAmount[0] : " + vesting.claimableAmount(id, owners[0].getAddress()));
        LOG.info("claimableAmount[1] : " + vesting.claimableAmount(id, owners[1].getAddress()));
        LOG.info("info : " + vesting.info(id));

        LOG.info(">>> removeAccounts");
        _removeVestingAccounts(vesting, govWallet, id, new Address[] { owners[0].getAddress()}, true);
        LOG.info(">>> claim");
        _claim(vesting, owners[0], id, false);
        _claim(vesting, owners[1], id, true);
        LOG.info("claimableAmount[1] : " + vesting.claimableAmount(id, owners[1].getAddress()));

        _addVestingAccounts(vesting, govWallet, id, List.of(Map.of(
                "address", owners[0].getAddress(),
                "eachAmount", BigInteger.ZERO,
                "totalAmount", amount
        )), true);

        _waitUtilTime(startTime.add(BigInteger.valueOf(50 * 1_000_000L)));
        LOG.info("info : " + vesting.info(id));
        LOG.info("claimableAmount[0] : " + vesting.claimableAmount(id, owners[1].getAddress()));
        LOG.info("claimableAmount[1] : " + vesting.claimableAmount(id, owners[1].getAddress()));

        LOG.infoExiting();
    }
}
