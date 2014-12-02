package kvstore;

import static kvstore.KVConstants.*;

import java.net.Socket;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TPCMaster {

    public int numSlaves;
    public KVCache masterCache;

    public static final int TIMEOUT = 3000;
    
    // slave id array
    public TPCSlaveInfo[]  slaveArray;
    // lock
    private static final int MAX_KEY_SIZE = 256;
    private static final int MAX_VAL_SIZE = 256 * 1024;
    public Lock writeLock;

    /**
     * Creates TPCMaster, expecting numSlaves slave servers to eventually register
     *
     * @param numSlaves number of slave servers expected to register
     * @param cache KVCache to cache results on master
     */
    public TPCMaster(int numSlaves, KVCache cache) {
        this.numSlaves = numSlaves;
        this.masterCache = cache;
        // implement me
        this.slaveArray = new TPCSlaveInfo[numSlaves];
        for(int i = 0; i < numSlaves; i++){
        	slaveArray[i] = null;
        }
        // set up slave id array
        // set up lock 
        this.writeLock = new ReentrantLock();
    }

    /**
     * Registers a slave. Drop registration request if numSlaves already
     * registered. Note that a slave re-registers under the same slaveID when
     * it comes back online.
     *
     * @param slave the slaveInfo to be registered
     */
    public void registerSlave(TPCSlaveInfo slave) {
        // implement me
    	// check if already registered
    	for(int i = 0; i < numSlaves; i++){
    		if(slaveArray[i] != null && slaveArray[i].getSlaveID() == slave.getSlaveID()){
    			slaveArray[i] = slave;
    			return;
    		}
    	}
    	// slave all fields saved to an array; add to slave array;
    	for(int i = 0; i < numSlaves; i++){
    		if(slaveArray[i] == null){
    			slaveArray[i] = slave;
    			return;
    		}
    	}
    }

    /**
     * Converts Strings to 64-bit longs. Borrowed from http://goo.gl/le1o0W,
     * adapted from String.hashCode().
     *
     * @param string String to hash to 64-bit
     * @return long hashcode
     */
    public static long hashTo64bit(String string) {
        long h = 1125899906842597L;
        int len = string.length();

        for (int i = 0; i < len; i++) {
            h = (31 * h) + string.charAt(i);
        }
        return h;
    }

    /**
     * Compares two longs as if they were unsigned (Java doesn't have unsigned
     * data types except for char). Borrowed from http://goo.gl/QyuI0V
     *
     * @param n1 First long
     * @param n2 Second long
     * @return is unsigned n1 less than unsigned n2
     */
    public static boolean isLessThanUnsigned(long n1, long n2) {
        return (n1 < n2) ^ ((n1 < 0) != (n2 < 0));
    }

    /**
     * Compares two longs as if they were unsigned, uses isLessThanUnsigned
     *
     * @param n1 First long
     * @param n2 Second long
     * @return is unsigned n1 less than or equal to unsigned n2
     */
    public static boolean isLessThanEqualUnsigned(long n1, long n2) {
        return isLessThanUnsigned(n1, n2) || (n1 == n2);
    }

    /**
     * Find primary replica for a given key.
     *
     * @param key String to map to a slave server replica
     * @return SlaveInfo of first replica
     */
    public TPCSlaveInfo findFirstReplica(String key) {
        // implement me
    	// calculate which key space the key falls into
    	// keySpaceSize for each slave = 2^64 / numSlaves
    	// slaveIndex = hashcode(64 bits) / keySpaceSize
    	// return slaveArray[slaveIndex]
    	long smallestGreater = Long.MAX_VALUE;
    	int resultIndex = -1;
    	long hashValue = hashTo64bit(key);
    	for(int i = 0; i < numSlaves; i++){
    		if(slaveArray[i] != null && isLessThanEqualUnsigned(hashValue,slaveArray[i].getSlaveID())){
    			if(isLessThanUnsigned(slaveArray[i].getSlaveID(), smallestGreater)){
    				smallestGreater = slaveArray[i].getSlaveID();
    				resultIndex = i;
    			}
    		}
    	}
    	long smallestId = Long.MAX_VALUE;
    	if(resultIndex == -1){
    		for(int i = 0; i < numSlaves; i ++){
    			if(slaveArray[i] != null && slaveArray[i].getSlaveID() < smallestId){
    				smallestId = slaveArray[i].getSlaveID();
    				resultIndex = i;
    			}
    		}
    	}
        return slaveArray[resultIndex];
    }

    /**
     * Find the successor of firstReplica.
     *
     * @param firstReplica SlaveInfo of primary replica
     * @return SlaveInfo of successor replica
     */
    public TPCSlaveInfo findSuccessor(TPCSlaveInfo firstReplica) {
        // implement me
    	// iterate through slave array; firstReplicaIndex = find index of firstReplica
    	// nextReplicaIndex = (firstReplicaIndex + 1) % numSlaves
    	// return nexReplicaIndex
    	
    	long first = firstReplica.getSlaveID();
    	int resultIndex = -1;
    	boolean notChecked = true;
    	long smallestGreater = Long.MAX_VALUE;
    	for(int i = 0; i < numSlaves; i++){
    		if(slaveArray[i] != null && isLessThanUnsigned(first, slaveArray[i].getSlaveID())){
    			if(isLessThanUnsigned(slaveArray[i].getSlaveID(), smallestGreater) || notChecked){
    				notChecked = false;
    				smallestGreater = slaveArray[i].getSlaveID();
    				resultIndex = i;
    			}
    		}
    	}
    	if(resultIndex == -1){
    		long smallestId = Long.MAX_VALUE;
    		for(int i = 0; i < numSlaves; i ++){
    			if(slaveArray[i] != null && isLessThanUnsigned(slaveArray[i].getSlaveID(), smallestId)){
    				smallestId = slaveArray[i].getSlaveID();
    				resultIndex = i;
    			}
    		}
    	}
    	return slaveArray[resultIndex];
    	
    	
    }

    /**
     * @return The number of slaves currently registered.
     */
    public int getNumRegisteredSlaves() {
        // implement me
    	// size of array
    	int count = 0;
    	for(int i = 0; i < numSlaves; i++){
    		if(slaveArray[i] != null){
    			count ++;
    		}
    	}
        return count;
    }

    /**
     * (For testing only) Attempt to get a registered slave's info by ID.
     * @return The requested TPCSlaveInfo if present, otherwise null.
     */
    public TPCSlaveInfo getSlave(long slaveId) {
        // implement me
    	// iterate through the slave array; compare each ID and find corresponding slaveId
        for(int i = 0; i < numSlaves; i++){
        	if(slaveArray[i].getSlaveID() == slaveId){
        		return slaveArray[i];
        	}
        }
        return null;
    }

    /**
     * Perform 2PC operations from the master node perspective. This method
     * contains the bulk of the two-phase commit logic. It performs phase 1
     * and phase 2 with appropriate timeouts and retries.
     *
     * See the spec for details on the expected behavior.
     *
     * @param msg KVMessage corresponding to the transaction for this TPC request
     * @param isPutReq boolean to distinguish put and del requests
     * @throws KVException if the operation cannot be carried out for any reason
     */
    public synchronized void handleTPCRequest(KVMessage msg, boolean isPutReq)
            throws KVException {
        // implement me
    	// state = [prepareSent, responseCollected, responseSent, decisionSent, ACKReceived]
    	// have not handled master failure/recover from state yet
    	// Lock.lock()
    	// if isPutReq
	    	// phase 1:
	    		// initialize sockets for each slave
	    		// send KVMessage(PREPARE) to each slave
	    		// wait for responses (ready or abort) for each socket
	    			// calculate responses if all ready or abort
	    	// phase 2:	
    			// initialize sockets again for each slave
	    		// send KVMessage(commit or abort) to each slave
	    		// wait for ACK responses for each slave
	    		// if any of them times out, close socket and resend ACK response for that slave
    			// after all ACK received, update master cache
	    // Lock.unlock()
    	
    	String key = msg.getKey();
    	String value = msg.getValue();
    	if (key == null || key.equals("")) {
    		throw new KVException(KVConstants.ERROR_INVALID_KEY);
    	}
    	if (key.length() > MAX_KEY_SIZE) {
    		throw new KVException(ERROR_OVERSIZED_KEY);
    	}
    	Lock lock = null;
    	try{
    		lock = this.masterCache.getLock(key);
        	if (lock != null){
        		lock.lock();
        	}
        	if (!isPutReq) {
        		this.masterCache.del(key);
                TPCSlaveInfo first_replica = this.findFirstReplica(key);
            	TPCSlaveInfo second_replica = this.findSuccessor(first_replica);
                Socket sock = first_replica.connectHost(TIMEOUT);
        		KVMessage kvMessage = new KVMessage(DEL_REQ);
            	kvMessage.setKey(key);
            	kvMessage.sendMessage(sock);
            	first_replica.closeHost(sock);
            	
            	KVMessage kvReturnMessage = new KVMessage(sock);
            	String returnMessage = kvReturnMessage.getMessage();
            	if (returnMessage == null) {
            		throw new KVException(KVConstants.ERROR_PARSER);
            	}
                if (!returnMessage.equals(KVConstants.SUCCESS)){
                	throw new KVException(returnMessage);
                }
                
                sock = second_replica.connectHost(TIMEOUT);
            	kvMessage.sendMessage(sock);
            	second_replica.closeHost(sock);
            	
            	kvReturnMessage = new KVMessage(sock);
            	returnMessage = kvReturnMessage.getMessage();
            	if (returnMessage == null) {
            		throw new KVException(KVConstants.ERROR_PARSER);
            	}
                if (!returnMessage.equals(KVConstants.SUCCESS)){
                	throw new KVException(returnMessage);
                }
                
        	} else {
        		this.masterCache.put(key, value);
        		KVMessage kvMessage = new KVMessage(PUT_REQ);
            	kvMessage.setKey(key);
            	kvMessage.setValue(value);
        		TPCSlaveInfo first_replica = this.findFirstReplica(key);
            	TPCSlaveInfo second_replica = this.findSuccessor(first_replica);
            	
            	Socket sock = first_replica.connectHost(TIMEOUT);
            	kvMessage.setKey(key);
            	kvMessage.sendMessage(sock);
            	first_replica.closeHost(sock);
        		
            	kvMessage.sendMessage(sock);
            	
            	KVMessage kvReturnMessage = new KVMessage(sock);
                String returnMessage = kvReturnMessage.getMessage();
                if (!returnMessage.equals(KVConstants.SUCCESS)){
                	
                	throw new KVException(returnMessage);
                }
                
                sock = second_replica.connectHost(TIMEOUT);
                kvMessage.sendMessage(sock);
            	
            	kvReturnMessage = new KVMessage(sock);
                returnMessage = kvReturnMessage.getMessage();
                if (!returnMessage.equals(KVConstants.SUCCESS)){
                	
                	throw new KVException(returnMessage);
                }
        	}
        	
        	if (lock != null){
            	lock.unlock();
            }
            
    	} catch (Exception e) {
    		if (lock != null){
            	lock.unlock();
            }
    		throw e;
    	}
    				
    }

    /**
     * Perform GET operation in the following manner:
     * - Try to GET from cache, return immediately if found
     * - Try to GET from first/primary replica
     * - If primary succeeded, return value
     * - If primary failed, try to GET from the other replica
     * - If secondary succeeded, return value
     * - If secondary failed, return KVExceptions from both replicas
     *
     * @param msg KVMessage containing key to get
     * @return value corresponding to the Key
     * @throws KVException with ERROR_NO_SUCH_KEY if unable to get
     *         the value from either slave for any reason
     */
    public String handleGet(KVMessage msg) throws KVException {
        // implement me
    	// Try to GET from cache, return immediately if found
    	// if not:
    		// initialize sockets for first slave
    		// findFirstReplica()
        	// Send KVMessage to first/primary slave to get first/primary replica
        // Wait for response; If primary succeeded, return value
        // If primary failed, findSuccessor(), initialize socket for second slave 
    		// send KVMessage(GET_REQ) to the other replica
        	// Wait for response; If secondary succeeded, return value
        // If secondary failed, return KVExceptions from both replicas
    	
    	
    	
    	String key = msg.getKey();
    	if (key == null || key.equals("")) {
    		throw new KVException(KVConstants.ERROR_INVALID_KEY);
    	}
    	if (key.length() > MAX_KEY_SIZE) {
    		throw new KVException(ERROR_OVERSIZED_KEY);
    	}
    	Lock lock = null;
    	try {
    		lock = this.masterCache.getLock(key);
        	if (lock != null){
            	lock.lock();
            }
            String val = this.masterCache.get(key);
            if(val == null) {
            	TPCSlaveInfo first_replica = this.findFirstReplica(key);
            	TPCSlaveInfo second_replica = this.findSuccessor(first_replica);
            	Socket sock;
            	try {
            		sock = first_replica.connectHost(TIMEOUT);
            		KVMessage kvMessage = new KVMessage(GET_REQ);
                	kvMessage.setKey(key);
                	kvMessage.sendMessage(sock);
                	
                	KVMessage kvReturnMessage = new KVMessage(sock, TIMEOUT);
                	val = kvReturnMessage.getValue();
                	String returnMessage = kvReturnMessage.getMessage();
                	first_replica.closeHost(sock);
                	if (returnMessage != null){
                    	throw new KVException(returnMessage);
                    }
            	} catch (Exception e) {
            		sock = second_replica.connectHost(TIMEOUT);
            		KVMessage kvMessage = new KVMessage(GET_REQ);
                	kvMessage.setKey(key);
                	kvMessage.sendMessage(sock);
                	
                	KVMessage kvReturnMessage = new KVMessage(sock);
                	val = kvReturnMessage.getValue();
                	String returnMessage = kvReturnMessage.getMessage();
                	second_replica.closeHost(sock);
                	if (returnMessage != null){
                    	throw new KVException(returnMessage);
                    }
            	}
            	
            	
            }
            this.masterCache.put(key, val);
            if (lock != null){
            	lock.unlock();
            }
        	return val;
    	} catch (Exception e) {
    		if (lock != null){
            	lock.unlock();
            }
    		throw new KVException(KVConstants.ERROR_NO_SUCH_KEY);
    	}
    }

}
