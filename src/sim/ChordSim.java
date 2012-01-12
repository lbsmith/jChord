import java.io.*;
import java.util.*;
import java.math.*;

public class ChordSim implements ChordApp
{
  public static BigInteger zero; 
  public static BigInteger max;

  private Random rand = new Random();

  private boolean verbose = true;
  private boolean logexception = false;

  private int numnodes = 10;
  private int numkeys = 0;

  private String connhost = "localhost";
  private int connport = 8080;

  private int useport = 8080;

  private LocalNode n;
  private VirtualNode vnodes;

  public ChordSim(String[] args)
  {
    Print("jChord Simulator");
    Print("By Lee Smith <lbsmithvt@gmail.com>");

    zero = new BigInteger("0");
    max = new BigInteger("2");
    max = max.pow(IDENTIFIER_BITS);

    if (!parseArgs(args)) return;

    vnodes = new VirtualNode(this, useport);

    n = new LocalNode(this, randomString(6) + rand.nextInt(999999), useport);

    for (int j = 0; j < numkeys; j++) {
      String randkey = randomString(16) + ".dat";
      n.addKey(randkey, randomData(KEY_TEST_SIZE));
    }

    vnodes.add(n);

    RemoteNode r = new RemoteNode(n, connhost, String.valueOf(connport));
 
    n.join(r);
    Print("  >>>   joining "+connhost + ":" + connport);

    LocalNode n2;

    for (int i = 0; i < numnodes-1; i++)
    {
      n2 = new LocalNode(this, randomString(6) + rand.nextInt(999999), useport);
 
      for (int j = 0; j < numkeys; j++) {
        String randkey = randomString(16) + ".dat";
        n2.addKey(randkey, randomData(KEY_TEST_SIZE));
      }

      vnodes.add(n2);

      n2.join(n.getSelf());
//      n2.join(vnodes.randomNodeAvoid((n2.getIdentifier())).getSelf()); 
    }
  }

  public void run() throws IOException
  {
    Print("\nInitial nodes created, ready for input.");
    Print("  commands: quit, add, remove, fail, inskey, randkey, size, status, lookup");
    Print("  debug: sha1, inrange, threadcount, verbose, logex\n");

    BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
    String input;
    String[] tokens;

    do {
      input = stdin.readLine();  
      tokens = input.split(" ");
 
      if (tokens[0].equals("quit")) System.exit(0);
      if (tokens[0].equals("help"))
      {
        Print("  commands: quit, add, remove, fail, inskey, randkey, size, status, lookup");
        Print("  debug: sha1, inrange, threadcount, verbose, logex\n");
      }
      if (tokens[0].equals("add"))
      {
        int addnum = 1;
        if (tokens.length >= 2) addnum = Integer.parseInt(tokens[1]);

        for (int i = 1; i <= addnum; i++) {
          LocalNode addnode = new LocalNode(this, randomString(6) + rand.nextInt(999999), useport);
   	  vnodes.add(addnode);

          if ((tokens.length > 1) && (tokens.length-1) % 2 == 0) {
            addnode.join(new RemoteNode(addnode, tokens[tokens.length-2], tokens[tokens.length-1]));
          } else {
            if (vnodes.size() > 1)
              addnode.join(vnodes.randomNodeAvoid((addnode.getIdentifier())).getSelf());
          }
        }
      }
      if (tokens[0].equals("randkey"))
      {
        int size;
        if (tokens.length == 2)
          size = Integer.parseInt(tokens[1]);
        else size = 1;
        String randkey = randomString(16) + ".dat";
        LocalNode r = vnodes.randomNode();
        r.addKey(randkey, randomData(KEY_TEST_SIZE * size));
      }
      if (tokens[0].equals("remove"))
      {
        vnodes.remove();
      }
      if (tokens[0].equals("fail"))
      {
        vnodes.fail();
      }
      if (tokens[0].equals("status")) 
      {
	Print(vnodes.toString());
      }
      if (tokens[0].equals("threadcount"))
      {
        Print(String.valueOf(Thread.activeCount()));
      }
      if (tokens[0].equals("size"))
      {
        Print(String.valueOf(vnodes.size())); 
      }
      if (tokens[0].equals("sha1"))
      {
        if (tokens.length == 2) {
          NodeIdentity tmp = new NodeIdentity(tokens[1], false);
          Print("["+tmp+"]");
        } else Print(">> usage: 'sha1 <string>' to return sha1 hash value of that string");
      }
      if (tokens[0].equals("inskey"))
      {
        if (tokens.length == 2) {
          try {
            LocalNode i = vnodes.randomNode();
            i.addKey(getFilename(tokens[1]), getFileData(tokens[1]));
          } catch (FileNotFoundException e) { Print("error: file \""+tokens[1]+"\" not found"); }
        } else Print(">> usage: 'inskey <filename>' to add contents of that file to the network");
      }
      if (tokens[0].equals("lookup"))
      {
        if (tokens.length == 2) {
          LocalNode i = vnodes.randomNode();
          i.lookup(tokens[1]);
        } else Print(">> usage: 'lookup <keystring>' to search the network for a key with that name");
      }
      if (tokens[0].equals("inrange"))
      {
        if (tokens.length == 4) {
          Print(String.valueOf(NodeIdentity.inRange(
              new NodeIdentity(tokens[1], true),
              new NodeIdentity(tokens[2], true),
              new NodeIdentity(tokens[3], true))));
        } else Print(">> usage: 'inrange <hex1> <hex2> <hex3>' to determine if hex2 < hex1 < hex3 on the chord");
      } 
      if (tokens[0].equals("verbose"))
      {
        if (verbose) {
          System.out.println("verbose OFF");
          verbose = false;
        } else {
          System.out.println("verbose ON");
          verbose = true;
        }
      }
      if (tokens[0].equals("logex"))
      {
        if (logexception) {
          System.out.println("log exceptions OFF");
          logexception = false;
        } else {
          System.out.println("log exceptions ON");
          logexception = true;
        }
      }
    } while (true);
  }

  private boolean parseArgs(String[] args)
  {
    String arg = "";
    int i = 0;
 
    try {
      for (i = 0; i < args.length; i++) 
      {
        if (args[i].equals("-connect")) {
          arg = args[i+1];
          int p = arg.indexOf(':');
          if (p == -1) {
            connhost = arg;
            connport = 8080;
          } else {
            connhost = arg.substring(0, p);
            connport = Integer.parseInt(arg.substring(p+1));
          }
          i++;
        } else if (args[i].equals("-quiet")) {
          verbose = false;
        } else if (args[i].equals("-logex")) {
          logexception = true;
        } else if (args[i].equals("-keys")) {
          numkeys = Integer.parseInt(args[i+1]);
          if (numkeys < 0) {
            System.out.println("warning: can't have fewer than 0 keys, using 0 keys");
            numkeys = 0;
          }
          i++;
        } else if (args[i].equals("-nodes")) {
          numnodes = Integer.parseInt(args[i+1]);
          if (numnodes < 0) {
            System.out.println("warning: can't have fewer than 0 nodes, using 0 nodes");
            numnodes = 0;
          }
          i++;
        } else if (args[i].equals("-port")) {
          useport = Integer.parseInt(args[i+1]);
          if (useport < 0) {
            System.out.println("warning: invalid port, using port 8080");
            useport = 8080;
          }
          i++;
        } else if (args[i].equals("-help")) {
          System.exit(1);
        } else {
          throw new Exception();
        }
      }
    } catch (Exception e) {
      System.out.println("Invalid argument \""+args[i]+"\", run with -help for a list of valid options\n");
      return false;
    }

    return true; 
  }

  public String getFilename(String path)
  {
    int i = 0;

    for (i = path.length()-1; i >= 0; i--)
      if (path.charAt(i) == '/') { i++; break; }

    if (i < 0) i = 0;

    return path.substring(i);
  }

  public byte[] getFileData(String filename) throws IOException
  {
    FileInputStream in = new FileInputStream(filename);

    byte[] buf = new byte[in.available()];
    in.read(buf);

    in.close();

    return buf;
  }

  public void ProcessLookup(MultiPartFile f)
  {
    Print ("received: "+f.getFilename()+", length="+f.getSize());

    try {
      FileOutputStream out = new FileOutputStream("downloads/"+f.getFilename());

      out.write(f.getData());

      out.close();
    } catch (IOException e) { Print("failed to open file"+f.getFilename()); }
  }

  public void Print(String out)
  {
    System.out.println(out);
  }

  public void Log(String out)
  {
    if (verbose) System.out.println(out);
  }

  public void LogEx(String out)
  {
    if (logexception) System.out.println(out);
  }

  private int rand(int lo, int hi)
  {
    int n = hi - lo + 1;
    int i = rand.nextInt() % n;
    if (i < 0)
      i = -i;
    return lo + i;
  }

  private String randomString(int length)
  {
    return new String(randomData(length));
  }
 
  private byte[] randomData(int length)
  {
    byte b[] = new byte[length];
    for (int i = 0; i < length; i++)
      b[i] = (byte)rand('a', 'z');
    return b;
  }
}
