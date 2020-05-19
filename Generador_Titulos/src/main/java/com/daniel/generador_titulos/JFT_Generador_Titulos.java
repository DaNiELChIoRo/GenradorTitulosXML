/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Cynthia Clarissa & Daniel R Meneses León
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;


import org.apache.commons.codec.binary.Base64;
import org.apache.commons.ssl.PKCS8Key;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;

/**
 * Creates new form Intefaz
 */
public class JFT_Generador_Titulos extends javax.swing.JFrame {

    public static final String URL = "jdbc:mysql://localhost:3306/UNIVERSITY_TITLES?useTimezone=true&serverTimezone=GMT%2B8&relaxAutoCommit=false";
    public static final String USERNAME = "root";
    public static final String PASSWORD = "mysql";
    PreparedStatement ps;
    ResultSet res;
    Connection conn = null;
    Statement stmt = null;
    String p = "|";

   
    String UNIVERSITY_NAME = "UNIVERSIDAD DE LAS AMÉRICAS CIUDAD DE MÉXICO, S.A";

    String cadenaOriginal = "";
    String noCertificadoResponsable = "";
    
    String nombre;
    String a_paterno;
    String a_materno;
    String correo;
    
    String curp;
    
    String nombreCarrera;
    
    String F_INICIO;
    String F_FIN;
    String F_EXAMEN; 
    
    // *** DATOS RESPONSABLE ***
    String RFC_FIRMANTE = "";
    String nombreFirmante;
    String primerApellidoFirmante;
    String segundoApellidoFirmante;
    
    String responsableCargo = "RESPONSABLE DE EXPEDICIÓN";
    String responsableIdCargo = "5";
    String responsableAbrTitulo = "DR.";
    
    String certificadoResponsable;
    String sello;

    private File cer;
    private File key;
    
    private boolean hasChoosenCer = false;
    private boolean hasChoosenKey = false;

    public JFT_Generador_Titulos() {
        initComponents();
        conn = getConnection();
    }

    public static Connection getConnection() {
        Connection conn = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = (Connection) DriverManager.getConnection(URL, USERNAME, PASSWORD);
            // JOptionPane.showMessageDialog(null, "Conexión exitosa");
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println(e);
            JOptionPane.showMessageDialog(null, "Error al intentar crear la conexión con la DB, Error: " + e);
        }
        return conn;
    }

    //toSign es el archivo a firmar!
    public String sign(String password, String toSign) throws Exception {
        final PKCS8Key pkcs8Key = new PKCS8Key(toByteArray(key), password.toCharArray());

        final PrivateKey privateKey = pkcs8Key.getPrivateKey();

        final Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(toSign.getBytes("UTF-8"));

        return Base64.encodeBase64String(signature.sign());
    }
    
    public String getCertifiyerSign(File file) throws Exception {
        final CertificateFactory cf = CertificateFactory.getInstance("X.509");
        FileInputStream fis = new FileInputStream(file);
        X509Certificate cert = (X509Certificate) cf.generateCertificate(fis);
        //String signerName = cert.getSubjectDN().getName();
        RDN[] rdns = new JcaX509CertificateHolder(cert).getSubject().getRDNs();
        for(RDN rnd: rdns) {                        
            String value = rnd.getFirst().getValue().toString();
            String type = rnd.getFirst().getType().getId();
            if (type.endsWith(".5")) {
                RFC_FIRMANTE = value;
            }
            else if(type.endsWith(".10") || type.endsWith(".3") || type.endsWith(".41")) {
                String[] splittedName = value.split("\\s+");
                segundoApellidoFirmante = splittedName[splittedName.length - 1];
                primerApellidoFirmante = splittedName[splittedName.length - 2];
                for(int i= 0; i < splittedName.length - 2; i++) {            
                    nombreFirmante += splittedName[i];
                }
            } 
            //System.out.println("value: " + value + " type: "+ type);
        }
                
        String rawSignature = cert.getSerialNumber().toString(16);
        String processedSignature = "";
        for(int i=0;i<rawSignature.length();i++) {
            if(i % 2 != 0 ) {
                processedSignature += rawSignature.charAt(i);
            }
        }
        if(processedSignature.isEmpty()) {
            throw new NonSerialazedCertifiedException();
        }
        return processedSignature;
    }

    private static byte[] toByteArray(File file) throws Exception {
        FileInputStream fis = new FileInputStream(file);

        byte[] fbytes = new byte[(int) file.length()];

        fis.read(fbytes);
        fis.close();

        return fbytes;
    }
    
    private void promptUserToSelectOutPath() {       
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Especifica la ruta de guardado del archivo");   

        int userSelection = fileChooser.showSaveDialog(rootPane);        
         
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            String pathToSave = fileChooser.getSelectedFile().getAbsolutePath() + ".xml";
            try {
                XMLCreatorWith(pathToSave);
            } catch(Exception ex) {
                //Handlingthe exception
                createExceptionAlert(ex.getLocalizedMessage());
            }    
            // TODO:- Prompt the user with a success message?
            // System.out.println("Save as file: " + pathToSave);
        }
    }
    
    private void XMLCreatorWith(String path) throws IOException {
        FileWriter fileWriter = new FileWriter(path);
        
        String[] datos =  {"<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
        "\n<TituloElectronico"
                + " folioControl=\"500001\""
                + " version=\"1.0\""
                + " xsi:schemaLocation=\"https://www.siged.sep.gob.mx/titulos/schema.xsd\""
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                + " xmlns=\"https://www.siged.sep.gob.mx/titulos/\">",
        "\n<FirmaResponsables>",
        "\n <FirmaResponsable nombre=\"" + nombreFirmante 
                + "\" primerApellido=\"" + primerApellidoFirmante
                + "\" segundoApellido=\"" + segundoApellidoFirmante
                + "\" curp=\"" + RFC_FIRMANTE
                + "\" idCargo=\"" + responsableIdCargo
                + "\" cargo=\"" + responsableCargo
                + "\" abrTitulo=\"" + responsableAbrTitulo + "\"",
        "\nsello=\"" + sello + "\"",
        "\ncertificadoResponsable=\"" + certificadoResponsable + "\"",
        "\nnoCertificadoResponsable=\"" + noCertificadoResponsable + "\" />",
        "\n </FirmaResponsables>",
        
        "\n\t <Institucion cveInstitucion=\"" 
            + "\" nombreInstitucion=\"" + "\" />",
        
        "\n\t <Carrera cveCarrera=\""
            + "\" nombreCarrera=\"" + nombreCarrera
            + "\" fechaInicio=\"" + F_INICIO
            + "\" fechaTerminacion=\"" + F_FIN
            + "\" idAutorizacionReconocimiento=\"" //1\""
            + "\" autorizacionReconocimiento=\""  //RVOE FEDERAL\""
            + "\" numeroRvoe=\"" + "\" />",
        
        "\n\t <Profesionista curp=\"" + curp
            + "\" nombre=\"" + nombre
            + "\" primerApellido=\"" + a_paterno
            + "\" segundoApellido=\"" + a_materno
            + "\" correoElectronico=\"" + correo + "\" />",
        
        "\n\t <Expedicion fechaExpedicion=\""
            + "\" idModalidadTitulacion=\""
            + "\" modalidadTitulacion=\""
            + "\" fechaExamenProfesional=\""
            + "\" cumplioServicioSocial=\""
            + "\" idFundamentoLegalServicioSocial=\""
            + "\" fundamentoLegalServicioSocial=\""
            + "\" idEntidadFederativa=\""
            + "\" entidadFederativa=\"" + "\" />",
        
         "\n\t <Antecedente institucionProcedencia=\""
            + "\" idTipoEstudioAntecedente=\""
            + "\" tipoEstudioAntecedente=\""
            + "\" idEntidadFederativa=\""
            + "\" entidadFederativa=\""
            + "\" fechaTerminacion=\"" + "\" />",
                
        "\n</TituloElectronico>"
        };
        
       for(String dato: datos) {
           fileWriter.write(dato);
       }
        
        fileWriter.close();
    }
    
    private void createExceptionAlert(String errorMessage) {
        JOptionPane.showMessageDialog(rootPane, errorMessage);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel2 = new javax.swing.JLabel();
        jTextField3 = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        Matricula_jTextField = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        studentName_jTextField = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jTextField4 = new javax.swing.JTextField();
        search_jButton = new javax.swing.JButton();
        generate_jButton = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        keyPath_jTextField = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        cerPath_jTextField = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        jPasswordField = new javax.swing.JPasswordField();
        keySelect_jButton = new javax.swing.JButton();
        cerSelect_jButton = new javax.swing.JButton();

        jLabel2.setText("jLabel2");

        jTextField3.setText("jTextField1");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Datos del alumno"));

        jLabel1.setText("Matricula");

        jLabel3.setText("Nombre del alumno");

        jLabel4.setText("Matricula");

        search_jButton.setText("Buscar");
        search_jButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                search_jButtonActionPerformed(evt);
            }
        });

        generate_jButton.setBackground(new java.awt.Color(204, 0, 102));
        generate_jButton.setText("Generar Titulo");
        generate_jButton.setEnabled(false);
        generate_jButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                generate_jButtonActionPerformed(evt);
            }
        });

        jButton1.setText("jButton1");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel3))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel4))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(168, 168, 168)
                                .addComponent(search_jButton)))
                        .addGap(0, 228, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(Matricula_jTextField))
                            .addComponent(studentName_jTextField)
                            .addComponent(jTextField4)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButton1)
                        .addGap(102, 102, 102)
                        .addComponent(generate_jButton)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Matricula_jTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addGap(18, 18, 18)
                .addComponent(search_jButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(studentName_jTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(generate_jButton)
                    .addComponent(jButton1))
                .addContainerGap(20, Short.MAX_VALUE))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Datos del Firmante"));

        jLabel5.setText(".key ");

        jLabel6.setText(".cer");

        jLabel7.setText("Contraseña");

        keySelect_jButton.setText("Select");
        keySelect_jButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                keySelect_jButtonActionPerformed(evt);
            }
        });

        cerSelect_jButton.setText("Select");
        cerSelect_jButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cerSelect_jButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel5)
                            .addComponent(jLabel6))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(cerPath_jTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 352, Short.MAX_VALUE)
                            .addComponent(keyPath_jTextField))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(keySelect_jButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(cerSelect_jButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPasswordField)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel5)
                        .addComponent(keyPath_jTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(keySelect_jButton, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addGap(26, 26, 26)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(cerPath_jTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cerSelect_jButton, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(jPasswordField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(18, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(40, 40, 40)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(54, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(38, Short.MAX_VALUE)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(60, 60, 60))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void search_jButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_search_jButtonActionPerformed

        // If the Student's id to search is Empty
        String studentID = Matricula_jTextField.getText();
        if (studentID.isEmpty()) {
            JOptionPane.showMessageDialog(rootPane, "No sé ingreso ningún número de matricula para buscar");
        }

        try {
            ps = conn.prepareStatement("SELECT * FROM alumno  WHERE Matricula =" + studentID);
            res = ps.executeQuery();
            if (res.next()) {
                //Enabling the Generate button
                if(hasChoosenKey && hasChoosenCer) {
                    generate_jButton.setEnabled(true);
                }

                //TODO:- Get the responsable Folio!!!!!!
                nombre = res.getString("Nombre");
                a_paterno = res.getString("Apellido_paterno");
                a_materno = res.getString("Apellido_materno");
                correo = res.getString("Correo");

                String folio_control = "000001";

                String fullName = nombre + " " + a_paterno + " " + a_materno;
                //String folio_control = res.getString("folio_control");
                curp = res.getString("CURP");
                String id_carrera = res.getString("Carrera");
                
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                           
                F_INICIO = formatter.format(res.getDate("Fecha inicio"));
                F_FIN = formatter.format(res.getDate("Fecha fin"));
                F_EXAMEN = formatter.format(res.getDate("Fecha examen"));
                

                String SS = res.getString("Servicio social");

                // TODO:- Get this from the carrer DB
                String ncarrera = "";
                String REVOE = "";
                
                //HERE YOU SET THE jTEXTFIELDS with the findings!!
                studentName_jTextField.setText(fullName);
                jTextField4.setText(res.getString("Matricula"));

                String ESTU_ANT = "";
                String FT_ESTANT = "";
                String Titulación = "";

                cadenaOriginal = "||1.0|" + folio_control + "|" + RFC_FIRMANTE + "|5|rector|mtro|70047|INSTITUTO TECNOLÓGICO DE COMITÁN (I.T.A. NO. 31 DE COMITAN, CHIS|" + id_carrera + p + ncarrera + "|" + p + F_INICIO + p + F_FIN + "|1|RVOE FEDERAL|" + REVOE + p + curp + p + "|" + nombre + p + a_paterno + p + a_materno + p + correo + "|2018-06-05|" + Titulación + "|portesis||" + F_EXAMEN + "||CUMPLIO SERVICIO SOCIAL|" + SS + "|ART. 55 LRART. 5 CONST|9|CIUDAD DE MÉXICO|" + UNIVERSITY_NAME + "|4|" + ESTU_ANT + "|4|CIUDAD DE MÉXICO||" + FT_ESTANT + "|||";

            } else {
                JOptionPane.showMessageDialog(null, "No Existen Datos");
            }

        } catch (SQLException ex) {
            Logger.getLogger(JFT_Generador_Titulos.class.getName()).log(Level.SEVERE, null, ex);
        }

    }//GEN-LAST:event_search_jButtonActionPerformed


    private void generate_jButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_generate_jButtonActionPerformed
        String password = String.valueOf(jPasswordField.getPassword());

        if (cadenaOriginal.isEmpty()) {
            JOptionPane.showMessageDialog(rootPane, "No se ha generado la cadena original!!");
        } else if (password.isEmpty()) {
            JOptionPane.showMessageDialog(rootPane, "No se ha introduccido la contraseña para el encriptado");
        }

        try {         
            certificadoResponsable = Base64.encodeBase64String(toByteArray(cer));                        
            sello = sign(password, cadenaOriginal);
            noCertificadoResponsable = getCertifiyerSign(cer);
            
            JOptionPane.showMessageDialog(rootPane, "certificadoResponsable: " + certificadoResponsable + "\n sello: " + sello);
            
            promptUserToSelectOutPath();
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(rootPane, "No Se pudo verificar la firma!, Error: " + ex);
        }
    }//GEN-LAST:event_generate_jButtonActionPerformed

    private void keySelect_jButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_keySelect_jButtonActionPerformed
        JFileChooser fileChooser = new JFileChooser();

        FileNameExtensionFilter filter = new FileNameExtensionFilter("extensiones .key", "key");
        fileChooser.addChoosableFileFilter(filter);
        fileChooser.setFileFilter(filter);

        int returnVal = fileChooser.showOpenDialog(jPanel1);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            key = fileChooser.getSelectedFile();
            keyPath_jTextField.setText(key.getAbsolutePath());
            hasChoosenKey = true;
        }
    }//GEN-LAST:event_keySelect_jButtonActionPerformed

    private void cerSelect_jButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cerSelect_jButtonActionPerformed
        JFileChooser fileChooser = new JFileChooser();

        FileNameExtensionFilter filter = new FileNameExtensionFilter("extensiones .cer", "cer");
        fileChooser.addChoosableFileFilter(filter);
        fileChooser.setFileFilter(filter);

        int returnVal = fileChooser.showOpenDialog(jPanel1);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            cer = fileChooser.getSelectedFile();
            cerPath_jTextField.setText(cer.getAbsolutePath());
            hasChoosenCer = true;
        }
    }//GEN-LAST:event_cerSelect_jButtonActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
         try {
            // First gettin' all the responsable data
            String password = String.valueOf(jPasswordField.getPassword());
            certificadoResponsable = Base64.encodeBase64String(toByteArray(cer));                        
            sello = sign(password, "algo de prueba para encriptar");
            noCertificadoResponsable = getCertifiyerSign(cer);
            //Then creating the file!
            promptUserToSelectOutPath();
         }  catch (Exception ex) {
             createExceptionAlert(ex.getLocalizedMessage());
         }
    }//GEN-LAST:event_jButton1ActionPerformed

    /**
     * @param args the command line arguments
     *
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
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(JFT_Generador_Titulos.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(JFT_Generador_Titulos.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(JFT_Generador_Titulos.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(JFT_Generador_Titulos.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new JFT_Generador_Titulos().setVisible(true);
            }
        });
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField Matricula_jTextField;
    private javax.swing.JTextField cerPath_jTextField;
    private javax.swing.JButton cerSelect_jButton;
    private javax.swing.JButton generate_jButton;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPasswordField jPasswordField;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField4;
    private javax.swing.JTextField keyPath_jTextField;
    private javax.swing.JButton keySelect_jButton;
    private javax.swing.JButton search_jButton;
    private javax.swing.JTextField studentName_jTextField;
    // End of variables declaration//GEN-END:variables
}
