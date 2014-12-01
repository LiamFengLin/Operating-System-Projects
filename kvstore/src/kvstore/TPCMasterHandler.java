package kvstore;

import static kvstore.KVConstants.*;

import java.io.IOException;
import java.net.Socket;
/**
 * Implements NetworkHandler to handle 2PC operation requests from the Master/
 * Coordinator Server
 */
public class TPCMasterHandler implements NetworkHandler {

    public long slaveID;
    public KVServer kvServer;
    public TPCLog tpcLog;
    public ThreadPool threadpool;

    // implement me

    /**
     * Constructs a TPCMasterHandler with one connection in its ThreadPool
     *
     * @param slaveID the ID for this slave server
     * @param kvServer KVServer for this slave
     * @param log the log for this slave
     */
    public TPCMasterHandler(long slaveID, KVServer kvServer, TPCLog log) {
        this(slaveID, kvServer, log, 1);
    }

    /**
     * Constructs a TPCMasterHandler with a variable number of connections
     * in its ThreadPool
     *
     * @param slaveID the ID for this slave server
     * @param kvServer KVServer for this slave
     * @param log the log for this slave
     * @param connections the number of connections in this slave's ThreadPool
     */
    public TPCMasterHandler(long slaveID, KVServer kvServer, TPCLog log, int connections) {
        this.slaveID = slaveID;
        this.kvServer = kvServer;
        this.tpcLog = log;
        this.threadpool = new ThreadPool(connections);
    }

    /**
     * Registers this slave server with the master.
     *
     * @param masterHostname
     * @param server SocketServer used by this slave server (which contains the
     *               hostname and port this slave is listening for requests on
     * @throws KVException with ERROR_INVALID_FORMAT if the response from the
     *         master is received and parsed but does not correspond to a
     *         success as defined in the spec OR any other KVException such
     *         as those expected in KVClient in project 3 if unable to receive
     *         and/or parse message
     */
    public void registerWithMaster(String masterHostname, SocketServer server)
            throws KVException {
        // implement me
    	String slaveHostname = server.getHostname();
    	String slavePort = Integer.toString(server.getPort());
    	String slaveId = Long.toString(this.slaveID);
    	String registrationMessage = slaveId + "@" + slaveHostname + ":" + slavePort;
    	KVMessage messageToMaster;
    	try {
        	Socket socketWithMaster = new Socket(masterHostname, 9090);
        	messageToMaster = new KVMessage(REGISTER);
			messageToMaster.setMessage(registrationMessage);
			messageToMaster.sendMessage(socketWithMaster);
			
			KVMessage kvReturnMessage = new KVMessage(socketWithMaster);
		    String kvResponseMessage = kvReturnMessage.getMessage();
		    if (!(kvResponseMessage.equals(registrationMessage) && kvReturnMessage.getMsgType().equals(RESP))){
		    	throw new KVException(ERROR_INVALID_FORMAT);
		    }
        } catch (KVException e) {
        	throw e;
        } catch (IOException e) {
        	throw new KVException(ERROR_COULD_NOT_CREATE_SOCKET);	
        }
    }

    /**
     * Creates a job to service the request on a socket and enqueues that job
     * in the thread pool. Ignore any InterruptedExceptions.
     *
     * @param master Socket connected to the master with the request
     */
    @Override
    public void handle(Socket master) {
    	Runnable handlerThread = new Runnable() {
			@Override
			public void run() {
				try {
					KVMessage message = new KVMessage(master);
					KVMessage response;

					String msgType = message.getMsgType();
					if (msgType.equals(KVConstants.GET_REQ)) {
						response = new KVMessage(RESP);
						response.setKey(message.getKey());
						response.setValue(kvServer.get(message.getKey()));
						response.sendMessage(master);
					}
				}  catch (KVException e) {
					try {
						e.getKVMessage().sendMessage(master);
					} catch (KVException e_again) {
						System.out.println(e_again.toString());
					}

				}
				
			}
		};
		try {
			threadpool.addJob(handlerThread);
		} catch (InterruptedException e) {
			System.out.println(e.getMessage());
		}
    	
    	
    }
}
