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
@PrepareForTest({Socket.class, KVMessage.class, TPCMasterHandler.class, TPCSlaveInfo.class, TPCMaster.class, TPCMasterHandler.class})
public class TPCMasterHandlerTest {
	
}