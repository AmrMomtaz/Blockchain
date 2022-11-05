import org.junit.jupiter.api.*;

import java.security.*;

/**
 * Test class for the blockchain. It implements the scenario discussed in the hint lecture.
 */
@SuppressWarnings("FinalizeCalledExplicitly")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BlockChainTest {

    private static KeyPair person; // Owns 100 BTC
    private static KeyPair personA;
    private static KeyPair personB;
    private static KeyPair personC;
    private static KeyPair personD;
    private static KeyPair personE;
    private static KeyPair personF;
    private static KeyPair personX;
    private static BlockChain blockChain;
    private static Block genesisBlock;
    private static Block blockA;
    private static Block blockB;
    private static Block blockC;
    private static Block blockD;
    private static Block blockE;
    private static Block blockF;

    @BeforeAll
    static void beforeAll() throws NoSuchAlgorithmException {
        person = TestingUtils.generateNewKeyPair();
        personA = TestingUtils.generateNewKeyPair();
        personB = TestingUtils.generateNewKeyPair();
        personC = TestingUtils.generateNewKeyPair();
        personD = TestingUtils.generateNewKeyPair();
        personE = TestingUtils.generateNewKeyPair();
        personF = TestingUtils.generateNewKeyPair();
        personX = TestingUtils.generateNewKeyPair();
    }

    @Test
    @Order(1)
    void testInitialization() {
        Transaction tx1 = new Transaction();
        tx1.addInput(null, 0);
        tx1.addOutput(10, person.getPublic());
        tx1.addOutput(15, person.getPublic());
        tx1.finalize();

        Transaction tx2 = new Transaction();
        tx2.addInput(null, 0);
        tx2.addOutput(20, person.getPublic());
        tx2.addOutput(30, person.getPublic());
        tx2.finalize();

        genesisBlock = new Block(null, person.getPublic());
        genesisBlock.addTransaction(tx1);
        genesisBlock.addTransaction(tx2);
        genesisBlock.finalize();

        blockChain = new BlockChain(genesisBlock);
    }

    @Test
    @Order(2)
    void addBlockA() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        Assertions.assertEquals(blockChain.getMaxHeightBlock(), genesisBlock);

        Transaction tx = new Transaction();
        tx.addOutput(10, personA.getPublic());
        tx.addInput(genesisBlock.getTransaction(0).getHash(), 0);
        tx.addSignature(TestingUtils.sign(person.getPrivate(), tx.getRawDataToSign(0)), 0);
        tx.finalize();

        blockChain.addTransaction(tx);

        blockA = new Block(genesisBlock.getHash(), personA.getPublic());
        blockA.addTransaction(tx);
        blockA.finalize();
        Assertions.assertTrue(blockChain.addBlock(blockA));
    }

    @Test
    @Order(3)
    void addBlockB_C() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        Assertions.assertEquals(blockA, blockChain.getMaxHeightBlock());

        Transaction tx = new Transaction();
        tx.addOutput(10, personB.getPublic());
        tx.addInput(blockA.getTransaction(0).getHash(), 0);
        tx.addSignature(TestingUtils.sign(personA.getPrivate(), tx.getRawDataToSign(0)), 0);
        tx.finalize();

        blockChain.addTransaction(tx);

        blockB = new Block(blockA.getHash(), personB.getPublic());
        blockB.addTransaction(tx);
        blockB.finalize();

        tx = new Transaction();
        tx.addOutput(10, personC.getPublic());
        tx.addInput(blockA.getTransaction(0).getHash(), 0);
        tx.addSignature(TestingUtils.sign(personA.getPrivate(), tx.getRawDataToSign(0)), 0);
        tx.finalize();

        blockC = new Block(blockA.getHash(), personC.getPublic());
        blockC.addTransaction(tx);
        blockC.finalize();

        Assertions.assertTrue(blockChain.addBlock(blockB));
        Assertions.assertTrue(blockChain.addBlock(blockC));
    }

    @Test
    @Order(4)
    void addBlockD_E() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        Assertions.assertEquals(blockB, blockChain.getMaxHeightBlock());

        Transaction tx = new Transaction();
        tx.addOutput(10, personD.getPublic());
        tx.addInput(blockB.getTransaction(0).getHash(), 0);
        tx.addSignature(TestingUtils.sign(personB.getPrivate(), tx.getRawDataToSign(0)), 0);
        tx.finalize();

        blockChain.addTransaction(tx);

        blockD = new Block(blockB.getHash(), personD.getPublic());
        blockD.addTransaction(tx);
        blockD.finalize();

        tx = new Transaction();
        tx.addOutput(10, personE.getPublic());
        tx.addInput(blockC.getTransaction(0).getHash(), 0);
        tx.addSignature(TestingUtils.sign(personC.getPrivate(), tx.getRawDataToSign(0)), 0);
        tx.finalize();

        blockE = new Block(blockC.getHash(), personE.getPublic());
        blockE.addTransaction(tx);
        blockE.finalize();

        Assertions.assertTrue(blockChain.addBlock(blockD));
        Assertions.assertTrue(blockChain.addBlock(blockE));
    }

    @Test
    @Order(5)
    void addBlockF() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        Assertions.assertEquals(blockD, blockChain.getMaxHeightBlock());

        Transaction tx = new Transaction();
        tx.addOutput(10, personF.getPublic());
        tx.addInput(blockD.getTransaction(0).getHash(), 0);
        tx.addSignature(TestingUtils.sign(personD.getPrivate(), tx.getRawDataToSign(0)), 0);
        tx.finalize();

        blockChain.addTransaction(tx);

        blockF = new Block(blockD.getHash(), personF.getPublic());
        blockF.addTransaction(tx);
        blockF.finalize();

        Assertions.assertTrue(blockChain.addBlock(blockF));
    }

    @Test
    @Order(6)
    /**
     * Cut OFF should less than 4 for this test to succeed
     */
    void addBlockX() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        Assertions.assertEquals(blockChain.getMaxHeightBlock(), blockF);

        Transaction tx = new Transaction();
        tx.addOutput(10, personX.getPublic());
        tx.addInput(genesisBlock.getTransaction(0).getHash(), 0);
        tx.addSignature(TestingUtils.sign(person.getPrivate(), tx.getRawDataToSign(0)), 0);
        tx.finalize();

        Block blockX = new Block(genesisBlock.getHash(), personX.getPublic());
        blockX.addTransaction(tx);
        blockX.finalize();

        Assertions.assertFalse(blockChain.addBlock(blockX));
    }
}