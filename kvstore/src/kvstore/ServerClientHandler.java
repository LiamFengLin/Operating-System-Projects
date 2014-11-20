package kvstore;

import static kvstore.KVConstants.DEL_REQ;
import static kvstore.KVConstants.GET_REQ;
import static kvstore.KVConstants.PUT_REQ;
import static kvstore.KVConstants.RESP;
import static kvstore.KVConstants.SUCCESS;

import java.io.IOException;
import java.net.Socket;

/**
 * This NetworkHandler will asynchronously handle the socket connections.
 * Uses a thread pool to ensure that none of its methods are blocking.
 */
public class ServerClientHandler implements NetworkHandler {

    public KVServer kvServer;
    public ThreadPool threadPool;

    /**
     * Constructs a ServerClientHandler with ThreadPool of a single thread.
     *
     * @param kvServer KVServer to carry out requests
     */
    public ServerClientHandler(KVServer kvServer) {
        this(kvServer, 1);
    }

    /**
     * Constructs a ServerClientHandler with ThreadPool of thread equal to
     * the number passed in as connections.
     *
     * @param kvServer KVServer to carry out requests
     * @param connections number of threads in threadPool to service requests
     */
    public ServerClientHandler(KVServer kvServer, int connections) {
        // implement me
    	this.threadPool = new ThreadPool(connections);
    	this.kvServer = kvServer;
    	
    }

    /**
     * Creates a job to service the request for a socket and enqueues that job
     * in the thread pool. Ignore any InterruptedExceptions.
     *
     * @param client Socket connected to the client with the request
     */
    @Override
    public void handle(Socket client) {
        // implement me
    	KVServer server =this.kvServer;
    	Thread handlerThread = new Thread() {
    		@Override
    		public void run() {
    			try {
    				while (true) {
    					KVMessage message = new KVMessage(client);
    					if (message.getMsgType() == KVConstants.GET_REQ) {
    						message.sendMessage(client);
    					} else if (message.getMsgType() == KVConstants.DEL_REQ) {
    						server.del(message.getKey());
    					} else if (message.getMsgType() == KVConstants.PUT_REQ) {
    						server.put(message.getKey(), message.getValue());
    					} 
    					
    				}
    			} catch (KVException e) {
    				try {
    					e.getKVMessage().sendMessage(client);
    				} catch (KVException e_again) {
    					System.out.println(e_again.toString());
    				}
    				
    			}
    		}
    	};
    	try {
    		this.threadPool.addJob(handlerThread);
    	} catch (InterruptedException e) {
    		System.out.println(e.getMessage());
    	}
    	
    }
    
    // implement me

}
