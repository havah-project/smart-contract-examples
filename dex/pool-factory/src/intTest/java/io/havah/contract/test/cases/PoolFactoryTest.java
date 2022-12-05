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
import io.havah.contract.test.score.LiquidityTokenScore;
import io.havah.contract.test.score.PoolFactoryScore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;

import static foundation.icon.test.Env.LOG;
import static foundation.icon.test.score.Score.getFilePath;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PoolFactoryTest extends TestBase {
    private static final Address ZERO_ADDRESS = new Address("cx0000000000000000000000000000000000000000");
    private static IconService iconService;
    private static TransactionHandler txHandler;

    private static byte[] tokenBytes;
    private static byte[] poolBytes;

    public static BigInteger getTxFee(TransactionResult result) {
        return result.getStepUsed().multiply(result.getStepPrice());
    }

    @BeforeAll
    static void setup() throws Exception {
        Env.Chain chain = Env.getDefaultChain();
        iconService = new IconService(new HttpProvider(chain.getEndpointURL(3)));
        txHandler = new TransactionHandler(iconService, chain);

        tokenBytes = Files.readAllBytes(Path.of(getFilePath("liquidity-token")));
        poolBytes = Files.readAllBytes(Path.of(getFilePath("liquidity-pool")));
    }

    void _setPoolContract(Wallet wallet, PoolFactoryScore factory, byte[] contract) throws Exception {
        TransactionResult result = factory.setPoolContract(wallet, contract);
        assertSuccess(result);
    }

    void _setTokenContract(Wallet wallet, PoolFactoryScore factory, byte[] contract) throws Exception {
        TransactionResult result = factory.setLpTokenContract(wallet, contract);
        assertSuccess(result);
    }

    Address _createPool(Wallet wallet, PoolFactoryScore factory, Address base, Address quote) throws Exception {
        TransactionResult result = factory.createPool(wallet, base, quote);
        assertSuccess(result);
        return factory.ensurePoolCreated(result, base, quote);
    }

    Address _findPool(PoolFactoryScore factory, Address base, Address quote) throws Exception {
        Address find = factory.findPool(base, quote);
        assertNotNull(find, "not found factory");
        assertEquals(find, factory.findPool(quote, base));
        return find;
    }

    @Test
    void Havah2TokenPoolFactoryTest() throws Exception {
        LOG.infoEntering("Havah-Token PoolFactoryTest");

        BigInteger amount = ICX.multiply(BigInteger.valueOf(300));
        KeyWallet ownerWallet = KeyWallet.create();
        txHandler.transfer(ownerWallet.getAddress(), amount);
        ensureIcxBalance(txHandler, ownerWallet.getAddress(), BigInteger.ZERO, amount);

        LOG.info("deploy poolFactory");
        PoolFactoryScore poolFactory = PoolFactoryScore.mustDeploy(txHandler, ownerWallet);
        assertNotNull(poolFactory, "factory is not deployed");

        LOG.info("set pool contract");
        _setPoolContract(ownerWallet, poolFactory, poolBytes);
        LOG.info("set token contract");
        _setTokenContract(ownerWallet, poolFactory, tokenBytes);

        LOG.info("deploy tokens");
        LiquidityTokenScore quoteToken = LiquidityTokenScore.mustDeploy(txHandler, ownerWallet, "QuoteToken", "HQT");
        assertNotNull(quoteToken, "quoteToken is not deployed");

        LOG.info("create pool");
        Address pool = _createPool(ownerWallet, poolFactory, ZERO_ADDRESS, quoteToken.getAddress());

        LOG.info("find pool");
        assertEquals(pool, _findPool(poolFactory, ZERO_ADDRESS, quoteToken.getAddress()));

        LOG.infoExiting();
    }

    @Test
    void Token2TokenPoolFactoryTest() throws Exception {
        LOG.infoEntering("Token-Token PoolFactoryTest");

        BigInteger amount = ICX.multiply(BigInteger.valueOf(300));
        KeyWallet ownerWallet = KeyWallet.create();
        txHandler.transfer(ownerWallet.getAddress(), amount);
        ensureIcxBalance(txHandler, ownerWallet.getAddress(), BigInteger.ZERO, amount);

        LOG.info("deploy poolFactory");
        PoolFactoryScore poolFactory = PoolFactoryScore.mustDeploy(txHandler, ownerWallet);
        assertNotNull(poolFactory, "factory is not deployed");

        LOG.info("set pool contract");
        _setPoolContract(ownerWallet, poolFactory, poolBytes);
        LOG.info("set token contract");
        _setTokenContract(ownerWallet, poolFactory, tokenBytes);

        LOG.info("deploy tokens");
        LiquidityTokenScore baseToken = LiquidityTokenScore.mustDeploy(txHandler, ownerWallet, "BaseToken", "HBT");
        LiquidityTokenScore quoteToken = LiquidityTokenScore.mustDeploy(txHandler, ownerWallet, "QuoteToken", "HQT");
        assertNotNull(baseToken, "baseToken is not deployed");
        assertNotNull(quoteToken, "quoteToken is not deployed");

        LOG.info("create pool");
        Address pool = _createPool(ownerWallet, poolFactory, baseToken.getAddress(), quoteToken.getAddress());

        LOG.info("find pool");
        assertEquals(pool, _findPool(poolFactory, baseToken.getAddress(), quoteToken.getAddress()));

        LOG.infoExiting();
    }
}
