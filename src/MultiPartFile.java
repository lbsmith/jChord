import java.util.*;

public class MultiPartFile
{
  private String name;
  private int numparts;
  private int numadded;
  private int size;
  private boolean complete;
  private List dataparts;
  private Hashtable keyinfo;

  public MultiPartFile(String n, int num)
  {
    name = n;
    numparts = num;
    numadded = 0;
    complete = false;
    dataparts = Collections.synchronizedList(new ArrayList(num));
    for (int i = 0; i < num; i++) dataparts.add(null);
    keyinfo = new Hashtable();
    size = 0;
  }

  public void add(int part, byte[] d)
  {
    dataparts.set(part-1, d);
    size += d.length;
    numadded++;
//System.out.println("add part "+part+" of "+numparts+" ("+numadded+" complete)");
    if (numadded == numparts) complete = true;
  }

  public boolean isComplete()
  {
    return complete;
  }

  public String getFilename()
  {
    return name;
  }

  public int getSize()
  {
    return size;
  }

  public byte[] getData()
  {
    int pos = 0;
    byte[] out = new byte[size];

    for (int i = 0; i < numparts; i++) {
      byte[] d = (byte[])(dataparts.get(i));

      for (int j = 0; j < d.length; j++) {
        out[pos] = d[j];
        pos++;
      }
    }
    
    return out;    
  }

  public void addPartNum(String key, int n)
  {
//    System.out.println("addpart "+n+" --> "+key);
    keyinfo.put(key, new Integer(n));
  }

  public int getPartNum(String key)
  {
    Integer n = (Integer)keyinfo.get(key);
//    System.out.println("getpart "+key+" --> "+n.intValue());
    return n.intValue();
  }
}
