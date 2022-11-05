import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.*;

/**
 * Test class for txHandler.isValidTx
 */
@SuppressWarnings("FinalizeCalledExplicitly")
class TxHandlerTest {

    private static TxHandler txHandler;
    private static Transaction tx1; // Gives 30 BTC (10,20) to person A
    private static Transaction tx2; // Gives 40 BTC (15,25) to person A
    private static Transaction tx3; // Gives 50 BTC (50) to person C
    private static KeyPair personA; // Money owner (70 BTC)
    private static KeyPair personB; // Recipient
    private static KeyPair personC; // Money owner (50 BTC)

    @BeforeAll
    static void beforeAll() throws NoSuchAlgorithmException, NoSuchProviderException {
        // Initializing persons' keys
        personA = TestingUtils.generateNewKeyPair();
        personB = TestingUtils.generateNewKeyPair();
        personC = TestingUtils.generateNewKeyPair();

        // Initializing Transactions
        tx1 = new Transaction();
        tx1.addInput(null, 0);
        tx1.addOutput(10, personA.getPublic());
        tx1.addOutput(20, personA.getPublic());
        tx1.finalize();

        tx2 = new Transaction();
        tx2.addInput(null,0);
        tx2.addOutput(15, personA.getPublic());
        tx2.addOutput(25, personA.getPublic());
        tx2.finalize();

        tx3 = new Transaction();
        tx3.addInput(null, 0);
        tx3.addOutput(50, personC.getPublic());
        tx3.finalize();

        // Initializing utxo pool and txHandler
        UTXOPool utxoPool = new UTXOPool();
        utxoPool.addUTXO(new UTXO(tx1.getHash(), 0), tx1.getOutput(0));
        utxoPool.addUTXO(new UTXO(tx1.getHash(), 1), tx1.getOutput(1));
        utxoPool.addUTXO(new UTXO(tx2.getHash(), 0), tx2.getOutput(0));
        utxoPool.addUTXO(new UTXO(tx2.getHash(), 1), tx2.getOutput(1));
        utxoPool.addUTXO(new UTXO(tx3.getHash(), 0), tx3.getOutput(0));
        txHandler = new TxHandler(utxoPool);
    }

    /**
     * Testing if the transaction have zero inputs and outputs.
     */
    @Test
    void test0() {
        Transaction newTx = new Transaction();
        newTx.finalize();

        Assertions.assertFalse(txHandler.isValidTx(newTx));
    }

    /**
	 * We have two transactions tx1, and tx2. Assume tx1 is valid, and its
	 * output is already in the UTXO pool. tx2 tries to spend that output. If
	 * tx2 provides a valid signature, and does not try to spend more than the
	 * output value, while specifying everything correctly it should be
	 * considered valid.
     * This test case is already provided by the doctor.
    */
    @Test
    void test1() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        Transaction newTx = new Transaction();
        newTx.addOutput(9, personB.getPublic());
        newTx.addInput(tx1.getHash(), 0);
        newTx.addSignature(TestingUtils.sign(personA.getPrivate(), newTx.getRawDataToSign(0)), 0);
        newTx.finalize();

        Assertions.assertTrue(txHandler.isValidTx(newTx));
    }

    /**
     * Testing the case where there is two transactions in tx2 which points to the
     * same transaction in UTXO as (Double spending in same transaction).
     * TESTING constraints (1) & (3)
     */
    @Test
    void test2() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        Transaction newTx = new Transaction();
        newTx.addInput(tx1.getHash(), 0);
        newTx.addInput(tx1.getHash(), 0);
        newTx.addOutput(20, personB.getPublic());
        newTx.addSignature(TestingUtils.sign(personA.getPrivate(), newTx.getRawDataToSign(0)), 0);
        newTx.addSignature(TestingUtils.sign(personA.getPrivate(), newTx.getRawDataToSign(1)), 1);
        newTx.finalize();
        Assertions.assertFalse(txHandler.isValidTx(newTx));
    }

    /**
     * Valid Transaction with many inputs and many outputs.
     */
    @Test
    void test3() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        Transaction newTx = new Transaction();
        newTx.addInput(tx1.getHash(), 0);
        newTx.addInput(tx1.getHash(), 1);
        newTx.addInput(tx2.getHash(), 0);
        newTx.addInput(tx2.getHash(), 1);
        newTx.addOutput(70, personB.getPublic());
        newTx.addSignature(TestingUtils.sign(personA.getPrivate(), newTx.getRawDataToSign(0)),0);
        newTx.addSignature(TestingUtils.sign(personA.getPrivate(), newTx.getRawDataToSign(1)),1);
        newTx.addSignature(TestingUtils.sign(personA.getPrivate(), newTx.getRawDataToSign(2)),2);
        newTx.addSignature(TestingUtils.sign(personA.getPrivate(), newTx.getRawDataToSign(3)),3);
        newTx.finalize();
        Assertions.assertTrue(txHandler.isValidTx(newTx));
    }

    /**
     * Invalid transaction where we use the same input twice in the transaction.
     */
    @Test
    void test4() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        Transaction newTx = new Transaction();
        newTx.addInput(tx1.getHash(), 1);
        newTx.addInput(tx1.getHash(), 1);
        newTx.addInput(tx2.getHash(), 1);
        newTx.addInput(tx2.getHash(), 1);
        newTx.addOutput(70, personB.getPublic());
        newTx.addSignature(TestingUtils.sign(personA.getPrivate(), newTx.getRawDataToSign(0)),0);
        newTx.addSignature(TestingUtils.sign(personA.getPrivate(), newTx.getRawDataToSign(1)),1);
        newTx.addSignature(TestingUtils.sign(personA.getPrivate(), newTx.getRawDataToSign(2)),2);
        newTx.addSignature(TestingUtils.sign(personA.getPrivate(), newTx.getRawDataToSign(3)),3);
        newTx.finalize();
        Assertions.assertFalse(txHandler.isValidTx(newTx));
    }

    /**
     * Failure test where the initial transaction is valid but not in UTXO.
     */
    @Test
    void test5() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        Transaction tx3 = new Transaction();
        tx3.addInput(null, 0);
        tx3.addOutput(50, personA.getPublic());
        tx3.finalize();

        Transaction newTx = new Transaction();
        newTx.addInput(tx3.getHash(), 0);
        newTx.addOutput(45, personB.getPublic());
        newTx.addSignature(TestingUtils.sign(personA.getPrivate(), newTx.getRawDataToSign(0)), 0);
        newTx.finalize();

        Assertions.assertFalse(txHandler.isValidTx(newTx));
    }

    /**
     * Failure test where a transaction is signed by inauthentic user.
     */
    @Test
    void test6() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        Transaction newTx = new Transaction();
        newTx.addInput(tx1.getHash(), 0);
        newTx.addOutput(45, personB.getPublic());
        // Person B spends person A's money to himself
        newTx.addSignature(TestingUtils.sign(personB.getPrivate(), newTx.getRawDataToSign(0)), 0);
        newTx.finalize();

        Assertions.assertFalse(txHandler.isValidTx(newTx));
    }

    /**
     * Failure test having negative values in output.
     */
    @Test
    void test7() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        Transaction newTx = new Transaction();
        newTx.addInput(tx1.getHash(), 0);
        newTx.addOutput(-5.0, personB.getPublic());
        newTx.addSignature(TestingUtils.sign(personA.getPrivate(), newTx.getRawDataToSign(0)),0);
        newTx.finalize();

        Assertions.assertFalse(txHandler.isValidTx(newTx));
    }

    /**
     * Failure test output sum greater than input sum.
     */
    @Test
    void test8() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        Transaction newTx = new Transaction();
        newTx.addInput(tx1.getHash(), 0);
        newTx.addOutput(10.1, personB.getPublic());
        newTx.addSignature(TestingUtils.sign(personA.getPrivate(), newTx.getRawDataToSign(0)),0);
        newTx.finalize();

        Assertions.assertFalse(txHandler.isValidTx(newTx));
    }

    /**
     * Valid test where inputs come from different users.
     */
    @Test
    void test9() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        Transaction newTx = new Transaction();
        newTx.addInput(tx1.getHash(), 0);
        newTx.addInput(tx3.getHash(), 0);
        newTx.addInput(tx2.getHash(), 0);
        newTx.addOutput(75, personB.getPublic());
        newTx.addSignature(TestingUtils.sign(personA.getPrivate(), newTx.getRawDataToSign(0)), 0);
        newTx.addSignature(TestingUtils.sign(personC.getPrivate(), newTx.getRawDataToSign(1)), 1);
        newTx.addSignature(TestingUtils.sign(personA.getPrivate(), newTx.getRawDataToSign(2)), 2);
        newTx.finalize();

        Assertions.assertTrue(txHandler.isValidTx(newTx));
    }

    /**
     * Valid test for transaction with no outputs
     */
    @Test
    void test10() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        Transaction newTx = new Transaction();
        newTx.addInput(tx1.getHash(), 0);
        newTx.addSignature(TestingUtils.sign(personA.getPrivate(), newTx.getRawDataToSign(0)),0);
        newTx.finalize();

        Assertions.assertTrue(txHandler.isValidTx(newTx));
    }

    /**
     * Invalid test for a transaction with no inputs
     */
    @Test
    void test11() {
        Transaction newTx = new Transaction();
        newTx.addOutput(15, personB.getPublic());
        newTx.finalize();

        Assertions.assertFalse(txHandler.isValidTx(newTx));
    }
}