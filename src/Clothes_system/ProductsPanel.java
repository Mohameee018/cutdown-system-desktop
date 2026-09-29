package Clothes_system;

import Clothes_system.cloud.CutdownCloudSyncService;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;

public class ProductsPanel extends JPanel {

    // =========================================================
    // SHARED PRODUCT TABLE MODEL
    // Used by OrdersPanel / ReturnsPanel / InventoryPanel
    // =========================================================

    private static DefaultTableModel productModel;

    public static DefaultTableModel getProductModel() {
        return productModel;
    }

    private static final Color BG = new Color(248,250,249);
    private static final Color WHITE = Color.WHITE;
    private static final Color DARK = new Color(25,45,45);
    private static final Color PRIMARY = new Color(25,115,105);
    private static final Color BORDER = new Color(225,232,232);

    private final ProductManager manager = ProductManager.getInstance();
    private JTable table;
    private DefaultTableModel model;
    private TableRowSorter<DefaultTableModel> sorter;
    private JTextField search;

    public ProductsPanel() {
        setBackground(BG);
        setLayout(new BorderLayout());
        buildUI();
        refreshTable();
    }

    private void buildUI() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG);
        header.setBorder(new EmptyBorder(20,30,15,30));

        JPanel titleBox = new JPanel();
        titleBox.setBackground(BG);
        titleBox.setLayout(new BoxLayout(titleBox,BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Products");
        title.setFont(new Font("Arial",Font.BOLD,24));
        title.setForeground(DARK);
        JLabel sub = new JLabel("Manage your clothing products");
        sub.setFont(new Font("Arial",Font.PLAIN,12));
        sub.setForeground(new Color(140,150,150));
        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(5));
        titleBox.add(sub);
        header.add(titleBox,BorderLayout.WEST);

        JButton add = button("+ Add Product",PRIMARY,Color.WHITE);
        add.addActionListener(e -> showProductDialog(null));
        header.add(add,BorderLayout.EAST);
        add(header,BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(BG);
        content.setBorder(new EmptyBorder(0,30,30,30));

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBackground(WHITE);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),new EmptyBorder(12,15,12,15)));
        search = textField();
        search.setPreferredSize(new Dimension(320,36));
        search.setToolTipText("Search by name, SKU or category");
        toolbar.add(search,BorderLayout.WEST);
        JLabel hint = new JLabel("Double-click a product to edit");
        hint.setFont(new Font("Arial",Font.PLAIN,11));
        hint.setForeground(new Color(140,150,150));
        toolbar.add(hint,BorderLayout.EAST);
        content.add(toolbar,BorderLayout.NORTH);

        String[] cols = {"Image","Product ID","SKU","Product Name","Category","Size","Color","Selling Price","Cost Price","Stock","Status","Action"};
        model = new DefaultTableModel(new Object[0][cols.length],cols) {
            public boolean isCellEditable(int r,int c){return false;}
            public Class<?> getColumnClass(int c){
                if(c==0)return ImageIcon.class;
                if(c==9)return Integer.class;
                return String.class;
            }
        };

        productModel = model;

        table = new JTable(model);
        table.setRowHeight(58);
        table.setFont(new Font("Arial",Font.PLAIN,12));
        table.setForeground(new Color(50,65,65));
        table.setGridColor(new Color(235,240,240));
        table.setSelectionBackground(new Color(230,243,241));
        table.setSelectionForeground(new Color(25,80,75));
        table.getTableHeader().setFont(new Font("Arial",Font.BOLD,12));
        table.getTableHeader().setForeground(new Color(70,85,85));
        table.getTableHeader().setBackground(new Color(245,248,248));
        table.getTableHeader().setPreferredSize(new Dimension(0,42));

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for(int i=0;i<cols.length;i++) if(i!=3)table.getColumnModel().getColumn(i).setCellRenderer(center);
        table.getColumnModel().getColumn(0).setCellRenderer(new ImageRenderer());
        table.getColumnModel().getColumn(11).setCellRenderer(new ActionRenderer());

        int[] widths={60,85,80,170,100,55,75,100,95,55,100,110};
        for(int i=0;i<widths.length;i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        content.add(new JScrollPane(table),BorderLayout.CENTER);
        add(content,BorderLayout.CENTER);

        search.getDocument().addDocumentListener(new DocumentListener(){
            private void filter(){
                String q=search.getText().trim();
                if(q.isEmpty()) sorter.setRowFilter(null);
                else sorter.setRowFilter(RowFilter.regexFilter("(?i)"+java.util.regex.Pattern.quote(q),1,2,3,4,5,6,10));
            }
            public void insertUpdate(DocumentEvent e){filter();}
            public void removeUpdate(DocumentEvent e){filter();}
            public void changedUpdate(DocumentEvent e){filter();}
        });

        table.addMouseListener(new java.awt.event.MouseAdapter(){
            public void mouseClicked(java.awt.event.MouseEvent e){
                if(e.getClickCount()==2 && SwingUtilities.isLeftMouseButton(e)){
                    int vr=table.rowAtPoint(e.getPoint());
                    if(vr>=0){
                        int mr=table.convertRowIndexToModel(vr);
                        Product p=manager.findById(model.getValueAt(mr,1).toString());
                        if(p!=null)showProductDialog(p);
                    }
                }
            }
        });
    }

    public void setSearchQuery(String query) { if (search != null) search.setText(query == null ? "" : query.trim()); }

    public void refreshData(){ refreshTable(); }

    private void refreshTable(){
        if(model==null)return;
        model.setRowCount(0);
        List<Product> products=manager.getAllProducts();
        for(Product p:products){
            String size="",color="";
            if(!p.getVariants().isEmpty()){
                Product.ProductVariant v=p.getVariants().get(0);
                size=v.getSize(); color=v.getColor();
            }
            model.addRow(new Object[]{imageFor(p),p.getId(),p.getSku(),p.getName(),p.getCategory(),size,color,
                    money(p.getSellingPrice()),money(p.getCostPrice()),p.getStockQuantity(),
                    p.isActive()?p.getStockStatus():"Inactive","Edit / Manage"});
        }
    }

    private void showProductDialog(Product existing){
        boolean edit=existing!=null;
        JDialog d=new JDialog(SwingUtilities.getWindowAncestor(this),edit?"Edit Product":"Add New Product",Dialog.ModalityType.APPLICATION_MODAL);
        d.setSize(650,700); d.setLocationRelativeTo(this); d.setResizable(false);

        JPanel main=new JPanel(new BorderLayout()); main.setBackground(WHITE);
        JPanel head=new JPanel(); head.setBackground(new Color(10,45,45));
        head.setLayout(new BoxLayout(head,BoxLayout.Y_AXIS)); head.setBorder(new EmptyBorder(20,25,20,25));
        JLabel t=new JLabel(edit?"Edit Product":"Add New Product"); t.setFont(new Font("Arial",Font.BOLD,21)); t.setForeground(Color.WHITE);
        JLabel st=new JLabel(edit?"Update product information":"Enter the product information below"); st.setFont(new Font("Arial",Font.PLAIN,12)); st.setForeground(new Color(190,215,210));
        head.add(t);head.add(Box.createVerticalStrut(5));head.add(st);main.add(head,BorderLayout.NORTH);

        JPanel form=new JPanel(new GridBagLayout()); form.setBackground(WHITE); form.setBorder(new EmptyBorder(18,30,10,30));
        GridBagConstraints g=new GridBagConstraints();g.insets=new Insets(5,5,5,5);g.fill=GridBagConstraints.HORIZONTAL;g.weightx=1;

        JTextField sku=textField(),name=textField(),desc=textField(),price=textField(),cost=textField(),color=textField();
        JComboBox<String> size=new JComboBox<>(new String[]{"One Size","XS","S","M","L","XL","XXL"}); styleCombo(size);
        JSpinner stock=new JSpinner(new SpinnerNumberModel(0,0,1000000,1)); styleSpinner(stock);
        JSpinner min=new JSpinner(new SpinnerNumberModel(5,0,1000000,1)); styleSpinner(min);

        // ============================================================
        // CATEGORY: dropdown of existing categories (still editable
        // so a brand-new category can be typed), plus a live list of
        // products already in that category so duplicates are easy
        // to spot and existing products are one click away to edit.
        // ============================================================
        java.util.List<String> existingCategories=new java.util.ArrayList<>();
        for(Product p:manager.getAllProducts()){
            String c=p.getCategory();
            if(c!=null && !c.isBlank() && !existingCategories.contains(c)) existingCategories.add(c);
        }
        java.util.Collections.sort(existingCategories,String.CASE_INSENSITIVE_ORDER);
        JComboBox<String> category=new JComboBox<>(existingCategories.toArray(new String[0]));
        category.setEditable(true); styleCombo(category);

        DefaultListModel<Product> categoryListModel=new DefaultListModel<>();
        JList<Product> categoryList=new JList<>(categoryListModel);
        categoryList.setVisibleRowCount(3);
        categoryList.setFont(new Font("Arial",Font.PLAIN,12));
        categoryList.setCellRenderer(new DefaultListCellRenderer(){
            public Component getListCellRendererComponent(JList<?> l,Object v,int i,boolean s,boolean f){
                super.getListCellRendererComponent(l,v,i,s,f);
                Product pr=(Product)v;
                setText(pr.getName()+"   ("+ (pr.getSku().isEmpty()?pr.getId():pr.getSku()) +")  — "+pr.getStockQuantity()+" in stock");
                return this;
            }
        });
        JScrollPane categoryListScroll=new JScrollPane(categoryList);
        categoryListScroll.setPreferredSize(new Dimension(250,70));
        JLabel categoryListHint=new JLabel("Double-click a product below to open it instead of creating a duplicate:");
        categoryListHint.setFont(new Font("Arial",Font.PLAIN,10));
        categoryListHint.setForeground(new Color(140,150,150));
        JPanel categoryListPanel=new JPanel(new BorderLayout(0,4));
        categoryListPanel.setBackground(WHITE);
        categoryListPanel.add(categoryListHint,BorderLayout.NORTH);
        categoryListPanel.add(categoryListScroll,BorderLayout.CENTER);

        final String[] editingProductId={edit?existing.getId():null};
        final JDialog[] dialogRef=new JDialog[1];
        dialogRef[0]=d;

        Runnable refreshCategoryList=()->{
            categoryListModel.clear();
            Object sel=category.getEditor().getItem();
            String text=sel==null?"":sel.toString().trim();
            if(text.isEmpty()) return;
            for(Product p:manager.getAllProducts()){
                if(!p.getCategory().equalsIgnoreCase(text)) continue;
                if(editingProductId[0]!=null && p.getId().equals(editingProductId[0])) continue;
                categoryListModel.addElement(p);
            }
        };
        category.addActionListener(e->refreshCategoryList.run());
        ((JTextField)category.getEditor().getEditorComponent()).getDocument().addDocumentListener(new DocumentListener(){
            public void insertUpdate(DocumentEvent e){refreshCategoryList.run();}
            public void removeUpdate(DocumentEvent e){refreshCategoryList.run();}
            public void changedUpdate(DocumentEvent e){refreshCategoryList.run();}
        });
        categoryList.addMouseListener(new java.awt.event.MouseAdapter(){
            public void mouseClicked(java.awt.event.MouseEvent e){
                if(e.getClickCount()==2){
                    Product chosen=categoryList.getSelectedValue();
                    if(chosen!=null && dialogRef[0]!=null){
                        dialogRef[0].dispose();
                        showProductDialog(chosen);
                    }
                }
            }
        });

        JLabel preview=new JLabel();preview.setHorizontalAlignment(SwingConstants.CENTER);preview.setPreferredSize(new Dimension(105,105));preview.setBorder(BorderFactory.createLineBorder(BORDER));
        final String[] image={edit?existing.getImagePath():""}; updatePreview(preview,image[0]);
        JButton choose=button("Choose Image",PRIMARY,Color.WHITE), clear=button("Use Default Icon",new Color(240,243,243),new Color(70,80,80));
        choose.addActionListener(e->{JFileChooser fc=new JFileChooser();fc.setDialogTitle("Choose Product Image");fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Image Files","jpg","jpeg","png","gif","webp"));if(fc.showOpenDialog(d)==JFileChooser.APPROVE_OPTION){File f=fc.getSelectedFile();if(validImage(f)){image[0]=f.getAbsolutePath();updatePreview(preview,image[0]);}else JOptionPane.showMessageDialog(d,"Please choose a valid image file.","Invalid Image",JOptionPane.WARNING_MESSAGE);}});
        clear.addActionListener(e->{image[0]="";updatePreview(preview,"");});
        JPanel imgBtns=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));imgBtns.setBackground(WHITE);imgBtns.add(choose);imgBtns.add(clear);
        JPanel img=new JPanel(new BorderLayout(0,8));img.setBackground(WHITE);img.add(preview,BorderLayout.WEST);img.add(imgBtns,BorderLayout.SOUTH);

        if(edit){
            sku.setText(existing.getSku());name.setText(existing.getName());category.setSelectedItem(existing.getCategory());desc.setText(existing.getDescription());
            price.setText(String.valueOf(existing.getSellingPrice()));cost.setText(String.valueOf(existing.getCostPrice()));stock.setValue(existing.getStockQuantity());min.setValue(existing.getMinimumStock());
            if(!existing.getVariants().isEmpty()){Product.ProductVariant v=existing.getVariants().get(0);color.setText(v.getColor());size.setSelectedItem(v.getSize().isEmpty()?"One Size":v.getSize());}
            refreshCategoryList.run();
        }

        sku.setEditable(edit);
        if (!edit) sku.setText(manager.generateProductId().replace("PRD-", "SKU-"));
        row(form,g,0,"Product Image",img);row(form,g,1,"SKU / Code",sku);row(form,g,2,"Product Name *",name);row(form,g,3,"Category *",category);
        row(form,g,4,"",categoryListPanel);row(form,g,5,"Description",desc);
        row(form,g,6,"Selling Price (EGP) *",price);row(form,g,7,"Cost Price (EGP) *",cost);row(form,g,8,"Size",size);row(form,g,9,"Color",color);row(form,g,10,"Stock Quantity",stock);row(form,g,11,"Minimum Stock",min);
        JScrollPane fs=new JScrollPane(form);fs.setBorder(null);fs.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);main.add(fs,BorderLayout.CENTER);

        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT,10,15));buttons.setBackground(WHITE);
        JButton variantsBtn=button("Variants / Colors",new Color(235,242,241),DARK);
        variantsBtn.addActionListener(e->{ if(edit) showVariantsDialog(existing); else JOptionPane.showMessageDialog(d,"Save the product first, then configure its colors, sizes, stock and photos.","Product Variants",JOptionPane.INFORMATION_MESSAGE); });
        buttons.add(variantsBtn);
        JButton cancel=button("Cancel",new Color(240,243,243),new Color(70,80,80));
        JButton save=button(edit?"Save Changes":"Save Product",PRIMARY,Color.WHITE);
        cancel.addActionListener(e->d.dispose());
        save.addActionListener(e->{
            String sSku=sku.getText().trim(),sName=name.getText().trim(),sDesc=desc.getText().trim(),sColor=color.getText().trim();
            Object catSel=category.getEditor().getItem();
            String sCat=catSel==null?"":catSel.toString().trim();
            if(sName.isEmpty()||sCat.isEmpty()||price.getText().trim().isEmpty()||cost.getText().trim().isEmpty()){JOptionPane.showMessageDialog(d,"Please fill in all required fields.","Missing Information",JOptionPane.WARNING_MESSAGE);return;}
            double sp,cp;try{sp=Double.parseDouble(price.getText().replace(",","").trim());cp=Double.parseDouble(cost.getText().replace(",","").trim());}catch(Exception ex){JOptionPane.showMessageDialog(d,"Please enter valid numeric prices.","Invalid Price",JOptionPane.ERROR_MESSAGE);return;}
            if(sp<0||cp<0){JOptionPane.showMessageDialog(d,"Prices cannot be negative.","Invalid Price",JOptionPane.WARNING_MESSAGE);return;}
            if(cp>sp && JOptionPane.showConfirmDialog(d,"Cost price is higher than selling price. Continue?","Check Price",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE)!=JOptionPane.YES_OPTION)return;
            if(!manager.isSkuAvailable(sSku,edit?existing.getId():null)){JOptionPane.showMessageDialog(d,"This SKU is already used by another product.","Duplicate SKU",JOptionPane.WARNING_MESSAGE);return;}
            int q=(Integer)stock.getValue(),mq=(Integer)min.getValue();String sz=size.getSelectedItem().toString();
            Product createdProduct=null;
            if(!edit){
                Product p=new Product(manager.generateProductId(),sSku,sName,sCat,sDesc,image[0],sp,cp,q,mq);
                p.addVariant(new Product.ProductVariant(p.getId()+"-V001",sColor,sz,q));
                if(!manager.addProduct(p)){JOptionPane.showMessageDialog(d,"Could not add the product.","Error",JOptionPane.ERROR_MESSAGE);return;}
                CutdownCloudSyncService.syncProductAsync(p);
                createdProduct=p;
            }else{
                existing.setSku(sSku);existing.setName(sName);existing.setCategory(sCat);existing.setDescription(sDesc);existing.setImagePath(image[0]);existing.setMinimumStock(mq);
                if(Double.compare(existing.getSellingPrice(),sp)!=0||Double.compare(existing.getCostPrice(),cp)!=0)existing.updatePrice(sp,cp,"Product edited");
                if(existing.getStockQuantity()!=q)existing.setStock(q,"Product edited");
                if(existing.getVariants().isEmpty())existing.addVariant(new Product.ProductVariant(existing.getId()+"-V001",sColor,sz,q));
                else{Product.ProductVariant v=existing.getVariants().get(0);v.setColor(sColor);v.setSize(sz);v.setStockQuantity(q);}
                manager.updateProduct(existing);
                CutdownCloudSyncService.syncProductAsync(existing);
            }
            refreshTable();d.dispose();
            JOptionPane.showMessageDialog(this,edit?"Product updated successfully!":"Product added successfully!","Success",JOptionPane.INFORMATION_MESSAGE);
            if(createdProduct!=null) showVariantsDialog(createdProduct);
        });
        if(edit){JButton toggle=button(existing.isActive()?"Deactivate":"Activate",new Color(245,235,235),new Color(130,55,55));toggle.addActionListener(e->{if(existing.isActive()){if(JOptionPane.showConfirmDialog(d,"Deactivate this product? Old orders will remain safe.","Deactivate Product",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION){manager.deactivateProduct(existing.getId());refreshTable();d.dispose();}}else{manager.activateProduct(existing.getId());refreshTable();d.dispose();}});buttons.add(toggle);}
        buttons.add(cancel);buttons.add(save);main.add(buttons,BorderLayout.SOUTH);d.setContentPane(main);d.setVisible(true);
    }

    private void showVariantsDialog(Product product) {
        if(product==null) return;
        JDialog d=new JDialog(SwingUtilities.getWindowAncestor(this),"Variants, Sizes & Color Photos",Dialog.ModalityType.APPLICATION_MODAL);
        d.setSize(850,650); d.setLocationRelativeTo(this);
        JPanel root=new JPanel(new BorderLayout(12,12));root.setBackground(WHITE);root.setBorder(new EmptyBorder(20,20,20,20));

        DefaultTableModel vm=new DefaultTableModel(new Object[]{"Color","Size","Stock"},0){
            public boolean isCellEditable(int r,int col){return true;}
        };
        for(Product.ProductVariant v:product.getVariants()) vm.addRow(new Object[]{v.getColor(),v.getSize(),v.getStockQuantity()});
        JTable vt=new JTable(vm);vt.setRowHeight(34);vt.setFont(new Font("Arial",Font.PLAIN,12));
        vt.getTableHeader().setFont(new Font("Arial",Font.BOLD,12));
        JScrollPane vs=new JScrollPane(vt);
        JPanel top=new JPanel(new BorderLayout(0,8));top.setBackground(WHITE);
        JLabel help=new JLabel("Each row is one exact Color + Size combination. Stock belongs to that combination.");
        help.setForeground(new Color(110,120,120));help.setFont(new Font("Arial",Font.PLAIN,11));top.add(help,BorderLayout.NORTH);top.add(vs,BorderLayout.CENTER);
        root.add(top,BorderLayout.CENTER);

        Map<String,List<String>> imageMap=new LinkedHashMap<>(Clothes_system.db.PersistenceRepository.readProductColorImages(product.getId()));
        JPanel photos=new JPanel(new GridBagLayout());photos.setBackground(WHITE);photos.setBorder(BorderFactory.createTitledBorder("Color photos — up to 3 images per color"));
        GridBagConstraints g=new GridBagConstraints();g.insets=new Insets(5,5,5,5);g.fill=GridBagConstraints.HORIZONTAL;g.weightx=1;
        JComboBox<String> colorPick=new JComboBox<>();colorPick.setEditable(false);styleCombo(colorPick);
        JTextField[] paths={textField(),textField(),textField()};
        for(JTextField f:paths)f.setPreferredSize(new Dimension(360,34));
        Runnable refreshColors=()->{
            String keep=(String)colorPick.getSelectedItem();colorPick.removeAllItems();
            LinkedHashSet<String> colors=new LinkedHashSet<>();
            for(int i=0;i<vm.getRowCount();i++){String cc=String.valueOf(vm.getValueAt(i,0)).trim();if(!cc.isEmpty())colors.add(cc);}
            for(String cc:colors)colorPick.addItem(cc);
            if(keep!=null&&colors.contains(keep))colorPick.setSelectedItem(keep); else if(colorPick.getItemCount()>0)colorPick.setSelectedIndex(0);
        };
        Runnable loadPhotos=()->{
            String color=(String)colorPick.getSelectedItem();List<String> list=color==null?Collections.emptyList():imageMap.getOrDefault(color,Collections.emptyList());
            for(int i=0;i<3;i++)paths[i].setText(i<list.size()?list.get(i):"");
        };
        Runnable saveCurrentPhotos=()->{
            String color=(String)colorPick.getSelectedItem(); if(color==null||color.isBlank()) return;
            List<String> list=new ArrayList<>(); for(JTextField f:paths) if(!f.getText().trim().isEmpty()) list.add(f.getText().trim());
            imageMap.put(color,list);
        };
        colorPick.addActionListener(e->{ if(colorPick.getItemCount()>0) loadPhotos.run(); });
        for(JTextField f:paths) f.getDocument().addDocumentListener(new DocumentListener(){
            public void insertUpdate(DocumentEvent e){saveCurrentPhotos.run();}
            public void removeUpdate(DocumentEvent e){saveCurrentPhotos.run();}
            public void changedUpdate(DocumentEvent e){saveCurrentPhotos.run();}
        });
        g.gridx=0;g.gridy=0;photos.add(new JLabel("Color"),g);g.gridx=1;photos.add(colorPick,g);
        for(int i=0;i<3;i++){final int n=i;g.gridx=0;g.gridy=i+1;photos.add(new JLabel("Image "+(i+1)),g);g.gridx=1;JPanel line=new JPanel(new BorderLayout(6,0));line.setBackground(WHITE);line.add(paths[i],BorderLayout.CENTER);JButton choose=button("Choose",new Color(240,243,243),DARK);choose.addActionListener(e->{JFileChooser fc=new JFileChooser();fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Image Files","jpg","jpeg","png","webp","gif"));if(fc.showOpenDialog(d)==JFileChooser.APPROVE_OPTION)paths[n].setText(fc.getSelectedFile().getAbsolutePath());});line.add(choose,BorderLayout.EAST);photos.add(line,g);}
        g.gridx=1;g.gridy=4;JButton clearPhotos=button("Clear color photos",new Color(245,235,235),new Color(130,55,55));clearPhotos.addActionListener(e->{for(JTextField f:paths)f.setText("");});photos.add(clearPhotos,g);
        JButton add=button("+ Add Variant",PRIMARY,Color.WHITE);add.addActionListener(e->vm.addRow(new Object[]{"","",0}));
        JButton remove=button("Remove Selected",new Color(245,235,235),new Color(130,55,55));remove.addActionListener(e->{int r=vt.getSelectedRow();if(r>=0)vm.removeRow(vt.convertRowIndexToModel(r));refreshColors.run();loadPhotos.run();});
        JButton save=button("Save Variants",PRIMARY,Color.WHITE);
        JButton cancel=button("Cancel",new Color(240,243,243),DARK);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));actions.setBackground(WHITE);actions.add(add);actions.add(remove);actions.add(cancel);actions.add(save);
        JPanel south=new JPanel(new BorderLayout(0,10));south.setBackground(WHITE);south.add(photos,BorderLayout.CENTER);south.add(actions,BorderLayout.SOUTH);root.add(south,BorderLayout.PAGE_END);
        cancel.addActionListener(e->d.dispose());
        save.addActionListener(e->{
            LinkedHashSet<String> keys=new LinkedHashSet<>();List<Product.ProductVariant> next=new ArrayList<>();int total=0;
            for(int i=0;i<vm.getRowCount();i++){
                String color=String.valueOf(vm.getValueAt(i,0)).trim(),size=String.valueOf(vm.getValueAt(i,1)).trim();int stock;
                try{stock=Integer.parseInt(String.valueOf(vm.getValueAt(i,2)).trim());}catch(Exception ex){JOptionPane.showMessageDialog(d,"Stock must be a whole number.","Invalid Stock",JOptionPane.WARNING_MESSAGE);return;}
                if(color.isEmpty()||size.isEmpty()||stock<0){JOptionPane.showMessageDialog(d,"Every variant needs a color, size and non-negative stock.","Invalid Variant",JOptionPane.WARNING_MESSAGE);return;}
                String key=color.toLowerCase(Locale.ROOT)+"|"+size.toLowerCase(Locale.ROOT);if(!keys.add(key)){JOptionPane.showMessageDialog(d,"Duplicate color + size combination: "+color+" / "+size,"Duplicate Variant",JOptionPane.WARNING_MESSAGE);return;}
                next.add(new Product.ProductVariant(product.getId()+"-V"+String.format("%03d",i+1),color,size,stock));total+=stock;
            }
            if(next.isEmpty()){JOptionPane.showMessageDialog(d,"Add at least one variant.","Variants",JOptionPane.WARNING_MESSAGE);return;}
            product.getVariants().clear();for(Product.ProductVariant v:next)product.addVariant(v);
            product.setStock(total,"Variant matrix updated");
            LinkedHashSet<String> colors=new LinkedHashSet<>();for(Product.ProductVariant v:next)colors.add(v.getColor());
            for(String color:colors){List<String> list=new ArrayList<>();for(JTextField f:paths){}}
            // Save photo fields for every color by reading the current color selection.
            saveCurrentPhotos.run();
            // Keep previously saved photos for other colors, while dropping colors no longer used.
            imageMap.keySet().removeIf(k->!colors.contains(k));
            Clothes_system.db.PersistenceRepository.saveProductColorImages(product.getId(),imageMap);
            Clothes_system.db.PersistenceRepository.saveProduct(product);
            CutdownCloudSyncService.syncProductAsync(product);
            JOptionPane.showMessageDialog(d,"Variants, sizes, stock and color photos saved.","Saved",JOptionPane.INFORMATION_MESSAGE);
            d.dispose();refreshTable();
        });
        refreshColors.run();loadPhotos.run();d.setContentPane(root);d.setVisible(true);
    }

    private void row(JPanel p,GridBagConstraints g,int y,String label,JComponent c){g.gridx=0;g.gridy=y;g.weightx=.3;JLabel l=new JLabel(label);l.setFont(new Font("Arial",Font.BOLD,12));l.setForeground(new Color(55,70,70));p.add(l,g);g.gridx=1;g.weightx=.7;p.add(c,g);}
    private JTextField textField(){JTextField f=new JTextField();f.setPreferredSize(new Dimension(250,38));f.setFont(new Font("Arial",Font.PLAIN,13));f.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(215,225,225)),new EmptyBorder(0,10,0,10)));return f;}
    private void styleCombo(JComboBox<String> c){c.setPreferredSize(new Dimension(250,38));c.setFont(new Font("Arial",Font.PLAIN,13));c.setBackground(WHITE);}
    private void styleSpinner(JSpinner s){s.setPreferredSize(new Dimension(250,38));s.setFont(new Font("Arial",Font.PLAIN,13));}
    private JButton button(String t,Color bg,Color fg){JButton b=new JButton(t);b.setFocusPainted(false);b.setFont(new Font("Arial",Font.BOLD,12));b.setForeground(fg);b.setBackground(bg);b.setBorder(new EmptyBorder(10,18,10,18));b.setCursor(new Cursor(Cursor.HAND_CURSOR));return b;}
    private String money(double n){return String.format("EGP %,.2f",n);}

    private ImageIcon imageFor(Product p){
        try{if(p.getImagePath()!=null&&!p.getImagePath().isBlank()){BufferedImage im=ImageIO.read(new File(p.getImagePath()));if(im!=null)return new ImageIcon(scale(im,42,42));}}catch(Exception ignored){}
        return defaultIcon(42);
    }
    private void updatePreview(JLabel l,String path){try{if(path!=null&&!path.isBlank()){BufferedImage im=ImageIO.read(new File(path));if(im!=null){l.setIcon(new ImageIcon(scale(im,95,95)));l.setText("");return;}}}catch(Exception ignored){}l.setIcon(defaultIcon(65));l.setText("Default");}
    private boolean validImage(File f){if(f==null||!f.isFile())return false;String n=f.getName().toLowerCase();return n.endsWith(".jpg")||n.endsWith(".jpeg")||n.endsWith(".png")||n.endsWith(".gif")||n.endsWith(".webp");}
    private Image scale(BufferedImage im,int w,int h){double r=Math.min((double)w/im.getWidth(),(double)h/im.getHeight());return im.getScaledInstance(Math.max(1,(int)(im.getWidth()*r)),Math.max(1,(int)(im.getHeight()*r)),Image.SCALE_SMOOTH);}
    private ImageIcon defaultIcon(int s){BufferedImage im=new BufferedImage(s,s,BufferedImage.TYPE_INT_ARGB);Graphics2D g=im.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setColor(new Color(238,243,242));g.fillRoundRect(0,0,s,s,12,12);g.setColor(PRIMARY);int x=s/2,w=(int)(s*.4),top=(int)(s*.32);g.fillRoundRect(x-w/2,top,w,(int)(s*.43),5,5);g.fillPolygon(new int[]{x-w/2,x-(int)(s*.37),x-(int)(s*.27),x-w/2},new int[]{top+5,(int)(s*.42),(int)(s*.52),(int)(s*.58)},4);g.fillPolygon(new int[]{x+w/2,x+(int)(s*.37),x+(int)(s*.27),x+w/2},new int[]{top+5,(int)(s*.42),(int)(s*.52),(int)(s*.58)},4);g.setColor(new Color(238,243,242));g.fillOval(x-s/10,top-s/20,s/5,s/7);g.dispose();return new ImageIcon(im);}

    private static class ImageRenderer extends DefaultTableCellRenderer{ImageRenderer(){setHorizontalAlignment(SwingConstants.CENTER);setVerticalAlignment(SwingConstants.CENTER);}public void setValue(Object v){setIcon(v instanceof ImageIcon?(ImageIcon)v:null);setText("");}}
    private static class ActionRenderer extends DefaultTableCellRenderer{ActionRenderer(){setHorizontalAlignment(SwingConstants.CENTER);}public void setValue(Object v){setText(v==null?"":v.toString());setForeground(PRIMARY);}}
}
