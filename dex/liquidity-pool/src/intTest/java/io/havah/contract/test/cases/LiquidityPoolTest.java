package io.havah.contract.test.cases;

import foundation.icon.icx.IconService;
import foundation.icon.icx.KeyWallet;
import foundation.icon.icx.Wallet;
import foundation.icon.icx.data.Address;
import foundation.icon.icx.data.TransactionResult;
import foundation.icon.icx.transport.http.HttpProvider;
import foundation.icon.test.Env;
import foundation.icon.test.TestBase;
import foundation.icon.test.TransactionHandler;
import io.havah.contract.test.score.LiquidityPoolScore;
import io.havah.contract.test.score.LiquidityTokenScore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import static foundation.icon.test.Env.LOG;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class LiquidityPoolTest extends TestBase {
    private static final Address ZERO_ADDRESS = new Address("cx0000000000000000000000000000000000000000");
    private static IconService iconService;
    private static TransactionHandler txHandler;
    private static KeyWallet[] wallets = new KeyWallet[2];

    public static BigInteger getTxFee(TransactionResult result) {
        return result.getStepUsed().multiply(result.getStepPrice());
    }

    @BeforeAll
    static void setup() throws Exception {
        Env.Chain chain = Env.getDefaultChain();
        iconService = new IconService(new HttpProvider(chain.getEndpointURL(3)));
        txHandler = new TransactionHandler(iconService, chain);

        // init wallets
        BigInteger amount = ICX.multiply(BigInteger.valueOf(300));
        for(int i=0; i<wallets.length; i++) {
            wallets[i] = KeyWallet.create();
            txHandler.transfer(wallets[i].getAddress(), amount);
            ensureIcxBalance(txHandler, wallets[i].getAddress(), BigInteger.ZERO, amount);
        }
    }

    void _mintToken(Wallet wallet, LiquidityTokenScore token, BigInteger value) throws Exception {
        assertSuccess(token.mint(wallet, value));
        assertEquals(token.balanceOf(wallet.getAddress()), value);
    }

    void _transferToken(Wallet wallet, LiquidityTokenScore token, Address to, BigInteger value) throws Exception {
        BigInteger balance = token.balanceOf(to);
        assertSuccess(token.transfer(wallet, to, value));
        assertEquals(token.balanceOf(to).subtract(balance), value);
    }

    void _setMinter(Wallet wallet, LiquidityTokenScore token, Address minter) throws Exception {
        assertSuccess(token.setMinter(wallet, minter));
        assertEquals(token.minter(), minter);
    }

    void _initialize(Wallet wallet, LiquidityPoolScore pool, Address baseToken, Address quoteToken, Address lpToken) throws Exception {
        assertFailure(pool.initialize(wallet, baseToken, quoteToken, null));
        assertFailure(pool.initialize(wallet, baseToken, null, lpToken));
        assertFailure(pool.initialize(wallet, null, quoteToken, lpToken));

        TransactionResult result = pool.initialize(wallet, baseToken, quoteToken, lpToken);
        assertSuccess(result);
        assertEquals(baseToken, pool.baseToken());
        assertEquals(quoteToken, pool.quoteToken());
        assertEquals(lpToken, pool.lpToken());
    }

    void _approve(Wallet wallet, LiquidityTokenScore token, Address to, BigInteger value) throws Exception {
        assertSuccess(token.approve(wallet, to, value));
        assertEquals(token.allowance(wallet.getAddress(), to), value);
    }

    TransactionResult _add(Wallet wallet, LiquidityPoolScore pool, LiquidityTokenScore base, LiquidityTokenScore quote, LiquidityTokenScore lp, BigInteger value, BigInteger baseValue, BigInteger quoteValue, BigInteger liquidity) throws Exception {
        Address walletAddr = wallet.getAddress();

        BigInteger baseBalance = base == null ? txHandler.getBalance(walletAddr) : base.balanceOf(walletAddr);
        BigInteger quoteBalance = quote.balanceOf(walletAddr);

        TransactionResult result = pool.add(wallet, value, baseValue, quoteValue);
        assertSuccess(result);
        pool.ensureLiquidityAdded(result, baseValue, quoteValue);

        assertEquals(base == null ? getTxFee(result).add(baseValue) : baseValue, baseBalance.subtract(base == null ? txHandler.getBalance(walletAddr) : base.balanceOf(walletAddr)));
        assertEquals(quoteValue, quoteBalance.subtract(quote.balanceOf(walletAddr)));

        BigInteger lpAmount = lp.balanceOf(walletAddr);
        assertEquals(lpAmount, BigInteger.valueOf((long)Math.sqrt(100 * 10000)).multiply(ICX));

        BigInteger baseDecimals = base == null ? BigInteger.valueOf(18) : base.decimals();
        BigInteger quoteDecimals = quote.decimals();
        Map<String, Object> basePrice = pool.getBasePriceInQuote();
        Map<String, Object> calcBasePrice = _getPriceAInB(baseValue, baseDecimals, quoteValue, quoteDecimals);
        assertEquals(basePrice.get("price"), calcBasePrice.get("price"));
        assertEquals(basePrice.get("decimals"), calcBasePrice.get("decimals"));

        Map<String, Object> quotePrice = pool.getQuotePriceInBase();
        Map<String, Object> calcQuotePrice = _getPriceAInB(quoteValue, quoteDecimals, baseValue, baseDecimals);
        assertEquals(quotePrice.get("price"), calcQuotePrice.get("price"));
        assertEquals(quotePrice.get("decimals"), calcQuotePrice.get("decimals"));

        return result;
    }

    TransactionResult _remove(Wallet wallet, LiquidityPoolScore pool, LiquidityTokenScore base, LiquidityTokenScore quote, LiquidityTokenScore lpToken, BigInteger removeValue) throws Exception {
        Address walletAddr = wallet.getAddress();
        Address poolAddr = pool.getAddress();
        
        BigInteger baseBalance = base == null ? txHandler.getBalance(walletAddr) : base.balanceOf(walletAddr);
        BigInteger quoteBalance = quote.balanceOf(walletAddr);
        BigInteger lpAmount = lpToken.balanceOf(walletAddr);
        BigInteger removeAmount = lpAmount.divide(BigInteger.TWO);

        BigInteger baseToWithdraw = base == null ? removeAmount.multiply(txHandler.getBalance(poolAddr)).divide(lpAmount) : removeAmount.multiply(base.balanceOf(poolAddr)).divide(lpAmount);
        BigInteger quoteToWithdraw = removeAmount.multiply(quote.balanceOf(poolAddr)).divide(lpAmount);

        TransactionResult result = pool.remove(wallet, removeValue);
        assertSuccess(result);
        pool.ensureLiquidityRemoved(result, removeValue);

        assertEquals(lpAmount.subtract(removeAmount), lpToken.balanceOf(walletAddr));

        baseBalance = base == null ? baseBalance.add(baseToWithdraw).subtract(getTxFee(result)) : baseBalance.add(baseToWithdraw);
        assertEquals(baseBalance, base == null ? txHandler.getBalance(walletAddr) : base.balanceOf(walletAddr));
        assertEquals(quoteBalance.add(quoteToWithdraw), quote.balanceOf(walletAddr));

        return result;
    }

    void _swap(Wallet wallet, LiquidityPoolScore pool, Address from, Address to, BigInteger value, Address receiver, BigInteger minimumReceive) throws Exception {
        Address poolAddr = pool.getAddress();
        LiquidityTokenScore fromSc = new LiquidityTokenScore(txHandler, from);
        LiquidityTokenScore toSc = new LiquidityTokenScore(txHandler, to);

        BigInteger oldFrom = from.equals(ZERO_ADDRESS) ? txHandler.getBalance(poolAddr) : fromSc.balanceOf(poolAddr);
        BigInteger oldTo = to.equals(ZERO_ADDRESS) ? txHandler.getBalance(poolAddr) : toSc.balanceOf(poolAddr);
        BigInteger lpPee = value.multiply(pool.fee()).divide(pool.feeScale());
        BigInteger inputWithoutFees = value.subtract(lpPee);

        BigInteger newFrom = oldFrom.add(inputWithoutFees);
        BigInteger newTo = (oldFrom.multiply(oldTo)).divide(newFrom);

        TransactionResult result = pool.swap(wallet, from, to, value, receiver, minimumReceive);
        assertSuccess(result);
        pool.ensureSwap(result, from, to, value);

        assertEquals(oldFrom.add(value), from.equals(ZERO_ADDRESS) ? txHandler.getBalance(poolAddr) : fromSc.balanceOf(poolAddr));
        assertEquals(newTo, to.equals(ZERO_ADDRESS) ? txHandler.getBalance(poolAddr) : toSc.balanceOf(poolAddr));
    }

    Map<String, Object> _getPriceAInB(BigInteger tokenA, BigInteger tokenADecimals, BigInteger tokenB, BigInteger tokenBDecimals) {
        Map<String, Object> map = new HashMap<>();
        switch (tokenADecimals.compareTo(tokenBDecimals)) {
            case -1:
                map.put("price", tokenB.divide(tokenA.multiply(BigInteger.TEN.pow(tokenBDecimals.subtract(tokenADecimals).intValue()))));
                map.put("decimals", tokenBDecimals);
                break;
            case 1:
                map.put("price", tokenB.multiply(BigInteger.TEN.pow(tokenADecimals.subtract(tokenBDecimals).intValue())).divide(tokenA));
                map.put("decimals", tokenADecimals);
                break;
            default:
                map.put("price", tokenB.divide(tokenA));
                map.put("decimals", tokenADecimals);
                break;
        }
        return map;
    }

    @Test
    void Havah2TokenPoolTest() throws Exception {
        LOG.infoEntering("HVH-Token Pool Test");

        BigInteger baseValue = ICX.multiply(BigInteger.valueOf(100));
        BigInteger quoteValue = ICX.multiply(BigInteger.valueOf(10000));

        LOG.info("deploy pool");
        LiquidityPoolScore poolScore = LiquidityPoolScore.mustDeploy(txHandler, wallets[0]);

        LOG.info("deploy tokens");
        LiquidityTokenScore lpToken = LiquidityTokenScore.mustDeploy(txHandler, wallets[0], "LiquidityToken", "HLT", BigInteger.valueOf(18));
        LiquidityTokenScore quoteToken = LiquidityTokenScore.mustDeploy(txHandler, wallets[0], "QuoteToken", "HQT", BigInteger.valueOf(10));

        LOG.info("lpToken setMinter to pool");
        _setMinter(wallets[0], lpToken, poolScore.getAddress());

        LOG.info("initialize");
        _initialize(wallets[0], poolScore, ZERO_ADDRESS, quoteToken.getAddress(), lpToken.getAddress());

        LOG.info("mint quoteToken");
        _mintToken(wallets[0], quoteToken, quoteValue);

        LOG.info("transfer quoteToken");
        _transferToken(wallets[0], quoteToken, wallets[1].getAddress(), quoteValue);

        LOG.info("approve quote");
        _approve(wallets[1], quoteToken, poolScore.getAddress(), quoteValue);

        LOG.info("add");
        _add(wallets[1], poolScore, null, quoteToken, lpToken, baseValue, baseValue, quoteValue, BigInteger.valueOf((long)Math.sqrt(100 * 10000)).multiply(ICX));
        BigInteger lpAmount = lpToken.balanceOf(wallets[1].getAddress());

        LOG.info("approve lp");
        _approve(wallets[1], lpToken, poolScore.getAddress(), lpAmount);

        LOG.info("remove");
        BigInteger removeAmount = lpAmount.divide(BigInteger.TWO);
        _remove(wallets[1], poolScore, null, quoteToken, lpToken, removeAmount);
        assertEquals(lpAmount.subtract(removeAmount), lpToken.balanceOf(wallets[1].getAddress()));

        LOG.info("swap");
        BigInteger swapValue = BigInteger.valueOf(20).multiply(ICX);
        _swap(wallets[1], poolScore, ZERO_ADDRESS, quoteToken.getAddress(), swapValue, wallets[1].getAddress(), BigInteger.ZERO);

        LOG.infoExiting();
    }

    @Test
    void Token2TokenPoolTest() throws Exception {
        LOG.infoEntering("Token-Token Pool Test");

        BigInteger baseValue = ICX.multiply(BigInteger.valueOf(100));
        BigInteger quoteValue = ICX.multiply(BigInteger.valueOf(10000));

        LOG.info("deploy pool");
        LiquidityPoolScore poolScore = LiquidityPoolScore.mustDeploy(txHandler, wallets[0]);

        LOG.info("deploy tokens");
        LiquidityTokenScore lpToken = LiquidityTokenScore.mustDeploy(txHandler, wallets[0], "LiquidityToken", "HLT", BigInteger.valueOf(18));
        LiquidityTokenScore baseToken = LiquidityTokenScore.mustDeploy(txHandler, wallets[0], "BaseToken", "HBT", BigInteger.valueOf(10));
        LiquidityTokenScore quoteToken = LiquidityTokenScore.mustDeploy(txHandler, wallets[0], "QuoteToken", "HQT", BigInteger.valueOf(15));

        LOG.info("lpToken setMinter to pool");
        _setMinter(wallets[0], lpToken, poolScore.getAddress());

        LOG.info("initialize");
        _initialize(wallets[0], poolScore, baseToken.getAddress(), quoteToken.getAddress(), lpToken.getAddress());

        LOG.info("mint baseToken");
        _mintToken(wallets[0], baseToken, baseValue);

        LOG.info("transfer baseToken");
        _transferToken(wallets[0], baseToken, wallets[1].getAddress(), baseValue);

        LOG.info("approve base");
        _approve(wallets[1], baseToken, poolScore.getAddress(), baseValue);

        LOG.info("mint quoteToken");
        _mintToken(wallets[0], quoteToken, quoteValue);

        LOG.info("transfer quoteToken");
        _transferToken(wallets[0], quoteToken, wallets[1].getAddress(), quoteValue);

        LOG.info("approve quote");
        _approve(wallets[1], quoteToken, poolScore.getAddress(), quoteValue);

        LOG.info("add");
        _add(wallets[1], poolScore, baseToken, quoteToken, lpToken, BigInteger.ZERO, baseValue, quoteValue, BigInteger.valueOf((long)Math.sqrt(100 * 10000)).multiply(ICX));
        BigInteger lpAmount = lpToken.balanceOf(wallets[1].getAddress());

        LOG.info("approve lp");
        _approve(wallets[1], lpToken, poolScore.getAddress(), lpAmount);

        LOG.info("remove");
        BigInteger removeAmount = lpAmount.divide(BigInteger.TWO);
        _remove(wallets[1], poolScore, baseToken, quoteToken, lpToken, removeAmount);
        assertEquals(lpAmount.subtract(removeAmount), lpToken.balanceOf(wallets[1].getAddress()));

        LOG.info("swap");
        BigInteger swapValue = BigInteger.valueOf(20).multiply(ICX);
        _approve(wallets[1], baseToken, poolScore.getAddress(), swapValue);
        _swap(wallets[1], poolScore, baseToken.getAddress(), quoteToken.getAddress(), swapValue, wallets[1].getAddress(), BigInteger.ZERO);

        LOG.infoExiting();
    }
}
