import java.util.*;

// this defines the methods that are called, upon receipt of a remote call request
// so it must translate parameters recieved into actual parameters expected by the LocalNode object
// may also include static methods to translate our custom objects into one which is easily transported
// ie. simplify a RemoteNode object into just its ID string

public class RPCListener
{
  private LocalNode parent;
  private int simlatency;

  public RPCListener(LocalNode p) //, int l)
  {
    parent = p;
    simlatency = 0; //l;
  }   

  public boolean isAlive()
  {
    return true;
  }

  public String getID()
  {
    return (parent.getIdentifier()).toString();
  }

  private void fakelag()
  {
    try {
      Thread.sleep(simlatency);
    } catch (InterruptedException e) { }
  }

  // returns 2 element Vector consisting of chord id, ip address of successor
  public Vector getSuccessor()
  {
    fakelag();

    RemoteNode s = parent.getSuccessor();
    Vector out = new Vector();

    out.addElement((s.getIdentifier()).toString());
    out.addElement(s.getAddress());
    out.addElement(s.getPort());

    return out;
  }

  public Vector getPredecessor()
  {
    fakelag();

    RemoteNode p = parent.getPredecessor();
    Vector out = new Vector();
 
    if (p != null) {
      out.addElement((p.getIdentifier()).toString());
      out.addElement(p.getAddress());
      out.addElement(p.getPort());
    }

    return out;
  }

  public Vector findSuccessor(String id)
  {
    fakelag();

    Vector out = new Vector();
    NodeIdentity n = new NodeIdentity(id, true);

    RemoteNode s = parent.findSuccessor(n);
    out.addElement((s.getIdentifier()).toString());
    out.addElement(s.getAddress());
    out.addElement(s.getPort()); 

    return out;
  }

  public void lookup(String keyid, String originid, int status, Vector route)
  {
    fakelag();

    Lookup l = new Lookup(keyid, originid, status, route);
 
    parent.lookup(l);
  }

  public void lookup(String keyid, String originid, int numparts, int status, Vector route)
  {
    fakelag();

    Lookup l = new Lookup(keyid, originid, status, route);

    l.setNumParts(numparts);

    parent.lookup(l);
  }

  public void lookup(String keyid, String originid, byte[] data, int status, Vector route)
  {
    fakelag();

    Lookup l = new Lookup(keyid, originid, status, route);

    l.setData(data);

    parent.lookup(l);
  }

  public Vector precedingNodeList(String id)
  {
    fakelag();

    Vector out = new Vector();
    Vector node;
    Enumeration e = parent.precedingNodeList(new NodeIdentity(id, true)).elements();
    NodeIdentity tmp;

    while (e.hasMoreElements()) {
      tmp = ((RemoteNode)e.nextElement()).getIdentifier();
      node = new Vector();
      node.add(tmp.toString());
      node.add(tmp.getAddress());
      node.add(new Integer(tmp.getPort()));
      out.add(node);
    } 

    return out;
  }

/*
  public Vector closestPrecedingFinger(String id)
  {
    Vector out = new Vector();
    NodeIdentity n = new NodeIdentity(id);

    RemoteNode s = parent.closestPrecedingFinger(n);
    out.addElement((s.getIdentifier()).toString());
    out.addElement(s.getAddress());
    out.addElement(s.getPort());

    return out;
  }
*/
 
  public boolean addKey(String keyid, int numkeys, boolean replicant)
  {
    fakelag();

    Key newkey = new Key(keyid, numkeys, true);
    newkey.setReplicant(replicant);

    return parent.addKey(newkey);
  }

  public boolean addKey(String keyid, byte[] data, boolean replicant)
  {
    fakelag();

    Key newkey = new Key(keyid, data, true);
    newkey.setReplicant(replicant);

    return parent.addKey(newkey);
  }

  public boolean hasKey(String keyid)
  {
    fakelag();

    return parent.hasKey(new NodeIdentity(keyid, true));
  }

  public void notify(String id, String addr, int port)
  {
    fakelag();

    Vector v = new Vector();
    v.add(id);
    v.add(addr);
    v.add(new Integer(port));
    NodeIdentity n = new NodeIdentity(v);
    
    parent.notify(n);
  }

  public boolean notifyLeave(String leaveid, String newid, String newaddr, int newport)
  {
    fakelag();

    Vector v = new Vector();
    v.add(newid);
    v.add(newaddr);
    v.add(new Integer(newport));
    NodeIdentity newnode = new NodeIdentity(v);
    NodeIdentity leavenode = new NodeIdentity(leaveid, true);

    return parent.notifyLeave(leavenode, newnode);
  }

  public Vector getSuccList()
  {
    fakelag();

    Vector out = new Vector();
    Vector node;
    Enumeration e = parent.succList.elements();
    NodeIdentity tmp;

    while (e.hasMoreElements()) {
      tmp = ((RemoteNode)e.nextElement()).getIdentifier();
      node = new Vector();
      node.add(tmp.toString());
      node.add(tmp.getAddress());
      node.add(new Integer(tmp.getPort()));
      out.add(node);
    } 

    return out;
  }

  public int getState()
  {
    fakelag();

    return parent.state;
  }
}
