package net.ncguy.subnet.data;

import java.util.Arrays;

/**
 * Created by Guy on 23/03/2016.
 */
public class IPAddress implements Comparable<IPAddress> {

    int o1, o2, o3, o4;

    public IPAddress() {
        defaults();
    }

    public IPAddress(int o1, int o2, int o3, int o4) {
        this.o1 = o1;
        this.o2 = o2;
        this.o3 = o3;
        this.o4 = o4;
    }

    public IPAddress(String addr) {
        build(addr);
    }
    public IPAddress(String[] addr) {
        this(String.format("%s.%s.%s.%s", addr[0], addr[1], addr[2], addr[3]));
    }

    public IPAddress(int[] octets) {
        if(octets.length != 4) {
            defaults();
            return;
        }
        for(int i = 0; i < octets.length; i++) {
            setOctet((i+1), octets[i]);
        }
    }

    public IPAddress(int networkBits) {
        String addr = "";
        for(int i = 0; i < 32; i++) {
            if(i != 0) if(i % 8 == 0) addr += ".";
            if(i < networkBits) addr += "1";
            else addr += "0";
        }
        build(addr);
    }

    public void build(String addr) {
//        if(!addr.matches("/\\.{3}/")) {
//            defaults();
//            return;
//        }
        String[] bytes = addr.split("\\.");
        int index = 1;
        for(String b : bytes) {
            try{
                int i = Integer.valueOf(b);
                if(i < 0 || i > 255) throw new NumberFormatException();
                setOctet(index++, i);
            }catch (Exception e) {
                System.out.println("Invalid Address ["+b+"], reverting to defaults");
                defaults();
                return;
            }
        }
    }

    public void setOctet(int octet, int value) {
        switch(octet) {
            case 1: o1 = value; break;
            case 2: o2 = value; break;
            case 3: o3 = value; break;
            case 4: o4 = value; break;
        }
    }

    public int getOctet(int octet) {
        octet = Math.max(1, Math.min(octet, 4));
        switch(octet) {
            case 1: return o1;
            case 2: return o2;
            case 3: return o3;
            case 4: return o4;
        }
        return o1;
    }

    public static boolean isValid(String addr) {
        String[] bytes = addr.split(".");
        for(String b : bytes) {
            try{
                int i = Integer.parseInt(b);
                if(i < 0 || i > 255) throw new NumberFormatException();
            }catch (Exception e) {
                System.out.println("Invalid Address, reverting to defaults");
                return false;
            }
        }
        return true;
    }

    public void defaults() {
        o1 = o2 = o3 = o4 = 0;
    }

    static String getPaddedBinary(int num, int len) { return String.format("%"+len+"s", Integer.toBinaryString(num)).replace(' ', '0'); }

    public IPAddress mask(IPAddress mask) {
        String[] set1, set2;
        set1 = toString().split("\\.");
        set2 = mask.toString().split("\\.");
        String[] maskedSet = new String[set1.length];
        for(int i = 0; i < 4; i++) {
            set1[i] = getPaddedBinary(Integer.parseInt(set1[i]), 8);
            set2[i] = getPaddedBinary(Integer.parseInt(set2[i]), 8);
            maskedSet[i] = "";
        }
        System.out.println(Arrays.toString(maskedSet));
        for(int i = 0; i < 4; i++) {
            char[] set1Char = set1[i].toCharArray();
            char[] set2Char = set2[i].toCharArray();
            for(int j = 0; j < 8; j++) {
                maskedSet[i] += Integer.parseInt(set1Char[j]+"") & Integer.parseInt(set2Char[j]+"");
            }
        }
        System.out.println(Arrays.toString(maskedSet));
        for(int i = 0; i < maskedSet.length; i++) {
            maskedSet[i] = Integer.parseInt(maskedSet[i], 2)+"";
        }
        System.out.println(Arrays.toString(maskedSet));
        return new IPAddress(maskedSet);
    }

    @Override
    public String toString() {
        return String.format("%s.%s.%s.%s", o1, o2, o3, o4);
    }

    public IPAddress copy() {
        return new IPAddress(toString());
    }

    public String[] toBinaryOctets() {
        String[] s = toString().split("\\.");
        for (int i = 0; i < s.length; i++) {
            s[i] = getPaddedBinary(Integer.parseInt(s[i]), 8);
        }
        return s;
    }
    public String toBinaryOctetString() {
        String s = "";
        for(String str : toBinaryOctets())
            s += str+".";
        return s.substring(0, s.length()-1);
    }

    public IPAddress flip() {
        IPAddress addr = copy();
        addr.mFlip();
        return addr;
    }
    public void mFlip() {
        String[] s = toBinaryOctets();
        for(int i = 0; i < s.length; i++)
            s[i] = s[i].replace("0", "2").replace("1", "0").replace("2", "1");
        for (int i = 0; i < s.length; i++)
            s[i] = Integer.parseInt(s[i], 2)+"";
        String str = "";
        for(String a : s) str += a+".";
        build(str.substring(0, str.length()-1));
    }

    public IPAddress add(IPAddress ipAddress) {
        IPAddress addr = copy();
        addr.o1 += ipAddress.o1;
        addr.o2 += ipAddress.o2;
        addr.o3 += ipAddress.o3;
        addr.o4 += ipAddress.o4;

        while(addr.o4 > 255) {
            addr.o4 -= 256;
            addr.o3 += 1;
        }
        while(addr.o3 > 255) {
            addr.o3 -= 256;
            addr.o2 += 1;
        }
        while(addr.o2 > 255) {
            addr.o2 -= 256;
            addr.o1 += 1;
        }
        if(addr.o1 > 255) addr.o1 = 255;
        return addr;
    }
    public IPAddress sub(IPAddress ipAddress) {
        IPAddress addr = copy();
        addr.o1 -= ipAddress.o1;
        addr.o2 -= ipAddress.o2;
        addr.o3 -= ipAddress.o3;
        addr.o4 -= ipAddress.o4;

        while(addr.o4 < 0) {
            addr.o4 += 256;
            addr.o3 -= 1;
        }
        while(addr.o3 < 0) {
            addr.o3 += 256;
            addr.o2 -= 1;
        }
        while(addr.o2 < 0) {
            addr.o2 += 256;
            addr.o1 -= 1;
        }
        if(addr.o1 < 0) addr.o1 = 0;
        return addr;
    }

    public long count() {
        int diff4 = Math.abs(o4);
        int diff3 = Math.abs(o3);
        int diff2 = Math.abs(o2);
        int diff1 = Math.abs(o1);
        long diff = diff4+1;
        diff *= diff3+1;
        diff *= diff2+1;
        diff *= diff1+1;
        return Math.abs(diff);
    }

    public long diff(IPAddress addr) {
        int diff4 = Math.abs(o4-addr.o4);
        int diff3 = Math.abs(o3-addr.o3);
        int diff2 = Math.abs(o2-addr.o2);
        int diff1 = Math.abs(o1-addr.o1);
        long diff = diff4+1;

        System.out.printf("Differences: \n" +
                "\t4: %s\n" +
                "\t3: %s\n" +
                "\t2: %s\n" +
                "\t1: %s\n",
                diff4, diff3, diff2, diff1);

        diff *= diff3+1;
        diff *= diff2+1;
        diff *= diff1+1;
        return Math.abs(diff)-2;
    }

    @Override
    public int compareTo(IPAddress o) {
        if(this.equals(o)) return 0;
        if(count() == o.count()) return 0;
        if(count() > o.count()) return 1;
        if(count() < o.count()) return -1;
        return 0;
    }

    public static class Factory {
        public static final IPAddress LOOPBACK = new IPAddress("127.0.0.1");
        public static final IPAddress LASTGATEWAY = new IPAddress();
    }

}
