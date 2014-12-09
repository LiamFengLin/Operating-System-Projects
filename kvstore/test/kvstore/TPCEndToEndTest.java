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
import autograder.AGCategories.AG_PROJ4_CODE;

public class TPCEndToEndTest extends TPCEndToEndTemplate {
	@Test()
    @Category(AG_PROJ4_CODE.class)
    @AGTestDetails(points = 1, desc = "Single put request")
	public void onePutRequest() {
		try {
			this.client.put("testKey", "testValue");
			assertEquals("testValue", this.client.get("testKey"));
        } catch (KVException e) {
            fail("Client threw unexpected exception!");
        	
        }
	}
	
//	create a slave failure
//  create multiple client put, get or del at same time

}