/*
 * “I acknowledge that I am aware of the academic integrity guidelines of this course,
 * and that I worked on this assignment independently without any unauthorized help” - Amr Momtaz
 */

// The BlockChain class should maintain only limited block nodes to satisfy the functionality.
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Represents the blockchain
 */
public class BlockChain {

    public static final int CUT_OFF_AGE = 10;

    //private Tree blockChainTree;

    // All the blocks in the blockHashMap are valid block which satisfies the
    // cut of age constraint
    private final HashMap<ByteArrayWrapper, Block> blockHashMap;
    private final HashMap<Block, UTXOPool> blockUTXOPoolMap;
    private final HashMap<Integer, List<Block>> blockLevelMap;
    private final Integer maximumHeight;
    private final TransactionPool transactionPool;

//    /**
//     *  Represents the tree data structure.
//     */
//    private static final class Tree {
//        private Node root;
//
//        public Tree(Node root) {
//            this.root = root;
//        }
//
//        public Node getRoot() {
//            return root;
//        }
//
//        /**
//         * Returns Node which corresponds to a specific block. (Traversing the tree)
//         */
//        public Node getNode(Block block) {
//            return getNodeHelper(root, new ByteArrayWrapper(block.getHash()));
//        }
//
//        private Node getNodeHelper(Node node, ByteArrayWrapper blockHash) {
//            if (node == null) // This condition should never be true
//                return null;
//            ByteArrayWrapper nodeBlockHash = new ByteArrayWrapper(node.getBlock().getHash());
//            if (blockHash.equals(nodeBlockHash))
//                return node;
//            else {
//                for (Node child : node.children) {
//                    if (child == null) // This also should never happen
//                        continue;
//                    Node childResult = getNodeHelper(child, blockHash);
//                    if (childResult != null)
//                        return childResult;
//                }
//            }
//            return null;
//        }
//
//    }
//
//    /**
//     *  Represents nodes in tree.
//     */
//    private static final class Node {
//        private Block block;
//        private List<Node> children;
//
//        public Node(Block block) {
//            this.block = block;
//            this.children = new ArrayList<>();
//        }
//
//        public Block getBlock() {
//            return block;
//        }
//
//        public List<Node> getChildren() {
//            return children;
//        }
//
//        public void addChild(Node node) {
//            this.children.add(node);
//        }
//    }

    /**
     * create an empty blockchain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block.
     * Initializes the blockchain data structures with the genesis block.
     */
    public BlockChain(Block genesisBlock) {
//        // Initialize the tree
//        Node root = new Node(genesisBlock);
//        this.blockChainTree = new Tree(root);

        // Initialize hashmap
        blockHashMap = new HashMap<>();
        ByteArrayWrapper genesisBlockHash = new ByteArrayWrapper(genesisBlock.getHash());
        blockHashMap.put(genesisBlockHash, genesisBlock);

        // Initialize the UTXO Pool map
        blockUTXOPoolMap = new HashMap<>();
        UTXOPool genesisBlockUtxoPool = new UTXOPool();
        for (Transaction tx : genesisBlock.getTransactions()) {
            int index = 0;
            for (Transaction.Output txOutput : tx.getOutputs()) {
                UTXO newUtxo = new UTXO(tx.getHash(), index++);
                genesisBlockUtxoPool.addUTXO(newUtxo, txOutput);
            }
        }
        // Adding coinbase transaction if it exists.
        Transaction coinBaseTransaction = genesisBlock.getCoinbase();
        if (coinBaseTransaction.getOutput(0).address != null
            && coinBaseTransaction.getOutput(0).value > 0) {

            UTXO coinBaseUtxo = new UTXO(coinBaseTransaction.getHash(), 0);
            genesisBlockUtxoPool.addUTXO(coinBaseUtxo, coinBaseTransaction.getOutput(0));
        }
        blockUTXOPoolMap.put(genesisBlock, genesisBlockUtxoPool);

        // Initialize block level hash map
        blockLevelMap = new HashMap<>();
        ArrayList<Block> initialLevelList = new ArrayList<>();
        initialLevelList.add(genesisBlock);
        blockLevelMap.put(1, initialLevelList);
        this.maximumHeight = 1;

        // Initialize the transaction pool
        this.transactionPool = new TransactionPool();
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        return blockLevelMap.get(maximumHeight).get(0);
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        return blockUTXOPoolMap.get(getMaxHeightBlock());
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        return transactionPool;
    }

    /**
     * Add {@code block} to the blockchain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}, where maxHeight is 
     * the current height of the blockchain.
	 * <p>
	 * Assume the Genesis block is at height 1.
     * For example, you can try creating a new block over the genesis block (i.e. create a block at 
	 * height 2) if the current blockchain height is less than or equal to CUT_OFF_AGE + 1. As soon as
	 * the current blockchain height exceeds CUT_OFF_AGE + 1, you cannot create a new block at height 2.
     * 
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        /*
         * Validate the block by doing the following:
         *
         * 1) If prev block hash is null it should return false.
         * 2) Check if the prev block exists.
         * 3) Check if all the transactions in the block are valid.
         * 4) Add the block in all the datastructures and fork if necessary and update UTXO pools
         * 5) Remove all transactions of the newly added block from the transactions pool.
         * 6) If the block is added has updated the max height remove old blockchains if necessary.
         */

        // satisfies (1)
        if (block.getPrevBlockHash() == null)
            return false;

        // satisfies (2)
        ByteArrayWrapper prevBlockHash = new ByteArrayWrapper(block.getPrevBlockHash());
        if (! blockHashMap.containsKey(prevBlockHash))
            return false;

        // satisfies (3)
        Block prevBlock = blockHashMap.get(prevBlockHash);
        UTXOPool prevBlockUtxoPool = blockUTXOPoolMap.get(prevBlock);
        UTXOPool newBlockUtxoPool = getNewBlockUtxoPoolIfValid(block, prevBlockUtxoPool);
        if (newBlockUtxoPool == null)
            return false;
        /// From here the block is validated and will be added to the blockchain.

        // satisfies (4)


        // satisfies (5)

        // satisfies (6)


        return true;
    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        this.transactionPool.addTransaction(tx);
    }

    //
    // Private Helper Methods
    //

    /**
     * If block transactions are valid returns the new UTXO pool of the block.
     * Returns null otherwise.
     */
    private UTXOPool getNewBlockUtxoPoolIfValid(Block newBlock, UTXOPool prevBlockUtxoPool) {
        UTXOPool tempUtxoPool = new UTXOPool(prevBlockUtxoPool);
        TxHandler txHandler = new TxHandler(tempUtxoPool);
        Transaction[] possibleTxs = newBlock.getTransactions().toArray(new Transaction[0]);
        Transaction[] validTxs = txHandler.handleTxs(possibleTxs);
        if (possibleTxs.length != validTxs.length) // Failure in validating the txs
            return null;
        else
            return tempUtxoPool;
    }

}