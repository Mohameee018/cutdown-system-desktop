package Clothes_system;

import Clothes_system.db.PersistenceRepository;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;

public class SettingsPanel extends JPanel {
    private final JTextField name=new JTextField(), type=new JTextField(), phone=new JTextField(), address=new JTextField(), logo=new JTextField();
    private final JTextArea footer=new JTextArea(), policy=new JTextArea();
    private final JComboBox<String> printer=new JComboBox<>(), paper=new JComboBox<>(new String[]{"A4","58mm","80mm"});
    private final JCheckBox auto=new JCheckBox("Auto Print after order");
    private final JButton primaryColorButton=new JButton("Choose Primary Color"), sidebarColorButton=new JButton("Choose Sidebar Color");
    private final JButton backupButton=new JButton("Backup Database..."), restoreButton=new JButton("Restore Database...");

    public SettingsPanel(){
        setLayout(new BorderLayout()); setBackground(new Color(248,250,249));
        JLabel title=new JLabel("Settings"); title.setFont(new Font("Segoe UI",Font.BOLD,28));
        JPanel head=new JPanel(new BorderLayout()); head.setOpaque(false); head.setBorder(BorderFactory.createEmptyBorder(25,30,15,30)); head.add(title,BorderLayout.WEST); add(head,BorderLayout.NORTH);
        JPanel form=new JPanel(new GridBagLayout()); form.setBackground(Color.WHITE); form.setBorder(BorderFactory.createEmptyBorder(25,30,25,30));
        addRow(form,0,"Store Name",name); addRow(form,1,"Store Type",type); addRow(form,2,"Store Phone",phone); addRow(form,3,"Store Address",address);
        JPanel logoPanel=new JPanel(new BorderLayout(5,0)); logoPanel.setOpaque(false); logoPanel.add(logo,BorderLayout.CENTER); JButton browse=new JButton("Browse"); logoPanel.add(browse,BorderLayout.EAST); addRow(form,4,"Store Logo",logoPanel);
        addArea(form,5,"Invoice Footer",footer); addArea(form,6,"Return Policy",policy);
        addRow(form,7,"Printer",printer); addRow(form,8,"Paper Size",paper); addRow(form,9,"Primary Color",primaryColorButton); addRow(form,10,"Sidebar Color",sidebarColorButton); addRow(form,11,"",auto);
        JPanel dataSafety=new JPanel(new FlowLayout(FlowLayout.LEFT,10,0)); dataSafety.setOpaque(false); dataSafety.add(backupButton); dataSafety.add(restoreButton);
        addRow(form,12,"Database",dataSafety);
        JButton save=new JButton("Save Settings"); save.setBackground(SettingsManager.getPrimaryColor()); save.setForeground(Color.WHITE); save.setFocusPainted(false);
        save.addActionListener(e->save());
        browse.addActionListener(e->{JFileChooser c=new JFileChooser(); if(c.showOpenDialog(this)==JFileChooser.APPROVE_OPTION)logo.setText(c.getSelectedFile().getAbsolutePath());});
        primaryColorButton.addActionListener(e->chooseColor(true)); sidebarColorButton.addActionListener(e->chooseColor(false));
        backupButton.addActionListener(e->doBackup()); restoreButton.addActionListener(e->doRestore());
        JPanel south=new JPanel(new FlowLayout(FlowLayout.RIGHT)); south.setOpaque(false); south.add(save); add(new JScrollPane(form),BorderLayout.CENTER); add(south,BorderLayout.SOUTH);
        load();
    }
    private void chooseColor(boolean primary){Color current=primary?SettingsManager.getPrimaryColor():SettingsManager.getSidebarColor(); Color c=JColorChooser.showDialog(this,primary?"Choose Primary Color":"Choose Sidebar Color",current); if(c!=null){(primary?primaryColorButton:sidebarColorButton).setBackground(c); (primary?primaryColorButton:sidebarColorButton).setForeground(contrast(c));}}
    private Color contrast(Color c){int y=(int)(0.299*c.getRed()+0.587*c.getGreen()+0.114*c.getBlue());return y<150?Color.WHITE:Color.BLACK;}
    private void addRow(JPanel p,int r,String label,Component c){GridBagConstraints a=new GridBagConstraints();a.gridx=0;a.gridy=r;a.insets=new Insets(8,8,8,15);a.anchor=GridBagConstraints.WEST;p.add(new JLabel(label),a);GridBagConstraints b=new GridBagConstraints();b.gridx=1;b.gridy=r;b.weightx=1;b.fill=GridBagConstraints.HORIZONTAL;b.insets=new Insets(8,8,8,8);p.add(c,b);}
    private void addArea(JPanel p,int r,String label,JTextArea a){a.setRows(3);a.setLineWrap(true);a.setWrapStyleWord(true);addRow(p,r,label,new JScrollPane(a));}
    private void load(){name.setText(SettingsManager.getStoreName());type.setText(SettingsManager.getStoreType());phone.setText(SettingsManager.getStorePhone());address.setText(SettingsManager.getStoreAddress());logo.setText(SettingsManager.getLogoPath());footer.setText(SettingsManager.getInvoiceFooter());policy.setText(SettingsManager.getReturnPolicy());printer.addItem(""); for(javax.print.PrintService ps:javax.print.PrintServiceLookup.lookupPrintServices(null,null))printer.addItem(ps.getName()); printer.setSelectedItem(SettingsManager.getPrinter()); paper.setSelectedItem(SettingsManager.getPaperSize()); auto.setSelected(SettingsManager.isAutoPrint()); primaryColorButton.setBackground(SettingsManager.getPrimaryColor());primaryColorButton.setForeground(contrast(SettingsManager.getPrimaryColor())); sidebarColorButton.setBackground(SettingsManager.getSidebarColor());sidebarColorButton.setForeground(contrast(SettingsManager.getSidebarColor()));}
    private void save(){SettingsManager.setStoreName(name.getText());SettingsManager.setStoreType(type.getText());SettingsManager.setStorePhone(phone.getText());SettingsManager.setStoreAddress(address.getText());SettingsManager.setLogoPath(logo.getText());SettingsManager.setInvoiceFooter(footer.getText());SettingsManager.setReturnPolicy(policy.getText());SettingsManager.setPrinter(String.valueOf(printer.getSelectedItem()));SettingsManager.setPaperSize(String.valueOf(paper.getSelectedItem()));SettingsManager.setAutoPrint(auto.isSelected());SettingsManager.setPrimaryColor(primaryColorButton.getBackground());SettingsManager.setSidebarColor(sidebarColorButton.getBackground());Clothes_system.refreshAllDataViews();Clothes_system.refreshSidebarBranding();JOptionPane.showMessageDialog(this,"Settings saved.","VYRA",JOptionPane.INFORMATION_MESSAGE);}

    // =========================================================
    // DATA SAFETY: Backup / Restore
    // =========================================================

    private void doBackup(){
        String defaultName="clothes_system_backup_"+new SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date())+".db";
        JFileChooser chooser=new JFileChooser();
        chooser.setDialogTitle("Save Database Backup");
        chooser.setSelectedFile(new File(defaultName));
        if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION) return;
        File dest=chooser.getSelectedFile();
        try{
            File written=PersistenceRepository.backupDatabase(dest);
            JOptionPane.showMessageDialog(this,
                "Backup created and verified:\n"+written.getAbsolutePath()
                +"\n\nSize: "+written.length()+" bytes",
                "Backup Complete",JOptionPane.INFORMATION_MESSAGE);
        }catch(Exception ex){
            JOptionPane.showMessageDialog(this,
                "Backup failed: "+ex.getMessage(),
                "Backup Failed",JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doRestore(){
        JFileChooser chooser=new JFileChooser();
        chooser.setDialogTitle("Select Database Backup to Restore");
        if(chooser.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION) return;
        File src=chooser.getSelectedFile();

        int confirm=JOptionPane.showConfirmDialog(this,
            "This will replace the current database with the selected backup.\n"
            +"The current database will first be saved aside as a safety copy.\n"
            +"The application must then be closed and reopened to load the restored data.\n\n"
            +"Continue?",
            "Restore Database",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);
        if(confirm!=JOptionPane.YES_OPTION) return;

        try{
            File restored=PersistenceRepository.restoreDatabase(src);
            Clothes_system.suppressSaveOnNextShutdown();
            JOptionPane.showMessageDialog(this,
                "Database restored from:\n"+src.getAbsolutePath()
                +"\n\nThe application will now close. Please reopen it to load the restored data.",
                "Restore Complete",JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        }catch(Exception ex){
            JOptionPane.showMessageDialog(this,
                "Restore failed, current database was left unchanged (or a safety copy was kept): "+ex.getMessage(),
                "Restore Failed",JOptionPane.ERROR_MESSAGE);
        }
    }
}
