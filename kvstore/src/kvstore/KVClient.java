package kvstore;

import static kvstore.KVConstants.DEL_REQ;
import static kvstore.KVConstants.ERROR_COULD_NOT_CONNECT;
import static kvstore.KVConstants.ERROR_COULD_NOT_CREATE_SOCKET;
import static kvstore.KVConstants.ERROR_INVALID_KEY;
import static kvstore.KVConstants.ERROR_INVALID_VALUE;
import static kvstore.KVConstants.GET_REQ;
import static kvstore.KVConstants.PUT_REQ;
import static kvstore.KVConstants.RESP;
import static kvstore.KVConstants.SUCCESS;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.locks.*;

/**
 * Client API used to issue requests to key-value server.
 */
public class KVClient implements KeyValueInterface {

    public String server;
    public int port;
    public Lock lock;

    /**
     * Constructs a KVClient connected to a server.
     *
     * @param server is the DNS reference to the server
     * @param port is the port on which the server is listening
     */
    public KVClient(String server, int port) {
        this.server = server;
        this.port = port;
        this.lock = new ReentrantLock();
    }

    /**
     * Creates a socket connected to the server to make a request.
     *
     * @return Socket connected to server
     * @throws KVException if unable to create or connect socket
     */
    public Socket connectHost() throws KVException {
        // implement me
        try {
        	Socket socket = new Socket(this.server, this.port);
        	return socket;
        } catch (Exception e) {
        	throw new KVException(ERROR_COULD_NOT_CONNECT);
        }
    }

    /**
     * Closes a socket.
     * Best effort, ignores error since the response has already been received.
     *
     * @param  sock Socket to be closed
     */
    public void closeHost(Socket sock) {
        try {
			sock.close();
		} catch (IOException e) {
		}
    }

    /**
     * Issues a PUT request to the server.
     *
     * @param  key String to put in server as key
     * @throws KVException if the request was not successful in any way
     */
    @Override
    public void put(String key, String value) throws KVException {
        // implement me
    	// kv message
    	this.lock.lock();
    	KVMessage kvMessage = new KVMessage(PUT_REQ);
    	kvMessage.setKey(key);
    	kvMessage.setValue(value);
    	Socket sock = connectHost();
    	kvMessage.sendMessage(sock);
    	
    	KVMessage kvReturnMessage = new KVMessage(sock);
        String returnMessage = kvReturnMessage.getMessage();
        System.out.println(kvReturnMessage.getMsgType());
        System.out.println(returnMessage);
        if (!returnMessage.equals(KVConstants.SUCCESS)){
        	throw new KVException(KVConstants.ERROR_INVALID_FORMAT);
        }
    	
        closeHost(sock);
        this.lock.unlock();
    }

    /**
     * Issues a GET request to the server.
     *
     * @param  key String to get value for in server
     * @return String value associated with key
     * @throws KVException if the request was not successful in any way
     */
    @Override
    public String get(String key) throws KVException {
        // implement me
    	// kv message
    	this.lock.lock();
    	System.out.println("started get function");
    	KVMessage kvMessage = new KVMessage(GET_REQ);
    	System.out.println("1");
    	kvMessage.setKey(key);
    	System.out.println("2");
    	Socket sock = connectHost();
    	System.out.println("3");
    	kvMessage.sendMessage(sock);
    	System.out.println("4");
    	
    	KVMessage kvReturnMessage = new KVMessage(sock);
    	System.out.println("5");
    	String returnVal = kvReturnMessage.getValue();
    	System.out.println("&&&&&&&&&&&&&&");
    	System.out.println(returnVal);
    	System.out.println("&&&&&&&&&&&&&&");
    	
    	String returnMessage = kvReturnMessage.getMessage();
    	if (returnMessage != null && returnMessage.equals(KVConstants.ERROR_NO_SUCH_KEY)){
        	throw new KVException(KVConstants.ERROR_NO_SUCH_KEY);
        } else if (returnMessage != null && returnMessage.equals(KVConstants.ERROR_OVERSIZED_KEY)){
        	throw new KVException(KVConstants.ERROR_OVERSIZED_KEY);
        }
    	
    	closeHost(sock);
    	this.lock.unlock();
        
        return returnVal;
    }

    /**
     * Issues a DEL request to the server.
     *
     * @param  key String to delete value for in server
     * @throws KVException if the request was not successful in any way
     */
    @Override
    public void del(String key) throws KVException {
        // implement me
    	// kv message
    	this.lock.lock();
    	Socket sock = connectHost();
    	KVMessage kvMessage = new KVMessage(DEL_REQ);
    	kvMessage.setKey(key);
    	kvMessage.sendMessage(sock);
    	
    	KVMessage kvReturnMessage = new KVMessage(sock);
    	String returnMessage = kvReturnMessage.getMessage();
        if (!returnMessage.equals(KVConstants.SUCCESS)){
        	throw new KVException(KVConstants.ERROR_INVALID_FORMAT);
        }
    	
    	closeHost(sock);
    	this.lock.unlock();
    }


}
