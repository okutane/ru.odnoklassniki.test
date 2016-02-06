package ru.odnoklassniki.test.java;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;


public class TcpMapper
{
	private final ProxySetting[] _settings;
	private final Selector _selector;
	
	public TcpMapper(ProxySetting[] settings)
			throws IOException
	{
		_settings = settings;
		_selector = Selector.open();
	}
	
	public void run()
		throws IOException
	{
		for (ProxySetting setting : _settings) {
			ServerSocketChannel server = ServerSocketChannel.open();
			server.configureBlocking(false);
			server.socket().bind(setting.getLocalAddress());
			
			server.register(_selector, SelectionKey.OP_ACCEPT, new ServerSocketHandler(setting.getRemoteAddress()));
		}
			
		while (true) {
			_selector.select();
			
			Iterator<SelectionKey> it = _selector.selectedKeys().iterator();
			while (it.hasNext()) {
				SelectionKey key = it.next();
				it.remove();
				SocketHandler handler = (SocketHandler) key.attachment();
				handler.handle(key);
			}
		}
	}

	private class SocketHandler
	{
		public final void handle(SelectionKey key)
			throws IOException
		{
			if (key.isValid()) {
				if (key.isAcceptable()) {
					onAcceptable(key);
				}
				if (key.isConnectable()) {
					onConnectable(key);
				}
				if (key.isReadable()) {
					onReadable(key);
				}
			}
		}
		
		void onReadable(SelectionKey key)
			throws IOException
		{
			throw new UnsupportedOperationException();
		}
		
		void onAcceptable(SelectionKey key)
			throws IOException
		{
			throw new UnsupportedOperationException();			
		}
		
		void onConnectable(SelectionKey key)
			throws IOException
		{
			throw new UnsupportedOperationException();		
		}
	}
	
	private class ServerSocketHandler
		extends SocketHandler
	{
		private final SocketAddress _remoteAddress;
		
		public ServerSocketHandler(SocketAddress remoteAddress)
		{
			_remoteAddress = remoteAddress;
		}
		
		void onAcceptable(SelectionKey key)
			throws IOException
		{
			ServerSocketChannel server = (ServerSocketChannel) key.channel();
			SocketChannel incomingConnection = server.accept();
			incomingConnection.configureBlocking(false);
			
			SocketChannel outgoingConnection = SocketChannel.open();
			outgoingConnection.configureBlocking(false);
			outgoingConnection.register(_selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ, new RemoteConnectionHandler(incomingConnection));
			outgoingConnection.connect(_remoteAddress);
		}
	}
	
	private class LinkedConnectionHandler
		extends SocketHandler
	{
		private final ByteBuffer _buffer = ByteBuffer.allocate(1024);
		protected final SocketChannel _linkedChannel;
		
		public LinkedConnectionHandler(SocketChannel linkedChannel)
		{
			_linkedChannel = linkedChannel;
		}
		
		@Override
		final void onReadable(SelectionKey key)
		{
			SocketChannel channel = (SocketChannel) key.channel();
			try {
				int bc = channel.read(_buffer);
				if (bc == -1) {
					throw new IOException();
					}
				_buffer.flip();
				_linkedChannel.write(_buffer);
				_buffer.clear();
			} catch (IOException e) {
				key.cancel();
				closeSafe(channel);
				SelectionKey linkedKey = _linkedChannel.keyFor(_selector);
				linkedKey.cancel();
				closeSafe(_linkedChannel);
			}
		}

		protected void closeSafe(SocketChannel channel)
		{
			try {
				channel.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private class RemoteConnectionHandler
		extends LinkedConnectionHandler
	{
		public RemoteConnectionHandler(SocketChannel linkedChannel)
		{
			super(linkedChannel);
		}
		
		@Override
		void onConnectable(SelectionKey key)
		{
			SocketChannel channel = (SocketChannel) key.channel();
			try {
				if (channel.finishConnect()) {
					_linkedChannel.register(_selector, SelectionKey.OP_READ, new LinkedConnectionHandler(channel));
				} else {
					throw new IOException("finishConnect: false");
				}
			} catch (IOException e) {
				e.printStackTrace();
				key.cancel();
				closeSafe(channel);
				closeSafe(_linkedChannel);
			}
		}
	}
}
