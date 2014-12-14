package kvstore;

import static autograder.TestUtils.*;
import static kvstore.KVConstants.*;
import static kvstore.Utils.*;
import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Random;

import kvstore.Utils.ErrorLogger;
import kvstore.Utils.RandomString;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import autograder.AGCategories.AGTestDetails;
import autograder.AGCategories.AG_PROJ3_CODE;
import autograder.AGCategories.AG_PROJ4_CODE;

public class TPCEndToEndTest extends TPCEndToEndTemplate {
	
	@Test(timeout = kTimeoutDefault)
    @Category(AG_PROJ4_CODE.class)
    @AGTestDetails(points = 1, desc = "Single put request")
	public void multiplePutDelRequest() {
		try {
			this.client.put("testKey", "testValue");
			this.client.put("testKey1", "testValue1");
			assertEquals("testValue", this.client.get("testKey"));
			assertEquals("testValue1", this.client.get("testKey1"));
			this.client.del("testKey");
			this.client.del("testKey1");
			this.client.get("testKey");
			this.client.get("testKey1");
			fail("Failed to delete!");
        } catch (KVException e) {
            
        }
	}
	@Test(timeout = kTimeoutSlow)
    @Category(AG_PROJ3_CODE.class)
    @AGTestDetails(points = 1, desc = "Single put request")
	public void onePutRequest() {
		try {
			this.client.put("testKey", "testValue");
			assertEquals("testValue", this.client.get("testKey"));
        } catch (KVException e) {
            fail("Client threw unexpected exception!");
        }
	}
	
	@Test(timeout = kTimeoutSlow)
    @Category(AG_PROJ3_CODE.class)
    @AGTestDetails(points = 1, desc = "Put pairs then get")
	public void thousandPutRequest(){
		
		int TestLength = 1;
		String[] TestKeys = new String[TestLength];
		String[] TestValues = new String[TestLength];
		
		/* Put*/
		for(int i = 0; i < TestLength; i ++){
			TestKeys[i] = "Key" + Integer.toString(i);
			TestValues[i] = "Value" + Integer.toString(i) + "Amy";
			try {
				this.client.put(TestKeys[i], TestValues[i]);
			} catch (KVException e) {
				// TODO Auto-generated catch block
				fail("Client threw unexpected exception!");
			}
		}
		
		/* Check for values*/
		for(int i = 0; i < TestLength; i ++){
	        try {
	        	assertEquals(TestValues[i], this.client.get(TestKeys[i]));
	        } catch (KVException e) {
	            fail("Client threw unexpected exception!");
	        }
		}
	}
	
	public void putTenDelTen(){
		int TestLength = 3;
		String[] TestKeys = new String[TestLength];
		String[] TestValues = new String[TestLength];
		
		/* Put */
		for(int i = 0; i < TestLength; i ++){
			TestKeys[i] = "Key" + Integer.toString(i);
			TestValues[i] = "Value" + Integer.toString(i) + "Amy";
			try {
				this.client.put(TestKeys[i], TestValues[i]);
			} catch (KVException e) {
				// TODO Auto-generated catch block
				fail("Client threw unexpected exception!");
			}
		}
		
		/* Delete*/
		for(int i = 0; i < TestLength; i++){
			try {
				this.client.del(TestKeys[i]);
			} catch (KVException e) {
				// TODO Auto-generated catch block
				fail("Client threw unexpected exception!");
			}
		}
		/* Check that non of the keys exists*/
		for(int i = 0; i < TestLength; i ++){
			try {
	            client.get(TestKeys[i]);
	            fail("Client did not throw exception!");
	        } catch (KVException kve) {
	            assertKVExceptionEquals(ERROR_NO_SUCH_KEY, kve);
	        }
		}
	}
//	
	@Test(timeout = kTimeoutSlow)
    @Category(AG_PROJ3_CODE.class)
    @AGTestDetails(points = 1, desc = "Put delete put delete")
	public void putDelPutDel(){
		int TestLength = 3;
		String[] TestKeys = new String[TestLength];
		String[] TestValues = new String[TestLength];
		
		/* Put, Delete, Put, Delete...*/
		for(int j = 0; j < 10; j ++){
			for(int i = 0; i < TestLength; i ++){
				TestKeys[i] = "Key" + Integer.toString(i);
				TestValues[i] = "Value" + Integer.toString(i) + "Amy";
				try {
					this.client.put(TestKeys[i], TestValues[i]);
				} catch (KVException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			for(int i = 0; i < TestLength; i++){
				try {
					this.client.del(TestKeys[i]);
				} catch (KVException e) {
					// TODO Auto-generated catch block
					fail("Client threw unexpected exception!");
				}
			}
			
			/* Check that non of the keys exists*/
			for(int i = 0; i < TestLength; i ++){
				try {
		            client.get(TestKeys[i]);
		            fail("Client did not throw exception!");
		        } catch (KVException kve) {
		            assertKVExceptionEquals(ERROR_NO_SUCH_KEY, kve);
		        }
			}
		}	
	}
	
	@Test(timeout = kTimeoutSlow)
    @Category(AG_PROJ3_CODE.class)
	@AGTestDetails(points = 1, desc = "Put delete put delete 10 times")
	public void putDelIterate(){
		int TestLength = 3;
		String[] TestKeys = new String[TestLength];
		String[] TestValues = new String[TestLength];
		for(int i = 0; i < TestLength; i ++){
			TestKeys[i] = "Key" + Integer.toString(i);
			TestValues[i] = "Value" + Integer.toString(i) + "Amy";
		}
		
		for(int j = 0; j < 10; j ++){
			for(int i = 0; i < TestLength; i ++){
				
				/* Try to get the values should throw exception*/
				try {
					client.get(TestKeys[i]);
					fail("Client did not throw exception!");
				} catch (KVException kve) {
					// TODO Auto-generated catch block
					assertKVExceptionEquals(ERROR_NO_SUCH_KEY, kve);
				}
				
				/* put values */
				try {
					this.client.put(TestKeys[i], TestValues[i]);
				} catch (KVException kve) {
					// TODO Auto-generated catch block
					fail("Client threw unexpected exception!");
				}
				
				/* get values */
				try {
					assertEquals(TestValues[i], client.get(TestKeys[i]));
				} catch (KVException kve) {
					// TODO Auto-generated catch block
					fail("Client threw unexpected exception!");
				}
				
				/* delete values*/
				try {
					this.client.del(TestKeys[i]);
				} catch (KVException e) {
					// TODO Auto-generated catch block
					fail("Client threw unexpected exception!");
				}
				
				/* Try to get the values should throw exception*/
				try {
					client.get(TestKeys[i]);
					fail("Client did not throw exception!");
				} catch (KVException kve) {
					// TODO Auto-generated catch block
					assertKVExceptionEquals(ERROR_NO_SUCH_KEY, kve);
				}
				
			}
		}
	}
	
	@Test(timeout = kTimeoutSlow)
    @Category(AG_PROJ3_CODE.class)
	@AGTestDetails(points = 1, desc = "delete non existant keys")
	public void delNonexistant(){
		int TestLength = 3;
		String[] TestKeys = new String[TestLength];
		String[] TestValues = new String[TestLength];
		for(int i = 0; i < TestLength; i ++){
			TestKeys[i] = "Key" + Integer.toString(i);
			TestValues[i] = "Value" + Integer.toString(i) + "Amy";
		}
		
		/* delete values should throw exception*/
		for(int i = 0; i < TestLength; i++){
			try {
				this.client.del(TestKeys[i]);
				fail("Client did not throw exception!");
			} catch (KVException kve) {
				// TODO Auto-generated catch block
				assertKVExceptionEquals(ERROR_NO_SUCH_KEY, kve);
			}
		}
		
		/* Put values*/
		for(int i = 0; i < TestLength; i++){
			try {
				this.client.put(TestKeys[i], TestValues[i]);
			} catch (KVException kve) {
				// TODO Auto-generated catch block
				fail("Client threw unexpected exception!");
			}
		}
		
		/* Delete values*/
		for(int i = 0; i < TestLength; i++){
			try {
				this.client.del(TestKeys[i]);
			} catch (KVException e) {
				// TODO Auto-generated catch block
				fail("Client threw unexpected exception!");
			}
		}
		
		/* delete values should throw exception*/
		for(int i = 0; i < TestLength; i++){
			try {
				this.client.del(TestKeys[i]);
				fail("Client did not throw exception!");
			} catch (KVException kve) {
				// TODO Auto-generated catch block
				assertKVExceptionEquals(ERROR_NO_SUCH_KEY, kve);
			}
		}
	}
	
	@Test(timeout = kTimeoutSlow)
    @Category(AG_PROJ3_CODE.class)
	@AGTestDetails(points = 1, desc = "key and value empty")
	public void emptyKeyValue(){
		int TestLength = 3;
		String[] TestKeys = new String[TestLength];
		String[] TestValues = new String[TestLength];
		for(int i = 0; i < TestLength; i ++){
			TestKeys[i] = "";
			TestValues[i] = "Value" + Integer.toString(i) + "Amy";
			try {
				this.client.put(TestKeys[i], TestValues[i]);
				fail("Client did not throw exception!");
			} catch (KVException kve) {
				// TODO Auto-generated catch block
				assertKVExceptionEquals(ERROR_INVALID_KEY, kve);
			}
			
			try {
				this.client.put(TestValues[i], TestKeys[i]);
				fail("Client did not throw exception!");
			} catch (KVException kve) {
				// TODO Auto-generated catch block
				assertKVExceptionEquals(ERROR_INVALID_VALUE, kve);
			}
		}
	}
}