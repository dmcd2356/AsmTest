/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package asmtest;

import java.awt.Graphics;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;

/**
 *
 * @author dan
 */
public class DebugMessage {
    
    public enum StatusType {
        Info, Warning, Error, EntryExit, Field, Method, Event;
    }

    // used in formatting printArray
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    
    private static final String newLine = System.getProperty("line.separator");

    private JTextPane    debugTextPane;
    private ElapsedTimer elapsedTimer;

    DebugMessage (JTextPane textPane, ElapsedTimer elapsed) {
        debugTextPane = textPane;
        elapsedTimer  = elapsed;
    }

    DebugMessage (JTextPane textPane) {
        debugTextPane = textPane;
        elapsedTimer  = null;
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
     * outputs the various types of messages to the status display.
     * all messages will guarantee the previous line was terminated with a newline,
     * and will preceed the message with a timestamp value and terminate with a newline.
     * 
     * @param type    - the type of message
     * @param message - the message to display
     */
    public void print(StatusType type, String message) {
        // skip if no message
        if (message == null || message.isEmpty())
            return;
        
        // show the timestamp if one is assigned
        if (elapsedTimer != null) {
            String tstamp = "[" + elapsedTimer.getElapsed() + "] ";
            appendToPane(tstamp, Util.TextColor.Brown, Util.FontType.Bold);
        }

        // add termination to message
        message += newLine;
        
        switch (type) {
            // the following preceed the message with a timestamp and terminate with a newline
            case EntryExit:
                appendToPane(message, Util.TextColor.Brown, Util.FontType.Normal);
                break;

            case Field:
                int offset = message.lastIndexOf(" ");
                if (offset > 0) {
                    String datatype = message.substring(0, offset);
                    String param = message.substring(offset);
                    appendToPane(datatype, Util.TextColor.Gold,  Util.FontType.Italic);
                    appendToPane(param,    Util.TextColor.Green, Util.FontType.Normal);
                }
                else {
                    appendToPane(message, Util.TextColor.Green, Util.FontType.Normal);
                }
                break;

            case Method:
                int offset1 = message.indexOf("(");
                int offset2 = message.indexOf(")");
                if (offset1 > 0 && offset2 > 0) {
                    String method = message.substring(0, offset1);
                    String param  = message.substring(offset1+1, offset2);
                    String retval = message.substring(offset2+1);
                    appendToPane(method, Util.TextColor.Blue,  Util.FontType.Normal);
                    appendToPane("(",    Util.TextColor.Black, Util.FontType.Normal);
                    appendToPane(param,  Util.TextColor.Gold,  Util.FontType.Italic);
                    appendToPane(")",    Util.TextColor.Black, Util.FontType.Normal);
                    appendToPane(retval, Util.TextColor.DkVio, Util.FontType.Italic);
                }
                else {
                    appendToPane(message, Util.TextColor.Blue, Util.FontType.Normal);
                }
                break;

            case Event:
                appendToPane(message, Util.TextColor.Gold, Util.FontType.Normal);
                break;

            default:    // fall through...
            case Info:
                appendToPane(message, Util.TextColor.Black, Util.FontType.Normal);
                break;

            case Error:
                appendToPane("ERROR: " + message, Util.TextColor.Red, Util.FontType.Bold);
                break;

            case Warning:
                appendToPane("WARNING: " + message, Util.TextColor.LtRed, Util.FontType.Bold);
                break;
        }

        // force an update
        Graphics graphics = debugTextPane.getGraphics();
        if (graphics != null)
            debugTextPane.update(graphics);
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
            appendToPane(address + ": ", Util.TextColor.Brown,  Util.FontType.Italic);
            appendToPane(hexdata + "  ", Util.TextColor.Black,  Util.FontType.Normal);
            appendToPane(ascdata + newLine, Util.TextColor.Green,  Util.FontType.Normal);
        }
    }
    
    /**
     * clears the status display.
     */
    public void clear() {
        debugTextPane.setText("");
    }
}
