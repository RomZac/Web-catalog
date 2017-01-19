package ruunlimit;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class Test_Article {

	@Test
	public void test_parametrs() {
		Catalog web = new Catalog();
		web.init();
		assertEquals("Online", web.web);		
	}

}
