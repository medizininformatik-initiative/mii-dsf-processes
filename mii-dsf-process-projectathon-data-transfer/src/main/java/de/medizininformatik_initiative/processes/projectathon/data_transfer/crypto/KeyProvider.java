package de.medizininformatik_initiative.processes.projectathon.data_transfer.crypto;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public interface KeyProvider
{
	PrivateKey getPrivateKey();

	PublicKey getPublicKey();

	static PublicKey fromBytes(byte[] bytes) throws NoSuchAlgorithmException, InvalidKeySpecException
	{
		PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));

		if (publicKey instanceof RSAPublicKey)
		{
			return publicKey;
		}
		else
		{
			throw new IllegalStateException("PublicKey not a RSAPublicKey");
		}
	}
}
