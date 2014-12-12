package kvstore;

import static org.junit.Assert.*;
import static kvstore.KVConstants.*;

import java.net.Socket;

import org.junit.*;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import org.powermock.modules.junit4.PowerMockRunner;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.api.mockito.PowerMockito;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Socket.class, KVMessage.class, TPCMasterHandler.class, TPCSlaveInfo.class, TPCMaster.class})
public class TPCSlaveInfoTest {
	TPCMaster master;
	KVCache masterCache;
    static final long SLAVE1 = 4611686018427387903L;  // Long.MAX_VALUE/2
    static final long SLAVE2 = 9223372036854775807L;  // Long.MAX_VALUE
    static final long SLAVE3 = -4611686018427387903L; // Long.MIN_VALUE/2
    static final long SLAVE4 = -0000000000000000001;  // Long.MIN_VALUE
    static final long SLAVE5 = 6230492013836775123L;  // Arbitrary long value

    TPCSlaveInfo slave1;
    TPCSlaveInfo slave2;
    TPCSlaveInfo slave3;
    TPCSlaveInfo slave4;
    TPCSlaveInfo slave5;

	@Before
	public void setupMaster() throws KVException {
		masterCache = new KVCache(5, 5);
		master = new TPCMaster(4, masterCache);

		slave1 = new TPCSlaveInfo(SLAVE1 + "@111.111.111.111:1");
		slave2 = new TPCSlaveInfo(SLAVE2 + "@111.111.111.111:2");
		slave3 = new TPCSlaveInfo(SLAVE3 + "@111.111.111.111:3");
		slave4 = new TPCSlaveInfo(SLAVE4 + "@111.111.111.111:4");
		slave5 = new TPCSlaveInfo(SLAVE5 + "@111.111.111.111:5");
	}
	
	@Test
	public void getIDTest() throws KVException {
		long testId1 = slave1.getSlaveID();
		long testId2 = slave2.getSlaveID();
		long testId3 = slave3.getSlaveID();
		long testId4 = slave4.getSlaveID();
		long testId5 = slave5.getSlaveID();
		assertEquals(testId1, SLAVE1);
		assertEquals(testId2, SLAVE2);
		assertEquals(testId3, SLAVE3);
		assertEquals(testId4, SLAVE4);
		assertEquals(testId5, SLAVE5);
	}
	
	@Test
	public void getNameTest() throws KVException {
		assertEquals(slave1.getHostname(), "111.111.111.111");
		assertEquals(slave2.getHostname(), "111.111.111.111");
		assertEquals(slave3.getHostname(), "111.111.111.111");
		assertEquals(slave4.getHostname(), "111.111.111.111");
		assertEquals(slave5.getHostname(), "111.111.111.111");
	}
}