package com.hjk.tail.impl;

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

import org.atmosphere.cpr.AtmosphereResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hjk.tail.TailHandler;
import com.hjk.tail.Tailer;

public class SrceTailer implements Tailer, Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(SrceTailer.class);
    private StringBuilder tail;
    private RandomAccessFile file;
    private long length;
    private long startingPoint;
    private Path folder;
    private TailHandler handler;
    private String uid;
    private AtmosphereResource resource;
    private volatile boolean run = true;
    private String filePath;

    public SrceTailer() {
    }

    public SrceTailer(final RandomAccessFile file, final long length, final Path folder, final TailHandler handler,
            String uid, AtmosphereResource resource, String path) {
        this.file = file;
        this.length = length;
        this.startingPoint = length -1;
        this.folder = folder;
        this.handler = handler;
        this.tail = new StringBuilder();
        this.uid = uid;
        this.resource = resource;
        this.filePath = path;
    }

    public static SrceTailer createTailer(final String path, final String folderPath, final TailHandler handler,
            String uid, AtmosphereResource resource) throws IOException {
        RandomAccessFile file = new RandomAccessFile(path, "r");
        Path folder = Paths.get(folderPath);
        return new SrceTailer(file, file.length(), folder, handler, uid, resource, path);
    }

    public void startTailerThread() {
        Thread thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void run() {
        LOG.info(("Watching path: " + folder));
        FileSystem fs = folder.getFileSystem();
        try (WatchService service = fs.newWatchService()) {

            folder.register(service, StandardWatchEventKinds.ENTRY_MODIFY);

            WatchKey key = null;
            while (isRun()) {
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

    @Override
    public void refreshTail() throws IOException {
        file.seek(length);
        for (long i = 0; i < file.length() - length; i++) {
            tail.append((char) file.readByte());
        }
        sendToHandler(tail.toString());
        tail.setLength(0);
        length = file.length();
    }

    public void sendToHandler(String line) throws IOException {
        handler.handle(line, uid, resource);
    }
    
    public String getPreviousLines(long linesCount) throws IOException{
        long passed = 0;
        StringBuilder preTail = new StringBuilder();
        while(startingPoint > -1){
            file.seek(startingPoint);
            char symbol = (char) file.readByte();
            if(symbol == '\n'){
                if(passed + 1 == linesCount){
                    break;
                }
                passed++;
            }
            preTail.append(symbol);
            startingPoint--;
        }
        return preTail.reverse().toString();
    }
    
    public boolean isRun() {
        return this.run;
    }

    public void startTail() {
        LOG.info("Starting tailer...");
        this.run = true;
    }

    public void stopTail() {
        LOG.info("Stopping tailer...");
        this.run = false;
    }

    public StringBuilder getTail() {
        return tail;
    }

    public void setTail(StringBuilder tail) {
        this.tail = tail;
    }

    public RandomAccessFile getFile() {
        return file;
    }

    public void setFile(RandomAccessFile file) {
        this.file = file;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public Path getFolder() {
        return folder;
    }

    public void setFolder(Path folder) {
        this.folder = folder;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    

}
