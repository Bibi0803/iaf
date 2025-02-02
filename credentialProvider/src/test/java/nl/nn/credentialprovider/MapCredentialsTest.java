package nl.nn.credentialprovider;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;


public class MapCredentialsTest {

	private Map<String,String> aliases;
	
	@Before
	public void setup() {
		aliases = new HashMap<>();
		aliases.put("noUsername/password","password from alias");
		aliases.put("straight/username","username from alias");
		aliases.put("straight/password","password from alias");
		aliases.put("singleValue","Plain Credential");
	}
	
	
	@Test
	public void testNoAlias() {
		
		String alias = null;
		String username = "fakeUsername";
		String password = "fakePassword";
		
		MapCredentials mc = new MapCredentials(alias, username, password, null);
		
		assertEquals(username, mc.getUsername());
		assertEquals(password, mc.getPassword());
	}

	@Test
	public void testNoFileSystem() {
		
		String alias = "fakeAlias";
		String username = "fakeUsername";
		String password = "fakePassword";
		
		MapCredentials mc = new MapCredentials(alias, username, password, null);
		
		assertEquals(username, mc.getUsername());
		assertEquals(password, mc.getPassword());
	}

	@Test
	public void testPlainAlias() {
		
		String alias = "straight";
		String username = "fakeUsername";
		String password = "fakePassword";
		String expectedUsername = "username from alias";
		String expectedPassword = "password from alias";
		
		MapCredentials mc = new MapCredentials(alias, username, password, aliases);
		
		assertEquals(expectedUsername, mc.getUsername());
		assertEquals(expectedPassword, mc.getPassword());
	}

	@Test
	public void testAliasWithoutUsername() {
		
		String alias = "noUsername";
		String username = "fakeUsername";
		String password = "fakePassword";
		String expectedUsername = username;
		String expectedPassword = "password from alias";
		
		MapCredentials mc = new MapCredentials(alias, username, password, aliases);
		
		assertEquals(expectedUsername, mc.getUsername());
		assertEquals(expectedPassword, mc.getPassword());
	}

	@Test
	public void testPlainCredential() {
		
		String alias = "singleValue";
		String username = null;
		String password = "fakePassword";
		String expectedUsername = null;
		String expectedPassword = "Plain Credential";
		
		MapCredentials mc = new MapCredentials(alias, username, password, aliases);
		
		assertEquals(expectedUsername, mc.getUsername());
		assertEquals(expectedPassword, mc.getPassword());
	}
}
