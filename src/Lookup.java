import java.util.*;

public class Lookup
{
  private NodeIdentity origin;
  private NodeIdentity keyid;
  private Vector route;
  private byte[] data;
  private int status;
  private int numparts;

  public static final int CONTINUE = 0;
  public static final int NOTFOUND = 1;
  public static final int COMPLETE = 2;
  public static final int MULTIPART = 3;

  public Lookup(String key, NodeIdentity orig)
  {
    keyid = new NodeIdentity(key, false); 
    origin = orig;
    route = new Vector();
    status = CONTINUE;
    numparts = 1;
  }

  public Lookup(String id, String orig, int s, Vector rte)
  {
    keyid = new NodeIdentity(id, true);
    origin = new NodeIdentity(orig, true);
    route = new Vector(rte);
    status = s;
    numparts = 1;
  } 

  public NodeIdentity getIdentifier()
  {
    return keyid;
  }

  public void addRoute(NodeIdentity id)
  {
    route.addElement("["+id.toString()+"]"); 
  }

  public Vector getRoute()
  {
    return route;
  } 

  public void setData(byte[] d)
  {
    data = d;
  }

  public byte[] getData()
  {
    return data;
  }

  public void setNumParts(int num)
  {
    numparts = num;
  }

  public int getNumParts()
  {
    return numparts;
  }
 
  public int getStatus()
  {
    return status;
  }

  public void setStatus(int s)
  {
    status = s;
  }

  public NodeIdentity getOrigin()
  {
    return origin;
  }
}
