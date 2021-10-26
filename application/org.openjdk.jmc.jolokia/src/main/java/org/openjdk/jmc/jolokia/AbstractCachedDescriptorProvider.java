package org.openjdk.jmc.jolokia;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.openjdk.jmc.rjmx.descriptorprovider.AbstractDescriptorProvider;
import org.openjdk.jmc.rjmx.descriptorprovider.IDescriptorListener;

@SuppressWarnings("restriction")
public abstract class AbstractCachedDescriptorProvider extends AbstractDescriptorProvider {

	private static final long LOCAL_REFRESH_INTERVAL = 20000;
	private Scanner scanner;
	private Thread scannerThread;
	/**
	 * Map<UUID, IServerDescriptor>
	 */
	private final Map<String, ServerConnectionDescriptor> knownDescriptors = new HashMap<>();

	/**
	 * This is where we periodically scan and report deltas to the listeners.
	 */
	private class Scanner implements Runnable {
		boolean isRunning;

		@Override
		public void run() {
			isRunning = true;
			while (isRunning) {
				try {
					scan();
					Thread.sleep(LOCAL_REFRESH_INTERVAL);
				} catch (InterruptedException ignore) {
					// Don't mind being interrupted.
				}
			}
		}

		/**
		 * Marks this scanner as terminated.
		 */
		public void shutdown() {
			isRunning = false;
		}

		protected void scan() {
			Map<String, ServerConnectionDescriptor> newOnes = discoverJvms();

			synchronized (knownDescriptors) {
				// Remove stale ones...
				for (Iterator<Entry<String, ServerConnectionDescriptor>> entryIterator = knownDescriptors.entrySet()
						.iterator(); entryIterator.hasNext();) {
					Entry<String, ServerConnectionDescriptor> entry = entryIterator.next();
					if (newOnes.containsKey(entry.getKey())) {
						continue;
					}
					entryIterator.remove();
					onDescriptorRemoved(entry.getKey());
				}

				// Add new ones...
				for (Entry<String, ServerConnectionDescriptor> entry : newOnes.entrySet()) {
					if (knownDescriptors.containsKey(entry.getKey())) {
						continue;
					}
					onDescriptorDetected(entry.getValue(), entry.getValue().getPath(), entry.getValue().serviceUrl(),
							entry.getValue());
				}
				knownDescriptors.clear();
				knownDescriptors.putAll(newOnes);
			}
		}
	}

	/**
	 * Sets up the thread.
	 */
	private void initialize() {

		scanner = new Scanner();
		scannerThread = new Thread(scanner, getName()); // $NON-NLS-1$
		scannerThread.start();
	}

	protected abstract boolean isEnabled();

	protected abstract Map<String, ServerConnectionDescriptor> discoverJvms();

	@Override
	public void addDescriptorListener(IDescriptorListener l) {
		synchronized (m_descriptorListeners) {
			if (m_descriptorListeners.size() == 0) {
				super.addDescriptorListener(l);
				initialize();
			} else {
				super.addDescriptorListener(l);
			}
		}
	}

	@Override
	public void removeDescriptorListener(IDescriptorListener l) {
		synchronized (m_descriptorListeners) {
			super.removeDescriptorListener(l);
			if (m_descriptorListeners.size() == 0 && scanner != null) {
				scanner.shutdown();
				scannerThread.interrupt();
			}
		}
	}

	/**
	 * Shuts down the scanner thread.
	 */
	public void shutdown() {
		scanner.shutdown();
	}

}
