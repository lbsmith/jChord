import java.util.*;
import java.net.*;

import org.apache.xmlrpc.*;

public class LocalNode 
{
  private NodeIdentity nodeID;
  private RemoteNode pred, self;
  private Stabilizer stabilizeThread;
  private Joiner joinThread;

  public FingerTable fingerTable;
  public Hashtable lookupTable;
  public RPCListener rpc;
  public Vector keys;
  public Vector succList;
  public int state;
//  public WebServer rpcserv;
  public ChordApp cb; // callback interface

  public static final int STATE_NONE = 0;
  public static final int STATE_JOINING = 1;
  public static final int STATE_RUNNING = 2;
  public static final int STATE_LEAVING = 3;

  public LocalNode(ChordApp parent, String ident, int port)
  {
    cb = parent;

    try {
      nodeID = new NodeIdentity(ident, (InetAddress.getLocalHost()).getHostAddress(), port);
    } catch (UnknownHostException e) { e.printStackTrace(); }

    fingerTable = new FingerTable(this);

    lookupTable = new Hashtable();

    // self is a special loopback node
    self = new RemoteNode(this, nodeID); 

    for (int i = 1; i < cb.IDENTIFIER_BITS+1; i++)
      fingerTable.updateFinger(i, self);

    succList = new Vector();

    for (int i = 0; i < cb.NUM_REPLICATE; i++)
      succList.addElement(self);

    pred = null;

    keys = new Vector();

    rpc = new RPCListener(this);   
 
    stabilizeThread = new Stabilizer(this);
//    stabilizeThread.start(); 

    state = STATE_RUNNING;

    cb.Log("\n (!) node [" + nodeID + "] created");
  }

  public String toString()
  {
    String out = new String();

    out += "node ["+nodeID+"] --> ";

    if (getSuccessor() != null)
      out += "succ=["+getSuccessor().getIdentifier()+"]\n";
    else out += "succ=(null)\n";

    if (pred != null)
      out += "    pred=["+pred.getIdentifier()+"]\n";
    else out += "    pred=(null)\n";

    out += "    succlist=\n";
    for (int i = 0; i < cb.NUM_REPLICATE; i++)
      out += "       "+i+":[" + ((RemoteNode)succList.elementAt(i)) + "]\n";

    out += "    keys=\n";

    Enumeration e = keys.elements();
    while (e.hasMoreElements()) {
      Key k = (Key)e.nextElement();
      out += "        ["+k.getIdentifier()+"] ";
      if (k.isReplicant()) out += "(Replicated)";
      out += "\n";
    }

    return out;
  }

  public RPCListener getRPC()
  {
    return rpc;
  }

  public NodeIdentity getIdentifier()
  {
    return nodeID;
  }

  public void stabilize()
  {
    stabilizeThread.run();
  }

  // Callback functions

  public ChordApp getCallback()
  {
    return cb;
  }

  public void Print(String s)
  {
    cb.Print(s);
  }

  public void Log(String s)
  {
    cb.Log(s);
  }

  public void LogEx(String s)
  {
    cb.LogEx(s);
  }

  // Node information

  public RemoteNode getSuccessor()
  {
    return fingerTable.getFinger(1);
  }

  public RemoteNode getPredecessor()
  {
    return pred;
  }

  // hack for simulator...how philosophic though
  public RemoteNode getSelf()
  {
    return self;
  }

  public void updateSuccList(Vector remote)
  {
    Vector local = new Vector(remote);

    local.removeElementAt(remote.size()-1);
    local.insertElementAt(getSuccessor(), 0); 

    succList = local;
  }

  public void updateFinger(int i, RemoteNode r)
  {
    fingerTable.updateFinger(i, r);
  }
 
  public void updatePred(RemoteNode r)
  {
    pred = r;
  }

  public Key getKey(NodeIdentity keyid)
  {
    Enumeration e = keys.elements();

    while (e.hasMoreElements()) {
      Key k = (Key)e.nextElement();
//      if ((k.getKeyString()).equals(keyid)) {
      if ((k.getIdentifier()).equals(keyid)) {
        // as a side effect, refresh this key's decay value
        if (k.isReplicant()) k.setDecay(Key.KEY_DECAY); 
 
        return k;
      }
    }

    return null;
  }

  public boolean hasKey(NodeIdentity keyid) //String keyid)
  {
    Enumeration e = keys.elements();

    while (e.hasMoreElements()) {
      Key k = (Key)e.nextElement();
//      if ((k.getKeyString()).equals(keyid)) {
      if ((k.getIdentifier()).equals(keyid)) {
        // as a side effect, refresh this key's decay value
        if (k.isReplicant()) k.setDecay(Key.KEY_DECAY); 
 
        return true;
      }
    }

    return false;
  }

  public boolean addKey(Key newkey)
  {
    if (hasKey(newkey.getIdentifier())) return false;
    else {
      Log("node ["+nodeID+"] received key ["+newkey+"]");
      return keys.add(newkey);
    }
  }

  public void addKey(String key, byte[] value)
  {
    int keyctr = 1;
    int bytectr;
    byte[] temp = new byte[cb.KEY_CHUNK_SIZE];
    Key k;

    for (bytectr = 0; bytectr < value.length; bytectr++)
    {
      if ((bytectr > 0) && ((bytectr % cb.KEY_CHUNK_SIZE) == 0)) {
        k = new Key(key+keyctr, temp, false);
        addKey(k);
        Print(" (+) adding key \""+key+keyctr+"\"\n"+
           "        id=["+k.getIdentifier()+"], datasize="+temp.length);

        keyctr++;
        temp = new byte[cb.KEY_CHUNK_SIZE];
      }

      temp[bytectr % cb.KEY_CHUNK_SIZE] = value[bytectr];
    } 

    byte[] temp2;
    if ((bytectr > 0) && ((bytectr % cb.KEY_CHUNK_SIZE) == 0)) 
      temp2 = new byte[cb.KEY_CHUNK_SIZE];
    else 
      temp2 = new byte[bytectr % cb.KEY_CHUNK_SIZE];

    for (int i = 0; i < temp2.length; i++)
      temp2[i] = temp[i];
 
    // whatever's left
    if (keyctr > 1) {
      k = new Key(key+keyctr, temp2, false);
      addKey(k);
      Print(" (+) adding key \""+key+keyctr+"\"\n"+
           "        id=["+k.getIdentifier()+"], datasize="+temp2.length);
      // write the original key as a reference for the
      // number of chunks
      k = new Key(key, keyctr, false);
      addKey(k);
      Print(" (+) adding key \""+key+"\"\n" +
         "        id=["+k.getIdentifier()+"], numparts="+keyctr);
    } else {
      k = new Key(key, temp2, false);
      addKey(k);
      Print(" (+) adding key \""+key+"\"\n"+
         "        id=["+k.getIdentifier()+"], datasize="+temp2.length);
    }
  }

  public void removeKey(Key delkey)
  {
    delkey.remove();
    keys.remove(delkey);
  }

  public void completeLookup(Lookup l)
  {
    Print("\nrouting data for lookup of key hashval ["+l.getIdentifier()+"]");

    Enumeration e = (l.getRoute()).elements();
    int c = 0;
    while (e.hasMoreElements()) {
      Print("  "+c+":"+(String)e.nextElement());
      c++;
    }

    if (l.getStatus() == Lookup.NOTFOUND)
      Print("==> could not find ["+l.getIdentifier()+"]\n");
    else {
      Print("==> found key ["+l.getIdentifier()+"]");

      if (l.getStatus() == Lookup.MULTIPART)
        Print("    multipart key: "+l.getNumParts()+" parts\n");
      else if (l.getStatus() == Lookup.COMPLETE)
        Print("    data length: "+(l.getData()).length+"\n");
    }

    if (l.getStatus() == Lookup.MULTIPART) {
      int parts = l.getNumParts();
      String name = (String)lookupTable.get(l.getIdentifier().toString());
      lookupTable.remove(l.getIdentifier());
      MultiPartFile f = new MultiPartFile(name, parts);

      for (int i = 1; i <= parts; i++) {
        Lookup n = new Lookup(name+i, nodeID);
        f.addPartNum(n.getIdentifier().toString(), i);
        lookupTable.put(n.getIdentifier().toString(), f);
        self.lookup(n);
      }
    } else if (l.getStatus() == Lookup.COMPLETE) {
      Object o = lookupTable.get(l.getIdentifier().toString());
      lookupTable.remove(o);

      if (o instanceof MultiPartFile) {
        MultiPartFile f = (MultiPartFile)o;

        int part = f.getPartNum(l.getIdentifier().toString());
        f.add(part, l.getData());

        if (f.isComplete())
          cb.ProcessLookup(f);
      } else {
        MultiPartFile f = new MultiPartFile((String)o, 1);
        f.add(1, l.getData());
        cb.ProcessLookup(f);
      }
    }
  }

  public void lookup(String keystr)
  {
    Lookup l = new Lookup(keystr, nodeID);

    lookupTable.put(l.getIdentifier().toString(), keystr);
    lookup(l);
  }

  public void lookup(Lookup l)
  {
    l.addRoute(nodeID);

    if (l.getStatus() != Lookup.CONTINUE) {
      // we have an answer...are we at the origin?
      if(nodeID.equals(l.getOrigin())) {
        completeLookup(l);
        return;
      } else {
        RemoteNode n = closestNode(l.getOrigin());
        if (n.equals(self)) n = n.getSuccessor();
        n.lookup(l);
      }
    } else {
      if (NodeIdentity.inRange(l.getIdentifier(), getPredecessor().getIdentifier(), nodeID)) {
        // I should have this key
        Key k = getKey(l.getIdentifier());;
        if (k != null) {
          if (k.getMulti()) {
            l.setStatus(Lookup.MULTIPART);
            l.setNumParts(k.getNumParts());
          } else { 
            l.setStatus(Lookup.COMPLETE);
            l.setData(k.getValueArray());
          }
        } else {
          l.setStatus(Lookup.NOTFOUND);
        }

        //if (nodeID.equals(l.getOrigin())) { lookup(l); return; } //{ cb.ProcessLookup(l); return; } 

        RemoteNode n = closestNode(l.getOrigin());
        n.lookup(l);
      } else if (NodeIdentity.inRange(l.getIdentifier(), nodeID, getSuccessor().getIdentifier())) {
        RemoteNode n = getSuccessor();
        n.lookup(l);
      } else { 
        // this isn't in my keyspace, forward request closer
        RemoteNode n = closestNode(l.getIdentifier());
        n.lookup(l);
      }
    } 
  }

  // maintainance stuff
  public void join(RemoteNode conn)
  {
    Log(" (*) [" + nodeID + "] is joining off of ["+conn.getIdentifier()+"]");

    state = STATE_JOINING;

    joinThread = new Joiner(this, conn);
    joinThread.start(); 

    state = STATE_RUNNING;

    Print(" (*) [" + nodeID + "] joined");
  }

  public void leave()
  {
    RemoteNode s = getSuccessor();

    // if its null then its piggy backing and nobody would notice if it left anyway
    if (pred != null) {
      pred.notifyLeave(s.getIdentifier());
      s.notifyLeave(pred.getIdentifier());
      pred = self;
      fingerTable.updateFinger(1, self);

      // send all our keys to our successor
      Key k;
      for (int i = 0; i < keys.size(); i++) {
        k = (Key)keys.elementAt(i);
        s.addKey(k, k.isReplicant());
        Log("key ["+k+"] transferred to node ["+s+"]");
      }
    }
    stabilizeThread.end();

    Print(" (*) [" + nodeID + "] left");
  }

  // hack for simulation
  public void fail()
  {
    stabilizeThread.end();
    
    Print(" (*) [" + nodeID + "] failed");
  }

  public void nodeFailed(NodeIdentity id)
  {
    stabilizeThread.setActive();

    if((pred != null) && (id.equals(pred.getIdentifier())))
    {
      pred = null;
      stabilizeThread.checkPredecessor();
    }

    RemoteNode s = getSuccessor();

    if((s != null) && (id.equals(s.getIdentifier())))
    {
      fingerTable.updateFinger(1, null);
      stabilizeThread.checkSuccessor();
    }

    for (int i = 0; i < cb.IDENTIFIER_BITS; i++)
    {
      s = fingerTable.getFinger(i);

      if((s != null) && (s.getIdentifier().equals(id))) 
        fingerTable.updateFinger(i, null);
    }
  }

  // id thinks he may be our succ or pred...
  public synchronized void notify(NodeIdentity id)
  {
    if((pred == null) || (NodeIdentity.inRange(id, pred.getIdentifier(), self.getIdentifier())))
    {
      pred = new RemoteNode(this, id);
      Log("(noti) node ["+nodeID+"] has new pred ["+id+"]");
    }
    
    RemoteNode s = getSuccessor();

    if ((s == null) || (NodeIdentity.inRange(id, self.getIdentifier(), s.getIdentifier())))
    {
      RemoteNode n = new RemoteNode(this, id);
      fingerTable.updateFinger(1, n);
      Log("(noti) node ["+nodeID+"] has new succ ["+n+"]");
    }
  }

  public boolean notifyLeave(NodeIdentity leavingid, NodeIdentity newid)
  {
    RemoteNode newnode = new RemoteNode(this, newid);

    if (leavingid.equals(getSuccessor().getIdentifier())) 
    {
      fingerTable.updateFinger(1, newnode);
      Log("(part) node ["+nodeID+"] has new succ ["+newnode+"]");
    }
    if (leavingid.equals(pred.getIdentifier())) 
    {
      pred = newnode;
      Log("(part) node ["+nodeID+"] has new pred ["+newnode+"]");
    }

    return true;
  }

  public RemoteNode findSuccessor(NodeIdentity id)
  {
/*    if ((NodeIdentity.inRange(id, nodeID, getSuccessor().getIdentifier())) ||
        (id.equals(getSuccessor().getIdentifier())))
      return getSuccessor();
    else {
//      Vector nodes = precedingNodeList(id);
      RemoteNode n = closestPrecedingFinger(id);
      return n.findSuccessor(id);
    }
*/
// iterative method
    RemoteNode n = findPredecessor(id);
    return n.getSuccessor();
  }

  public RemoteNode findPredecessor(NodeIdentity id)
  {
    RemoteNode n = self;

    while (!NodeIdentity.inRange(id, n.getIdentifier(), n.getSuccessor().getIdentifier()))
    {
      Vector l = n.precedingNodeList(id);
      Enumeration e = l.elements();
      RemoteNode next;

      while (e.hasMoreElements()) {
        next = (RemoteNode)e.nextElement();

        if (NodeIdentity.inRange(next.getIdentifier(), n.getIdentifier(), id))
        {
          if (next.isAlive())
            n = next; 
        }
      } 
    }

    return n;
  }

  public RemoteNode closestNode(NodeIdentity id)
  {
    Vector l = precedingNodeList(id);

    RemoteNode n = self;
    Enumeration e = l.elements();
    RemoteNode next;

    while (e.hasMoreElements()) {
      next = (RemoteNode)e.nextElement();

      if (NodeIdentity.inRange(next.getIdentifier(), n.getIdentifier(), id))
      {
        if (next.isAlive())
          n = next; 
      }
    }
   
    return n;
  }


  public Vector precedingNodeList(NodeIdentity id)
  {
    Vector out = new Vector();
    RemoteNode node;

    Enumeration e = succList.elements();
    while (e.hasMoreElements()) {
      node = (RemoteNode)e.nextElement();
      if (NodeIdentity.inRange(node.getIdentifier(), nodeID, id))
        out.addElement(node);
    }

    for (int i = cb.IDENTIFIER_BITS; i > 0; i--)
    {
      node = fingerTable.getFinger(i);
      if (node == null) continue;
      if (NodeIdentity.inRange(node.getIdentifier(), nodeID, id))
        out.addElement(node);
    }

    return out;
  }
}

