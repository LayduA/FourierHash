package src.hashops;

public class Compressor {
    public static String getSeq(String hash) {
        StringBuilder sb = new StringBuilder();
        int count = 1;
        for (int i = 0; i < hash.length(); i++) {
            if (i == hash.length() - 1) {
                sb.append(count);
            }
            else if (hash.charAt(i+1) == hash.charAt(i)) {
                count += 1;
            } else {
                sb.append(count);
                count = 1;
            }
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        String hash = "0001101";//hexToBin(getHash("oui"));
        while(hash.length() > 1) {
            hash = getSeq(hash);
            System.out.println(hash.length() + " " + hash);
        }

    }
}
