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

public class EndToEndTest extends EndToEndTemplate {
	@Test(timeout = kTimeoutSlow)
    @Category(AG_PROJ3_CODE.class)
    @AGTestDetails(points = 1, desc = "Single put request")
	public void onePutRequest() {
		try {
			System.out.println("===============");
			this.client.put("testKey", "testValue");
			System.out.println("===============");
			System.out.println(this.client.get("testKey"));
			System.out.println("===============");
			
		} catch (Exception e) {
			throw new RuntimeException((Exception)e);
		}
	}
}
