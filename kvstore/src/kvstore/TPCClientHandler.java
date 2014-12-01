package kvstore;

import static kvstore.KVConstants.*;

import java.io.IOException;
import java.net.Socket;

/**
 * This NetworkHandler will asynchronously handle the socket connections.
 * It uses a threadPool to ensure that none of it's methods are blocking.
 */
public class TPCClientHandler implements NetworkHandler {

    public TPCMaster tpcMaster;
    public ThreadPool threadPool;

    /**
     * Constructs a TPCClientHandler with ThreadPool of a single thread.
     *
     * @param tpcMaster TPCMaster to carry out requests
     */
    public TPCClientHandler(TPCMaster tpcMaster) {
        this(tpcMaster, 1);
    }

    /**
     * Constructs a TPCClientHandler with ThreadPool of a single thread.
     *
     * @param tpcMaster TPCMaster to carry out requests
     * @param connections number of threads in threadPool to service requests
     */
    public TPCClientHandler(TPCMaster tpcMaster, int connections) {
        // implement me
    	this.threadPool = new ThreadPool(connections);
    	this.tpcMaster = tpcMaster;
    }

    /**
     * Creates a job to service the request on a socket and enqueues that job
     * in the thread pool. Ignore InterruptedExceptions.
     *
     * @param client Socket connected to the client with the request
     */
    @Override
    public void handle(Socket client) {
        // implement me
    	Runnable handlerThread = new Runnable() {
			@Override
			public void run() {
				try {
					KVMessage message = new KVMessage(client);
					KVMessage response;

					String msgType = message.getMsgType();
					if (msgType.equals(KVConstants.GET_REQ)) {
						response = new KVMessage(RESP);
						response.setKey(message.getKey());
						response.setValue(tpcMaster.handleGet(message));
						response.sendMessage(client);
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
