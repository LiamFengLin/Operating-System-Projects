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
    private int phase;
    private KVMessage action;

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
        this.phase = 1;
        this.action = null;
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
    	KVMessage last = this.tpcLog.getLastEntry();
    	if (last != null) {
    		if (last.getMsgType().equals("putreq")) {
    			this.phase = 1;
    			this.action = last;
    		} else if (last.getMsgType().equals("delreq")) {
    			this.phase = 1;
    			this.action = last;
    		} else if (last.getMsgType().equals("commit")) {
    			this.phase = 2;
    		} else if (last.getMsgType().equals("abort")) {
    			this.phase = 1;
    			this.action = null;
    		}
    	}

    	
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
    	final Socket f_socket = master;
    	Runnable handlerThread = new Runnable() {
			@Override
			public void run() {
				try {
					KVMessage message = new KVMessage(f_socket);
					KVMessage response;

					String msgType = message.getMsgType();
					if (msgType.equals(KVConstants.GET_REQ)) {
						response = new KVMessage(RESP);
						response.setKey(message.getKey());
						response.setValue(kvServer.get(message.getKey()));
						response.sendMessage(f_socket);
					} else if (phase == 2 && msgType.equals(KVConstants.COMMIT)) {
						tpcLog.appendAndFlush(message);
						if (action != null) {
							if (action.getMsgType().equals(KVConstants.DEL_REQ)) {
								kvServer.del(action.getKey());								
							} else if (action.getMsgType().equals(KVConstants.PUT_REQ)) {
								kvServer.put(action.getKey(), action.getValue());
							}
						}
						response = new KVMessage(ACK);
						response.sendMessage(f_socket);
						phase = 1;

					} else if (phase == 1 && msgType.equals(KVConstants.PUT_REQ)) {
						tpcLog.appendAndFlush(message);
						action = message;
						response = new KVMessage(READY);
						response.sendMessage(f_socket);
						phase = 2;
					} else if (phase == 1 && msgType.equals(KVConstants.DEL_REQ)) {
						tpcLog.appendAndFlush(message);
						action = message;
						response = new KVMessage(READY);
						response.sendMessage(f_socket);
						phase = 2;
					} else if (phase == 2 && msgType.equals(KVConstants.ABORT))  {
						tpcLog.appendAndFlush(message);
						action = null;
						phase = 1;
					}
				}  catch (KVException e) {
					try {
						e.getKVMessage().sendMessage(f_socket);
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
