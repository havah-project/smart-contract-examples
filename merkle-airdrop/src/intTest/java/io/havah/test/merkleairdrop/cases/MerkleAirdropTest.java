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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static foundation.icon.test.Env.LOG;
import static org.junit.jupiter.api.Assertions.*;

public class MerkleAirdropTest extends TestBase {
    private static final Address ZERO_ADDRESS = new Address("hx0000000000000000000000000000000000000000");
    private static IconService iconService;
    private static TransactionHandler txHandler;

    private static Wallet govWallet;
    private static Wallet[] owners = new KeyWallet[5];
    private static MerkleAirdropScore airdrop;
    private static SampleTokenScore hsp20token;

    @BeforeAll
    static void setup() throws Exception {
        Env.Chain chain = Env.getDefaultChain();
        iconService = new IconService(new HttpProvider(chain.getEndpointURL(3)));
        txHandler = new TransactionHandler(iconService, chain);
        govWallet = txHandler.getChain().godWallet;

        owners[0] = KeyWallet.load(new Bytes("0x982d49546ed9998a10af77bbcd6856a76501ad5049d55d0f5220b7664831b87c")); // hx3e65ce9ff07186df3ee2bda02d20420e2da5da80
        owners[1] = KeyWallet.load(new Bytes("0xb8eb504a175604c65fe2f4bbfd886147ab053de4322180c7142cf26dcc2e5d12")); // hx34e7759532571fe15c129a045627b437869c818c
        owners[2] = KeyWallet.load(new Bytes("0x982d54ca13dcb468780768574bebcfd905c53ce0edbdd194dbc6fc2539a8dd0b")); // hx1dc6d2f7fe9e1f969279e816b3fdbfbe4134bf3d
        owners[3] = KeyWallet.load(new Bytes("0xff35d8489b1e804437752c606611f5e74252915f2cd34708a98554e570b3a312")); // hxe0afc6ff8a605f24abd42b2cf2f1e0de11a797ff
        owners[4] = KeyWallet.load(new Bytes("0x681b64f95f313b828426cacfa6c4df63ffe8dd4a002dff34d585527d5cd9cc0e")); // hx36b8ecb38486d273c4cb87fd8d2509b2e441c02d

        BigInteger amount = ICX.multiply(BigInteger.valueOf(300));
        for(int i=0; i<owners.length; i++) {
            Bytes txHash = txHandler.transfer(owners[i].getAddress(), amount);
            assertSuccess(txHandler.getResult(txHash));
        }

        LOG.info("deploy MerkleAirdrop");
        airdrop = MerkleAirdropScore.mustDeploy(txHandler, govWallet);
        Bytes txHash = txHandler.transfer(airdrop.getAddress(), amount);
        assertSuccess(txHandler.getResult(txHash));

        hsp20token = SampleTokenScore.mustDeploy(txHandler, govWallet, BigInteger.valueOf(18), amount);
        assertSuccess(hsp20token.transfer(govWallet, airdrop.getAddress(), amount));
        LOG.info("airdrop balanceOf : " + hsp20token.balanceOf(airdrop.getAddress()));
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
        TransactionResult result = score.addAirdrop(wallet, token, merkleRoot, startTime, endTime, totalAmount);
        if (success) {
            assertSuccess(result);
            LogFinder.ensureAirdropAdded(result, score.getAddress(), score.lastId(), token, merkleRoot,
                    startTime, _getSafeInteger(endTime), _getSafeString(totalAmount));
        } else {
            assertFailure(result);
        }
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

    @Test
    void validProofTest() throws Exception {
        LOG.infoEntering("MerkleAirdrop", "validProofTest");

        byte[] hash1 = _makeHash(owners[0].getAddress(), ICX.multiply(BigInteger.valueOf(10)));
        byte[] hash2 = _makeHash(owners[1].getAddress(), ICX.multiply(BigInteger.valueOf(20)));
        byte[] hash3 = _makeHash(owners[2].getAddress(), ICX.multiply(BigInteger.valueOf(30)));
        byte[] hash4 = _makeHash(owners[3].getAddress(), ICX.multiply(BigInteger.valueOf(40)));
        byte[] hash5 = _makeHash(owners[4].getAddress(), ICX.multiply(BigInteger.valueOf(50)));

        assertTrue(airdrop.isValidProof(root, hash1, getProof(hash1)));
        assertTrue(airdrop.isValidProof(root, hash2, getProof(hash2)));
        assertTrue(airdrop.isValidProof(root, hash3, getProof(hash3)));
        assertTrue(airdrop.isValidProof(root, hash4, getProof(hash4)));
        assertTrue(airdrop.isValidProof(root, hash5, getProof(hash5)));

        LOG.infoExiting();
    }

    @Test
    void MerkleAirdropBasicTest() throws Exception {
        LOG.infoEntering("MerkleAirdrop", "MerkleAirdropBasicTest");

        BigInteger amount = ICX.multiply(BigInteger.valueOf(150L));

        assertEquals(airdrop.lastId(), BigInteger.valueOf(-1));

        BigInteger startTime = _getTimestamp();
        BigInteger endTime = startTime.add(BigInteger.valueOf(30 * 1_000_000L));
        _addAirdrop(airdrop, owners[0], ZERO_ADDRESS, root, startTime, null, amount, false);
        _addAirdrop(airdrop, govWallet, ZERO_ADDRESS, root, startTime, endTime, amount, true);

        BigInteger id = BigInteger.ZERO;
        assertEquals(airdrop.lastId(), id);

        byte[] hash1 = _makeHash(owners[0].getAddress(), ICX.multiply(BigInteger.valueOf(10)));
        byte[] hash2 = _makeHash(owners[1].getAddress(), ICX.multiply(BigInteger.valueOf(20)));
        byte[] hash3 = _makeHash(owners[2].getAddress(), ICX.multiply(BigInteger.valueOf(30)));
//        byte[] hash4 = _makeHash(owners[3].getAddress(), ICX.multiply(BigInteger.valueOf(40)));
//        byte[] hash5 = _makeHash(owners[4].getAddress(), ICX.multiply(BigInteger.valueOf(50)));

        LOG.info("id 0 info : " + airdrop.info(id));

        LOG.info(">>> claim");
        assertFalse(airdrop.isClaimed(id, owners[0].getAddress()));
        assertTrue(airdrop.isClaimable(id, owners[0].getAddress(), ICX.multiply(BigInteger.valueOf(10)), getProof(hash1)));
        _claim(airdrop, owners[0], id, ZERO_ADDRESS, ICX.multiply(BigInteger.valueOf(10)), getProof(hash1), true);
        _claim(airdrop, owners[0], id, ZERO_ADDRESS, ICX.multiply(BigInteger.valueOf(10)), getProof(hash1), false);
        assertTrue(airdrop.isClaimed(id, owners[0].getAddress()));

        LOG.info(">>> giveaway");
        assertFalse(airdrop.isClaimed(id, owners[1].getAddress()));
        assertTrue(airdrop.isClaimable(id, owners[1].getAddress(), ICX.multiply(BigInteger.valueOf(20)), getProof(hash2)));
        _giveaway(airdrop, govWallet, id, ZERO_ADDRESS, owners[1].getAddress(), ICX.multiply(BigInteger.valueOf(20)), getProof(hash2), true);
        _giveaway(airdrop, govWallet, id, ZERO_ADDRESS, owners[1].getAddress(), ICX.multiply(BigInteger.valueOf(20)), getProof(hash2), false);
        assertTrue(airdrop.isClaimed(id, owners[1].getAddress()));

        LOG.info(">>> withdraw");
        _withdraw(airdrop, owners[2], ZERO_ADDRESS, ICX.multiply(BigInteger.valueOf(50)), null, false);
        _withdraw(airdrop, govWallet, ZERO_ADDRESS, ICX.multiply(BigInteger.valueOf(25)), null, true);
        _withdraw(airdrop, govWallet, ZERO_ADDRESS, ICX.multiply(BigInteger.valueOf(25)), owners[2].getAddress(), true);

        id = BigInteger.ONE;
        _addAirdrop(airdrop, govWallet, hsp20token.getAddress(), root, _getTimestamp(), null, amount, true);

        LOG.info("stage 1 info : " + airdrop.info(id));

        LOG.info(">>> claim");
        _claim(airdrop, owners[0], id, hsp20token.getAddress(), ICX.multiply(BigInteger.valueOf(10)), getProof(hash1), true);

        LOG.info(">>> giveaway");
        _giveaway(airdrop, govWallet, id, hsp20token.getAddress(), owners[1].getAddress(), ICX.multiply(BigInteger.valueOf(20)), getProof(hash2), true);

        LOG.info(">>> withdraw");
        _withdraw(airdrop, govWallet, hsp20token.getAddress(), ICX.multiply(BigInteger.valueOf(25)), null, true);
        _withdraw(airdrop, govWallet, hsp20token.getAddress(), ICX.multiply(BigInteger.valueOf(25)), owners[2].getAddress(), true);

        _waitUtilTime(endTime.add(BigInteger.valueOf(1_000_000L)));

        id = BigInteger.ZERO;
        LOG.info("stage 0 info : " + airdrop.info(id));

        LOG.info(">>> claim");
        assertFalse(airdrop.isClaimed(id, owners[2].getAddress()));
        assertFalse(airdrop.isClaimable(id, owners[2].getAddress(), ICX.multiply(BigInteger.valueOf(30)), getProof(hash3)));
        _claim(airdrop, owners[2], id, ZERO_ADDRESS, ICX.multiply(BigInteger.valueOf(30)), getProof(hash3), false);

        LOG.infoExiting();
    }
}
