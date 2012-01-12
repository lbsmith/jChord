import java.math.*;
import java.util.*;

public class NodeIdentity
{
  private BigInteger nodeID;
  private String address;
  private int port;
  private boolean search;

  public NodeIdentity(String ident, String addr, int p)
  {
    address = addr;
    port = p;
    search = false;

    SHA1 s = new SHA1(); 
    s.init();
    s.updateASCII(ident);
    s.finish();
    nodeID = new BigInteger(1, s.digestBits);
  }

  public NodeIdentity(BigInteger ident, String addr, int p)
  {
    address = addr;
    nodeID = ident;
    port = p;
    search = false;
  }

  // from RemoteNode, format is:
  //   (String)nodeid, (String)ip, (Integer)port
  public NodeIdentity(Vector id)
  {
    nodeID = new BigInteger((String)id.elementAt(0), 16);
    address = (String)id.elementAt(1);
    port = ((Integer)id.elementAt(2)).intValue();
    search = false;
  }

  // just represents an id, used for searching
  public NodeIdentity(String ident, boolean encoded)
  {
    if (!encoded) {
      SHA1 s = new SHA1(); 
      s.init();
      s.updateASCII(ident);
      s.finish();
      nodeID = new BigInteger(1, s.digestBits);
    } else nodeID = new BigInteger(ident, 16);
    search = true;
  }

  public String toString()
  {
    return nodeID.toString(16);
  }

  public String getAddress()
  {
    return address;
  }

  public int getPort()
  {
    return port;
  }

  public BigInteger getAsBigInt()
  {
    return nodeID;
  }

  public boolean equals(NodeIdentity test)
  {
    if (nodeID.compareTo(test.getAsBigInt()) == 0) return true;
    else return false;
  }

  // always go clockwise around the chord
  public static boolean inRange(BigInteger test, BigInteger low, BigInteger high)
  {
    if (test == null) return false;

    // low = high, entire chord
    if (low.compareTo(high) == 0) return true;

    if (low.compareTo(high) > 0)
    {
      if ((test.compareTo(low) > 0) || (test.compareTo(high) < 0))
        return true;
      else return false;
    } else {
      if ((test.compareTo(low) > 0) && (test.compareTo(high) < 0))
        return true;
      else return false;
    }
  }

  public static boolean inRange(NodeIdentity test, NodeIdentity low, NodeIdentity high)
  {
    if (test == null) return false;
    return inRange(test.getAsBigInt(), low.getAsBigInt(), high.getAsBigInt());
  }

  public static boolean inRange(RemoteNode test, RemoteNode low, RemoteNode high)
  {
    if (test == null) return false;
    return inRange(test.getIdentifier(), low.getIdentifier(), high.getIdentifier());
  }
}

