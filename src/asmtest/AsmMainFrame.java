/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package asmtest;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import static org.objectweb.asm.Opcodes.ASM4;
import org.objectweb.asm.util.TraceClassVisitor;

/**
 *
 * @author dmcd2356
 */
public class AsmMainFrame extends javax.swing.JFrame {

    // specifies the debug message types (and, hence, the associated text color)
    public enum DebugType {
        Error, Warning, Success, Info, Entry, Exit, Field, Method;
    }
    
    /**
     * Creates new form AsmMainFrame
     */
    public AsmMainFrame() {
        initComponents();
        
        // setup output panel for writing to
        output = new DebugMessage(this.outputTextPane);
        setDebugColorScheme(output);

        // init default selections
        jarFile = null;
        this.classFileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));

        properties = new PropertiesFile(output);
        if (properties != null) {
            // get the previous jar file and class selection if found
            String jarfilename = properties.getPropertiesItem(PropertiesFile.Type.JarFile);
            String classname   = properties.getPropertiesItem(PropertiesFile.Type.Class);
            
            jarFile = new File(jarfilename);
            if (!jarFile.isFile())
                jarFile = null;
            else {
                // set the jar file selection
                this.jarSelectTextField.setText(jarFile.getAbsolutePath());
                // set the default dir for the jar selection
                String jarpath = jarfilename.substring(0, jarfilename.lastIndexOf('/'));
                this.classFileChooser.setCurrentDirectory(new File(jarpath));
                // setup the class list selection
                setupClassList(jarFile);
                // set the class selection (if found)
                this.classComboBox.setSelectedItem(classname);
            }
        }
    }

    /**
     * converts the DebugType to the corresponding String key value for DebugMessage.print.
     * Note that ErrorExit and EntryExit are not listed because they are turned into
     * the other types when sending to print.
     * 
     * @param type - the DebugType value to convert
     * @return the corresponding string entry
     */
    private String getDebugTypeString (DebugType type) {
        switch (type) {
            case Error :    return "ERROR";
            case Warning :  return "WARN ";
            case Success :  return "PASS ";
            default :
            case Info :     return "DEBUG";
            case Entry :    return "ENTRY";
            case Exit :     return "EXIT ";
            case Field :    return "FIELD";
            case Method :   return "METH ";
        }
    }
    
    /**
     * sets up the DebugMessage instance with the color selections to use.
     * 
     * @param handler - the DebugMessage instance to apply it to
     */
//        Error, Warning, Success, Info, Entry, Exit, Field, Method;
    private void setDebugColorScheme (DebugMessage handler) {
        handler.setTypeColor (getDebugTypeString(DebugType.Error),   asmtest.Util.TextColor.Red,   asmtest.Util.FontType.Bold);
        handler.setTypeColor (getDebugTypeString(DebugType.Warning), asmtest.Util.TextColor.DkRed, asmtest.Util.FontType.Bold);
        handler.setTypeColor (getDebugTypeString(DebugType.Success), asmtest.Util.TextColor.Green, asmtest.Util.FontType.Bold);
        handler.setTypeColor (getDebugTypeString(DebugType.Info),    asmtest.Util.TextColor.Black, asmtest.Util.FontType.Bold);
        handler.setTypeColor (getDebugTypeString(DebugType.Entry),   asmtest.Util.TextColor.Brown, asmtest.Util.FontType.Bold);
        handler.setTypeColor (getDebugTypeString(DebugType.Exit),    asmtest.Util.TextColor.Brown, asmtest.Util.FontType.Bold);
        handler.setTypeColor (getDebugTypeString(DebugType.Field),   asmtest.Util.TextColor.Gold,  asmtest.Util.FontType.BoldItalic);
        handler.setTypeColor (getDebugTypeString(DebugType.Method),  asmtest.Util.TextColor.Blue,  asmtest.Util.FontType.Bold);
    }
    
    private void debugprint(DebugType type, String message) {
        output.print(getDebugTypeString(type), message);
    }
    
    /**
     * finds the classes in a jar file & sets the Class ComboBox to these values.
     * 
     * @param jarFile - the jar file to examine
     */
    private void setupClassList (File jarFile) {
        if (jarFile == null)
            return;

        // init the class list to none
        this.classComboBox.removeAllItems();
        ZipInputStream zip;

        // read the jar file contents
        try {
            zip = new ZipInputStream(jarFile.toURI().toURL().openStream());
        } catch (IOException ex) {
            debugprint(DebugType.Error, ex.getMessage());
            return;
        }
        while (true) {
            // read each entry looking for those that end in ".class"
            try {
                ZipEntry entry = zip.getNextEntry();
                if (entry == null)
                    break;
                String fullname = entry.getName();
                int offset = fullname.lastIndexOf('/');
                if (offset > 0) {
                    String path = fullname.substring(0, offset);
                    String fname = fullname.substring(offset+1);
                    if (fname.endsWith(".class")) {
                        this.classComboBox.addItem(fullname);
                    }
                }
            } catch (IOException ex) {
                debugprint(DebugType.Error, ex.getMessage());
                break;
            }
        }

        // make sure to close the zip file when done
        try {
            zip.close();
        } catch (IOException ex) {
            debugprint(DebugType.Error, ex.getMessage());
        }
                
        // set the 1st entry as the default selection
        if (classComboBox.getItemCount() > 0)
            this.classComboBox.setSelectedIndex(0);
    }

    public class ClassPrinter extends ClassVisitor {
        public ClassPrinter() {
            super(ASM4);
        }
        
        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            debugprint(DebugType.Entry, name + " extends " + superName + " {");
        }
        
        @Override
        public void visitSource(String source, String debug) {
        }
        
        @Override
        public void visitOuterClass(String owner, String name, String desc) {
        }
        
        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            return null;
        }
        
        @Override
        public void visitAttribute(Attribute attr) {
        }

        @Override
        public void visitInnerClass(String name, String outerName, String innerName, int access) {
        }

        @Override
        public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
            debugprint(DebugType.Field, " " + desc + " " + name);
            /*
                int offset = message.lastIndexOf(" ");
                if (offset > 0) {
                    String datatype = message.substring(0, offset);
                    String param = message.substring(offset);
                    appendToPane(datatype, Util.TextColor.Gold,  Util.FontType.Italic);
                    appendToPane(param,    Util.TextColor.Green, Util.FontType.Normal);
                }
            */
            return null;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            debugprint(DebugType.Method, " " + name + desc);
            /*
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
            */
            return null;
        }

        @Override
        public void visitEnd() {
            debugprint(DebugType.Exit, "}");
        }
    }    

    public class TransformationAdapter extends ClassVisitor {
        public TransformationAdapter(ClassVisitor cv) {
            super(ASM4, cv);
        }
        
        @Override
        public void visit(int version, int access, String name, String signature,
                          String superName, String[] interfaces) {
            cv.visit(version, access, name, signature, superName, interfaces);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        classFileChooser = new javax.swing.JFileChooser();
        selectionPanel = new javax.swing.JPanel();
        classComboBox = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        loadButton = new javax.swing.JButton();
        jarSelectTextField = new javax.swing.JTextField();
        runPanel = new javax.swing.JPanel();
        readButton = new javax.swing.JButton();
        infoButton = new javax.swing.JButton();
        writeButton = new javax.swing.JButton();
        bytecodeButton = new javax.swing.JButton();
        clearButton = new javax.swing.JButton();
        viewTabbedPane = new javax.swing.JTabbedPane();
        outputScrollPane = new javax.swing.JScrollPane();
        outputTextPane = new javax.swing.JTextPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("AsmTest");
        setMinimumSize(new java.awt.Dimension(250, 100));

        selectionPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        selectionPanel.setMinimumSize(new java.awt.Dimension(610, 100));

        classComboBox.setToolTipText("<html>\nSpecifies the Class of the method to be tested.\n</html>");
        classComboBox.setMaximumSize(new java.awt.Dimension(32767, 24));
        classComboBox.setMinimumSize(new java.awt.Dimension(441, 24));
        classComboBox.setPreferredSize(new java.awt.Dimension(500, 24));
        classComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                classComboBoxActionPerformed(evt);
            }
        });

        jLabel1.setText("Class");

        loadButton.setText("Jar File");
        loadButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadButtonActionPerformed(evt);
            }
        });

        jarSelectTextField.setMinimumSize(new java.awt.Dimension(50, 25));
        jarSelectTextField.setPreferredSize(new java.awt.Dimension(530, 25));

        javax.swing.GroupLayout selectionPanelLayout = new javax.swing.GroupLayout(selectionPanel);
        selectionPanel.setLayout(selectionPanelLayout);
        selectionPanelLayout.setHorizontalGroup(
            selectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(selectionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(selectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(loadButton)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(selectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(classComboBox, 0, 954, Short.MAX_VALUE)
                    .addComponent(jarSelectTextField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        selectionPanelLayout.setVerticalGroup(
            selectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, selectionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(selectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jarSelectTextField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(loadButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(selectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(classComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addGap(33, 33, 33))
        );

        runPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        readButton.setText("Read");
        readButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                readButtonActionPerformed(evt);
            }
        });

        infoButton.setText("Info");
        infoButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                infoButtonActionPerformed(evt);
            }
        });

        writeButton.setText("Write");
        writeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                writeButtonActionPerformed(evt);
            }
        });

        bytecodeButton.setText("Bytecode");
        bytecodeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bytecodeButtonActionPerformed(evt);
            }
        });

        clearButton.setText("Clear");
        clearButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout runPanelLayout = new javax.swing.GroupLayout(runPanel);
        runPanel.setLayout(runPanelLayout);
        runPanelLayout.setHorizontalGroup(
            runPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(runPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(readButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(infoButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(bytecodeButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(writeButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(clearButton)
                .addContainerGap())
        );
        runPanelLayout.setVerticalGroup(
            runPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(runPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(runPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(readButton)
                    .addComponent(infoButton)
                    .addComponent(writeButton)
                    .addComponent(bytecodeButton)
                    .addComponent(clearButton))
                .addContainerGap(24, Short.MAX_VALUE))
        );

        outputScrollPane.setViewportView(outputTextPane);

        viewTabbedPane.addTab("Output", outputScrollPane);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(viewTabbedPane)
                    .addComponent(selectionPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(runPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(selectionPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(runPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(viewTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 383, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void loadButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadButtonActionPerformed
        // this filechooser is setup to allow file and directory selections so that
        // the user can look for a directory with jar files in search for the
        // required rt.jar file.
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Jar Files","jar");
        this.classFileChooser.setFileFilter(filter);
        this.classFileChooser.setMultiSelectionEnabled(false);
        int retVal = this.classFileChooser.showOpenDialog(this);
        if (retVal == JFileChooser.APPROVE_OPTION) {
            jarFile = this.classFileChooser.getSelectedFile();
            this.jarSelectTextField.setText(jarFile.getAbsolutePath());
            
            // save in properties
            properties.setPropertiesItem(PropertiesFile.Type.JarFile, jarFile.getAbsolutePath());

            // now load up the class selections and set default selection
            setupClassList(jarFile);
        }
    }//GEN-LAST:event_loadButtonActionPerformed

    private void readButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_readButtonActionPerformed
        // make sure we have a valid jar file and class selection
        String clsname = (String)this.classComboBox.getSelectedItem();
        if (jarFile == null || !jarFile.isFile() || clsname == null || clsname.isEmpty())
            return;

        try {
            // read the class from the stream
            ClassLoader classLoader = new URLClassLoader(new URL[] { jarFile.toURI().toURL() });
            InputStream is = classLoader.getResourceAsStream(clsname);
            classReaderData = new ClassReader(is);
            if (classReaderData != null)
                debugprint(DebugType.Info, "Class read successfully!");
        } catch (IOException ex) {
            debugprint(DebugType.Error, ex.getMessage());
        }
    }//GEN-LAST:event_readButtonActionPerformed

    private void infoButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_infoButtonActionPerformed
        if (classReaderData != null) {
            // print the class info
            classReaderData.accept(new ClassPrinter(), 0);
        }
    }//GEN-LAST:event_infoButtonActionPerformed

    private void writeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_writeButtonActionPerformed
        if (classReaderData != null) {
            // create a writer & sync with reader input to optimize for changes only
            ClassWriter cw = new ClassWriter(classReaderData, 0);
            
            // perform the code transformation
            TransformationAdapter ca = new TransformationAdapter(cw);
            classReaderData.accept(ca, 0);

            // Display trace info
            // TODO: this doesn't seem to print anything
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintWriter printWriter = new PrintWriter(baos, true);
//            Printer printer = new ASMifier();
//            printer.print(printWriter);
            TraceClassVisitor tcv = new TraceClassVisitor(ca, printWriter); // cw

            // TODO: documentation in sections 2.3.2 and 2.3.3 indicated that
            // the following was needed, but I'm not sure what for.
            //tcv.visit(int version, int access, String name, String signature, String superName, String[] interfaces);
            //tcv.visitSource(String source, String debug);
            //tcv.visitOuterClass(String owner, String name, String desc);
            //tcv.visitAnnotation(String desc, boolean visible);
            //tcv.visitAttribute(Attribute attr);
            //tcv.visitInnerClass(String name, String outerName, String innerName, int access);
            //tcv.visitField(int access, String name, String desc, String signature, Object value);
            //tcv.visitMethod(int access, String name, String desc, String signature, String[] exceptions);
            //tcv.visitEnd();

            String tracedata = baos.toString();
            debugprint(DebugType.Info, tracedata);
            
            // perform verification on the transformed code
            // TODO: not sure how you get the results of the check...
//            CheckClassAdapter cca = new CheckClassAdapter(tcv); // (ClassVisitor)ca
        }
    }//GEN-LAST:event_writeButtonActionPerformed

    private void bytecodeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bytecodeButtonActionPerformed
        if (classReaderData != null) {
            // create a writer & sync with reader input to optimize for changes only
            ClassWriter cw = new ClassWriter(classReaderData, 0);
            
            // display the converted data
            byte[] bytes = cw.toByteArray();
            output.printArray(bytes);
        }
    }//GEN-LAST:event_bytecodeButtonActionPerformed

    private void classComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_classComboBoxActionPerformed
        // save the selection in the properties file
        String classname = (String)classComboBox.getSelectedItem();
        properties.setPropertiesItem(PropertiesFile.Type.Class, classname);
    }//GEN-LAST:event_classComboBoxActionPerformed

    private void clearButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearButtonActionPerformed
        // clear the prev output
        output.clear();
    }//GEN-LAST:event_clearButtonActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(AsmMainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new AsmMainFrame().setVisible(true);
            }
        });
    }

    private final PropertiesFile properties;
    private final DebugMessage output;
    private ClassReader classReaderData;
    private File        jarFile;

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton bytecodeButton;
    private javax.swing.JComboBox classComboBox;
    private javax.swing.JFileChooser classFileChooser;
    private javax.swing.JButton clearButton;
    private javax.swing.JButton infoButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JTextField jarSelectTextField;
    private javax.swing.JButton loadButton;
    private javax.swing.JScrollPane outputScrollPane;
    private javax.swing.JTextPane outputTextPane;
    private javax.swing.JButton readButton;
    private javax.swing.JPanel runPanel;
    private javax.swing.JPanel selectionPanel;
    private javax.swing.JTabbedPane viewTabbedPane;
    private javax.swing.JButton writeButton;
    // End of variables declaration//GEN-END:variables
}
