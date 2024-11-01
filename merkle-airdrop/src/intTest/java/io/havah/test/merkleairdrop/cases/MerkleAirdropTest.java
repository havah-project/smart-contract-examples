package io.havah.test.merkleairdrop.cases;

import foundation.icon.icx.Call;
import foundation.icon.icx.IconService;
import foundation.icon.icx.KeyWallet;
import foundation.icon.icx.Wallet;
import foundation.icon.icx.data.Address;
import foundation.icon.icx.data.Block;
import foundation.icon.icx.data.Bytes;
import foundation.icon.icx.data.TransactionResult;
import foundation.icon.icx.transport.http.HttpProvider;
import foundation.icon.icx.transport.jsonrpc.RpcItem;
import foundation.icon.icx.transport.jsonrpc.RpcObject;
import foundation.icon.icx.transport.jsonrpc.RpcValue;
import foundation.icon.test.Env;
import foundation.icon.test.ResultTimeoutException;
import foundation.icon.test.TestBase;
import foundation.icon.test.TransactionHandler;
import io.havah.test.merkleairdrop.score.LogFinder;
import io.havah.test.merkleairdrop.score.MerkleAirdropScore;
import io.havah.test.merkleairdrop.score.SampleTokenScore;
import io.havah.test.merkleairdrop.score.VestingScore;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static foundation.icon.test.Env.LOG;
import static org.junit.jupiter.api.Assertions.*;

public class MerkleAirdropTest extends TestBase {
    private static final Address ZERO_ADDRESS = new Address("hx0000000000000000000000000000000000000000");
    private static IconService iconService;
    private static TransactionHandler txHandler;

    private static Wallet govWallet;
    private static Wallet[] owners = new KeyWallet[5];
    private static BigInteger[] airdropAmountPerOwner = new BigInteger[] {
            ICX.multiply(BigInteger.valueOf(10)),
            ICX.multiply(BigInteger.valueOf(20)),
            ICX.multiply(BigInteger.valueOf(30)),
            ICX.multiply(BigInteger.valueOf(40)),
            ICX.multiply(BigInteger.valueOf(50))
    };
    private static MerkleAirdropScore airdropContract;
    private static SampleTokenScore hsp20token;

    public int _compare(byte[] a, byte[] b) {
        if (a == b)
            return 0;
        if (a == null || b == null)
            return a == null ? -1 : 1;

        int count = Math.min(a.length, b.length);
        for (int i = 0; i < count; i++) {
            int cmp = Integer.compare(0xff & a[i], 0xff & b[i]);
            if (cmp != 0) {
                return cmp;
            }
        }

        return a.length - b.length;
    }

    protected byte[] _makeHash(byte[] a, byte[] b) {
        Keccak.Digest256 keccak256 = new Keccak.Digest256();
        return keccak256.digest(_concat(a, b));
    }

    public boolean makeRootHash(byte[] _merkleRoot, byte[] _hash, byte[][] _proof) {
        byte[] hash = _hash;
        for (byte[] leaf : _proof) {
            if (_compare(hash, leaf) <= 0) {
                hash = _makeHash(hash, leaf);
            } else {
                hash = _makeHash(leaf, hash);
            }
        }

        return _compare(hash, _merkleRoot) == 0;
    }


    @BeforeAll
    static void setup() throws Exception {
        Env.Chain chain = Env.getDefaultChain();
        iconService = new IconService(new HttpProvider(chain.getEndpointURL(3)));
        txHandler = new TransactionHandler(iconService, chain);
        govWallet = txHandler.getChain().godWallet;

        owners = new Wallet[] {
                KeyWallet.load(new Bytes("0x982d49546ed9998a10af77bbcd6856a76501ad5049d55d0f5220b7664831b87c")), // hx3e65ce9ff07186df3ee2bda02d20420e2da5da80
                KeyWallet.load(new Bytes("0xb8eb504a175604c65fe2f4bbfd886147ab053de4322180c7142cf26dcc2e5d12")), // hx34e7759532571fe15c129a045627b437869c818c
                KeyWallet.load(new Bytes("0x982d54ca13dcb468780768574bebcfd905c53ce0edbdd194dbc6fc2539a8dd0b")), // hx1dc6d2f7fe9e1f969279e816b3fdbfbe4134bf3d
                KeyWallet.load(new Bytes("0xff35d8489b1e804437752c606611f5e74252915f2cd34708a98554e570b3a312")), // hxe0afc6ff8a605f24abd42b2cf2f1e0de11a797ff
                KeyWallet.load(new Bytes("0x681b64f95f313b828426cacfa6c4df63ffe8dd4a002dff34d585527d5cd9cc0e")) // hx36b8ecb38486d273c4cb87fd8d2509b2e441c02d
        };
//        owners[0] = KeyWallet.load(new Bytes("0x982d49546ed9998a10af77bbcd6856a76501ad5049d55d0f5220b7664831b87c")); // hx3e65ce9ff07186df3ee2bda02d20420e2da5da80
//        owners[1] = KeyWallet.load(new Bytes("0xb8eb504a175604c65fe2f4bbfd886147ab053de4322180c7142cf26dcc2e5d12")); // hx34e7759532571fe15c129a045627b437869c818c
//        owners[2] = KeyWallet.load(new Bytes("0x982d54ca13dcb468780768574bebcfd905c53ce0edbdd194dbc6fc2539a8dd0b")); // hx1dc6d2f7fe9e1f969279e816b3fdbfbe4134bf3d
//        owners[3] = KeyWallet.load(new Bytes("0xff35d8489b1e804437752c606611f5e74252915f2cd34708a98554e570b3a312")); // hxe0afc6ff8a605f24abd42b2cf2f1e0de11a797ff
//        owners[4] = KeyWallet.load(new Bytes("0x681b64f95f313b828426cacfa6c4df63ffe8dd4a002dff34d585527d5cd9cc0e")); // hx36b8ecb38486d273c4cb87fd8d2509b2e441c02d

        BigInteger amount = ICX.multiply(BigInteger.valueOf(300));
        for (Wallet owner : owners) {
            Bytes txHash = txHandler.transfer(owner.getAddress(), amount);
            assertSuccess(txHandler.getResult(txHash));
        }
    }

    protected String _cleanHexPrefix(String s) {
        if(s.startsWith("0x"))
            return s.substring(2);
        return s;
    }

    protected byte[] _hexToBytes(String s) {
        s = _cleanHexPrefix(s);
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    private static String _bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    byte[] _concat(byte[] a, byte[] b) {
        byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }

    protected byte[] _makeHash(Address address, BigInteger amount) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("keccak-256");
        md.update(_concat(address.toString().getBytes(), amount.toString().getBytes()));
        return md.digest();
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

    protected String _getSafeString(Object obj) {
        return obj == null ? "" : obj.toString();
    }

    protected BigInteger _getSafeInteger(BigInteger obj) {
        return obj == null ? BigInteger.ZERO : obj;
    }

    protected void _addAirdrop(MerkleAirdropScore score, Wallet wallet, Address token, byte[] merkleRoot,
                               BigInteger startTime, BigInteger endTime, BigInteger totalAmount, boolean success)
            throws IOException, ResultTimeoutException {
/*
        TransactionResult result = score.addAirdrop(wallet, token, merkleRoot, startTime, endTime, totalAmount);
        if (success) {
            assertSuccess(result);
            LogFinder.ensureAirdropAdded(result, score.getAddress(), score.lastId(), token, merkleRoot,
                    startTime, _getSafeInteger(endTime), _getSafeString(totalAmount));
        } else {
            assertFailure(result);
        }
*/
    }

    protected BigInteger _getBalance(Address token, Address owner) throws IOException {
        if(!token.equals(ZERO_ADDRESS)) {
            RpcObject params = new RpcObject.Builder()
                    .put("_owner", new RpcValue(owner))
                    .build();
            Call<RpcItem> call = new Call.Builder()
                    .to(token)
                    .method("balanceOf")
                    .params(params)
                    .build();
            return txHandler.call(call).asInteger();
        }
        return txHandler.getBalance(owner);
    }

    protected void _claim(MerkleAirdropScore score, Wallet wallet, BigInteger stage, Address token, BigInteger amount,
                          byte[][] proof, boolean success) throws IOException, ResultTimeoutException {
        BigInteger oldBalance = _getBalance(token, wallet.getAddress());
        TransactionResult result = score.claim(wallet, stage, amount, proof);
        if (success) {
            assertSuccess(result);
            LogFinder.ensureClaimed(result, score.getAddress(), stage, token, wallet.getAddress(), amount);
            if(token.equals(ZERO_ADDRESS)) {
                BigInteger fee = result.getStepPrice().multiply(result.getStepUsed());
                assertEquals(_getBalance(token, wallet.getAddress()), oldBalance.add(amount).subtract(fee));
            } else {
                assertEquals(_getBalance(token, wallet.getAddress()), oldBalance.add(amount));
            }
        } else {
            assertFailure(result);
        }
    }

    protected void _giveaway(MerkleAirdropScore score, Wallet wallet, BigInteger stage, Address token, Address recipient,
                             BigInteger amount, byte[][] proof, boolean success) throws IOException, ResultTimeoutException {
        BigInteger oldBalance = _getBalance(token, recipient);
        TransactionResult result = score.giveaway(wallet, stage, recipient, amount, proof);
        if (success) {
            assertSuccess(result);
            LogFinder.ensureClaimed(result, score.getAddress(), stage, token, recipient, amount);
            if(wallet.getAddress().equals(recipient) && token.equals(ZERO_ADDRESS)) {
                BigInteger fee = result.getStepPrice().multiply(result.getStepUsed());
                assertEquals(_getBalance(token, recipient), oldBalance.add(amount).subtract(fee));
            } else {
                assertEquals(_getBalance(token, recipient), oldBalance.add(amount));
            }
        } else {
            assertFailure(result);
        }
    }

    protected void _withdraw(MerkleAirdropScore score, Wallet wallet, Address token, BigInteger amount,
                             Address recipient, boolean success) throws IOException, ResultTimeoutException {
        Address to = recipient == null ? wallet.getAddress() : recipient;
        BigInteger oldBalance = _getBalance(token, to);
        TransactionResult result = score.withdraw(wallet, token, amount, recipient);
        if (success) {
            assertSuccess(result);
            LogFinder.ensureWithdrawn(result, score.getAddress(), token, to, amount);
            if(to.equals(recipient)) {
                assertEquals(_getBalance(token, to), oldBalance.add(amount));
            } else {
                if(token.equals(ZERO_ADDRESS)) {
                    BigInteger fee = result.getStepPrice().multiply(result.getStepUsed());
                    assertEquals(_getBalance(token, to), oldBalance.add(amount).subtract(fee));
                } else {
                    assertEquals(_getBalance(token, to), oldBalance.add(amount));
                }
            }
        } else {
            assertFailure(result);
        }
    }

    /**
     *
     * [
     * "hx3e65ce9ff07186df3ee2bda02d20420e2da5da8010000000000000000000",
     * "hx34e7759532571fe15c129a045627b437869c818c20000000000000000000",
     * "hx1dc6d2f7fe9e1f969279e816b3fdbfbe4134bf3d30000000000000000000",
     * "hxe0afc6ff8a605f24abd42b2cf2f1e0de11a797ff40000000000000000000",
     * "hx36b8ecb38486d273c4cb87fd8d2509b2e441c02d50000000000000000000"
     * ]
     *
     * Tree
     * └─ 8935f1f69424db7af043d2194308fd86cc3ac83b7992a059353605f303c76bab
     *    ├─ f199d54237e659b413c31fcf754fe8ded9a038d459d1e4f25eee5453c8720489
     *    │  ├─ 2042a4a20d55fb2674893a3546128f9b35a1be4268ad4b4bade94197a819d8c3
     *    │  │  ├─ 1a80ebcb34050d07dc991be3ae963b14365ffbae2429539c1edbf38332275644
     *    │  │  └─ 45c58c145c622424d4fd4cdcb7a68f39460b20cba50317e79e5ff9c13b0d510b
     *    │  └─ da2e76cab6fbf4e4b0f004c7233ef5ead63a0c044e473fe13b8900e0b5e54283
     *    │     ├─ 473757d59afeaeb75454bb32452b0ac207b1b91b6e8e1e18e932748fc0b3d64c
     *    │     └─ 5a6abcfd8c8c89d6e936619996cd737f65eb8cd13a554876e316f566372fb9b7
     *    └─ d58456e77fe12f0297cc516f90187acf1f87459b2871ba931e0f312af759652c
     *       └─ d58456e77fe12f0297cc516f90187acf1f87459b2871ba931e0f312af759652c
     *          └─ d58456e77fe12f0297cc516f90187acf1f87459b2871ba931e0f312af759652c
     *
     */

    protected byte[] root = _hexToBytes("0x8935f1f69424db7af043d2194308fd86cc3ac83b7992a059353605f303c76bab");

    protected byte[][] getProof(byte[] hash) {
        switch (_bytesToHex(hash)) {
            case "1a80ebcb34050d07dc991be3ae963b14365ffbae2429539c1edbf38332275644":
                return new byte[][] {
                        _hexToBytes("45c58c145c622424d4fd4cdcb7a68f39460b20cba50317e79e5ff9c13b0d510b"),
                        _hexToBytes("da2e76cab6fbf4e4b0f004c7233ef5ead63a0c044e473fe13b8900e0b5e54283"),
                        _hexToBytes("d58456e77fe12f0297cc516f90187acf1f87459b2871ba931e0f312af759652c")
                };
            case "45c58c145c622424d4fd4cdcb7a68f39460b20cba50317e79e5ff9c13b0d510b":
                return new byte[][] {
                        _hexToBytes("1a80ebcb34050d07dc991be3ae963b14365ffbae2429539c1edbf38332275644"),
                        _hexToBytes("da2e76cab6fbf4e4b0f004c7233ef5ead63a0c044e473fe13b8900e0b5e54283"),
                        _hexToBytes("d58456e77fe12f0297cc516f90187acf1f87459b2871ba931e0f312af759652c")
                };
            case "473757d59afeaeb75454bb32452b0ac207b1b91b6e8e1e18e932748fc0b3d64c":
                return new byte[][] {
                        _hexToBytes("5a6abcfd8c8c89d6e936619996cd737f65eb8cd13a554876e316f566372fb9b7"),
                        _hexToBytes("2042a4a20d55fb2674893a3546128f9b35a1be4268ad4b4bade94197a819d8c3"),
                        _hexToBytes("d58456e77fe12f0297cc516f90187acf1f87459b2871ba931e0f312af759652c")
                };
            case "5a6abcfd8c8c89d6e936619996cd737f65eb8cd13a554876e316f566372fb9b7":
                return new byte[][] {
                        _hexToBytes("473757d59afeaeb75454bb32452b0ac207b1b91b6e8e1e18e932748fc0b3d64c"),
                        _hexToBytes("2042a4a20d55fb2674893a3546128f9b35a1be4268ad4b4bade94197a819d8c3"),
                        _hexToBytes("d58456e77fe12f0297cc516f90187acf1f87459b2871ba931e0f312af759652c")
                };
            case "d58456e77fe12f0297cc516f90187acf1f87459b2871ba931e0f312af759652c":
                return new byte[][] {
                        _hexToBytes("f199d54237e659b413c31fcf754fe8ded9a038d459d1e4f25eee5453c8720489")
                };
        }
        return null;
    }

    static final String REWARD_STATUS_OPTION = "rewardOption";
    static final String REWARD_STATUS_TOTAL = "total";
    static final String REWARD_STATUS_CLAIMABLE = "claimable";
    static final String REWARD_STATUS_REMAINED = "remained";

    boolean checkRewardStatus(Address address, BigInteger rewardOption, BigInteger total, BigInteger claimable, BigInteger remained) throws IOException {
        Map rewardStatus = airdropContract.getRewardStatus(address);
        BigInteger _option = (BigInteger) rewardStatus.get(REWARD_STATUS_OPTION);
        BigInteger _total = (BigInteger) rewardStatus.get(REWARD_STATUS_TOTAL);
        BigInteger _claimable = (BigInteger) rewardStatus.get(REWARD_STATUS_CLAIMABLE);
        BigInteger _remained = (BigInteger) rewardStatus.get(REWARD_STATUS_REMAINED);
        if (!_option.equals(rewardOption) || !_total.equals(total) || !_claimable.equals(claimable) || !_remained.equals(remained)) {
            System.out.println("reward expected(" + _option + " actual(" + rewardOption + ")");
            System.out.println("total expected(" + _total + " actual(" + total + ")");
            System.out.println("claimable expected(" + _claimable + " actual(" + claimable + ")");
            System.out.println("remained expected(" + _remained + " actual(" + remained + ")");
            return false;
        }
        return true;
    }

    @Test
    void airdrop() throws Exception {
        BigInteger AIRDROP_AMOUNT = ICX.multiply(BigInteger.valueOf(100));
        hsp20token = SampleTokenScore.mustDeploy(txHandler, govWallet, BigInteger.valueOf(18), AIRDROP_AMOUNT);

        // setting -->
        VestingScore vestingScore = VestingScore.mustDeploy(txHandler, govWallet);
//        BigInteger startTime = _getTimestamp();
//        BigInteger ONE_YEAR = BigInteger.valueOf(31536000).multiply(BigInteger.valueOf(1000000));
//        BigInteger endTime = startTime.add(ONE_YEAR);
        // 1730422800000
        // 1735693200000
        BigInteger startTime = BigInteger.valueOf(1730422800_000_000L);
        BigInteger endTime = BigInteger.valueOf(1735693200_000_000L);
        System.out.println("startTime(" + startTime + "), endTime(" + endTime + ")");

        vestingScore.registerMonthlyVesting(govWallet, hsp20token.getAddress(), startTime, endTime,
                BigInteger.ONE, BigInteger.TWO, new ArrayList());
        List list = vestingScore.vestingTimes(BigInteger.ZERO);
        System.out.println("schedule : " + Arrays.toString(list.toArray()));

        airdropContract = MerkleAirdropScore.mustDeploy(txHandler, govWallet);
        hsp20token.transfer(govWallet, airdropContract.getAddress(), AIRDROP_AMOUNT);
        vestingScore.setRewardManager(govWallet, airdropContract.getAddress());

        Wallet treasury = KeyWallet.create();
        TransactionResult result;
        result = airdropContract.setAirdrop(govWallet, hsp20token.getAddress(), root, startTime, endTime, AIRDROP_AMOUNT);
        assertSuccess(result);
        result = airdropContract.setTreasury(govWallet, treasury.getAddress());
        assertSuccess(result);
        result = airdropContract.setVestingContract(govWallet, vestingScore.getAddress());
        assertSuccess(result);
//        result = airdropContract.setRewardToken(govWallet, hsp20token.getAddress());
//        assertSuccess(result);

        Wallet claimer1 = owners[0];
        BigInteger airdrop1 = airdropAmountPerOwner[0];
        byte[] hash = _makeHash(claimer1.getAddress(), airdrop1);
        byte[][] proofHash = getProof(hash);
        result = airdropContract.selectRewardOption(claimer1, 1, airdrop1, proofHash);
        assertSuccess(result);

        // check claimed
        BigInteger claimed = airdrop1.divide(BigInteger.TWO);
        assertEquals(hsp20token.balanceOf(claimer1.getAddress()), claimed);

        // check treasury
        BigInteger balanceOfTreasury = airdrop1.subtract(claimed);
        assertEquals(hsp20token.balanceOf(treasury.getAddress()), balanceOfTreasury);

        checkRewardStatus(claimer1.getAddress(), BigInteger.ONE, claimed, BigInteger.ZERO, BigInteger.ZERO);

        Wallet claimer2 = owners[1];
        BigInteger airdrop2 =  airdropAmountPerOwner[1];
        hash = _makeHash(claimer2.getAddress(), airdrop2);
        result = airdropContract.selectRewardOption(claimer2, 2, airdrop2, getProof(hash));
        assertSuccess(result);

        // check claimed
        assertEquals(hsp20token.balanceOf(claimer2.getAddress()), BigInteger.ZERO);

        // check treasury
        // not changed
        assertEquals(hsp20token.balanceOf(treasury.getAddress()), balanceOfTreasury);
        assertEquals(hsp20token.balanceOf(vestingScore.getAddress()), airdrop2);

        checkRewardStatus(claimer2.getAddress(), BigInteger.ZERO, airdrop2, BigInteger.ZERO, airdrop2);

        // failure - invalid option select
        Wallet claimer3 = owners[2];
        BigInteger airdrop3 = airdropAmountPerOwner[2];
        hash = _makeHash(claimer3.getAddress(), airdrop3);
        result = airdropContract.selectRewardOption(claimer3, 3, airdrop3, getProof(hash));
        assertFailure(result);

        // failure - invalid hash
        hash = _makeHash(claimer2.getAddress(), airdrop2);
        result = airdropContract.selectRewardOption(claimer3, 2, airdrop3, getProof(hash));
        assertFailure(result);

        // failure - invalid claimer
        Wallet claimer4 = owners[3];
        hash = _makeHash(claimer3.getAddress(), airdrop3);
        result = airdropContract.selectRewardOption(claimer4, 2, airdrop3, getProof(hash));
        assertFailure(result);

        // already with claimer1
        hash = _makeHash(claimer1.getAddress(), airdrop1);
        result = airdropContract.selectRewardOption(claimer1, 2, airdrop1, getProof(hash));
        assertFailure(result);

        // already with claimer2
        hash = _makeHash(claimer2.getAddress(), airdrop2);
        result = airdropContract.selectRewardOption(claimer2, 1, airdrop2, getProof(hash));
        assertFailure(result);

        // success
        hash = _makeHash(claimer3.getAddress(), airdrop3);
        result = airdropContract.selectRewardOption(claimer3, 2, airdrop3, getProof(hash));
        assertSuccess(result);

        // check claimed
        assertEquals(hsp20token.balanceOf(claimer3.getAddress()), BigInteger.ZERO);

        // check treasury
        // not changed
        assertEquals(hsp20token.balanceOf(treasury.getAddress()), balanceOfTreasury);
        assertEquals(hsp20token.balanceOf(vestingScore.getAddress()), airdrop2.add(airdrop3));

        // TODO: getStatus
    }
}
