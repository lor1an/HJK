package org.apache.commons.io.input;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.TailerListener;

public class Tailer implements Runnable {

	private static final int DEFAULT_DELAY_MILLIS = 1000;

	private static final String RAF_MODE = "r";

	private static final int DEFAULT_BUFSIZE = 4096;
	private final byte inbuf[];

	private final File file;
	private final long delayMillis;
	private final boolean end;

	private final TailerListener listener;

	private final boolean reOpen;
	private volatile boolean run = true;

	public Tailer(File file, TailerListener listener) {
		this(file, listener, DEFAULT_DELAY_MILLIS);
	}

	public Tailer(File file, TailerListener listener, long delayMillis) {
		this(file, listener, delayMillis, false);
	}

	public Tailer(File file, TailerListener listener, long delayMillis,
			boolean end) {
		this(file, listener, delayMillis, end, DEFAULT_BUFSIZE);
	}

	public Tailer(File file, TailerListener listener, long delayMillis,
			boolean end, boolean reOpen) {
		this(file, listener, delayMillis, end, reOpen, DEFAULT_BUFSIZE);
	}

	public Tailer(File file, TailerListener listener, long delayMillis,
			boolean end, int bufSize) {
		this(file, listener, delayMillis, end, false, bufSize);
	}

	public Tailer(File file, TailerListener listener, long delayMillis,
			boolean end, boolean reOpen, int bufSize) {
		this.file = file;
		this.delayMillis = delayMillis;
		this.end = end;

		this.inbuf = new byte[bufSize];

		// Save and prepare the listener
		this.listener = listener;
		listener.init(this);
		this.reOpen = reOpen;
	}

	public static Tailer create(File file, TailerListener listener,
			long delayMillis, boolean end, int bufSize) {
		Tailer tailer = new Tailer(file, listener, delayMillis, end, bufSize);
		Thread thread = new Thread(tailer);
		thread.setDaemon(true);
		thread.start();
		return tailer;
	}

	public static Tailer create(File file, TailerListener listener,
			long delayMillis, boolean end, boolean reOpen, int bufSize) {
		Tailer tailer = new Tailer(file, listener, delayMillis, end, reOpen,
				bufSize);
		Thread thread = new Thread(tailer);
		thread.setDaemon(true);
		thread.start();
		return tailer;
	}

	public static Tailer create(File file, TailerListener listener,
			long delayMillis, boolean end) {
		return create(file, listener, delayMillis, end, DEFAULT_BUFSIZE);
	}

	public static Tailer create(File file, TailerListener listener,
			long delayMillis, boolean end, boolean reOpen) {
		return create(file, listener, delayMillis, end, reOpen, DEFAULT_BUFSIZE);
	}

	public static Tailer create(File file, TailerListener listener,
			long delayMillis) {
		return create(file, listener, delayMillis, false);
	}

	public static Tailer create(File file, TailerListener listener) {
		return create(file, listener, DEFAULT_DELAY_MILLIS, false);
	}

	public File getFile() {
		return file;
	}

	public long getDelay() {
		return delayMillis;
	}

	public void run() {
		RandomAccessFile reader = null;
		try {
			long last = 0; // The last time the file was checked for changes
			long position = 0; // position within the file
			// Open the file
			while (run && reader == null) {
				try {
					reader = new RandomAccessFile(file, RAF_MODE);
				} catch (FileNotFoundException e) {
					listener.fileNotFound();
				}

				if (reader == null) {
					try {
						Thread.sleep(delayMillis);
					} catch (InterruptedException e) {
					}
				} else {
					// The current position in the file
					position = end ? file.length() : 0;
					last = System.currentTimeMillis();
					reader.seek(position);
				}
			}

			while (run) {

				boolean newer = FileUtils.isFileNewer(file, last); // IO-279,
																	// must be
																	// done
																	// first

				// Check the file length to see if it was rotated
				long length = file.length();

				if (length < position) {

					// File was rotated
					listener.fileRotated();

					// Reopen the reader after rotation
					try {
						// Ensure that the old file is closed iff we re-open it
						// successfully
						RandomAccessFile save = reader;
						reader = new RandomAccessFile(file, RAF_MODE);
						position = 0;
						// close old file explicitly rather than relying on GC
						// picking up previous RAF
						IOUtils.closeQuietly(save);
					} catch (FileNotFoundException e) {
						// in this case we continue to use the previous reader
						// and position values
						listener.fileNotFound();
					}
					continue;
				} else {

					// File was not rotated

					// See if the file needs to be read again
					if (length > position) {

						// The file has more content than it did last time
						position = readLines(reader);
						last = System.currentTimeMillis();

					} else if (newer) {

						/*
						 * This can happen if the file is truncated or
						 * overwritten with the exact same length of
						 * information. In cases like this, the file position
						 * needs to be reset
						 */
						position = 0;
						reader.seek(position); // cannot be null here

						// Now we can read new lines
						position = readLines(reader);
						last = System.currentTimeMillis();
					}
				}
				if (reOpen) {
					IOUtils.closeQuietly(reader);
				}
				try {
					Thread.sleep(delayMillis);
				} catch (InterruptedException e) {
				}
				if (run && reOpen) {
					reader = new RandomAccessFile(file, RAF_MODE);
					reader.seek(position);
				}
			}

		} catch (Exception e) {

			listener.handle(e);

		} finally {
			IOUtils.closeQuietly(reader);
		}
	}

	public void stop() {
		this.run = false;
	}

	private long readLines(RandomAccessFile reader) throws IOException {
		StringBuilder sb = new StringBuilder();

		long pos = reader.getFilePointer();
		long rePos = pos; // position to re-read

		int num;
		boolean seenCR = false;
		while (run && ((num = reader.read(inbuf)) != -1)) {
			for (int i = 0; i < num; i++) {
				byte ch = inbuf[i];
				switch (ch) {
				case '\n':
					seenCR = false; // swallow CR before LF
					listener.handle(sb.toString());
					sb.setLength(0);
					rePos = pos + i + 1;
					break;
				case '\r':
					if (seenCR) {
						sb.append('\r');
					}
					seenCR = true;
					break;
				default:
					if (seenCR) {
						seenCR = false; // swallow final CR
						listener.handle(sb.toString());
						sb.setLength(0);
						rePos = pos + i + 1;
					}
					sb.append((char) ch); // add character, not its ascii value
				}
			}

			pos = reader.getFilePointer();
		}

		reader.seek(rePos); // Ensure we can re-read if necessary
		return rePos;
	}

}
