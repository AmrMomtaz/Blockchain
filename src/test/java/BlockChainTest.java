import org.junit.jupiter.api.*;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.Hashtable;

/**
 * Test class for the blockchain
 */
@SuppressWarnings("FinalizeCalledExplicitly")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BlockChainTest {

    private static KeyPair personA; // Owns 100 BTC
    private BlockChain blockChain;

    @BeforeAll
    static void beforeAll() throws NoSuchAlgorithmException {
        personA = TestingUtils.generateNewKeyPair();
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

        this.blockChain = new BlockChain(genesisBlock);
    }
}
