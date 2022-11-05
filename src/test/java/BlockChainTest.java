import org.junit.jupiter.api.*;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Hashtable;

/**
 * Test class for the blockchain. It implements the scenario discussed in the hint lecture.
 */
@SuppressWarnings("FinalizeCalledExplicitly")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BlockChainTest {

    private static KeyPair personA; // Owns 100 BTC
    private static KeyPair personB;
    private static BlockChain blockChain;
    private static Block genesisBlock;
    private static Block blockA;

    @BeforeAll
    static void beforeAll() throws NoSuchAlgorithmException {
        personA = TestingUtils.generateNewKeyPair();
        personB = TestingUtils.generateNewKeyPair();
    }

    @Test
    @Order(1)
    void testInitialization() {
        Transaction tx1 = new Transaction();
        tx1.addInput(null, 0);
        tx1.addOutput(10, personA.getPublic());
        tx1.addOutput(15, personA.getPublic());
        tx1.finalize();

        Transaction tx2 = new Transaction();
        tx2.addInput(null, 0);
        tx2.addOutput(20, personA.getPublic());
        tx2.addOutput(30, personA.getPublic());
        tx2.finalize();

        Block genesisBlock = new Block(null, personA.getPublic());
        genesisBlock.addTransaction(tx1);
        genesisBlock.addTransaction(tx2);
        genesisBlock.finalize();

        blockChain = new BlockChain(genesisBlock);
        BlockChainTest.genesisBlock = genesisBlock;
    }

    @Test
    @Order(2)
    void addBlockA() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        Assertions.assertEquals(blockChain.getMaxHeightBlock(), genesisBlock);

        Transaction tx = new Transaction();
        tx.addOutput(7.5, personB.getPublic());
        tx.addInput(genesisBlock.getTransaction(0).getHash(), 0);
        tx.addSignature(TestingUtils.sign(personA.getPrivate(), tx.getRawDataToSign(0)), 0);
        tx.finalize();

        blockChain.addTransaction(tx);

        Block blockA = new Block(genesisBlock.getHash(), personB.getPublic());
        blockA.addTransaction(tx);
        blockA.finalize();
        Assertions.assertTrue(blockChain.addBlock(blockA));
    }
}
