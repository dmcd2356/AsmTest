/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package asmtest;

import java.awt.Graphics;
import java.util.HashMap;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;

/**
 *
 * @author dan
 */
public class DebugMessage {
    
    // used in formatting printArray
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    
    private static final String newLine = System.getProperty("line.separator");

    private final JTextPane    debugTextPane;
    private long  startTime;
    private boolean showTime;
    private boolean showHours;
    private boolean showType;
    private final HashMap<String, FontInfo> messageTypeTbl;

    DebugMessage (JTextPane textPane) {
        debugTextPane = textPane;
        startTime = System.currentTimeMillis(); // get the start time
        showTime = false;
        showHours = false;
        showType = false;
        messageTypeTbl = new HashMap<>();
        setTypeColor ("Hexdata", Util.TextColor.Black, Util.FontType.Bold);
    }
    
    /**
     * returns the elapsed time in seconds.
     * The format of the String is: "HH:MM:SS"
     * 
     * @return a String of the formatted time
     */
    private String getElapsedTime () {
        // get the elapsed time in secs
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - startTime;
        if (elapsedTime < 0)
            elapsedTime = 0;
        
        // split value into hours, min and secs
        Long msecs = elapsedTime % 1000;
        Long secs = (elapsedTime / 1000);
        Long hours = 0L;
        secs += msecs >= 500 ? 1 : 0;
        if (showHours) {
            hours = secs / 3600;
        }
        secs %= 3600;
        Long mins = secs / 60;
        secs %= 60;

        // now stringify it
        String elapsed = "";
        if (showHours) {
            if (hours < 10)   elapsed = "0";
            elapsed += hours.toString();
            elapsed += ":";
        }
        elapsed += (mins < 10) ? "0" + mins.toString() : mins.toString();
        elapsed += ":";
        elapsed += (secs < 10) ? "0" + secs.toString() : secs.toString();
        return elapsed;
    }

    /**
     * sets the specified input string to a fixed length all uppercase version
     * 
     * @param value - the string value
     * @return the properly formatted version
     */
    private String formatStringLength (String value) {
        value += "      ";
        return value.substring(0, 6).toUpperCase();
    }
    
    /**
     * A generic function for appending formatted text to a JTextPane.
     * 
     * @param tp    - the TextPane to append to
     * @param msg   - message contents to write
     * @param color - color of text
     * @param font  - the font selection
     * @param size  - the font point size
     * @param ftype - type of font style
     */
    private void appendToPane(String msg, Util.TextColor color, String font, int size, Util.FontType ftype)
    {
        if (debugTextPane == null) {
            return;
        }
        
        AttributeSet aset = Util.setTextAttr(color, font, size, ftype);
        int len = debugTextPane.getDocument().getLength();
        debugTextPane.setCaretPosition(len);
        debugTextPane.setCharacterAttributes(aset, false);
        debugTextPane.replaceSelection(msg);
    }

    /**
     * A generic function for appending formatted text to a JTextPane.
     * 
     * @param tp    - the TextPane to append to
     * @param msg   - message contents to write
     * @param color - color of text
     * @param ftype - type of font style
     */
    private void appendToPane(String msg, Util.TextColor color, Util.FontType ftype)
    {
        appendToPane(msg, color, "Courier", 11, ftype);
    }

    /**
     * enables/disables the display of time when using the print command
     * @param enable - true to enable display of time preceeding message
     */
    public void enableTime(boolean enable) {
        showTime = enable;
    }
    
    /**
     * enables/disables the display of hours in time display when using the print command
     * @param enable - true to enable display of hours preceeding message
     */
    public void enableHours(boolean enable) {
        showHours = enable;
    }
    
    /**
     * enables/disables the display of message type when using the print command
     * @param enable - true to enable display of message type preceeding message
     */
    public void enableType(boolean enable) {
        showType = enable;
    }
    
    /**
     * resets the start time
     */
    public void resetTime() {
        startTime = System.currentTimeMillis(); // get the start time
    }
    
    /**
     * clears the display.
     */
    public void clear() {
        debugTextPane.setText("");
    }

    /**
     * updates the display immediately
     */
    public void updateDisplay () {
        Graphics graphics = debugTextPane.getGraphics();
        if (graphics != null)
            debugTextPane.update(graphics);
    }
    
    /**
     * sets the association between a type of message and the characteristics
     * in which to print the message.
     * 
     * @param type  - the type to associate with the font characteristics
     * @param color - the color to assign to the type
     * @param font  - the font attributes to associate with the type
     */
    public void setTypeColor (String type, Util.TextColor color, Util.FontType font) {
        // limit the type to a 5-char length (pad with spaces if necessary)
        type = formatStringLength(type);
        
        FontInfo fontinfo = new FontInfo(color, font);
        if (messageTypeTbl.containsKey(type))
            messageTypeTbl.replace(type, fontinfo);
        else
            messageTypeTbl.put(type, fontinfo);
    }
    
    /**
     * outputs the timestamp info to the debug window.
     */
    public void printTimestamp() {
        String tstamp = "[" + getElapsedTime() + "] ";
        appendToPane(tstamp, Util.TextColor.Brown, Util.FontType.Bold);
    }
    
    /**
     * outputs the message type to the debug window.
     * 
     * @param type - the message type to display
     */
    public void printType(String type) {
        type = formatStringLength(type);
        printRaw(type, type + ": ");
    }
    
    /**
     * outputs the header info (timestamp and message type) to the debug window
     * if they are enabled.
     * 
     * @param type - the message type to display
     */
    public void printHeader(String type) {
        if (showTime)
            printTimestamp();
        if (showType) {
            printType(type);
        }
    }
    
    /**
     * outputs a termination char to the debug window
     */
    public void printTerm() {
        appendToPane(newLine, Util.TextColor.Black, "Courier", 11, Util.FontType.Normal);
    }
    
    /**
     * displays a message in the debug window (no termination).
     * 
     * @param type  - the type of message to display
     * @param message - message contents to display
     */
    public void printRaw(String type, String message) {
        if (message != null && !message.isEmpty()) {
            // limit the type to a 5-char length (pad with spaces if necessary)
            type = formatStringLength(type);
        
            // get the color and font for the specified type
            // (if not found, use default values)
            Util.TextColor color = Util.TextColor.Black;
            Util.FontType ftype = Util.FontType.Bold;
            FontInfo fontinfo = messageTypeTbl.get(type);
            if (fontinfo != null) {
                color = fontinfo.color;
                ftype = fontinfo.fonttype;
            }

            appendToPane(message, color, "Courier", 11, ftype);
        }
    }

    /**
     * outputs the various types of messages to the status display.
     * all messages will guarantee the previous line was terminated with a newline,
     * and will preceed the message with a timestamp value and terminate with a newline.
     * 
     * @param type    - the type of message
     * @param message - the message to display
     */
    public void print(String type, String message) {
        if (message != null && !message.isEmpty()) {
            // limit the type to a 5-char length (pad with spaces if necessary)
            type = formatStringLength(type);
        
            // print the header info if enabled (timestamp, message type)
            printHeader(type);

            // now print the message with a terminator
            printRaw(type, message + newLine);
        }
    }

    /**
     * prints the array of bytes to the status display.
     * 
     * @param array - the data to display
     */
    public void printArray(byte[] array) {
        final int bytesperline = 32; // the number of bytes to print per line
        
        appendToPane("Size of array = " + array.length + " bytes" + newLine, Util.TextColor.Black,  Util.FontType.Normal);

        // print line at a time
        for (int offset = 0; offset < array.length; offset += bytesperline) {
            char[] hexChars = new char[bytesperline * 3];
            char[] ascChars = new char[bytesperline];
            for (int ix = 0; ix < bytesperline; ix++) {
                if (ix + offset >= array.length) {
                    hexChars[ix * 3 + 0] = ' ';
                    hexChars[ix * 3 + 1] = ' ';
                    hexChars[ix * 3 + 2] = ' ';
                }
                else {
                    // make an array of displayable chars
                    int v = array[ix + offset] & 0xFF;
                    if (v >= 32 && v <= 126)
                        ascChars[ix] = (char) v;
                    else
                        ascChars[ix] = '.';

                    // make the array of hex values
                    hexChars[ix * 3 + 0] = hexArray[v >>> 4];
                    hexChars[ix * 3 + 1] = hexArray[v & 0x0F];
                    hexChars[ix * 3 + 2] = ' ';
                }
            }
            String hexdata = new String(hexChars);
            String ascdata = new String(ascChars);

            // generate the address value
            String address = "000000" + Integer.toHexString(offset);
            address = address.substring(address.length() - 6);
            
            // display the data
            printHeader("Hexdata");
            appendToPane(address + ": ", Util.TextColor.Brown,  Util.FontType.Italic);
            appendToPane(hexdata + "  ", Util.TextColor.Black,  Util.FontType.Normal);
            appendToPane(ascdata + newLine, Util.TextColor.Green,  Util.FontType.Normal);
        }
    }
    
    /**
     * a simple test of the colors
     */
    public void testColors () {
        appendToPane("-----------------------------------------" + newLine, Util.TextColor.Black, Util.FontType.Normal);
        for (Util.TextColor color : Util.TextColor.values()) {
            appendToPane("This is a sample of the color: " + color + newLine, color, Util.FontType.Bold);
        }
        appendToPane("-----------------------------------------" + newLine, Util.TextColor.Black, Util.FontType.Normal);
    }
    
    public class FontInfo {
        Util.TextColor  color;      // the font color
        Util.FontType   fonttype;   // the font attributes (e.g. Italics, Bold,..)
        String          font;       // the font family (e.g. Courier)
        int             size;       // the font size
        
        FontInfo (Util.TextColor col, Util.FontType type) {
            color = col;
            fonttype = type;
            font = "Courier";
            size = 11;
        }
    }
}
