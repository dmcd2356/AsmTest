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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.util.TraceClassVisitor;
import static org.objectweb.asm.Opcodes.ASM4;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;

/**
 *
 * @author dmcd2356
 */
public class AsmMainFrame extends javax.swing.JFrame {

    // specifies the local debug message types
    private enum DebugType {
        Error, Warn, Info, Entry, Exit, Event, Desc, Field, Method, Detail, Return;
    }
    
    /**
     * Creates new form AsmMainFrame
     */
    public AsmMainFrame() {
        initComponents();
        
        // setup output panel for writing to
        output = new DebugMessage(this.outputTextPane);
        output.enableTime(false);
        output.enableType(false);
        setDebugColorScheme(output);

        // init default selections
        jarpath = "";
        clsNodeMap = new HashMap<>();
        this.classFileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));

        properties = new PropertiesFile(output);
        if (properties != null) {
            // get the previous jar file and class selection if found
            String jarfilename = properties.getPropertiesItem(PropertiesFile.Type.JarFile);
            String classname   = properties.getPropertiesItem(PropertiesFile.Type.Class);
            
            File jarFile = new File(jarfilename);
            if (jarFile.isFile()) {
                // set the jar file selection
                jarpath = jarfilename;
                this.jarSelectTextField.setText(jarpath);
                // set the default dir for the jar selection
                String pathonly = jarfilename.substring(0, jarpath.lastIndexOf('/'));
                this.classFileChooser.setCurrentDirectory(new File(pathonly));
                // setup the class list selection
                setupClassList(jarpath);
                // set the class selection (if found)
                this.classComboBox.setSelectedItem(classname);
            }
        }
    }

    /**
     * sets up the DebugMessage instance with the color selections to use.
     * 
     * @param handler - the DebugMessage instance to apply it to
     */
    private void setDebugColorScheme (DebugMessage handler) {
        handler.setTypeColor (DebugType.Error.toString(),  Util.TextColor.Red,   Util.FontType.Bold);
        handler.setTypeColor (DebugType.Warn.toString(),   Util.TextColor.DkRed, Util.FontType.Normal);
        handler.setTypeColor (DebugType.Info.toString(),   Util.TextColor.Black, Util.FontType.Normal);
        handler.setTypeColor (DebugType.Entry.toString(),  Util.TextColor.Brown, Util.FontType.Normal);
        handler.setTypeColor (DebugType.Exit.toString(),   Util.TextColor.Brown, Util.FontType.Normal);
        handler.setTypeColor (DebugType.Event.toString(),  Util.TextColor.Gold,  Util.FontType.Italic);
        handler.setTypeColor (DebugType.Desc.toString(),   Util.TextColor.Gold,  Util.FontType.Italic);
        handler.setTypeColor (DebugType.Field.toString(),  Util.TextColor.Green, Util.FontType.Normal);
        handler.setTypeColor (DebugType.Method.toString(), Util.TextColor.Blue,  Util.FontType.Normal);
        handler.setTypeColor (DebugType.Detail.toString(), Util.TextColor.Blue,  Util.FontType.Italic);
        handler.setTypeColor (DebugType.Return.toString(), Util.TextColor.DkVio, Util.FontType.Italic);
    }
    
    private void printTaggedInfo (String tag, String info) {
        output.printRaw(DebugType.Info.toString(), tag + ": ");
        output.printRaw(DebugType.Detail.toString(), info);
        output.printTerm();
    }
    
    /**
     * finds the classes in a jar file & sets the Class ComboBox to these values.
     * 
     * @param jarFile - the jar file to examine
     */
    private void setupClassList (String jarPath) {
        JarFile jarFile;
        try {
            jarFile = new JarFile(jarPath);
        } catch (IOException ex) {
            output.print(DebugType.Error.toString(), ex.getMessage());
            return;
        }

        // init the class list to none
        this.classComboBox.removeAllItems();
        clsNodeMap = new HashMap<>();

        // read the jar file contents
        int count = 0;
        Enumeration value = jarFile.entries();
        while (value.hasMoreElements()) {
            JarEntry entry = (JarEntry)value.nextElement();
            String fullname = entry.getName();
            if (fullname.endsWith(".class")) {
                // add class entry to class combobox selection
                this.classComboBox.addItem(fullname);
                        
                // add entry to map: use class name to access class node
                InputStream inStream;
                try {
                    inStream = jarFile.getInputStream(entry);
                    ClassReader clsReader = new ClassReader(inStream);
                    ClassNode clsNode = new ClassNode();
                    clsReader.accept(clsNode, 0);
                    String clsName = clsNode.name;
                    clsNodeMap.put(clsName, clsNode);
                    ++count;
                } catch (IOException ex) {
                    output.print(DebugType.Error.toString(), ex.getMessage());
                    return;
                }
            }
        }

        // set the 1st entry as the default selection
        if (classComboBox.getItemCount() > 0)
            this.classComboBox.setSelectedIndex(0);
        
        output.print(DebugType.Info.toString(), count + " classes found in jar file");
    }

    public class ClassPrinter extends ClassVisitor {
        public ClassPrinter() {
            super(ASM4);
        }
        
        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            output.print(DebugType.Entry.toString(), name + " extends " + superName + " {");
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
            output.printHeader(DebugType.Method.toString());
            output.printRaw(DebugType.Desc.toString(),  " " + desc);
            output.printRaw(DebugType.Field.toString(), " " + name);
            output.printTerm();
            return null;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            // split description field into the parameters and the return value
            String retval = "";
            int offset = desc.lastIndexOf(')');
            if (offset > 0) {
                retval = desc.substring(offset+1);
                desc   = desc.substring(0, offset+1);
            }
            output.printHeader(DebugType.Method.toString());
            output.printRaw(DebugType.Method.toString(), " " + name);
            output.printRaw(DebugType.Desc.toString(),   " " + desc);
            output.printRaw(DebugType.Return.toString(), retval);
            output.printTerm();
            return null;
        }

        @Override
        public void visitEnd() {
            output.print(DebugType.Exit.toString(), "}");
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
        methodComboBox = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        runPanel = new javax.swing.JPanel();
        readButton = new javax.swing.JButton();
        printButton = new javax.swing.JButton();
        writeButton = new javax.swing.JButton();
        bytecodeButton = new javax.swing.JButton();
        clearButton = new javax.swing.JButton();
        showinfoButton = new javax.swing.JButton();
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

        methodComboBox.setToolTipText("<html>\nSpecifies the Class of the method to be tested.\n</html>");
        methodComboBox.setMaximumSize(new java.awt.Dimension(32767, 24));
        methodComboBox.setMinimumSize(new java.awt.Dimension(441, 24));
        methodComboBox.setPreferredSize(new java.awt.Dimension(500, 24));
        methodComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                methodComboBoxActionPerformed(evt);
            }
        });

        jLabel2.setText("Method");

        javax.swing.GroupLayout selectionPanelLayout = new javax.swing.GroupLayout(selectionPanel);
        selectionPanel.setLayout(selectionPanelLayout);
        selectionPanelLayout.setHorizontalGroup(
            selectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(selectionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(selectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(loadButton)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(selectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(classComboBox, 0, 954, Short.MAX_VALUE)
                    .addComponent(jarSelectTextField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(methodComboBox, 0, 954, Short.MAX_VALUE))
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(selectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(methodComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addGap(33, 33, 33))
        );

        runPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        readButton.setText("Read");
        readButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                readButtonActionPerformed(evt);
            }
        });

        printButton.setText("Print");
        printButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printButtonActionPerformed(evt);
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

        showinfoButton.setText("Show Info");
        showinfoButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showinfoButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout runPanelLayout = new javax.swing.GroupLayout(runPanel);
        runPanel.setLayout(runPanelLayout);
        runPanelLayout.setHorizontalGroup(
            runPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(runPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(readButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(showinfoButton)
                .addGap(7, 7, 7)
                .addComponent(printButton)
                .addGap(86, 86, 86)
                .addComponent(bytecodeButton)
                .addGap(42, 42, 42)
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
                    .addComponent(printButton)
                    .addComponent(writeButton)
                    .addComponent(bytecodeButton)
                    .addComponent(clearButton)
                    .addComponent(showinfoButton))
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
                .addComponent(viewTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 351, Short.MAX_VALUE)
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
            File jarFile = this.classFileChooser.getSelectedFile();
            jarpath = jarFile.getAbsolutePath();
            this.jarSelectTextField.setText(jarpath);
            
            // save in properties
            properties.setPropertiesItem(PropertiesFile.Type.JarFile, jarpath);

            // now load up the class selections and set default selection
            setupClassList(jarpath);
        }
    }//GEN-LAST:event_loadButtonActionPerformed

    private void readButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_readButtonActionPerformed
        // make sure we have a valid jar file and class selection
        String clsname = (String)this.classComboBox.getSelectedItem();
        File jarFile = new File(jarpath);
        if (!jarFile.isFile() || clsname == null || clsname.isEmpty())
            return;

        try {
            // read the class from the stream
            ClassLoader classLoader = new URLClassLoader(new URL[] { jarFile.toURI().toURL() });
            InputStream is = classLoader.getResourceAsStream(clsname);
            classReaderData = new ClassReader(is);
            if (classReaderData != null) {
                output.print(DebugType.Info.toString(), "Class read: " + clsname);

                // get the ClassNode for the specified class from the hash map
                String basename = clsname;
                if (basename.endsWith(".class"))
                    basename = basename.substring(0, basename.length()-".class".length());
                classNode = clsNodeMap.get(basename);
                if (classNode != null) {
                    // now get the methods for the class and place in the method selection combobox
                    this.methodComboBox.removeAllItems();
                    for (MethodNode method : (List<MethodNode>)classNode.methods) {
                        this.methodComboBox.addItem(method.name);
                    }

                    // set the 1st entry as the default selection
                    int count = methodComboBox.getItemCount();
                    if (count > 0)
                        this.methodComboBox.setSelectedIndex(0);
        
                    output.print(DebugType.Info.toString(), count + " methods found in class");
                }
            }
        } catch (IOException ex) {
            output.print(DebugType.Error.toString(), ex.getMessage());
        }
    }//GEN-LAST:event_readButtonActionPerformed

    private void printButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_printButtonActionPerformed
        if (classReaderData != null) {
            // print the class info
            classReaderData.accept(new ClassPrinter(), 0);
        }
    }//GEN-LAST:event_printButtonActionPerformed

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
            output.print(DebugType.Info.toString(), "TraceClassVisitor read");

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
            output.print(DebugType.Info.toString(), tracedata);
            
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
            output.printByteArray(bytes, true);
        }
    }//GEN-LAST:event_bytecodeButtonActionPerformed

    private void classComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_classComboBoxActionPerformed
        // save the selection in the properties file
        String classname = (String)classComboBox.getSelectedItem();
        properties.setPropertiesItem(PropertiesFile.Type.Class, classname);
        // init the method selection list to none (must press "Read")
        this.methodComboBox.removeAllItems();
    }//GEN-LAST:event_classComboBoxActionPerformed

    private void clearButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearButtonActionPerformed
        // clear the prev output
        output.clear();
    }//GEN-LAST:event_clearButtonActionPerformed

    private void showinfoButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showinfoButtonActionPerformed
        String clsname = (String)this.classComboBox.getSelectedItem();
        if (clsname == null || clsname.isEmpty())
            return;
        
        if (classReaderData != null && classNode != null) {
            printTaggedInfo ("Class", clsname);
            
            String methodname = (String)methodComboBox.getSelectedItem();
            if (methodname == null) {
                output.print(DebugType.Warn.toString(), "  No method selection made!");
                return;
            }
            
            // find method selection
            for (MethodNode method : (List<MethodNode>)classNode.methods) {
                if (methodname.equals(method.name)) {
                    printTaggedInfo ("Method", method.name);
                    printTaggedInfo ("desc", method.desc);
                    printTaggedInfo ("instructions.size", "" + method.instructions.size());
                    printTaggedInfo ("maxStack", "" + method.maxStack);
                    // print the parameters for the method
                    if (method.parameters == null) {
                        printTaggedInfo ("parameters", "(null)");
                    }
                    else {
                        output.print(DebugType.Info.toString(), "parameters:");
                        for (ParameterNode param : (List<ParameterNode>)method.parameters) {
                            output.print(DebugType.Detail.toString(), "  " + param.name);
                        }
                    }
                    // print the local variables for the method
                    if (method.localVariables != null) {
                        output.print(DebugType.Info.toString(), "localVariables: " + method.maxLocals);
                        for (LocalVariableNode local : (List<LocalVariableNode>)method.localVariables) {
                            output.print(DebugType.Detail.toString(), "  " + local.name);
                        }
                    }
                }
            }
        }
    }//GEN-LAST:event_showinfoButtonActionPerformed

    private void methodComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_methodComboBoxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_methodComboBoxActionPerformed

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
    private ClassNode   classNode;
    private String      jarpath;
    private Map<String, ClassNode> clsNodeMap;

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton bytecodeButton;
    private javax.swing.JComboBox classComboBox;
    private javax.swing.JFileChooser classFileChooser;
    private javax.swing.JButton clearButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JTextField jarSelectTextField;
    private javax.swing.JButton loadButton;
    private javax.swing.JComboBox methodComboBox;
    private javax.swing.JScrollPane outputScrollPane;
    private javax.swing.JTextPane outputTextPane;
    private javax.swing.JButton printButton;
    private javax.swing.JButton readButton;
    private javax.swing.JPanel runPanel;
    private javax.swing.JPanel selectionPanel;
    private javax.swing.JButton showinfoButton;
    private javax.swing.JTabbedPane viewTabbedPane;
    private javax.swing.JButton writeButton;
    // End of variables declaration//GEN-END:variables
}
