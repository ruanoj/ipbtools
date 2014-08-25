package uk.co.gockett.ipbtools.topology;

/**
 * Utility class providing some IP address conversion functions
 * @author ruanoj@github
 */
public class IPUtils {
    /**
    * Convert an IP address to a hex string
    *
    * @param ipAddress Input IP address
    *
    * @return The IP address in hex form
    */
    public static String toHex(String ipAddress) {
        return Long.toHexString(IPUtils.ipToLong(ipAddress));
    }

    /**
    * Convert an IP address to a number
    *
    * @param ipAddress Input IP address
    *
    * @return The IP address as a number
    */
    public static long ipToLong(String ipAddress) {
        String[] atoms = ipAddress.split("\\.");
        long result = 0;

        for (int i = 3; i >= 0; i--) {
            result |= (Long.parseLong(atoms[3 - i]) << (i * 8));
        }
        return result & 0xFFFFFFFF;
    }

    public static String longToIp(long longValue) {
        int[] byteIPAddress = {0,0,0,0};
        for (int i=3; i>=0; i--) {
            byteIPAddress[i] = (int)(longValue % 256);
            longValue = longValue >> 8;
        }
        String returnValue = new String(
                byteIPAddress[0] + "." +
                byteIPAddress[1] + "." +
                byteIPAddress[2] + "." +
                byteIPAddress[3]);
        return returnValue;
    }

    public static String longToIp(Long longValue) {
        return longToIp(longValue.longValue());
    }

    public static String intArrayToIp(final int[]array, final int offset) {
        String returnValue =
            String.valueOf(array[offset]) + "." +
            String.valueOf(array[offset+1]) + "." +
            String.valueOf(array[offset+2]) + "." +
            String.valueOf(array[offset+3]);
        return returnValue;
    }

    public static String intArrayToIp(final int[]array) {
        return intArrayToIp(array, 0);
    }
}
