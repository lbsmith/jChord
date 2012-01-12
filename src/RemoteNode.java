import java.math.*;
import java.util.*;

import org.apache.xmlrpc.*;

public class RemoteNode
{
  private NodeIdentity nodeID;
  private XmlRpcClientLite xmlrpc;
  private LocalNode parent;

  public RemoteNode(LocalNode p, NodeIdentity id)
  {
    nodeID = id;
    parent = p;
    try {
      xmlrpc = new XmlRpcClientLite ("http://" + nodeID.getAddress() + ":" + nodeID.getPort() + "/" + id.toString());
    } catch (java.net.MalformedURLException e) {
      parent.Log("bad url string formed");
    } 
  }

  public RemoteNode(LocalNode p, String addr, String port)
  {
    parent = p;

    try {
      xmlrpc = new XmlRpcClientLite ("http://" + addr + ":" + port + "/discovery");
    } catch (java.net.MalformedURLException e) {
      parent.Log("bad url string formed");
    }
 
    try {
      String result = (String) xmlrpc.execute ("discovery.getID", new Vector());
      Vector tmp = new Vector();
      tmp.add(result);
      tmp.add(addr);
      tmp.add(new Integer(port));
      nodeID = new NodeIdentity(tmp);
      xmlrpc = new XmlRpcClientLite ("http://" + addr + ":" + port + "/" + nodeID.toString());
    } catch (Exception e) { parent.LogEx("getid exception"); e.printStackTrace(); }
  }

  public String getAddress()
  {
    return nodeID.getAddress();
  }

  public Integer getPort()
  {
    return new Integer(nodeID.getPort());
  }

  public NodeIdentity getIdentifier()
  {
    return nodeID;
  }

  public boolean equals(RemoteNode r)
  {
    if (nodeID.getAsBigInt().compareTo(r.getIdentifier().getAsBigInt()) == 0)
      return true;
    else return false;
  }

  public String toString()
  {
    return nodeID.toString();
  }

  public boolean isAlive()
  {
    try {
      Boolean result = (Boolean) xmlrpc.execute (nodeID.toString()+".isAlive", new Vector());
      return result.booleanValue();
    } catch (Exception e) { 
      parent.LogEx("["+nodeID+"] isalive exception: "+e); 
      parent.nodeFailed(nodeID);
      return false; 
    }
  }

  public RemoteNode getSuccessor()
  {
    try {
      Vector result = (Vector) xmlrpc.execute (nodeID.toString()+".getSuccessor", new Vector());
      NodeIdentity n = new NodeIdentity(result);
      return new RemoteNode(parent, n);
    } catch (Exception e) { 
      parent.LogEx("["+nodeID+"] getsucc exception: "+e); 
//e.printStackTrace();
      parent.nodeFailed(nodeID);
      return null; 
    }
  }

  public RemoteNode findSuccessor(NodeIdentity id)
  {
    try {
      Vector params = new Vector();
      params.addElement(id.toString()); 
      Vector result = (Vector) xmlrpc.execute (nodeID.toString()+".findSuccessor", params);
      NodeIdentity n = new NodeIdentity(result);
      return new RemoteNode(parent, n);
    } catch (Exception e) { 
      parent.LogEx("["+nodeID+"] findsucc exception: "+e); 
      parent.nodeFailed(nodeID);
      return null; 
    }
  }

  public void lookup(Lookup l)
  {
//    Simulator.Log("forwarding lookup to "+nodeID);
    try {
      Vector params = new Vector();
      params.addElement((l.getIdentifier()).toString());
      params.addElement((l.getOrigin()).toString());
      if (l.getStatus() == Lookup.COMPLETE)
        params.addElement(l.getData());
      else if (l.getStatus() == Lookup.MULTIPART)
        params.addElement(new Integer(l.getNumParts()));
      params.addElement(new Integer(l.getStatus()));
      params.addElement(l.getRoute());
      xmlrpc.executeAsync (nodeID.toString()+".lookup", params, null);
      return;
    } catch (Exception e) {
      parent.LogEx("["+nodeID+"] lookup exception: "+e);
      parent.nodeFailed(nodeID);
      return;
    }
  }

  public Vector precedingNodeList(NodeIdentity id)
  {
    try {
      Vector params = new Vector();
      params.addElement(id.toString());
      Vector result = (Vector) xmlrpc.execute (nodeID.toString()+".precedingNodeList", params);
      if (result.size() == 0) return null;
      else {
        Vector out = new Vector();
        Vector node;
        for (int i = 0; i < result.size(); i++) {
          node = (Vector)result.elementAt(i);
          out.add(new RemoteNode(parent, new NodeIdentity(node)));
        }
        return out;
      }
    } catch (Exception e) { 
      parent.LogEx("["+nodeID+"] precedinglist exception: "+e); 
      parent.nodeFailed(nodeID);
      return null; 
    }
  }

  public void notify(NodeIdentity id)
  {
    try {
      Vector params = new Vector();
      params.addElement(id.toString()); 
      params.addElement(id.getAddress());
      params.addElement(new Integer(id.getPort()));
      xmlrpc.executeAsync (nodeID.toString()+".notify", params, null);
//      return ((Boolean) xmlrpc.execute ("RPC2.notify", params)).booleanValue();
    } catch (Exception e) { 
      parent.LogEx("["+nodeID+"] notify exception: "+e);
      parent.nodeFailed(nodeID); 
      //return false; 
    }
  }

// whoever is sending the message is leaving, and id is the next best 
  public boolean notifyLeave(NodeIdentity id)
  {
    try {
      Vector params = new Vector();
      params.addElement((parent.getIdentifier()).toString());
      params.addElement(id.toString()); 
      params.addElement(id.getAddress());
      params.addElement(new Integer(id.getPort()));
      return ((Boolean) xmlrpc.execute (nodeID.toString()+".notifyLeave", params)).booleanValue();
    } catch (Exception e) { 
      parent.LogEx("["+nodeID+"] notifyleave exception: "+e); 
      parent.nodeFailed(nodeID);
      return false; 
    }
  }

  public RemoteNode getPredecessor()
  {
    try {
      Vector result = (Vector) xmlrpc.execute (nodeID.toString()+".getPredecessor", new Vector());
      if (result.size() == 0) return null;
      else {
        NodeIdentity n = new NodeIdentity(result);
        return new RemoteNode(parent, n);
      }
    } catch (Exception e) { 
      parent.LogEx("["+nodeID+"] getpred exception: "+e); 
      parent.nodeFailed(nodeID);
      return null; 
    }
  }

  public boolean addKey(Key k, boolean replicant)
  {
    try {
      Vector params = new Vector();
      params.addElement((k.getIdentifier()).toString());
      if (k.getNumParts() > 1)
        params.addElement(new Integer(k.getNumParts()));
      else
        params.addElement(k.getValueArray());
      params.addElement(new Boolean(replicant));
      return ((Boolean) xmlrpc.execute (nodeID.toString()+".addKey", params)).booleanValue();
    } catch (Exception e) { 
      parent.LogEx("["+nodeID+"] addkey exception: "+e); 
      parent.nodeFailed(nodeID);
      return false; 
    }
  }

  public boolean hasKey(Key k)
  {
    try {
      Vector params = new Vector();
      params.addElement((k.getIdentifier()).toString());
      return ((Boolean) xmlrpc.execute (nodeID.toString()+".hasKey", params)).booleanValue();
    } catch (Exception e) { 
      parent.LogEx("["+nodeID+"] haskey exception: "+e); 
      parent.nodeFailed(nodeID);
      return false; 
    }
  }

  public Vector getSuccList()
  {
    try {
      Vector result = (Vector) xmlrpc.execute (nodeID.toString()+".getSuccList", new Vector());
      if (result.size() == 0) return null;
      else {
        Vector out = new Vector();
        Vector node;
        NodeIdentity id;
        for (int i = 0; i < result.size(); i++) {
          node = (Vector)result.elementAt(i);
          out.add(new RemoteNode(parent, new NodeIdentity(node)));
        }
        return out;
      }
    } catch (Exception e) { 
      parent.LogEx("["+nodeID+"] getsucclist exception: "+e); 
      parent.nodeFailed(nodeID);
      return null; 
    }
  }

  public int getState()
  {
    try {
      Integer result = (Integer) xmlrpc.execute (nodeID.toString()+".getState", new Vector());
      return result.intValue();
    } catch (Exception e) { 
      parent.LogEx("["+nodeID+"] getstate exception: "+e);  
      parent.nodeFailed(nodeID);
      return LocalNode.STATE_NONE; 
    }
  }

  // RemoteNode is a wrapper for simulated RPCs which will eventually
  // be implemented using XML.  The plan is for every chord node that
  // the local node must deal with (succ, pred, fingers...) will be a
  // RemoteNode, which will have actual functions defined for it which
  // translate the function into a message which is sent across the 
  // network, to be recieved by a Node and processed through some mechanism.
  // perhaps through a message parser which pulls apart the message and 
  // performs the corresponding action.  
}
