package kvstore;

import static kvstore.KVConstants.*;

import java.io.IOException;
import java.net.*;
import java.util.regex.*;

/**
 * Data structure to maintain information about SlaveServers
 */
public class TPCSlaveInfo {

    public long slaveID;
    public String hostname;
    public int port;
    
    private static final Pattern INFO_STRING_PAT = Pattern.compile("(-?\\d++)@(\\w++\\.\\w++\\.\\w++\\.\\w++):(\\d++)");

    /**
     * Construct a TPCSlaveInfo to represent a slave server.
     *
     * @param info as "SlaveServerID@Hostname:Port"
     * @throws KVException ERROR_INVALID_FORMAT if info string is invalid
     */
    public TPCSlaveInfo(String info) throws KVException {
        // implement me
    	// check that it is the correct format
    	Matcher matchedInfo = INFO_STRING_PAT.matcher(info);
    	String slaveId = "";
    	String hostname = "";
    	String port = "";
    	matchedInfo.find();
    	slaveId = matchedInfo.group(1);
    	hostname = matchedInfo.group(2);
    	port = matchedInfo.group(3);
    	if (slaveId == null || hostname == null || port == null){
    		throw new KVException(KVConstants.ERROR_INVALID_FORMAT);
    	}
    	try {
    		this.slaveID = Long.parseLong(slaveId);
    	} catch (NumberFormatException e) {
    		throw new KVException(KVConstants.ERROR_INVALID_FORMAT);
    	}
    	this.hostname = hostname;
    	try {
    		this.port = Integer.parseInt(port);
    	} catch (NumberFormatException e) {
    		throw new KVException(KVConstants.ERROR_INVALID_FORMAT);
    	}
    }

    public long getSlaveID() {
        return slaveID;
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    /**
     * Create and connect a socket within a certain timeout.
     *
     * @return Socket object connected to SlaveServer, with timeout set
     * @throws KVException ERROR_SOCKET_TIMEOUT, ERROR_COULD_NOT_CREATE_SOCKET,
     *         or ERROR_COULD_NOT_CONNECT
     */
    public Socket connectHost(int timeout) throws KVException {
        // implement me
    	// need to split IOException into that from the socket and that from connet
    	boolean socketNoError = false;
    	try {
        	Socket socket = new Socket(this.hostname, this.port);
        	socketNoError = true;
        	socket.connect(new InetSocketAddress(this.hostname, this.port), timeout);
        	return socket;
        } catch (SocketTimeoutException e) {
        	throw new KVException(ERROR_SOCKET_TIMEOUT);
        } catch (IOException e) {
        	if (socketNoError) {
        		throw new KVException(ERROR_COULD_NOT_CONNECT);
        	} else {
        		throw new KVException(ERROR_COULD_NOT_CREATE_SOCKET);	
        	}
        }
    }

    /**
     * Closes a socket.
     * Best effort, ignores error since the response has already been received.
     *
     * @param sock Socket to be closed
     */
    public void closeHost(Socket sock) {
        // implement me
    	try {
			sock.close();
		} catch (IOException e) {
		}
    }
}
