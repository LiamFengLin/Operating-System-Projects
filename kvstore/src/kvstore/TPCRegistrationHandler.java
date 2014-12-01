package kvstore;

import static kvstore.KVConstants.*;

import java.io.IOException;
import java.net.Socket;

/**
 * This NetworkHandler will asynchronously handle the socket connections.
 * Uses a thread pool to ensure that none of its methods are blocking.
 */
public class TPCRegistrationHandler implements NetworkHandler {

    private ThreadPool threadpool;
    private TPCMaster master;

    /**
     * Constructs a TPCRegistrationHandler with a ThreadPool of a single thread.
     *
     * @param master TPCMaster to register slave with
     */
    public TPCRegistrationHandler(TPCMaster master) {
        this(master, 1);
    }

    /**
     * Constructs a TPCRegistrationHandler with ThreadPool of thread equal to the
     * number given as connections.
     *
     * @param master TPCMaster to carry out requests
     * @param connections number of threads in threadPool to service requests
     */
    public TPCRegistrationHandler(TPCMaster master, int connections) {
        this.threadpool = new ThreadPool(connections);
        this.master = master;
    }

    /**
     * Creates a job to service the request on a socket and enqueues that job
     * in the thread pool. Ignore any InterruptedExceptions.
     *
     * @param slave Socket connected to the slave with the request
     */
    @Override
    public void handle(final Socket slave) {
        // implement me
    	final TPCMaster master = this.master;
		Runnable handlerThread = new Runnable() {
			@Override
			public void run() {
				try {
					KVMessage message = new KVMessage(slave);
					KVMessage response;
					String msgType = message.getMsgType();
					String registrationMessage = message.getMessage();
					if (msgType.equals(KVConstants.REGISTER)) {
						TPCSlaveInfo newSlave = new TPCSlaveInfo(registrationMessage);
						master.registerSlave(newSlave);
						response = new KVMessage(RESP);
						response.setMessage(registrationMessage);
						response.sendMessage(slave);
					}
				} catch (KVException e) {
					try {
						e.getKVMessage().sendMessage(slave);
					} catch (KVException e_again) {
						System.out.println(e_again.toString());
					}
				}
			}
		};
		try {
			this.threadpool.addJob(handlerThread);
		} catch (InterruptedException e) {
			System.out.println(e.getMessage());
		}

    }
    
    // implement me
}
