package advancedsoftware;

import java.sql.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class Tasks extends JPanel {

    private Connection con = MainFrame.con;
    private JTable tableTasks;
    private DefaultTableModel model;
    private JTextField txtSearch, txtTaskName, txtDueDate;
    private JComboBox<String> comboProject, comboEmployee, comboStatus;
    private JButton btnSave, btnDelete, btnDoInsert, btnClear;
    private JPanel formPanel;
    private int selectedTaskId = -1;

    public Tasks() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(240, 240, 240));

        createToolbar();
        createTable();
        createForm();

        fetchData();
        fillCombos();
        setFormMode("ADD");
    }

    private void createToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.setBackground(new Color(212, 208, 200));
        toolbar.add(new JLabel("Search Tasks:"));
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
        String[] cols = {"ID", "Task Name", "Project", "Assigned To", "Due Date", "Status"};
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        tableTasks = new JTable(model);
        tableTasks.setRowHeight(30);
        tableTasks.setSelectionBackground(new Color(200, 220, 255));
        tableTasks.getTableHeader().setFont(new Font("Ubuntu", Font.BOLD, 12));

        tableTasks.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int row = tableTasks.getSelectedRow();
                if (row != -1) {
                    selectedTaskId = (int) tableTasks.getValueAt(row, 0);
                    txtTaskName.setText(tableTasks.getValueAt(row, 1).toString());
                    comboProject.setSelectedItem(tableTasks.getValueAt(row, 2).toString());
                    comboEmployee.setSelectedItem(tableTasks.getValueAt(row, 3).toString());
                    txtDueDate.setText(tableTasks.getValueAt(row, 4).toString());
                    comboStatus.setSelectedItem(tableTasks.getValueAt(row, 5).toString());
                    setFormMode("EDIT");
                }
            }
        });
        add(new JScrollPane(tableTasks), BorderLayout.CENTER);
    }

    private void createForm() {
        formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(new Color(252, 252, 252));
        updateFormBorder("Add New Task");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Fields Initialization
        txtTaskName = new JTextField(15);
        txtDueDate = new JTextField(10); // YYYY-MM-DD
        comboProject = new JComboBox<>();
        comboEmployee = new JComboBox<>();
        comboStatus = new JComboBox<>(new String[]{"Pending", "In Progress", "Completed", "Canceled"});

        addFormField("Task Name:", txtTaskName, gbc, 0, 0);
        addFormField("Due Date (YYYY-MM-DD):", txtDueDate, gbc, 1, 0);
        addFormField("Project:", comboProject, gbc, 0, 1);
        addFormField("Assign To:", comboEmployee, gbc, 1, 1);
        addFormField("Status:", comboStatus, gbc, 0, 2);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setOpaque(false);
        btnDoInsert = createBtn("Add Task", new Color(40, 167, 69));
        btnSave = createBtn("Save Changes", new Color(0, 123, 255));
        btnDelete = createBtn("Delete", new Color(220, 53, 69));
        btnClear = createBtn("Clear", new Color(108, 117, 125));

        btnPanel.add(btnDoInsert); btnPanel.add(btnSave); btnPanel.add(btnDelete); btnPanel.add(btnClear);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        formPanel.add(btnPanel, gbc);

        // Events
        btnDoInsert.addActionListener(e -> doInsert());
        btnSave.addActionListener(e -> doUpdate());
        btnDelete.addActionListener(e -> doDelete());
        btnClear.addActionListener(e -> { setFormMode("ADD"); clearFields(); });

        add(formPanel, BorderLayout.SOUTH);
    }

    private void addFormField(String label, JComponent comp, GridBagConstraints gbc, int x, int y) {
        gbc.gridx = x * 2; gbc.gridy = y; gbc.gridwidth = 1;
        formPanel.add(new JLabel(label), gbc);
        gbc.gridx = x * 2 + 1;
        formPanel.add(comp, gbc);
    }

    private JButton createBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg); b.setForeground(Color.WHITE);
        b.setFocusPainted(false); b.setFont(new Font("Ubuntu", Font.BOLD, 12));
        return b;
    }

    private void updateFormBorder(String title) {
        formPanel.setBorder(BorderFactory.createTitledBorder(new LineBorder(new Color(200, 200, 200)), 
                title, TitledBorder.RIGHT, TitledBorder.TOP, new Font("Ubuntu", Font.BOLD, 14), new Color(0, 51, 153)));
    }

    // --- Logic Functions ---

public void fetchData() {
    model.setRowCount(0);
    try {
        // الـ SQL ده بيضمن ظهور المهمة حتى لو مفيش مشروع أو موظف مربوط بيها
        String sql = "SELECT t.task_id, t.title, p.project_name, e.full_name, t.due_date, t.status " +
                     "FROM TASKS t " +
                     "LEFT JOIN PROJECTS p ON t.project_id = p.project_id " +
                     "LEFT JOIN EMPLOYEES e ON t.assigned_to = e.employee_id";
        
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        
        while (rs.next()) {
            model.addRow(new Object[]{
                rs.getInt("task_id"),
                rs.getString("title"), // تأكدت إن الاسم هنا task_name زي السكريبت
                rs.getString("project_name") == null ? "No Project" : rs.getString("project_name"),
                rs.getString("full_name") == null ? "Not Assigned" : rs.getString("full_name"),
                rs.getDate("due_date"),
                rs.getString("status")
            });
        }
        
        // لو لسه الجدول فاضي، اطبع رسالة في الـ Console عشان نتأكد إن فيه داتا أصلاً
        if (model.getRowCount() == 0) {
            System.out.println("No tasks found in database table 'TASKS'.");
        }
        
    } catch (Exception e) {
        // دي هتطبع لك الخطأ بالتفصيل في الـ Output تحت لو فيه مشكلة في الـ SQL
        e.printStackTrace(); 
        JOptionPane.showMessageDialog(this, "Database Error: " + e.getMessage());
    }
}

    private void fillCombos() {
        try {
            ResultSet rsP = con.createStatement().executeQuery("SELECT project_name FROM PROJECTS");
            while (rsP.next()) comboProject.addItem(rsP.getString("project_name"));
            ResultSet rsE = con.createStatement().executeQuery("SELECT full_name FROM EMPLOYEES");
            while (rsE.next()) comboEmployee.addItem(rsE.getString("full_name"));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void doInsert() {
        try {
            String sql = "INSERT INTO TASKS (task_name, project_id, assigned_to, due_date, status) VALUES (?, " +
                         "(SELECT project_id FROM PROJECTS WHERE project_name=?), " +
                         "(SELECT employee_id FROM EMPLOYEES WHERE full_name=?), ?, ?)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, txtTaskName.getText());
            ps.setString(2, comboProject.getSelectedItem().toString());
            ps.setString(3, comboEmployee.getSelectedItem().toString());
            ps.setString(4, txtDueDate.getText());
            ps.setString(5, comboStatus.getSelectedItem().toString());
            ps.executeUpdate();
            fetchData(); clearFields();
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
    }

    private void doUpdate() {
        try {
            String sql = "UPDATE TASKS SET task_name=?, project_id=(SELECT project_id FROM PROJECTS WHERE project_name=?), " +
                         "assigned_to=(SELECT employee_id FROM EMPLOYEES WHERE full_name=?), due_date=?, status=? WHERE task_id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, txtTaskName.getText());
            ps.setString(2, comboProject.getSelectedItem().toString());
            ps.setString(3, comboEmployee.getSelectedItem().toString());
            ps.setString(4, txtDueDate.getText());
            ps.setString(5, comboStatus.getSelectedItem().toString());
            ps.setInt(6, selectedTaskId);
            ps.executeUpdate();
            fetchData(); setFormMode("ADD");
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Update Error: " + e.getMessage()); }
    }

    private void doDelete() {
        if(JOptionPane.showConfirmDialog(this, "Delete task?") == 0) {
            try {
                PreparedStatement ps = con.prepareStatement("DELETE FROM TASKS WHERE task_id=?");
                ps.setInt(1, selectedTaskId);
                ps.executeUpdate();
                fetchData(); setFormMode("ADD");
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void searchInDatabase(String q) {
        model.setRowCount(0);
        try {
            String sql = "SELECT t.task_id, t.task_name, p.project_name, e.full_name, t.due_date, t.status " +
                         "FROM TASKS t LEFT JOIN PROJECTS p ON t.project_id = p.project_id " +
                         "LEFT JOIN EMPLOYEES e ON t.assigned_to = e.employee_id " +
                         "WHERE t.task_name LIKE ? OR p.project_name LIKE ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, "%"+q+"%"); ps.setString(2, "%"+q+"%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{rs.getInt("task_id"), rs.getString("task_name"), 
                    rs.getString("project_name"), rs.getString("full_name"), rs.getDate("due_date"), rs.getString("status")});
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void setFormMode(String mode) {
        boolean isAdd = mode.equals("ADD");
        btnDoInsert.setVisible(isAdd);
        btnSave.setVisible(!isAdd);
        btnDelete.setVisible(!isAdd);
        updateFormBorder(isAdd ? "Add New Task" : "Edit Task Details");
    }

    private void clearFields() {
        txtTaskName.setText(""); txtDueDate.setText("");
        selectedTaskId = -1;
    }
}
