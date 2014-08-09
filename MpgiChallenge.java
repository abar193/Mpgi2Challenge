import java.io.BufferedReader; // 1 
import java.io.IOException;    // 2
import java.io.FileReader;     // 3
import java.util.List;         // 4
import java.util.ArrayList;    // 5

/** 
   * My implementation for MpgiChallenge. 
   * Third version. In first, I was creating an array of prefixes and array of indexes 
   *    (like "all the ip's, starting with 42 can be found from there in array"). 
   *    In my second attempt I tried to speed up, by building n-Tree with max depth = 4, and 
   *    by storing prefixes there. <br> 
   * Finally, my best-so-far solution is a binary tree. Every prefix is being translated to 
   *    binary form, and placed inside the tree - either on leaves or nodes. Left side stands 
   *    for "0"-bit, and right - for "1"-bit. To find the best mask for ip we simply have to
   *    translate ip into binary form, and go through the tree. Each node with the value is 
   *    acceptable mask, the last one is the best one. <br>
   * Note: While reading prefixes, I assume there are no duplicating values. If there are some,
   *    they will crash this program.
   * @author Anton Bardishev (Bardisevs)
   */
public class MpgiChallenge {
	
  Node headNode;
  /**
   * Initializes binary tree, and fills it with prefixes from file.
   * @param filename Name of file, that should be opened.
   */
  public void readPrefixList(String filename){
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line = null;
			String[] splits;

			headNode = new Node();
			
			// Loop initialization
			long[] ips = new long[5];
      int i = 0;
			while((line = br.readLine()) != null) {
			  // Parse input line 
			  splits = line.split("[./]"); // alternative: split("\\.|\\/"); 
			  if(splits.length != 5) {
				  throw new Exception("Either wrong input, or my regexp failed. Alternative one may work");
				}

			  // Translate to binary form
				for(i = 0; i <= 4; i++) {
				  ips[i] = Integer.parseInt(splits[i]);
				}
				long addr = (ips[0] << 24) | (ips[1] << 16) | (ips[2] << 8) | (ips[3]);
				
				// Place mask inside the tree
				Node n = findOrCreateLeafForIp(addr, (int)ips[4]);
				if(n.value != null) {
				  System.out.println("Error with line " + line + " dublicate value " 
				          + Long.toString(n.value.ipaddress) + " " + Integer.toString(n.value.mask));
				  return;
				}
				n.value = new Record(addr, (int)ips[4]);
			}
			br.close();
		} catch(Exception e) {
			// Should not happen
		}
	}
	
	/**
	 * Goes through the tree, creating any missing nodes, until reaches depth(mask),  
	 *   with path = ip.
	 * @return Node created or found.
	 */
  Node findOrCreateLeafForIp(long ip, int mask) {
    Node leafNode = headNode;
    long checkMask = 0x80000000L; // mask for checking i-bit in ip
    long eraseMask = 0x7FFFFFFFL; // = checkNode - 1, mask for erasing i-bit from ip
    // Go deeper, creating missing nodes
    for(int i = 1; i <= mask; i++) {
      if((ip & checkMask) == 0) {
        if(leafNode.left == null) {
          leafNode.left = new Node();
        }
        leafNode = leafNode.left;
      } else {
        if(leafNode.right == null) {
          leafNode.right = new Node();
        }
        leafNode = leafNode.right;
      }
      ip = ip & eraseMask;
      eraseMask >>= 1;
			checkMask >>= 1;
    }
    return leafNode;
  }
  
  /**
   * Returns best mask for the given ip from the tree.
   * @return Record of the best matching prefix, or null.
   */
  Record bestPrefixForIp(long ip) {
    Node leafNode = headNode;
    long checkMask = 0x80000000L;
    long eraseMask = 0x7FFFFFFFL;  
    Record bestRecord = null;
    // Go as deep as we can, following ip
    for(int i = 1; i <= 32; i++) {
      if((ip & checkMask) == 0) {
        if(leafNode.left == null) {
          break;
        }
        leafNode = leafNode.left;
      } else {
        if(leafNode.right == null) {
          break;
        }
        leafNode = leafNode.right;
      }
      ip = ip & eraseMask;
      eraseMask >>= 1;
      checkMask >>= 1;
			if(leafNode.value != null) { 
			  // The deeper value is, the better.
			  bestRecord = leafNode.value;
			}
    }
    
    return bestRecord;
  }
  
  /** 
   * Translates string-written ip (dot-separated, not a prefix) to int.
   * Crashes with wrong input string.
   * @return Int value for ip, same way as it's stored in Records. 
   */
  long ipToInt(String ip) {
    String[] splits = ip.split("[.]"); 
    long[] ips = new long[4];
    for(int i = 0; i < 4; i++) {
      ips[i] = Long.parseLong(splits[i]);
    }
    return ips[0] << 24 | ips[1] << 16 | ips[2] << 8 | ips[3];
  }
  
  /** Translates ip from int value to human-readable string */
  String intToIp(long ip) {
    return Long.toString((ip & 0xFF000000) >> 24) + "." + Long.toString((ip & 0xFF0000) >> 16)
            + "." + Long.toString((ip & 0xFF00) >> 8) + "." + Long.toString(ip & 0xFF);
  }

  
	/**
	 * Get the longest matching prefix for a given IP address, according to a previously read prefix data.
	 * @param ipAddress The IP address (e.g., "88.13.1.91").
	 * @return null if no prefix file was read<br>
	 * empty String ("") if no prefix exists for given ipAddress<br>
	 * dotted decimal notation of the prefix (e.g., "88.13.1.0/24") otherwise.
	 */
	public String getLongestPrefixMatch(String ipAddress){
	  // Translate ip to binary form
	  long ip = ipToInt(ipAddress);
	  // Find best prefix
	  Record r = bestPrefixForIp(ip);
	  
	  if(r == null) {
	    return "";
	  } else {
	    return intToIp(r.ipaddress) + "/" + r.mask;
	  }
	}
	
	/**
	 * Match the IP addresses which have been measured in the specified file with the previously read prefix list.
	 * @param filename The file containing the measurement data.
	 * @return The number of IP addresses which are reachable according to the prefix list.
	 */
	public int countReachableIPsFromFile(String filename){
	  long reachables = 0;
    try {
      BufferedReader br = new BufferedReader(new FileReader(filename));
      String line;
      
      while((line = br.readLine()) != null) {
        // Here we have to check every single ip, status unimportant 
        // Perhaps I should use here .split(" ") to be sure, but split takes longer, and 
        //     with 88,052,800 records every action counts.
        if(getLongestPrefixMatch(line.substring(0, line.length() - 2)) != "") {
          reachables++;
        }
      }
      
      br.close();
    } catch (Exception e) {
      
    }
    return (int)reachables;
	}
	
	/**
	 * Search in a file of measured IP addresses for active IP addresses, for which no information is provided
	 * in the previously read prefix list.
	 * @param filename The file containing measurement data.
	 * @return The number of positive measurement results for which no prefix exists.
	 */
	public int countActiveUnreachableIPsFromFile(String filename){
	  long nores = 0;
	  try {
		  BufferedReader br = new BufferedReader(new FileReader(filename));
		  String line;
		  
		  while((line = br.readLine()) != null) {
		    // Check if the staus is '1', and find mask then
		    if(line.charAt(line.length() - 1) == '1') { 
  		    if(getLongestPrefixMatch(line.substring(0, line.length() - 2)) == "") {
  		      nores++;
  		    }
		    }
		  }
		  
		  br.close();
		} catch (Exception e) {
		  return 0;
		}
		return (int)nores;
	}
	/*
  // TODO: remove before release	
	public static void main(String[] args) {
		MpgiChallenge mc = new MpgiChallenge();
		mc.readPrefixList("mpgi.prefixes");

		System.out.println(mc.getLongestPrefixMatch("112.229.75.52").compareTo("112.224.0.0/11") == 0); 
		System.out.println(mc.getLongestPrefixMatch("153.104.90.11").compareTo("153.104.0.0/16") == 0); 
		System.out.println(mc.getLongestPrefixMatch("1.0.0.0").compareTo("1.0.0.0/24") == 0); // left-most test
		System.out.println(mc.getLongestPrefixMatch("1.0.0.1").compareTo("1.0.0.0/24") == 0); 
		System.out.println(mc.getLongestPrefixMatch("64.41.16.7").compareTo("64.41.16.0/21") == 0); // something from the middle
		System.out.println(mc.getLongestPrefixMatch("219.90.97.66").compareTo("219.90.97.0/24") == 0);
		System.out.println(mc.getLongestPrefixMatch("219.90.122.104").compareTo("219.90.122.104/32") == 0); // exact match
		System.out.println(mc.getLongestPrefixMatch("223.255.247.0").compareTo("223.255.247.0/24") == 0); // right-most
		System.out.println(mc.getLongestPrefixMatch("223.255.254.1").compareTo("223.255.254.0/24") == 0);
		System.out.println(mc.getLongestPrefixMatch("223.255.254.1").compareTo("223.255.254.0/24") == 0); 
		System.out.println(mc.getLongestPrefixMatch("29.171.113.167").compareTo("") == 0);
		System.out.println(mc.getLongestPrefixMatch("87.145.14.12").compareTo("") == 0);
		System.out.println(mc.getLongestPrefixMatch("1.52.0.5").compareTo("1.52.0.0/14") == 0);
		System.out.println(mc.getLongestPrefixMatch("3.0.15.92").compareTo("3.0.0.0/8") == 0); 
		System.out.println(mc.getLongestPrefixMatch("1.0.4.102")); 
		System.out.println(mc.countActiveUnreachableIPsFromFile("mpgi.sampled.100"));
		System.out.println(mc.countReachableIPsFromFile("mpgi.sampled.100"));
	}
	*/
	
	/**
	 * Single prefix with ip-address, and a mask.
	 * @author Anton Bardishev
	 *
	 */
	public class Record {
	  long ipaddress;
	  int mask;
	  public Record(long a, int m) {
	    ipaddress = a;
	    mask = m;
	  }
	}
	
	/**
	 * Binary tree node.
	 * @author Anton Bardishev
	 *
	 */
  public class Node {
    /** The left (0) and the right (1) childs. */
    Node left, right;
    /** Prefix, ending at this node*/
    Record value; 
  }
}
