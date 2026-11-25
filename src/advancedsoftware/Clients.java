package advancedsoftware;

import java.sql.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class Clients extends JPanel {

    private Connection con = MainFrame.con;
    private JTable tableClients;
    private DefaultTableModel model;
    private JTextField txtSearch, txtCompany, txtContact, txtPhone, txtEmail;
    private JButton btnSave, btnDelete, btnDoInsert, btnClear;
    private JPanel formPanel;
    private int selectedClientId = -1;

    public Clients() {
        // إعداد التصميم العام
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(240, 240, 240));
        
        createToolbar();
        createTable();
        createForm();
        
        fetchData();
        setFormMode("ADD");
    }

    private void createToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.setBackground(new Color(212, 208, 200));
        
        toolbar.add(new JLabel("Search (Company/Contact):"));
        txtSearch = new JTextField(20);
        toolbar.add(txtSearch);
        
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { search(); }
            public void removeUpdate(DocumentEvent e) { search(); }
            public void changedUpdate(DocumentEvent e) { search(); }
            void search() { searchInDatabase(txtSearch.getText().trim()); }
        });

        add(toolbar, BorderLayout.NORTH);
    }

    private void createTable() {
        // لاحظ هنا أسماء الأعمدة للعرض فقط
        String[] cols = {"ID", "Company Name", "Contact Person", "Phone", "Email"};
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        
        tableClients = new JTable(model);
        tableClients.setRowHeight(30);
        tableClients.setSelectionBackground(new Color(200, 220, 255));
        tableClients.setSelectionForeground(Color.BLACK);
        tableClients.setGridColor(new Color(230, 230, 230));
        tableClients.getTableHeader().setFont(new Font("Ubuntu", Font.BOLD, 12));
        
        tableClients.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int row = tableClients.getSelectedRow();
                if (row != -1) {
                    selectedClientId = (int) tableClients.getValueAt(row, 0);
                    txtCompany.setText(tableClients.getValueAt(row, 1).toString());
                    txtContact.setText(tableClients.getValueAt(row, 2).toString());
                    txtPhone.setText(tableClients.getValueAt(row, 3).toString());
                    txtEmail.setText(tableClients.getValueAt(row, 4).toString());
                    setFormMode("EDIT");
                }
            }
        });

        add(new JScrollPane(tableClients), BorderLayout.CENTER);
    }

    private void createForm() {
        formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(new Color(252, 252, 252));
        updateFormBorder("Add New Client");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addFormField("Company Name:", txtCompany = new JTextField(15), gbc, 0, 0);
        addFormField("Contact Person:", txtContact = new JTextField(15), gbc, 1, 0);
        addFormField("Phone:", txtPhone = new JTextField(15), gbc, 0, 1);
        addFormField("Email:", txtEmail = new JTextField(15), gbc, 1, 1);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setOpaque(false);
        
        btnDoInsert = createBtn("Add Client", new Color(40, 167, 69));
        btnSave = createBtn("Save Changes", new Color(0, 123, 255));
        btnDelete = createBtn("Delete", new Color(220, 53, 69));
        btnClear = createBtn("Clear", new Color(108, 117, 125));

        btnPanel.add(btnDoInsert);
        btnPanel.add(btnSave);
        btnPanel.add(btnDelete);
        btnPanel.add(btnClear);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        formPanel.add(btnPanel, gbc);

        btnDoInsert.addActionListener(e -> doInsert());
        btnSave.addActionListener(e -> doUpdate());
        btnDelete.addActionListener(e -> doDelete());
        btnClear.addActionListener(e -> { setFormMode("ADD"); clearFields(); });

        add(formPanel, BorderLayout.SOUTH);
    }

    private void addFormField(String label, JTextField txt, GridBagConstraints gbc, int x, int y) {
        gbc.gridx = x * 2; gbc.gridy = y; gbc.gridwidth = 1;
        formPanel.add(new JLabel(label), gbc);
        gbc.gridx = x * 2 + 1;
        formPanel.add(txt, gbc);
    }

    private JButton createBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(new Font("Ubuntu", Font.BOLD, 12));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void updateFormBorder(String title) {
        formPanel.setBorder(BorderFactory.createTitledBorder(
            new LineBorder(new Color(200, 200, 200)), title, 
            TitledBorder.RIGHT, TitledBorder.TOP, 
            new Font("Ubuntu", Font.BOLD, 14), new Color(0, 51, 153)));
    }

    // --- Logic المصحح بناءً على ملف الـ SQL ---

    public void fetchData() {
        model.setRowCount(0);
        try {
            // استخدام SELECT * آمن هنا طالما بنجيب بالأسماء الصح
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM CLIENTS");
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("client_id"), 
                    rs.getString("company_name"), 
                    rs.getString("contact_person"), // تم التعديل من contact_name لـ contact_person
                    rs.getString("phone"), 
                    rs.getString("email")
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void searchInDatabase(String q) {
        model.setRowCount(0);
        try {
            // تم تعديل أسماء الأعمدة في البحث أيضاً
            String sql = "SELECT * FROM CLIENTS WHERE company_name LIKE ? OR contact_person LIKE ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, "%"+q+"%"); 
            ps.setString(2, "%"+q+"%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("client_id"), 
                    rs.getString("company_name"), 
                    rs.getString("contact_person"), 
                    rs.getString("phone"), 
                    rs.getString("email")
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void doInsert() {
        try {
            String sql = "INSERT INTO CLIENTS (company_name, contact_person, phone, email) VALUES (?,?,?,?)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, txtCompany.getText()); 
            ps.setString(2, txtContact.getText());
            ps.setString(3, txtPhone.getText());   
            ps.setString(4, txtEmail.getText());
            ps.executeUpdate();
            fetchData(); clearFields();
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Insert Error: " + e.getMessage()); }
    }

    private void doUpdate() {
        try {
            String sql = "UPDATE CLIENTS SET company_name=?, contact_person=?, phone=?, email=? WHERE client_id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, txtCompany.getText()); 
            ps.setString(2, txtContact.getText());
            ps.setString(3, txtPhone.getText());   
            ps.setString(4, txtEmail.getText());
            ps.setInt(5, selectedClientId);
            ps.executeUpdate();
            fetchData(); setFormMode("ADD");
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Update Error: " + e.getMessage()); }
    }

    private void doDelete() {
        if(JOptionPane.showConfirmDialog(this, "Delete client?") == 0) {
            try {
                PreparedStatement ps = con.prepareStatement("DELETE FROM CLIENTS WHERE client_id=?");
                ps.setInt(1, selectedClientId);
                ps.executeUpdate();
                fetchData(); setFormMode("ADD");
            } catch (Exception e) { 
                JOptionPane.showMessageDialog(this, "Error: Check if this client has active projects!"); 
            }
        }
    }

    private void setFormMode(String mode) {
        boolean isAdd = mode.equals("ADD");
        btnDoInsert.setVisible(isAdd);
        btnSave.setVisible(!isAdd);
        btnDelete.setVisible(!isAdd);
        updateFormBorder(isAdd ? "Add New Client" : "Edit Client Details");
        formPanel.revalidate();
        formPanel.repaint();
    }

    private void clearFields() {
        txtCompany.setText(""); txtContact.setText(""); txtPhone.setText(""); txtEmail.setText("");
        selectedClientId = -1;
    }
}
