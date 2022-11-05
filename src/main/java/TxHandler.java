/*
 * “I acknowledge that I am aware of the academic integrity guidelines of this course,
 * and that I worked on this assignment independently without any unauthorized help” - Amr Momtaz
 */

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class TxHandler {

    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. 
     */
    public TxHandler(UTXOPool utxoPool) { this.utxoPool = new UTXOPool(utxoPool); }

    public UTXOPool getUtxoPool() {
        return utxoPool;
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {

        // Ensures that there is no empty transactions with zero inputs and outputs
        if ((tx.getInputs() == null || tx.getInputs().size() == 0) &&
            (tx.getOutputs() == null || tx.getOutputs().size() == 0))
            return false;

        if (! existsInUTXO(tx))
            return false;

        if (! ensureSignatures(tx))
            return false;

        return ensureTxValues(tx);
    }

    ///
    /// Helper methods and fields for isValidTx()
    ///

    /**
     *  Ensures that the claimed output exists in the UTXO only one time.
     *  Satisfies rules (1) & (3)
     */
    private boolean existsInUTXO(Transaction tx) {
        HashSet<UTXO> visitedUTXO = new HashSet<>();
        for (Transaction.Input txInput : tx.getInputs()) {
            UTXO utxo = getUtxoIfAny(txInput);
            // Transaction not found in UTXO
            if (utxo == null)
                return false;
            // Same transaction in UTXO was taken as input more than one time
            if (visitedUTXO.contains(utxo))
                return false;
            // Adding it to Visited UTXO set to satisfy rule (3)
            visitedUTXO.add(utxo);
        }
        return true;
    }

    /**
     * Ensures that all transactions inputs are signed correctly by its owner.
     * Satisfies rule (2).
     */
    private boolean ensureSignatures(Transaction tx) {
        int index = 0;
        for (Transaction.Input txInput : tx.getInputs()) {
            UTXO utxo = getUtxoIfAny(txInput);
            if (utxo == null)
                throw new RuntimeException("Failed to get UTXO when trying to ensure signature");

            PublicKey publicKey = utxoPool.getTxOutput(utxo).address;
            if (! Crypto.verifySignature(publicKey, tx.getRawDataToSign(index++), txInput.signature))
                return false;
        }
        return true;
    }

    /**
     * Ensures that there are no negative numbers in the Tx output and that Sum(Tx.outputs) <= Sum(Tx.inputs)
     * Satisfies Rule (4) & (5).
     */
    private boolean ensureTxValues(Transaction tx) {
        double outputsSum = 0, inputsSum = 0;
        for (Transaction.Output txOutput : tx.getOutputs()) {
            if (txOutput.value < 0)
                return false;
            outputsSum += txOutput.value;
        }
        for (Transaction.Input txInput : tx.getInputs()) {
            UTXO utxo = getUtxoIfAny(txInput);
            Transaction.Output txOutput = utxoPool.getTxOutput(utxo);
            if (txOutput.value < 0) // Just in case :)
                return false;
            inputsSum += txOutput.value;
        }
        return (inputsSum >= outputsSum);
    }

    /**
     * Returns a UTXO for a given transaction input if it exists.
     */
    private UTXO getUtxoIfAny (Transaction.Input txInput) {
        ByteArrayWrapper prevTxHash = new ByteArrayWrapper(txInput.prevTxHash);
        for (UTXO utxo : utxoPool.getAllUTXO()) {
            ByteArrayWrapper utxoTxHash = new ByteArrayWrapper(utxo.getTxHash());
            if (prevTxHash.equals(utxoTxHash) && txInput.outputIndex == utxo.getIndex())
                return utxo;
        }
        return null;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     *
     * The algorithm is as follows:
     *      1) We iterate over all possibleTxs, and we check if the Tx is valid for each one.
     *      2) If it's not we continue in the rest of Txs.
     *      3) If it's valid we do the following:
     *          i) We add it in the accepted transaction list.
     *          ii) We remove all the Tx inputs from the UTXO pool (To avoid double spending across Txs).
     *          iii) We add all new Tx outputs in the UTXO pool since they would be unspent
     *          iv) We set this Tx to null in the possibleTxs (increases performance).
     *          v) We Iterate over the whole array from the beginning.
     *
     *  Complexity:
     *  Time complexity = O(n^2) such that n is the number of elements in possibleTxs.
     *  The worst case scenario would happen if all Txs depend on each other, and they are sorted such that
     *  the first valid Tx is the last element and the second valid is the second last element and so on...
     *
     *  Space complexity = O(1), the algorithm runs in-place.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        List<Transaction> acceptedTransactions = new ArrayList<>();
        for (int i = 0 ; i < possibleTxs.length ; i++) {
            if (possibleTxs[i] == null)
                continue;
            Transaction possibleTx = possibleTxs[i];
            if (isValidTx(possibleTx)) {
                acceptedTransactions.add(possibleTx);
                // Removing all transaction inputs from UTXO pool
                for (Transaction.Input txInput : possibleTx.getInputs()) {
                    UTXO utxo = getUtxoIfAny(txInput);
                    if (utxo == null)
                        throw new RuntimeException("Failed to get UTXO when trying to remove it from " +
                                                    "UTXO pool when handling txs");
                    utxoPool.removeUTXO(utxo);
                }
                // Adding all transaction output (new transactions) in UTXO pool
                int txOutputSize = possibleTx.getOutputs().size();
                for (int txOutputIndex = 0 ; txOutputIndex < txOutputSize ; txOutputIndex++){
                    utxoPool.addUTXO(new UTXO(possibleTx.getHash(), txOutputIndex), possibleTx.getOutput(txOutputIndex));
                }
                possibleTxs[i] = null;
                i = -1;
            }
        }
        return acceptedTransactions.toArray(new Transaction[0]);
    }
}
