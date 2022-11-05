/*
 * “I acknowledge that I am aware of the academic integrity guidelines of this course,
 * and that I worked on this assignment independently without any unauthorized help” - Amr Momtaz
 */

// The BlockChain class should maintain only limited block nodes to satisfy the functionality.
// You should not have all the blocks added to the blockchain in memory
// as it would cause a memory overflow.

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the blockchain
 */
public class BlockChain {

    public static final int CUT_OFF_AGE = 10;

    // All the blocks in the blockHashMap are valid block which satisfies the
    // cut of age constraint
    private final HashMap<ByteArrayWrapper, Block> blockHashMap;
    private final HashMap<Block, UTXOPool> blockUTXOPoolMap;
    private final HashMap<Integer, List<Block>> blockLevelMap;
    private Integer maximumHeight;
    private final TransactionPool transactionPool;


    /**
     * create an empty blockchain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block.
     * Initializes the blockchain data structures with the genesis block.
     */
    public BlockChain(Block genesisBlock) {
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
     *
     * Validate the block by doing the following:
     *
     * 1) If prev block hash is null returns false.
     * 2) Check if the prev block exists.
     * 3) Check if all the transactions in the block are valid.
     * 4) Add the block in all the datastructures and fork if necessary and update UTXO pools
     * 5) If the block is added has updated the max height remove old blockchains if necessary.
     * 6) Remove all transactions of the newly added block from the transactions pool.
     */
    public boolean addBlock(Block block) {

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
        ByteArrayWrapper newBlockHash = new ByteArrayWrapper(block.getHash());
        Integer newBlockHeight = getBlockHeight(prevBlock) + 1;
        updateDataStructures(block, newBlockUtxoPool, newBlockHash, newBlockHeight);

        // satisfies (5)
        handleDeletingOldBlocksIfNecessary(newBlockHeight);

        // satisfies (6)
        removeTxsFromTxPool(block);

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
        TxHandler txHandler = new TxHandler(prevBlockUtxoPool);
        Transaction[] possibleTxs = newBlock.getTransactions().toArray(new Transaction[0]);
        Transaction[] validTxs = txHandler.handleTxs(possibleTxs);
        if (possibleTxs.length != validTxs.length) // Failure in validating the txs
            return null;
        else {
            UTXOPool newUtxoPool = txHandler.getUtxoPool();
            newUtxoPool.addUTXO(new UTXO(newBlock.getCoinbase().getHash(), 0), newBlock.getCoinbase().getOutput(0));
            return newUtxoPool;
        }
    }

    /**
     * Returns the height of a given block.
     */
    private Integer getBlockHeight(Block block) {
        ByteArrayWrapper blockHash = new ByteArrayWrapper(block.getHash());
        for (Map.Entry<Integer, List<Block>> entry : blockLevelMap.entrySet()) {
            for (Block value : entry.getValue()){
                ByteArrayWrapper valueHash = new ByteArrayWrapper(value.getHash());
                if (blockHash.equals(valueHash))
                    return entry.getKey();
            }
        }
        throw new RuntimeException("Failure in getting the block height");
    }

    /**
     * Updates data-structures with the new block.
     */
    private void updateDataStructures(Block newBlock, UTXOPool newBlockUtxoPool,
                                      ByteArrayWrapper newBlockHash, Integer newBlockHeight) {

        blockHashMap.put(newBlockHash, newBlock);
        blockUTXOPoolMap.put(newBlock, newBlockUtxoPool);
        if (blockLevelMap.containsKey(newBlockHeight))
            blockLevelMap.get(newBlockHeight).add(newBlock);
        else {
            List<Block> newLevel = new ArrayList<>();
            newLevel.add(newBlock);
            blockLevelMap.put(newBlockHeight, newLevel);
        }
    }

    /**
     *  Deletes blocks from memory to satisfy the cutoff age constraint if necessary
     */
    private void handleDeletingOldBlocksIfNecessary(Integer newBlockHeight) {
        if (newBlockHeight <= this.maximumHeight)
            return;
        Integer levelToBeRemoved = newBlockHeight - CUT_OFF_AGE - 1;
        if (levelToBeRemoved > 0) {
            for (Block block : blockLevelMap.get(levelToBeRemoved)) {
                ByteArrayWrapper blockHash = new ByteArrayWrapper(block.getHash());
                if (blockHashMap.remove(blockHash) == null)
                    throw new RuntimeException("Failure in deleting from blockHashMap");

                if (blockUTXOPoolMap.remove(block) == null)
                    throw new RuntimeException("Failure in deleting from blockUTXOPoolMap");
            }
            if (blockLevelMap.remove(levelToBeRemoved) == null)
                throw new RuntimeException("Failure in deleting from blockLevelMap");
            System.gc();
        }
        this.maximumHeight = newBlockHeight;
    }

    /**
     * Removes all the transactions from transaction pool of a given block
     */
    private void removeTxsFromTxPool (Block newBlock) {
        for (Transaction tx : newBlock.getTransactions())
            transactionPool.removeTransaction(tx.getHash());
    }
}