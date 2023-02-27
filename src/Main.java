import java.io.*;

public class Main {
    public static void main(String[] args) throws IOException {
        // System.out.println("hello!");
        // String path = "../tests/" + args[0];
        // System.out.println(path);
        String path = args[0];
        FileInputStream fin = new FileInputStream(path);
        InputStreamReader isr = new InputStreamReader(fin);
        BufferedReader bf = new BufferedReader(isr);
        String temp = "";
        while ((temp = bf.readLine()) != null) {
            System.out.println(temp);
        }
        bf.close();
    }
}