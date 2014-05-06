/*
    Non blocking (NIO) tcp/http server implementation.
*/
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.net.StandardSocketOptions;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;


public class NIOServer {

private static final int BUFFER_SIZE = 4096;

private int port;
private InetAddress hostAddress = null;
private Selector selector;
private ByteBuffer readBuffer = ByteBuffer.allocate(BUFFER_SIZE);



public NIOServer(int port) throws IOException {
    this.port = port;
    selector = SelectorProvider.provider().openSelector();

//
// http://docs.oracle.com/javase/7/docs/api/java/nio/channels/SocketChannel.html
//
    ServerSocketChannel serverChannel = ServerSocketChannel.open();
    serverChannel.configureBlocking(false);    
    serverChannel.socket().bind(new InetSocketAddress(hostAddress, port));
    serverChannel.register(selector, SelectionKey.OP_ACCEPT);

    nioLoop();
}

private void nioLoop() {
    while (true) {
        try{
            selector.select();
            Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
            while (selectedKeys.hasNext()) {
                SelectionKey key = selectedKeys.next();
                selectedKeys.remove();
                if (!key.isValid()) {
                    continue;
                }
                if (key.isAcceptable()) {
                    accept(key);
                } else if (key.isReadable()) {
                    read(key);
                } else if (key.isWritable()) {
                    write(key);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}

private void accept(SelectionKey key) throws IOException {
    ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
    SocketChannel socketChannel = serverSocketChannel.accept();
    socketChannel.configureBlocking(false);
    socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
    socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
    socketChannel.register(selector, SelectionKey.OP_READ);
}


//
// http://docs.oracle.com/javase/7/docs/api/java/nio/channels/SelectionKey.html
//
// Object  attach(Object ob)
// Attaches the given object to this key.
// Object  attachment()
// Retrieves the current attachment.
// 

private void read(SelectionKey key) throws IOException {
    SocketChannel socketChannel = (SocketChannel) key.channel();
    readBuffer.clear();
    int nRead;

    try {
        nRead = socketChannel.read(readBuffer);
        if (nRead == -1) {
            socketChannel.close();
            return;
        }
    } catch (IOException e) {
        socketChannel.close();
        return;
    }

    //System.out.println("nRead="+nRead);
    //System.out.println("readBuffer="+new String(readBuffer.array(), "UTF-8"));

    socketChannel.register(selector, SelectionKey.OP_WRITE);
}

//
// http://tutorials.jenkov.com/java-nio/scatter-gather.html
// https://forums.bukkit.org/threads/ultra-fast-java-nio-webserver-less-than-350-lines.101080/
//

private void write(SelectionKey key) throws IOException {
    SocketChannel socketChannel = (SocketChannel) key.channel();
    ByteBuffer responseBuffer = ByteBuffer.wrap("HTTP/1.1 200 OK\r\nConnection: close\r\nContent-Length: 7\r\n\r\nHello\r\n".getBytes("UTF-8"));

    socketChannel.write(responseBuffer);
//    if (responseBuffer.remaining() > 0) {
//            socketChannel.close();
//            return;
//    }

//    key.interestOps(SelectionKey.OP_READ);

    key.channel().close();
    key.cancel();
}


public final class NIOSession {

    private final SocketChannel channel;    

    public NIOSession(SocketChannel channel) {
        this.channel = channel;
    }

}


public static void main(String[] args) throws IOException {
    int port = 9090;
    System.out.println("Starting server "+port);
    new NIOServer(port);
}

}