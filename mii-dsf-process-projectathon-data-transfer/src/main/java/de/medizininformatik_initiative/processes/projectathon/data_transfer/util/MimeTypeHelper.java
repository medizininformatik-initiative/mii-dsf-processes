package de.medizininformatik_initiative.processes.projectathon.data_transfer.util;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MimeTypeHelper
{
	private static final Logger logger = LoggerFactory.getLogger(MimeTypeHelper.class);

	/**
	 * Detects the mime-type of the provided data and validates if the detected mime-type equals the declared mime-type.
	 * Logs a warning if the full mime-types do not match, throws a {@link RuntimeException} if the base mime-types do
	 * not match.
	 *
	 * @param data
	 *            of which the mime-type should be detected
	 * @param declared
	 *            the declared mime-type of the data
	 * @throws RuntimeException
	 *             if the detected and the declared base mime-type do not match
	 */
	public static void validate(byte[] data, String declared)
	{
		MediaType declaredMimeType = MediaType.parse(declared);
		MediaType detectedMimeType = MediaType.EMPTY;

		try
		{
			TikaConfig tika = new TikaConfig();
			TikaInputStream input = TikaInputStream.get(data);
			Metadata metadata = new Metadata();

			// gives only a hint to the possible mime-type
			// this needed because text/csv cannot be detected without any hint and would resolve to text/plain
			metadata.add(Metadata.CONTENT_TYPE, declaredMimeType.toString());

			detectedMimeType = tika.getDetector().detect(input, metadata);
		}
		catch (Exception exception)
		{
			throw new RuntimeException("Could not detect mime-type of binary", exception);
		}

		if (!declaredMimeType.equals(detectedMimeType))
			logger.warn("Declared mime-type='{}' does not match detected mime-type='{}'", declaredMimeType.toString(),
					detectedMimeType.toString());

		if (!declaredMimeType.getType().equals(detectedMimeType.getType()))
		{
			throw new RuntimeException("Declared base mime-type of '" + declaredMimeType.toString()
					+ "' does not match detected base mime-type of '" + detectedMimeType.toString() + "'");
		}
	}
}
