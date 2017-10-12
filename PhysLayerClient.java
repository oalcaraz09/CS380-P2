
/*  Oscar Alcaraz
	CS 380 Networks
	Project 2
*/


import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;


public class PhysLayerClient {

    private static double BASE_VALUE = 0;

    public static void main(String[] args) {

        try {
        	
            Socket socket = new Socket("18.221.102.182",38002);
            
            if(socket.isConnected()) {
            	
                System.out.println("\nSuccesfully Connected to Server!\n");

                //Operations to decode message
                System.out.println(establishBaseValue(socket));
                
                //ArrayList for Signal
                ArrayList<Integer> signal = receiveBytes(socket);
                
                //ArrayList for Five Bit
                ArrayList<Integer> five_bit = nrziEncoding(signal);
                
                ////ArrayList for decoded bits
                ArrayList<Integer> decodedBits = referenceTable(five_bit);
                
                //ArrayList for final bits
                ArrayList<Integer> mergedBits = merge(decodedBits);
                
                sendMessageBack(socket, mergedBits);

            }

            String message = (checkResponse(socket)) ? "Response Good." : "Bad Response.";
            System.out.println(message);

            socket.close();
            System.out.println("\nDisconnected from Server!");

        } catch (Exception e) { e.printStackTrace(); }
    }

    // Gets the Preamble from the Server
    // Calculates the Average to determine if the signal is high/low
    public static String establishBaseValue(Socket socket) {

        try {
        	
            InputStream inStream = socket.getInputStream();

            double preamble = 0;
            
            for(int i = 0; i < 64; ++i) {
            	
                preamble += inStream.read();
            }

            BASE_VALUE = preamble / 64;
            DecimalFormat dc = new DecimalFormat("#.##");
            
           return "Basevalue Established from Preamble: " + dc.format(BASE_VALUE);

        } catch (Exception e) { e.printStackTrace(); }
        
        return "Unable to Establish Basevalue.";
    }
    
    // references the 5-bit table
    public static ArrayList<Integer> referenceTable(ArrayList<Integer> signal) {

        BitTable table = new BitTable();
        ArrayList<Integer> decodedMessage = new ArrayList<>();

        for(int i = 0; i < signal.size() - 4; i += 5) {
        	
            ArrayList<Integer> sub = new ArrayList<>();
            
            sub.add(signal.get(i));     sub.add(signal.get(i + 1));
            sub.add(signal.get(i + 2)); sub.add(signal.get(i + 3));
            sub.add(signal.get(i + 4));

            decodedMessage.add(table.get5BitValue(sub));

        }

        return decodedMessage;
    }

    // Receives the bytes from the server and converts
    // them into 1 or 0 for the signal. Then NRZI
    public static ArrayList<Integer> receiveBytes(Socket socket) {

        try {
        	
            InputStream inStream = socket.getInputStream();

            //ArrayList for the signal
            ArrayList<Integer> signal = new ArrayList<>();

            for(int i = 0; i < 320; ++i) {
            	
                int input = inStream.read();
                signal.add(determineSignal(input));
            }

            return signal;
            
        } catch (Exception e) { e.printStackTrace(); }
        
        return null;
    }


    // Check if a signal is high or low (1 or 0)
    public static int determineSignal(int check) {

        return  (check > BASE_VALUE) ? 1 : 0;

    }

    //Decode into NRZI
    public static ArrayList<Integer> nrziEncoding(ArrayList<Integer> code) {

    	//Array List for decoded bits in NRZI
        ArrayList<Integer> decoded = new ArrayList<>();
        int previousBit = 0;
        
        for(int i = 0; i < code.size(); ++i) {
        	
            if(code.get(i) == previousBit){
            	
                decoded.add(0);
                
            } 
            else{
            	
                decoded.add(1);
            }
            
            previousBit = code.get(i);

        }
        
        return decoded;
    }

    //Merges the bits from low and high into a single list
    //returns the new merged list
    public static ArrayList<Integer> merge(ArrayList<Integer> bits) {
    	
    	//ArrayList for the new Merged List
        ArrayList<Integer> merged = new ArrayList<>();
        
        for(int i = 0; i < bits.size(); i += 2) {
        	
            int upper = bits.get(i);
            int lower = bits.get(i + 1);
            upper = (16 * upper) + lower;
            
            merged.add(upper);
            
        }
        
        return merged;
    }

    
    //Send the message to the Server
    //Send the HEX version of the bytes
    public static void sendMessageBack(Socket socket, ArrayList<Integer> message) {
    	
        try {
        	
            int size = message.size();
            
            OutputStream outStream = socket.getOutputStream();
            byte[] sendByte = new byte[message.size()];

            System.out.print("Received Bytes: ");
            
            for(int i = 0; i < size; ++i) {
            	
                int current = message.get(i);
                System.out.print(Integer.toHexString(current).toUpperCase());
                sendByte[i] = (byte) current;
                
            }
            
            System.out.println();
            outStream.write(sendByte);

        } catch (Exception e) { e.printStackTrace(); }
        
    }

   
    //Checks if response id valid or not
    // 1 = true
    // 0 = false
    public static boolean checkResponse(Socket socket) {
    	
        try {
        	
            InputStream inStream = socket.getInputStream();
            int response = inStream.read();
            
            return ( response == 1 ) ? true : false;
            
        } catch (Exception e) { e.printStackTrace(); }
        
        return false;
    }


    // 4B/5B Table to be referenced 
    static final class BitTable {
    	
        HashMap<String, String> table;

        public BitTable() {
        	
            table = new HashMap<String, String>();
            makeTable();
            
        }
        
        //Table
        private void makeTable() {
        	
            table.put("11110", "0000"); table.put("10010", "1000");
            table.put("01001", "0001"); table.put("10011", "1001");
            table.put("10100", "0010"); table.put("10110", "1010");
            table.put("10101", "0011"); table.put("10111", "1011");
            table.put("01010", "0100"); table.put("11010", "1100");
            table.put("01011", "0101"); table.put("11011", "1101");
            table.put("01110", "0110"); table.put("11100", "1110");
            table.put("01111", "0111"); table.put("11101", "1111");
            
        }

        //Gets the Value of the 5 Bit Table
        public int get5BitValue(ArrayList<Integer> signal) {
        	
            String key = convertToString(signal);
            
            return Integer.parseInt(table.get(key), 2);
            
        }

        // Converts the Bit Signal to a String
        private String convertToString(ArrayList<Integer> signal) {
        	
            String signalString = "";
            
            for(Integer i : signal) {
            	
                signalString += i + "";
            }
            
            return signalString;
        }

    }

}