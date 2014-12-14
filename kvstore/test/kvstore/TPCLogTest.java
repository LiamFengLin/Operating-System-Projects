package kvstore;

import static org.junit.Assert.*;

import kvstore.KVConstants;
import kvstore.KVMessage;
import kvstore.KVServer;
import kvstore.TPCLog;
import static kvstore.KVConstants.*;

import java.io.File;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;

import org.junit.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import org.powermock.modules.junit4.PowerMockRunner;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.api.mockito.PowerMockito;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Socket.class, KVMessage.class, TPCMasterHandler.class, TPCSlaveInfo.class, TPCLog.class, TPCMaster.class})
public class TPCLogTest {

	public final static int port=8080;
	private static final String LOG_PATH = "TPCLog.log";
	
	private TPCLog tpcLog;
    private KVServer kvTestServer;
	
	@Before
    public void setupTPCLog() throws Exception {
		kvTestServer = new KVServer(10, 10);
        tpcLog = new TPCLog(LOG_PATH, kvTestServer);
    }

    @After
    public void tearDown() {
        File log = new File(LOG_PATH);
        if (log.exists() && !log.delete()) { // true iff delete failed.
            System.err.printf("deleting log-file at %s failed.\n", log.getAbsolutePath());
        }
    }
	
	@Test
    public void testAppendAndFlush() throws KVException {
       
        //append 3 KVMessage
        KVMessage testMessage1 = new KVMessage(GET_REQ);
        testMessage1.setKey("testKey1");
        KVMessage testMessage2 = new KVMessage(PUT_REQ);
        testMessage2.setKey("testKey2");
        testMessage2.setValue("testValue2");
        KVMessage testMessage3 = new KVMessage(DEL_REQ);
        testMessage3.setKey("testKey3");
        tpcLog.appendAndFlush(testMessage1);
        tpcLog.appendAndFlush(testMessage2);
        tpcLog.appendAndFlush(testMessage3);
        
        //read from disk
        TPCLog tpcLog2 = new TPCLog(LOG_PATH, kvTestServer);
        tpcLog2.loadFromDisk();
        KVMessage lastEntry = tpcLog2.getLastEntry();
        assertEquals(KVConstants.DEL_REQ, lastEntry.getMsgType());
    }
	
	@Test
	public void testRebuildKVTestServer() throws KVException {
        
        KVMessage put1 = new KVMessage(PUT_REQ);
        put1.setKey("testPut1");
        put1.setValue("testValue1");
        tpcLog.appendAndFlush(put1);
        
        // added <testPut1, testValue1>
        KVMessage commitPut1 = new KVMessage(COMMIT);
        tpcLog.appendAndFlush(commitPut1);
        
        // added <testPut2, testValue2>
        KVMessage put2 = new KVMessage(PUT_REQ);
        put2.setKey("testPut2");
        put2.setValue("testValue2");
        tpcLog.appendAndFlush(put2);
        
        KVMessage commitPut2 = new KVMessage(COMMIT);
        tpcLog.appendAndFlush(commitPut2);
        
        // delete aborted <testPut2, testValue2>
        KVMessage del2 = new KVMessage(DEL_REQ);
        del2.setKey("testPut2");
        tpcLog.appendAndFlush(del2);
        
        KVMessage abort2 = new KVMessage(ABORT);
        tpcLog.appendAndFlush(abort2);
        
        // delete succeeded <testPut2, testValue2>
        KVMessage del1 = new KVMessage(DEL_REQ);
        del1.setKey("testPut1");
        tpcLog.appendAndFlush(del1);
        
        KVMessage commitDel1 = new KVMessage(COMMIT);
        tpcLog.appendAndFlush(commitDel1);
        
        //read from disk
        TPCLog tpcLog2 = new TPCLog(LOG_PATH, kvTestServer);
        tpcLog2.loadFromDisk();
        
        //rebuild
        tpcLog2.rebuildServer();
        String correctVersion = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><KVStore><KVPair><Key>testPut2</Key><Value>testValue2</Value></KVPair></KVStore><?xml version=\"1.0\" encoding=\"UTF-8\"?><KVCache><Set Id=\"0\"/><Set Id=\"1\"><CacheEntry isReferenced=\"true\"><Key>testPut2</Key><Value>testValue2</Value></CacheEntry></Set><Set Id=\"2\"/><Set Id=\"3\"/><Set Id=\"4\"/><Set Id=\"5\"/><Set Id=\"6\"/><Set Id=\"7\"/><Set Id=\"8\"/><Set Id=\"9\"/></KVCache>";
        assertEquals(correctVersion, kvTestServer.toString());
    }

}