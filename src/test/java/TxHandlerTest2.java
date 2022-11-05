import org.junit.jupiter.api.*;

import java.security.*;

/**
 * Test class for TxHandler.handleTxs
 */
@SuppressWarnings("FinalizeCalledExplicitly")
public class TxHandlerTest2 {

    private static TxHandler txHandler;
    private static Transaction tx0; // Initial Transaction in UTXO pool
    private static Transaction tx1,tx2,tx3; // Three transactions depends on each other [tx1 <- tx2 <- tx3]
    private static Transaction tx4, tx5; // Two transactions where {tx1, tx4} uses tx0. [tx4 <- tx5]
    private static Transaction tx6; // Valid transaction not in UTXO pool
    private static Transaction tx7; // Invalid transaction [tx6 <- tx7]
    private static Transaction tx8, tx9; // Two valid transaction where [tx8 <- tx9]

    @BeforeAll
    static void beforeAll() throws NoSuchAlgorithmException, NoSuchProviderException, SignatureException, InvalidKeyException {
        KeyPair personA = TestingUtils.generateNewKeyPair(); // Money owner (200 BTC)
        KeyPair personB = TestingUtils.generateNewKeyPair(); // Recipient
        KeyPair personC = TestingUtils.generateNewKeyPair(); // Recipient
        KeyPair personD = TestingUtils.generateNewKeyPair(); // Recipient

        tx0 = new Transaction();
        tx0.addInput(null, 0);
        tx0.addOutput(100, personA.getPublic());
        tx0.addOutput(100, personA.getPublic());
        tx0.finalize();

        tx1 = new Transaction();
        tx1.addInput(tx0.getHash(), 0);
        tx1.addOutput(100, personB.getPublic());
        tx1.addSignature(TestingUtils.sign(personA.getPrivate(), tx1.getRawDataToSign(0)), 0);
        tx1.finalize();

        tx2 = new Transaction();
        tx2.addInput(tx1.getHash(), 0);
        tx2.addOutput(100, personC.getPublic());
        tx2.addSignature(TestingUtils.sign(personB.getPrivate(), tx2.getRawDataToSign(0)), 0);
        tx2.finalize();

        tx3 = new Transaction();
        tx3.addInput(tx2.getHash(), 0);
        tx3.addOutput(100, personD.getPublic());
        tx3.addSignature(TestingUtils.sign(personC.getPrivate(), tx3.getRawDataToSign(0)), 0);
        tx3.finalize();

        tx4 = new Transaction();
        tx4.addInput(tx0.getHash(), 0);
        tx4.addOutput(100, personC.getPublic());
        tx4.addSignature(TestingUtils.sign(personA.getPrivate(), tx4.getRawDataToSign(0)), 0);
        tx4.finalize();

        tx5 = new Transaction();
        tx5.addInput(tx4.getHash(), 0);
        tx5.addOutput(100, personD.getPublic());
        tx5.addSignature(TestingUtils.sign(personC.getPrivate(), tx5.getRawDataToSign(0)), 0);
        tx5.finalize();

        tx6 = new Transaction();
        tx6.addInput(null, 0);
        tx6.addOutput(50, personD.getPublic());
        tx6.finalize();

        tx7 = new Transaction();
        tx7.addInput(tx6.getHash(), 0);
        tx7.addOutput(50, personC.getPublic());
        tx7.addSignature(TestingUtils.sign(personD.getPrivate(), tx7.getRawDataToSign(0)), 0);
        tx7.finalize();

        tx8 = new Transaction();
        tx8.addInput(tx0.getHash(), 1);
        tx8.addOutput(100, personB.getPublic());
        tx8.addSignature(TestingUtils.sign(personA.getPrivate(), tx8.getRawDataToSign(0)), 0);
        tx8.finalize();

        tx9 = new Transaction();
        tx9.addInput(tx8.getHash(), 0);
        tx9.addOutput(100, personC.getPublic());
        tx9.addSignature(TestingUtils.sign(personB.getPrivate(), tx9.getRawDataToSign(0)), 0);
        tx9.finalize();
    }

    @BeforeEach
    void setUp() {
        UTXOPool utxoPool = new UTXOPool();
        utxoPool.addUTXO(new UTXO(tx0.getHash(),0), tx0.getOutput(0));
        utxoPool.addUTXO(new UTXO(tx0.getHash(),1), tx0.getOutput(1));
        txHandler = new TxHandler(utxoPool);
    }

    @Test
    void test0() {
        Transaction[] possibleTxs = new Transaction[]{tx1, tx2, tx3};
        Transaction[] result = txHandler.handleTxs(possibleTxs);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals(result[0], tx1);
        Assertions.assertEquals(result[1], tx2);
        Assertions.assertEquals(result[2], tx3);
    }

    @Test
    void test1() {
        Transaction[] possibleTxs = new Transaction[]{tx3, tx2, tx1};
        Transaction[] result = txHandler.handleTxs(possibleTxs);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals(result[0], tx1);
        Assertions.assertEquals(result[1], tx2);
        Assertions.assertEquals(result[2], tx3);
    }

    @Test
    void test2() {
        Transaction[] possibleTxs = new Transaction[]{tx3, tx2, tx1, tx4, tx5, tx7, tx9, tx8};
        Transaction[] result = txHandler.handleTxs(possibleTxs);
        Assertions.assertEquals(5, result.length);
        Assertions.assertEquals(result[0], tx1);
        Assertions.assertEquals(result[1], tx2);
        Assertions.assertEquals(result[2], tx3);
        Assertions.assertEquals(result[3], tx8);
        Assertions.assertEquals(result[4], tx9);
    }

    @Test
    void test3() {
        Transaction[] possibleTxs = new Transaction[]{tx1, tx4};
        Transaction[] result = txHandler.handleTxs(possibleTxs);
        Assertions.assertEquals(result[0], tx1);
    }

    @Test
    void test4() {
        Transaction[] possibleTxs = new Transaction[]{tx4, tx1, tx2, tx5, tx9, tx8};
        Transaction[] result = txHandler.handleTxs(possibleTxs);
        Assertions.assertEquals(4, result.length);
        Assertions.assertEquals(result[0], tx4);
        Assertions.assertEquals(result[1], tx5);
        Assertions.assertEquals(result[2], tx8);
        Assertions.assertEquals(result[3], tx9);
    }

    @Test
    void test5() {
        Transaction[] possibleTxs = new Transaction[]{tx1, tx4, tx3, tx2};
        Transaction[] result = txHandler.handleTxs(possibleTxs);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals(result[0], tx1);
        Assertions.assertEquals(result[1], tx2);
        Assertions.assertEquals(result[2], tx3);
    }

    @Test
    void test6() {
        Transaction[] possibleTxs = new Transaction[]{tx2, tx3, tx5, tx7, tx9};
        Transaction[] result = txHandler.handleTxs(possibleTxs);
        Assertions.assertEquals(0, result.length);
    }
}
