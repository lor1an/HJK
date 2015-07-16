package com.test;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SrceTailer implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(SrceTailer.class);
    private static SrceTailer tailer;
    private StringBuilder tail;
    private RandomAccessFile file;
    private long length;
    private Path folder;
    private TailHandler handler;
    private volatile boolean run = true;

    public SrceTailer(RandomAccessFile file, long length, Path folder, TailHandler handler) {
        this.file = file;
        this.length = length;
        this.folder = folder;
        this.handler = handler;
        this.tail = new StringBuilder();
    }

    public synchronized static SrceTailer createTailer(String path, String folderPath, TailHandler handler)
            throws IOException {
        if (tailer == null) {
            RandomAccessFile file = new RandomAccessFile(path, "r");
            Path folder = Paths.get(folderPath);
            tailer = new SrceTailer(file, file.length(), folder, handler);
            Thread thread = new Thread(tailer);
            thread.setDaemon(true);
            thread.start();
        }
        return tailer;
    }

    @Override
    public void run() {
        LOG.info(("Watching path: " + folder));
        FileSystem fs = folder.getFileSystem();
        try (WatchService service = fs.newWatchService()) {

            folder.register(service, StandardWatchEventKinds.ENTRY_MODIFY);

            WatchKey key = null;
            while (run) {
                key = service.take();

                Kind<?> kind = null;
                for (WatchEvent<?> watchEvent : key.pollEvents()) {
                    kind = watchEvent.kind();
                    if (StandardWatchEventKinds.ENTRY_MODIFY == kind) {
                        refreshTail();
                    }
                }
                if (!key.reset()) {
                    break;
                }
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("Exception during watching directory", e);
        }

    }

    public void refreshTail() throws IOException {
        file.seek(length);
        for (long i = 0; i < file.length() - length; i++) {
            tail.append((char) file.readByte());
        }
        sendToHandler(tail.toString());
        tail.setLength(0);
        length = file.length();
    }

    public void sendToHandler(String line) {
        handler.handle(line);
    }

    public void stop() {
        LOG.info("Stopping tailer...");
        this.run = false;
    }

}
