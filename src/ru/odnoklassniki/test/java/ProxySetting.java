package ru.odnoklassniki.test.java;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

class ProxySetting
{
	private final String _name;
	private final SocketAddress _localAddress;
	private final SocketAddress _remoteAddress;
	
	public ProxySetting(String name, int localPort, InetSocketAddress remoteAddress) {
		_name = name;
		_localAddress = new InetSocketAddress(localPort);
		_remoteAddress = remoteAddress;
	}
	
	public SocketAddress getLocalAddress() {
		return _localAddress;
	}
	
	public SocketAddress getRemoteAddress() {
		return _remoteAddress;
	}

	public String getName() {
		return _name;
	}

	static ProxySetting[] fromProperties(Properties proxySettings) {
		List<ProxySetting> parsedSettings = new ArrayList<ProxySetting>();
		for (Entry<Object, Object> entry : proxySettings.entrySet()) {
			String propertyName = (String)entry.getKey();
			String[] propertyNameParts = propertyName.split(REGEX_FOR_SEPARATOR);
			if (propertyNameParts.length != 2) {
				continue;
			}
			String propertyKind = propertyNameParts[1];
			if (propertyKind.equals(LOCAL_PORT)) {
				String entryName = propertyNameParts[0];
				int localPort = Integer.parseInt((String)entry.getValue());
				String remoteHost = proxySettings.getProperty(entryName + SEPARATOR + REMOTE_HOST);
				int remotePort = Integer.parseInt(proxySettings.getProperty(entryName + SEPARATOR + REMOTE_PORT));
				ProxySetting setting = new ProxySetting(entryName, localPort, new InetSocketAddress(remoteHost, remotePort));
				parsedSettings.add(setting);
			}
		}
		return parsedSettings.toArray(new ProxySetting[parsedSettings.size()]);
	}
	
	private static final String SEPARATOR = ".";
	private static final String REGEX_FOR_SEPARATOR = "\\.";
	private static final String REMOTE_PORT = "remotePort";
	private static final String REMOTE_HOST = "remoteHost";
	private static final String LOCAL_PORT = "localPort";
}