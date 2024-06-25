package io.havah.test.vesting.score;

import foundation.icon.icx.Wallet;
import foundation.icon.icx.data.Address;
import foundation.icon.icx.data.TransactionResult;
import foundation.icon.icx.transport.jsonrpc.RpcArray;
import foundation.icon.icx.transport.jsonrpc.RpcObject;
import foundation.icon.icx.transport.jsonrpc.RpcValue;
import foundation.icon.test.ResultTimeoutException;
import foundation.icon.test.TransactionFailureException;
import foundation.icon.test.TransactionHandler;
import foundation.icon.test.score.Score;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static foundation.icon.test.Env.LOG;

public class VestingScore  extends Score {
    public VestingScore(Score other) {
        super(other);
    }

    public static VestingScore mustDeploy(TransactionHandler txHandler, Wallet wallet)
            throws ResultTimeoutException, TransactionFailureException, IOException {
        LOG.infoEntering("deploy", "Vesting");

        RpcObject params = new RpcObject.Builder()
                .put("_name", new RpcValue("Vesting"))
                .build();

        Score score = txHandler.deploy(wallet, getFilePath("vesting"), params);
        LOG.infoExiting("scoreAddr = " + score.getAddress());
        return new VestingScore(score);
    }

    public BigInteger lastId() throws IOException {
        return call("lastId", null).asInteger();
    }

    public List vestingTimes(BigInteger id) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_id", new RpcValue(id))
                .build();

        List list = new ArrayList();
        RpcArray arr = call("vestingTimes", params).asArray();
        for(int i=0; i<arr.size(); i++) {
            list.add(arr.get(i).asInteger());
        }
        return list;
    }

    public TransactionResult registerOnetimeVesting(Wallet wallet, Address token, BigInteger startTime, List accounts)
            throws IOException, ResultTimeoutException {
        RpcArray.Builder arr = new RpcArray.Builder();
        for(int i=0; i<accounts.size(); i++) {
            Map account = (Map)accounts.get(i);
            arr.add(new RpcObject.Builder()
                    .put("address", new RpcValue((Address) account.get("address")))
                    .put("eachAmount", new RpcValue((BigInteger) account.get("eachAmount")))
                    .put("totalAmount", new RpcValue((BigInteger) account.get("totalAmount")))
                    .build()
            );
        }

        RpcObject params = new RpcObject.Builder()
                .put("_token", new RpcValue(token))
                .put("_startTime", new RpcValue(startTime))
                .put("_accounts", arr.build())
                .build();

        return invokeAndWaitResult(wallet, "registerOnetimeVesting", params);
    }

    public TransactionResult registerLinearVesting(Wallet wallet, Address token, BigInteger startTime, BigInteger endTime,
                                                    List accounts) throws IOException, ResultTimeoutException {
        RpcArray.Builder arr = new RpcArray.Builder();
        for(int i=0; i<accounts.size(); i++) {
            Map account = (Map)accounts.get(i);
            arr.add(new RpcObject.Builder()
                    .put("address", new RpcValue((Address) account.get("address")))
                    .put("eachAmount", new RpcValue((BigInteger) account.get("eachAmount")))
                    .put("totalAmount", new RpcValue((BigInteger) account.get("totalAmount")))
                    .build()
            );
        }

        RpcObject params = new RpcObject.Builder()
                .put("_token", new RpcValue(token))
                .put("_startTime", new RpcValue(startTime))
                .put("_endTime", new RpcValue(endTime))
                .put("_accounts", arr.build())
                .build();

        return invokeAndWaitResult(wallet, "registerLinearVesting", params);
    }

    public TransactionResult registerPeriodicVesting(Wallet wallet, Address token, BigInteger startTime, BigInteger endTime,
            BigInteger timeInterval, List accounts) throws IOException, ResultTimeoutException {
        RpcArray.Builder arr = new RpcArray.Builder();
        for(int i=0; i<accounts.size(); i++) {
            Map account = (Map)accounts.get(i);
            arr.add(new RpcObject.Builder()
                    .put("address", new RpcValue((Address) account.get("address")))
                    .put("eachAmount", new RpcValue((BigInteger) account.get("eachAmount")))
                    .put("totalAmount", new RpcValue((BigInteger) account.get("totalAmount")))
                    .build()
            );
        }

        RpcObject params = new RpcObject.Builder()
                .put("_token", new RpcValue(token))
                .put("_startTime", new RpcValue(startTime))
                .put("_endTime", new RpcValue(endTime))
                .put("_timeInterval", new RpcValue(timeInterval))
                .put("_accounts", arr.build())
                .build();


        return invokeAndWaitResult(wallet, "registerPeriodicVesting", params);
    }

    public TransactionResult registerDailyVesting(Wallet wallet, Address token, BigInteger startTime, BigInteger endTime,
                                                     BigInteger hour, List accounts) throws IOException, ResultTimeoutException {
        RpcArray.Builder arr = new RpcArray.Builder();
        for(int i=0; i<accounts.size(); i++) {
            Map account = (Map)accounts.get(i);
            arr.add(new RpcObject.Builder()
                    .put("address", new RpcValue((Address) account.get("address")))
                    .put("eachAmount", new RpcValue((BigInteger) account.get("eachAmount")))
                    .put("totalAmount", new RpcValue((BigInteger) account.get("totalAmount")))
                    .build()
            );
        }

        RpcObject params = new RpcObject.Builder()
                .put("_token", new RpcValue(token))
                .put("_startTime", new RpcValue(startTime))
                .put("_endTime", new RpcValue(endTime))
                .put("_hour", new RpcValue(hour))
                .put("_accounts", arr.build())
                .build();


        return invokeAndWaitResult(wallet, "registerDailyVesting", params);
    }

    public TransactionResult registerWeeklyVesting(Wallet wallet, Address token, BigInteger startTime, BigInteger endTime,
                                                  BigInteger weekday, BigInteger hour, List accounts) throws IOException, ResultTimeoutException {
        RpcArray.Builder arr = new RpcArray.Builder();
        for(int i=0; i<accounts.size(); i++) {
            Map account = (Map)accounts.get(i);
            arr.add(new RpcObject.Builder()
                    .put("address", new RpcValue((Address) account.get("address")))
                    .put("eachAmount", new RpcValue((BigInteger) account.get("eachAmount")))
                    .put("totalAmount", new RpcValue((BigInteger) account.get("totalAmount")))
                    .build()
            );
        }

        RpcObject params = new RpcObject.Builder()
                .put("_token", new RpcValue(token))
                .put("_startTime", new RpcValue(startTime))
                .put("_endTime", new RpcValue(endTime))
                .put("_weekday", new RpcValue(weekday))
                .put("_hour", new RpcValue(hour))
                .put("_accounts", arr.build())
                .build();


        return invokeAndWaitResult(wallet, "registerWeeklyVesting", params);
    }

    public TransactionResult registerMonthlyVesting(Wallet wallet, Address token, BigInteger startTime, BigInteger endTime,
                                                   BigInteger day, BigInteger hour, List accounts) throws IOException, ResultTimeoutException {
        RpcArray.Builder arr = new RpcArray.Builder();
        for(int i=0; i<accounts.size(); i++) {
            Map account = (Map)accounts.get(i);
            arr.add(new RpcObject.Builder()
                    .put("address", new RpcValue((Address) account.get("address")))
                    .put("eachAmount", new RpcValue((BigInteger) account.get("eachAmount")))
                    .put("totalAmount", new RpcValue((BigInteger) account.get("totalAmount")))
                    .build()
            );
        }

        RpcObject params = new RpcObject.Builder()
                .put("_token", new RpcValue(token))
                .put("_startTime", new RpcValue(startTime))
                .put("_endTime", new RpcValue(endTime))
                .put("_day", new RpcValue(day))
                .put("_hour", new RpcValue(hour))
                .put("_accounts", arr.build())
                .build();


        return invokeAndWaitResult(wallet, "registerMonthlyVesting", params);
    }

    public TransactionResult registerYearlyVesting(Wallet wallet, Address token, BigInteger startTime, BigInteger endTime,
                                                   BigInteger month, BigInteger day, BigInteger hour, List accounts)
            throws IOException, ResultTimeoutException {
        RpcArray.Builder arr = new RpcArray.Builder();
        for(int i=0; i<accounts.size(); i++) {
            Map account = (Map)accounts.get(i);
            arr.add(new RpcObject.Builder()
                    .put("address", new RpcValue((Address) account.get("address")))
                    .put("eachAmount", new RpcValue((BigInteger) account.get("eachAmount")))
                    .put("totalAmount", new RpcValue((BigInteger) account.get("totalAmount")))
                    .build()
            );
        }

        RpcObject params = new RpcObject.Builder()
                .put("_token", new RpcValue(token))
                .put("_startTime", new RpcValue(startTime))
                .put("_endTime", new RpcValue(endTime))
                .put("_month", new RpcValue(month))
                .put("_day", new RpcValue(day))
                .put("_hour", new RpcValue(hour))
                .put("_accounts", arr.build())
                .build();


        return invokeAndWaitResult(wallet, "registerYearlyVesting", params);
    }

    public Map info(BigInteger id) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_id", new RpcValue(id))
                .build();

        RpcObject obj = call("info", params).asObject();
        return Map.of(
                "type", obj.getItem("type").asString(),
                "startTime", obj.getItem("startTime").asInteger(),
                "endTime", obj.getItem("endTime").asInteger(),
                "timeInterval", obj.getItem("timeInterval").asInteger(),
                "month", obj.getItem("month").asString(),
                "day", obj.getItem("day").asString(),
                "weekday", obj.getItem("weekday").asString(),
                "hour", obj.getItem("hour").asString(),
                "totalAmount", obj.getItem("totalAmount").asInteger(),
                "totalClaimed", obj.getItem("totalClaimed").asInteger()
        );
    }

    public TransactionResult claim(Wallet wallet, BigInteger id) throws IOException, ResultTimeoutException {
        RpcObject params = new RpcObject.Builder()
                .put("_id", new RpcValue(id))
                .build();
        return invokeAndWaitResult(wallet, "claim", params);
    }

    public BigInteger claimableAmount(BigInteger id, Address address) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_id", new RpcValue(id))
                .put("_address", new RpcValue(address))
                .build();
        return call("claimableAmount", params).asInteger();
    }

    public TransactionResult addVestingAccounts(Wallet wallet, BigInteger id, List accounts) throws IOException, ResultTimeoutException {
        RpcArray.Builder arr = new RpcArray.Builder();
        for(int i=0; i<accounts.size(); i++) {
            Map account = (Map)accounts.get(i);
            arr.add(new RpcObject.Builder()
                    .put("address", new RpcValue((Address) account.get("address")))
                    .put("eachAmount", new RpcValue((BigInteger) account.get("eachAmount")))
                    .put("totalAmount", new RpcValue((BigInteger) account.get("totalAmount")))
                    .build()
            );
        }
        RpcObject params = new RpcObject.Builder()
                .put("_id", new RpcValue(id))
                .put("_accounts", arr.build())
                .build();

        return invokeAndWaitResult(wallet, "addVestingAccounts", params);
    }

    public TransactionResult removeVestingAccounts(Wallet wallet, BigInteger id, Address[] accounts) throws IOException, ResultTimeoutException {
        RpcArray.Builder arr = new RpcArray.Builder();
        for(int i=0; i<accounts.length; i++) {
            arr.add(new RpcValue(accounts[i]));
        }
        RpcObject params = new RpcObject.Builder()
                .put("_id", new RpcValue(id))
                .put("_accounts", arr.build())
                .build();

        return invokeAndWaitResult(wallet, "removeVestingAccounts", params);
    }
}
