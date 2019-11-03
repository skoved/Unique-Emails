package com.github.skoved.uniqueemails;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
class UniqueEmailsApplicationTests {

	@Autowired
	EmailController emailController;

	@Test
	void removeCommentBeginning() {
		String email = "(comment)samkoved@gmail.com";
		assertEquals("samkoved@gmail.com", emailController.removeComments(email),
				"Remove Comment Beginning passed!");
	}

	@Test
	void removeCommentEnd() {
		String email = "samkoved@gmail.com(comment)";
		assertEquals("samkoved@gmail.com", emailController.removeComments(email),
				"Remove Comment End passed!");
	}

	@Test
	void removeCommentBeforeAt() {
		String email = "samkoved(comment)@gmail.com";
		assertEquals("samkoved@gmail.com", emailController.removeComments(email),
				"Remove Comment Before At passed!");
	}

	@Test
	void removeCommentAfterAt() {
		String email = "samkoved@(comment)gmail.com";
		assertEquals("samkoved@gmail.com", emailController.removeComments(email),
				"Remove Comment After At passed!");
	}

	@Test
	void invalidComments() {
		String email1 = "(commentsamkoved@gmail.com";
		String email2 = "samkoved(comment@gmail.com";
		String email3 = "samkoved@comment)gmail.com";
		String email4 = "samkoved@gmail.comcomment)";
		assertNull(emailController.removeComments(email1));
		assertNull(emailController.removeComments(email2));
		assertEquals(email3, emailController.removeComments(email3));
		assertNull(emailController.removeComments(email4), "Invalid Comment passed!");
	}

	@Test
	void ignoreEscapedParens() {
		String email1 = "( \\( )samkoved@gmail.com";
		String email2 = "samkoved( \\( )@gmail.com";
		String email3 = "samkoved@( \\( )gmail.com";
		String email4 = "samkoved@gmail.com( \\( )";
		assertEquals("samkoved@gmail.com", emailController.removeComments(email1));
		assertEquals("samkoved@gmail.com", emailController.removeComments(email2));
		assertEquals("samkoved@gmail.com", emailController.removeComments(email3));
		assertEquals("samkoved@gmail.com", emailController.removeComments(email4), "Ignore Escaped Parens passed!");
	}

	@Test
	void embeddedComments() {
		String email1 = "(comment( () ))samkoved@gmail.com";
		String email2 = "samkoved(comment( () ))@gmail.com";
		String email3 = "samkoved@(comment( () ))gmail.com";
		String email4 = "samkoved@gmail.com(comment( () ))";
		assertEquals("samkoved@gmail.com", emailController.removeComments(email1));
		assertEquals("samkoved@gmail.com", emailController.removeComments(email2));
		assertEquals("samkoved@gmail.com", emailController.removeComments(email3));
		assertEquals("samkoved@gmail.com", emailController.removeComments(email4), "Embedded Comments passed!");
	}

	@Test
	void unwrapTest() {
		String email1 = "samkoved@gmail.com";
		String email2 = "<samkoved@gmail.com>";
		String email3 = "Sam Koved <samkoved@gmail.com>";
		String email4 = "Sam Koved <samkoved@gmail.com";
		assertEquals(email1, emailController.unwrap(email1));
		assertEquals(email1, emailController.unwrap(email2));
		assertEquals(email1, emailController.unwrap(email3));
		assertEquals(email4, emailController.unwrap(email4), "Unwrap Test passed!");
	}

	@Test
	void formatEmailTest() {
		String email1 = "domain!samkoved@gmail.com";
		String email2 = "\"this is my good email!\"@gmail.com";
		String email3 = "samkoved%gmail.com@yahoo.com";
		assertEquals("samkoved@domain", emailController.formatEmail(email1));
		assertEquals(email2.replace(".", ""), emailController.formatEmail(email2));
		assertEquals("samkoved@gmailcom", emailController.formatEmail(email3),
				"Format Email Test passed!");
	}
}
