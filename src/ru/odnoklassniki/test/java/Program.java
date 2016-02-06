package ru.odnoklassniki.test.java;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Program
{
	/**
	 * @param args
	 */
	public static void main(String[] args)
		throws Exception
	{
		Properties proxySettings = readProperties("/proxy.properties");
		ProxySetting[] settings = ProxySetting.fromProperties(proxySettings);
		TcpMapper mapper = new TcpMapper(settings);
		mapper.run();
	}

	private static Properties readProperties(String path)
		throws IOException
	{
		Properties proxySettings = new Properties();
		InputStream stream = Program.class.getResourceAsStream(path);
		try {
			proxySettings.load(stream);
		} finally {
			stream.close();
		}
		return proxySettings;
	}
}
