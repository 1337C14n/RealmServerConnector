package realmConnection;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import packets.Packet;

public class RealmConnection extends Observable implements Observer {

  private String address;
  private int port;

  private boolean connected = true;

  private Socket socket;
  private ObjectOutputStream out;
  private ObjectInputStream in;

  private RealmWriter writer;
  private Thread RealmWriterThread;

  private RealmReader reader;
  private Thread RealmReaderThread;

  private BeatingHeart beatingHeart;
  private Thread beatingHeartThread;

  protected ConcurrentLinkedQueue<Packet> incomingPacketQueue;
  protected ConcurrentLinkedQueue<Packet> outgoingPacketQueue;

  private ConcurrentHashMap<Integer, Object> synchronizers;

  public RealmConnection(String address, int port) throws UnknownHostException, IOException {
    this.address = address;
    this.port = port;

    this.socket = createSocket();

    this.in = createInputStream();
    this.out = createOutputStream();

    // Have to flush output stream
    try {
      this.out.flush();
    } catch (IOException e) {

    }

    this.incomingPacketQueue = new ConcurrentLinkedQueue<>();
    this.outgoingPacketQueue = new ConcurrentLinkedQueue<>();

    synchronizers = new ConcurrentHashMap<>();

    // Create and register synchronizers
    Object heartBeatSynchro = new Object();
    Object readerSynchro = new Object();
    Object writerSynchro = new Object();

    synchronizers.put(Synchronizer.HEARTBEAT.getSynchronizedObject(), heartBeatSynchro);
    synchronizers.put(Synchronizer.READER.getSynchronizedObject(), readerSynchro);
    synchronizers.put(Synchronizer.WRITER.getSynchronizedObject(), writerSynchro);

    // Create connection Threads
    writer = new RealmWriter(this, out);
    RealmWriterThread = new Thread(writer);
    RealmWriterThread.start();

    reader = new RealmReader(this, in);
    RealmReaderThread = new Thread(reader);
    RealmReaderThread.start();
    // Enable HeartBeat
    beatingHeart = new BeatingHeart(this, out);
    beatingHeartThread = new Thread(beatingHeart);
    beatingHeartThread.start();

  }

  @Override
  public void update(Observable arg, Object event) {
    if (event instanceof DisconnectException) {
      System.out.println("Threw Disconnect Event: " + event.getClass().getSimpleName());
      if (connected) {
        this.dispose();
      }
      connected = false;
      this.setChanged();
      this.notifyObservers(new DisconnectException());

      // Once we notify of a disconnect we can get rid of the observers we will
      // be shutting down
      this.deleteObservers();
    }
  }

  public void write(Packet packet) {
    outgoingPacketQueue.add(packet);
    synchronized (getSynchro(Synchronizer.WRITER)) {
      getSynchro(Synchronizer.WRITER).notify();
    }
  }

  public Packet read() {
    if (connected == true) {
      synchronized (getSynchro(Synchronizer.READER)) {
        if (incomingPacketQueue.poll() == null) {
          try {
            getSynchro(Synchronizer.READER).wait();
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
        Packet packet = incomingPacketQueue.poll();
        System.out.println("Returning captured Packet");
        return packet;
      }
    } else {
      synchronized (getSynchro(Synchronizer.READER)) {
        try {
          getSynchro(Synchronizer.READER).wait(5);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      return null;
    }
  }

  protected Packet readOutgoing() {
    return outgoingPacketQueue.poll();
  }

  protected boolean hasPackets() {
    return !incomingPacketQueue.isEmpty();
  }

  protected boolean hasOutgoingPackets() {
    return !outgoingPacketQueue.isEmpty();
  }

  private ObjectInputStream createInputStream() {
    try {
      return new ObjectInputStream(new BufferedInputStream((socket.getInputStream())));
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  private ObjectOutputStream createOutputStream() {
    try {
      return new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  private Socket createSocket() throws UnknownHostException, IOException {
    return new Socket(address, port);
  }

  public void dispose() {

    if (writer != null) {
      writer.disconnect();
    }
    if (reader != null) {
      reader.disconnect();
    }
    if (beatingHeart != null) {
      beatingHeart.disconnect();
    }

    try {
      if (socket != null) {
        socket.close();
      }
      if (in != null) {
        in.close();
      }
      if (out != null) {
        out.close();
      }
    } catch (IOException e) {

    }
    synchronized (getSynchro(Synchronizer.HEARTBEAT)) {
      getSynchro(Synchronizer.HEARTBEAT).notify();
    }
    synchronized (getSynchro(Synchronizer.READER)) {
      getSynchro(Synchronizer.READER).notify();
    }
    synchronized (getSynchro(Synchronizer.WRITER)) {
      getSynchro(Synchronizer.WRITER).notify();
    }

    writer = null;
    reader = null;
    beatingHeart = null;
  }

  public boolean isConnected() {
    return connected;
  }

  protected Object getSynchro(Synchronizer sync) {
    if (synchronizers.containsKey(sync.getSynchronizedObject())) {
      return synchronizers.get(sync.getSynchronizedObject());
    }
    return null;
  }
}
