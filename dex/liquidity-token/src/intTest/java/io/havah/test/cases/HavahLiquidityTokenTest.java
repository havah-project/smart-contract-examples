package io.havah.test.cases;

import foundation.icon.icx.IconService;
import foundation.icon.icx.KeyWallet;
import foundation.icon.icx.data.Address;
import foundation.icon.icx.data.TransactionResult;
import foundation.icon.icx.transport.http.HttpProvider;
import foundation.icon.test.Env;
import foundation.icon.test.ResultTimeoutException;
import foundation.icon.test.TestBase;
import foundation.icon.test.TransactionHandler;
import io.havah.test.score.HavahLiquidityTokenScore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;

import static foundation.icon.test.Env.LOG;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class HavahLiquidityTokenTest extends TestBase {
    protected static final Address ZERO_ADDRESS = new Address("hx0000000000000000000000000000000000000000");
    private static IconService iconService;
    private static TransactionHandler txHandler;

    private static KeyWallet[] wallets;

    private static KeyWallet ownerWallet;

    private static HavahLiquidityTokenScore tokenScore;

    private static BigInteger totalSupply;

    @BeforeAll
    static void setup() throws Exception {
        Env.Chain chain = Env.getDefaultChain();
        iconService = new IconService(new HttpProvider(chain.getEndpointURL(3)));
        txHandler = new TransactionHandler(iconService, chain);

        // init wallets
        wallets = new KeyWallet[3];
        BigInteger amount = ICX.multiply(BigInteger.valueOf(300));
        for (int i = 0; i < wallets.length; i++) {
            wallets[i] = KeyWallet.create();
            txHandler.transfer(wallets[i].getAddress(), amount);
        }
        ownerWallet = KeyWallet.create();
        txHandler.transfer(ownerWallet.getAddress(), amount);
        for (KeyWallet wallet : wallets) {
            ensureIcxBalance(txHandler, wallet.getAddress(), BigInteger.ZERO, amount);
        }
        ensureIcxBalance(txHandler, ownerWallet.getAddress(), BigInteger.ZERO, amount);

        // deploy HLT SCORE
        tokenScore = HavahLiquidityTokenScore.mustDeploy(txHandler, ownerWallet);
        LOG.info("HLTScore : " + tokenScore.getAddress());
    }

    @Test
    void name() throws IOException {
        LOG.infoEntering("name");
        assertEquals(true, HavahLiquidityTokenScore.name.equals(tokenScore.name()));
        LOG.infoExiting();
    }

    @Test
    void symbol() throws IOException {
        LOG.infoEntering("symbol");
        assertEquals(true, HavahLiquidityTokenScore.symbol.equals(tokenScore.symbol()));
        LOG.infoExiting();
    }

    @Test
    void decimals() throws IOException {
        LOG.infoEntering("decimals");
        assertEquals(true, BigInteger.ZERO.compareTo(tokenScore.decimals()) == 0);
        LOG.infoExiting();
    }

    @Test
    void totalSupply() throws IOException {
        LOG.infoEntering("totalSupply");
        LOG.info("totalSupply : " + tokenScore.totalSupply());
        LOG.infoExiting();
    }

    @Test
    void balanceOf() throws IOException {
        LOG.infoEntering("balanceOf");
        LOG.info("ownerWallet balanceOf : " + tokenScore.balanceOf(ownerWallet.getAddress()));
        LOG.infoExiting();
    }

    @Test
    void mintAndBurn() throws IOException, ResultTimeoutException {
        LOG.infoEntering("mintAndBurn");
        BigInteger amount = BigInteger.TWO;
        totalSupply = tokenScore.totalSupply();

        // mint and transfer
        LOG.info("mint and transfer");
        assertFailure(tokenScore.mint(wallets[0], amount));
        TransactionResult result = tokenScore.mint(ownerWallet, amount);
        assertSuccess(result);
        result = tokenScore.transfer(ownerWallet, wallets[0].getAddress(), amount);
        assertSuccess(result);
        tokenScore.ensureTransfer(result, ownerWallet.getAddress(), wallets[0].getAddress(), amount);

        // totalSupply
        LOG.info("totalSupply");
        totalSupply = totalSupply.add(amount);
        assertEquals(true, totalSupply.compareTo(tokenScore.totalSupply()) == 0);

        // balance
        LOG.info("balance");
        assertEquals(true, amount.compareTo(tokenScore.balanceOf(wallets[0].getAddress())) == 0);

        // burn
        LOG.info("burn");
        result = tokenScore.burn(wallets[0], amount);
        assertSuccess(result);
        tokenScore.ensureTransfer(result, wallets[0].getAddress(), ZERO_ADDRESS, amount);

        totalSupply = totalSupply.subtract(amount);
        LOG.infoExiting();
    }

    @Test
    void approveAndTranfer() throws IOException, ResultTimeoutException {
        LOG.infoEntering("approveAndTranfer");
        BigInteger amount = BigInteger.TEN;
        totalSupply = tokenScore.totalSupply();

        // mint
        LOG.info("mint");
        assertSuccess(tokenScore.mint(ownerWallet, amount));

        // totalSupply
        LOG.info("totalSupply");
        totalSupply = totalSupply.add(amount);
        assertEquals(true, totalSupply.compareTo(tokenScore.totalSupply()) == 0);

        // transfer
        LOG.info("transfer");
        assertSuccess(tokenScore.transfer(ownerWallet, wallets[0].getAddress(), amount));

        // approve
        LOG.info("approve");
        TransactionResult result = tokenScore.approve(wallets[0], wallets[1].getAddress(), amount);
        assertSuccess(result);
        tokenScore.ensureApproval(result, wallets[0].getAddress(), wallets[1].getAddress(), amount);

        // allowance
        LOG.info("allowance");
        assertEquals(true, tokenScore.allowance(wallets[0].getAddress(), wallets[1].getAddress()).compareTo(amount) == 0);

        // transferFrom
        LOG.info("transferFrom");
        assertFailure(tokenScore.transferFrom(ownerWallet, wallets[0].getAddress(), wallets[2].getAddress(), amount));
        assertFailure(tokenScore.transferFrom(wallets[0], wallets[0].getAddress(), wallets[2].getAddress(), amount));
        result = tokenScore.transferFrom(wallets[1], wallets[0].getAddress(), wallets[2].getAddress(), amount);
        assertSuccess(result);
        tokenScore.ensureTransfer(result, wallets[0].getAddress(), wallets[2].getAddress(), amount);

        // transfer and burn
        LOG.info("transfer and burn");
        assertSuccess(tokenScore.transfer(wallets[2], ownerWallet.getAddress(), amount));
        result = tokenScore.burn(ownerWallet, amount);
        assertSuccess(result);
        tokenScore.ensureTransfer(result, ownerWallet.getAddress(), ZERO_ADDRESS, amount);

        totalSupply = totalSupply.subtract(amount);
        LOG.infoExiting();
    }
}
