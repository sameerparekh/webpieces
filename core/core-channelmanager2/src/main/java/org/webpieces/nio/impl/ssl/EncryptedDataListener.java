package org.webpieces.nio.impl.ssl;

import java.nio.ByteBuffer;

import org.webpieces.nio.api.channels.Channel;
import org.webpieces.nio.api.handlers.DataListener;

public class EncryptedDataListener implements DataListener {

	@Override
	public void incomingData(Channel channel, ByteBuffer b, boolean isOpeningConnection) {
	}

	@Override
	public void farEndClosed(Channel channel) {
	}

	@Override
	public void failure(Channel channel, ByteBuffer data, Exception e) {
	}

	@Override
	public void applyBackPressure(Channel channel) {
	}

	@Override
	public void releaseBackPressure(Channel channel) {
	}

}
