package kvstore;

import static autograder.TestUtils.kTimeoutQuick;
import static kvstore.KVConstants.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import autograder.AGCategories.AGTestDetails;
import autograder.AGCategories.AG_PROJ4_CODE;

@RunWith(PowerMockRunner.class)
@PrepareForTest(TPCMasterHandler.class)
public class TPCMasterHandlerTest {

    private KVServer server;
    private TPCMasterHandler masterHandler;
    private Socket sock1;
    private Socket sock2;
    private Socket sock3;
    private Socket sock4;
    private Socket sock5;
    private Socket sock6;
    private Socket sock7;
    private Socket sock8;

    private static final String LOG_PATH = "TPCMasterHandlerTest.log";

    @Before
    public void setupTPCMasterHandler() throws Exception {
        server = new KVServer(10, 10);
        TPCLog log = new TPCLog(LOG_PATH, server);
        Utils.setupMockThreadPool();
        masterHandler = new TPCMasterHandler( 1L, server, log);
    }

    @After
    public void tearDown() {
        server = null;
        masterHandler = null;
        sock1 = null;
        sock2 = null;
        sock3 = null;
        File log = new File(LOG_PATH);

        if (log.exists() && !log.delete()) { // true iff delete failed.
            System.err.printf("deleting log-file at %s failed.\n", log.getAbsolutePath());
        }
    }

    @Test
    @Category(AG_PROJ4_CODE.class)
    @AGTestDetails(points = 1,
        desc = "Test a single commit PUT request")
    public void testPutCommit() throws KVException {
    	setupSocketSuccess();
        InputStream putreqFile = getClass().getClassLoader().getResourceAsStream("putreq.txt");
        InputStream commitFile = getClass().getClassLoader().getResourceAsStream("commit.txt");
        ByteArrayOutputStream tempOut = new ByteArrayOutputStream();
        ByteArrayOutputStream tempOut2 = new ByteArrayOutputStream();
        try {
            doNothing().when(sock1).setSoTimeout(anyInt());
            when(sock1.getInputStream()).thenReturn(putreqFile);
            when(sock1.getOutputStream()).thenReturn(tempOut);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //handle put request
        masterHandler.handle(sock1);

        try {
            doNothing().when(sock3).setSoTimeout(anyInt());
            when(sock3.getInputStream()).thenReturn(new ByteArrayInputStream(tempOut.toByteArray()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //check response
        KVMessage check = new KVMessage(sock3);
        assertEquals(READY, check.getMsgType());
        
        try {
            doNothing().when(sock2).setSoTimeout(anyInt());
            when(sock2.getInputStream()).thenReturn(commitFile);
            when(sock2.getOutputStream()).thenReturn(tempOut2);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //handle commit action
        masterHandler.handle(sock2);
        
        try {
            doNothing().when(sock4).setSoTimeout(anyInt());
            when(sock4.getInputStream()).thenReturn(new ByteArrayInputStream(tempOut2.toByteArray()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //check response
        KVMessage check2 = new KVMessage(sock4);
        assertEquals(ACK, check2.getMsgType());
    }

    @Test
    @Category(AG_PROJ4_CODE.class)
    @AGTestDetails(points = 1,
        desc = "Test a single abort PUT request")
    public void testPutAbort() throws KVException {
    	setupSocketSuccess();
        InputStream putreqFile = getClass().getClassLoader().getResourceAsStream("putreq.txt");
        InputStream abortFile = getClass().getClassLoader().getResourceAsStream("abort.txt");
        ByteArrayOutputStream tempOut = new ByteArrayOutputStream();
        ByteArrayOutputStream tempOut2 = new ByteArrayOutputStream();
        try {
            doNothing().when(sock1).setSoTimeout(anyInt());
            when(sock1.getInputStream()).thenReturn(putreqFile);
            when(sock1.getOutputStream()).thenReturn(tempOut);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //handle put request
        masterHandler.handle(sock1);

        try {
            doNothing().when(sock3).setSoTimeout(anyInt());
            when(sock3.getInputStream()).thenReturn(new ByteArrayInputStream(tempOut.toByteArray()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //check response
        KVMessage check = new KVMessage(sock3);
        assertEquals(READY, check.getMsgType());
        
        try {
            doNothing().when(sock2).setSoTimeout(anyInt());
            when(sock2.getInputStream()).thenReturn(abortFile);
            when(sock2.getOutputStream()).thenReturn(tempOut2);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //handle commit action
        masterHandler.handle(sock2);
        
        try {
            doNothing().when(sock4).setSoTimeout(anyInt());
            when(sock4.getInputStream()).thenReturn(new ByteArrayInputStream(tempOut2.toByteArray()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //check response
        KVMessage check2 = new KVMessage(sock4);
        assertEquals(ACK, check2.getMsgType());
    }
    
    @Test
    @Category(AG_PROJ4_CODE.class)
    @AGTestDetails(points = 1,
        desc = "Test a series of actions")
    public void testSeriesActions() throws KVException {
    	setupSocketSuccess();
    	InputStream getreqFile = getClass().getClassLoader().getResourceAsStream("getreq.txt");
    	InputStream getreqFile2 = getClass().getClassLoader().getResourceAsStream("getreq.txt");
        InputStream putreqFile = getClass().getClassLoader().getResourceAsStream("putreq.txt");
        InputStream abortFile = getClass().getClassLoader().getResourceAsStream("abort.txt");
        InputStream commitFile = getClass().getClassLoader().getResourceAsStream("commit.txt");
        ByteArrayOutputStream tempOut = new ByteArrayOutputStream();
        ByteArrayOutputStream tempOut2 = new ByteArrayOutputStream();
        ByteArrayOutputStream tempOut3 = new ByteArrayOutputStream();
        ByteArrayOutputStream tempOut4 = new ByteArrayOutputStream();
        //ACTION1 GET
        try {
            doNothing().when(sock1).setSoTimeout(anyInt());
            when(sock1.getInputStream()).thenReturn(getreqFile);
            when(sock1.getOutputStream()).thenReturn(tempOut);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //handle get request
        masterHandler.handle(sock1);

        try {
            doNothing().when(sock3).setSoTimeout(anyInt());
            when(sock3.getInputStream()).thenReturn(new ByteArrayInputStream(tempOut.toByteArray()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //check response should be KEY_NOT_FOUND
        KVMessage check = new KVMessage(sock3);
        assertEquals(RESP, check.getMsgType());
        assertEquals(ERROR_NO_SUCH_KEY, check.getMessage());
        
        //ACTION2 PUT
        try {
            doNothing().when(sock2).setSoTimeout(anyInt());
            when(sock2.getInputStream()).thenReturn(putreqFile);
            when(sock2.getOutputStream()).thenReturn(tempOut2);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //handle put action
        masterHandler.handle(sock2);
        
        try {
            doNothing().when(sock4).setSoTimeout(anyInt());
            when(sock4.getInputStream()).thenReturn(new ByteArrayInputStream(tempOut2.toByteArray()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //check response should be READY
        KVMessage check2 = new KVMessage(sock4);
        assertEquals(READY, check2.getMsgType());
        
        //ACTION3 COMMIT
        try {
            doNothing().when(sock5).setSoTimeout(anyInt());
            when(sock5.getInputStream()).thenReturn(commitFile);
            when(sock5.getOutputStream()).thenReturn(tempOut3);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //handle commit action
        masterHandler.handle(sock5);
        
        try {
            doNothing().when(sock6).setSoTimeout(anyInt());
            when(sock6.getInputStream()).thenReturn(new ByteArrayInputStream(tempOut3.toByteArray()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //check response should be ACK
        KVMessage check3 = new KVMessage(sock6);
        assertEquals(ACK, check3.getMsgType());
        
        //ACTION4 GET
        try {
            doNothing().when(sock7).setSoTimeout(anyInt());
            when(sock7.getInputStream()).thenReturn(getreqFile2);
            when(sock7.getOutputStream()).thenReturn(tempOut4);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //handle get request
        masterHandler.handle(sock7);

        try {
            doNothing().when(sock8).setSoTimeout(anyInt());
            when(sock8.getInputStream()).thenReturn(new ByteArrayInputStream(tempOut4.toByteArray()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //check response should be FOUND
        KVMessage check4 = new KVMessage(sock8);
        assertEquals(RESP, check4.getMsgType());
        assertEquals("key", check4.getKey());
        assertEquals("value", check4.getValue());
        
    }

    /* begin helper methods. */

    private void setupSocketSuccess() {
        sock1 = mock(Socket.class);
        sock2 = mock(Socket.class);
        sock3 = mock(Socket.class);
        sock4 = mock(Socket.class);
        sock5 = mock(Socket.class);
        sock6 = mock(Socket.class);
        sock7 = mock(Socket.class);
        sock8 = mock(Socket.class);
    }
}