package com.hjk.tail;

import java.io.IOException;

public interface Tailer {
    
    void refreshTail() throws IOException;
}
