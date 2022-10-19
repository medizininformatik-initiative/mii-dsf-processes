package de.medizininformatik_initiative.process.projectathon.data_transfer.crypto;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;

import org.highmed.pseudonymization.crypto.AesGcmUtil;

public class RsaAesGcmUtil
{
	private static final String RSA_CIPHER = "RSA/ECB/PKCS1Padding";
	private static final int RSA_KEY_LENGTH = 4096;
	private static final int ENCRYPTED_AES_KEY_LENGTH = 512;

	public static byte[] encrypt(PublicKey publicKey, byte[] data, String sendingOrganizationIdentifier,
			String receivingOrganizationIdentifier)
			throws NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException,
			InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, ShortBufferException
	{
		SecretKey aesKey = AesGcmUtil.generateAES256Key();

		byte[] aad = getAad(sendingOrganizationIdentifier, receivingOrganizationIdentifier);
		byte[] encryptedAesKey = encryptRsa(aesKey, publicKey);
		byte[] encryptedData = AesGcmUtil.encrypt(data, aad, aesKey);

		if (encryptedAesKey.length != ENCRYPTED_AES_KEY_LENGTH)
			throw new IllegalStateException("Encrypted AES key length " + ENCRYPTED_AES_KEY_LENGTH + " expected");

		byte[] output = new byte[encryptedAesKey.length + encryptedData.length];
		System.arraycopy(encryptedAesKey, 0, output, 0, encryptedAesKey.length);
		System.arraycopy(encryptedData, 0, output, encryptedAesKey.length, encryptedData.length);

		return output;
	}

	public static byte[] decrypt(PrivateKey privateKey, byte[] encrypted, String sendingOrganizationIdentifier,
			String receivingOrganizationIdentifier)
			throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException, NoSuchPaddingException,
			NoSuchAlgorithmException, InvalidAlgorithmParameterException
	{
		byte[] encryptedAesKey = new byte[ENCRYPTED_AES_KEY_LENGTH];
		byte[] encryptedData = new byte[encrypted.length - ENCRYPTED_AES_KEY_LENGTH];
		System.arraycopy(encrypted, 0, encryptedAesKey, 0, ENCRYPTED_AES_KEY_LENGTH);
		System.arraycopy(encrypted, ENCRYPTED_AES_KEY_LENGTH, encryptedData, 0,
				encrypted.length - ENCRYPTED_AES_KEY_LENGTH);
		byte[] aad = getAad(sendingOrganizationIdentifier, receivingOrganizationIdentifier);

		SecretKey key = decryptRsa(encryptedAesKey, privateKey);
		return AesGcmUtil.decrypt(encryptedData, aad, key);
	}

	public static KeyPair generateRsa4096KeyPair() throws NoSuchAlgorithmException
	{
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(RSA_KEY_LENGTH);
		return keyGen.generateKeyPair();
	}

	private static byte[] encryptRsa(SecretKey aesKey, PublicKey publicKey) throws BadPaddingException,
			IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException
	{
		if (!"AES".equals(aesKey.getAlgorithm()))
			throw new IllegalArgumentException("AES key expected");

		Cipher cipher = Cipher.getInstance(RSA_CIPHER);
		cipher.init(Cipher.ENCRYPT_MODE, publicKey);
		return cipher.doFinal(aesKey.getEncoded());
	}

	private static SecretKey decryptRsa(byte[] encryptedKey, PrivateKey privateKey) throws BadPaddingException,
			IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException
	{
		Cipher cipher = Cipher.getInstance(RSA_CIPHER);
		cipher.init(Cipher.DECRYPT_MODE, privateKey);
		byte[] decrypted = cipher.doFinal(encryptedKey);

		return new SecretKeySpec(decrypted, "AES");
	}

	private static byte[] getAad(String sendingOrganizationIdentifier, String receivingOrganizationIdentifier)
	{
		return (sendingOrganizationIdentifier + "|" + receivingOrganizationIdentifier).getBytes(StandardCharsets.UTF_8);
	}
}
