package org.openjdk.jmc.flightrecorder.flameview.websocket;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

public class WebSocketEndpoint extends WebSocketAdapter {
	private CountDownLatch closureLatch = new CountDownLatch(1);

	@Override
	public void onWebSocketConnect(Session sess) {
		super.onWebSocketConnect(sess);
		System.out.println("Socket Connected: " + sess);
	}

	@Override
	public void onWebSocketText(String message) {
		super.onWebSocketText(message);
		System.out.println("Received TEXT message: " + message);

		if (message.toLowerCase(Locale.US).contains("bye")) {
			getSession().close(StatusCode.NORMAL, "Thanks");
		}
	}

	@Override
	public void onWebSocketClose(int statusCode, String reason) {
		super.onWebSocketClose(statusCode, reason);
		System.out.println("Socket Closed: [" + statusCode + "] " + reason);
		closureLatch.countDown();
	}

	@Override
	public void onWebSocketError(Throwable cause) {
		super.onWebSocketError(cause);
		cause.printStackTrace(System.err);
	}

	public void awaitClosure() throws InterruptedException {
		System.out.println("Awaiting closure from remote");
		closureLatch.await();
	}

}
