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
public class TPCMasterHandlerGetTest {

    private KVServer server;
    private TPCMasterHandler masterHandler;
    private Socket sock1;
    private Socket sock2;
    private Socket sock3;

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

    @Test(timeout = kTimeoutQuick)
    @Category(AG_PROJ4_CODE.class)
    @AGTestDetails(points = 1,
        desc = "Test a single PUT handling")
    public void singlePutTest() {
    	
    }



    /* begin helper methods. */

    private void setupSocketSuccess() {
        sock1 = mock(Socket.class);
        sock2 = mock(Socket.class);
        sock3 = mock(Socket.class);
    }

}
