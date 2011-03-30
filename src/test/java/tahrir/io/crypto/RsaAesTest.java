package tahrir.io.crypto;

import java.io.*;
import java.security.interfaces.*;

import org.testng.Assert;
import org.testng.annotations.*;

import tahrir.io.serialization.TrSerializer;
import tahrir.tools.*;

public class RsaAesTest {
	private Tuple2<RSAPublicKey, RSAPrivateKey> keyPair1;
	private Tuple2<RSAPublicKey, RSAPrivateKey> keyPair2;

	@BeforeClass
	public void init() {
		keyPair1 = TrCrypto.createRsaKeyPair();
		keyPair2 = TrCrypto.createRsaKeyPair();
	}

	@Test
	public void testRaw() throws Exception {
		final ByteArraySegment pt = new ByteArraySegment("plaintext".getBytes());
		final ByteArraySegment ct = TrCrypto.encryptRaw(pt, keyPair1.a);
		final ByteArraySegment dpt = TrCrypto.decryptRaw(ct, keyPair1.b);
		Assert.assertEquals(dpt, pt);
	}

	@Test
	public void testObjectEncryptDecrypt() throws Exception {
		final TestObject plain = new TestObject();
		plain.i1 = 12;
		plain.str = "hello";

		final TrPPKEncrypted<TestObject> cipher = TrCrypto.encrypt(plain, keyPair1.a);

		System.out.format("AES Cypher size: %d, RSA cypher size: %d%n", cipher.aesCypherText.length,
				cipher.rsaEncryptedAesKey.length);

		final TestObject decrypted = TrCrypto.decrypt(TestObject.class, cipher, keyPair1.b);

		Assert.assertEquals(decrypted, plain);
	}

	@Test
	public void testSerialization() throws Exception {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
		final DataOutputStream dos = new DataOutputStream(baos);
		TrSerializer.serializeTo(keyPair1.a, dos);
		dos.flush();
		System.out.format("Public key serialized size: %d %n", baos.size());
		TrSerializer.serializeTo(keyPair1.b, dos);
		dos.flush();
		System.out.format("Both keys serialized size: %d %n", baos.size());

		final DataInputStream dis = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
		final RSAPublicKey pubKey = TrSerializer.deserializeFrom(RSAPublicKey.class, dis);
		final RSAPrivateKey privKey = TrSerializer.deserializeFrom(RSAPrivateKey.class, dis);
		final TestObject plain = new TestObject();
		plain.i1 = 12;
		plain.str = "hello";

		final TrPPKEncrypted<TestObject> cipher = TrCrypto.encrypt(plain, pubKey);

		System.out.format("AES Cypher size: %d, RSA cypher size: %d%n", cipher.aesCypherText.length,
				cipher.rsaEncryptedAesKey.length);

		final TestObject decrypted = TrCrypto.decrypt(TestObject.class, cipher, privKey);

		Assert.assertEquals(decrypted, plain);
	}

	@Test
	public void testObjectSignVerify() throws Exception {
		final TestObject obj1 = new TestObject();
		obj1.i1 = 12;
		obj1.str = "hello";
		final TestObject obj2 = new TestObject();
		obj1.i1 = 13;
		obj1.str = "goodbye";
		final TrSignature sig = TrCrypto.sign(obj1, keyPair1.b);
		System.out.format("Object signature size: %d%n", sig.signature.length);
		Assert.assertTrue(TrCrypto.verify(sig, obj1, keyPair1.a));
		Assert.assertFalse(TrCrypto.verify(sig, obj2, keyPair1.a));
		Assert.assertFalse(TrCrypto.verify(sig, obj1, keyPair2.a));
	}

	public static class TestObject {
		public int i1;
		public String str;

		@Override
		public boolean equals(final Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof TestObject))
				return false;
			final TestObject other = (TestObject) obj;
			if (i1 != other.i1)
				return false;
			if (str == null) {
				if (other.str != null)
					return false;
			} else if (!str.equals(other.str))
				return false;
			return true;
		}

	}
}
