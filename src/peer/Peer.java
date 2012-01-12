import java.io.*;

public class Peer 
{
  public static void main(String[] args) throws IOException
  {
    ChordPeer c = new ChordPeer(args);
    c.run();
  }
}
