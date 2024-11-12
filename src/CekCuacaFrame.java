/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import org.json.JSONObject;
import java.util.Scanner;

public class CekCuacaFrame extends javax.swing.JFrame {

    private String kota, suhuCuaca, cuaca;
    private List<String> daftarFavorit;
    private DefaultTableModel tableModel;

    public CekCuacaFrame() {
        initComponents(); // Inisialisasi komponen GUI yang dibuat di desain (palette)
        
        // Inisialisasi tableModel untuk tabel cuaca
        tableModel = (DefaultTableModel) tblCuaca.getModel();
        
        // Action listener untuk tombol Cek Cuaca
        btnCekCuaca.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                kota = txtKota.getText().trim();
                if (!kota.isEmpty()) {
                    cekCuaca(kota);
                }
            }
        });

        // Item listener untuk JComboBox agar mengisi txtKota dengan pilihan kota
        cmbKota.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    String kota = (String) cmbKota.getSelectedItem();
                    if (kota != null && !kota.isEmpty()) {
                        txtKota.setText(kota); // Memasukkan nama kota ke txtKota
                    }
                }
            }
        });

        // Action listener untuk tombol Simpan Ke CSV
        btnSimpanCSV.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!suhuCuaca.isEmpty() && !cuaca.isEmpty()) {
                    simpanKeTabel(kota, suhuCuaca, cuaca);
                    simpanKeCSV(kota, suhuCuaca, cuaca);
                }
            }
        });

        // Action listener untuk tombol Hapus Data
        btnHapus.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] selectedRows = tblCuaca.getSelectedRows(); // Mendapatkan semua baris yang dipilih
                if (selectedRows.length > 0) {
                    List<String> kotaList = new ArrayList<>();
                    for (int i = selectedRows.length - 1; i >= 0; i--) {
                        String kota = (String) tableModel.getValueAt(selectedRows[i], 0);
                        kotaList.add(kota);
                        tableModel.removeRow(selectedRows[i]);
                    }
                    hapusDariCSV(kotaList);
                    btnHapus.setEnabled(false);
                }
            }
        });

        // Focus listener untuk txtKota
        txtKota.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                txtKota.setText(""); // Mengosongkan teks saat mendapat fokus
            }

            @Override
            public void focusLost(FocusEvent e) {
                // Tidak ada aksi saat fokus hilang
            }
        });

        // List selection listener untuk tabel cuaca
        tblCuaca.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tblCuaca.getSelectedRow() != -1) {
                btnHapus.setEnabled(true); // Mengaktifkan tombol hapus saat ada data dipilih
            }
        });

        // Memuat daftar lokasi favorit
        daftarFavorit = new ArrayList<>();
        daftarFavorit.add("Jakarta");
        daftarFavorit.add("Banjarmasin");
        daftarFavorit.add("Banjarbaru");
        updateComboBox();

        // Memuat data cuaca dari file CSV
        muatDataCSV();
    }

    private void cekCuaca(String kota) {
        try {
            String apiKey = "API_KEY"; // Ganti dengan API Key Anda
            String urlString = "http://api.openweathermap.org/data/2.5/weather?q=" + kota + "&appid=" + apiKey + "&units=metric&lang=id";
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            Scanner sc = new Scanner(url.openStream());
            StringBuilder jsonResult = new StringBuilder();
            while (sc.hasNext()) {
                jsonResult.append(sc.nextLine());
            }
            sc.close();
            
            // Parsing JSON
            JSONObject jsonObj = new JSONObject(jsonResult.toString());
            cuaca = jsonObj.getJSONArray("weather").getJSONObject(0).getString("description");
            suhuCuaca = String.valueOf(jsonObj.getJSONObject("main").getDouble("temp"));
            String iconCode = jsonObj.getJSONArray("weather").getJSONObject(0).getString("icon");

            // Menampilkan data cuaca
            lblHasilCuaca.setText("Cuaca: " + cuaca + ", Suhu: " + suhuCuaca + "°C");
            
            // Menampilkan gambar ikon cuaca
            tampilkanIconCuaca(iconCode);

            // Menambahkan kota ke daftar favorit jika belum ada
            if (!daftarFavorit.contains(kota)) {
                daftarFavorit.add(kota);
                updateComboBox();
            }

        } catch (Exception ex) {
            lblHasilCuaca.setText("Gagal mendapatkan data cuaca!");
            ex.printStackTrace();
        }
    }

    private void tampilkanIconCuaca(String iconCode) {
        try {
            String iconUrl = "http://openweathermap.org/img/wn/" + iconCode + "@2x.png";
            URL url = new URL(iconUrl);
            ImageIcon icon = new ImageIcon(url);
            lblCuacaIcon.setIcon(icon);
        } catch (Exception e) {
            e.printStackTrace();
            lblCuacaIcon.setIcon(null);  // Jika gagal menampilkan ikon
        }
    }

    private void simpanKeCSV(String kota, String suhu, String cuaca) {
        try {
            FileWriter writer = new FileWriter("data_cuaca.csv", true);
            writer.append(kota + "," + suhu + "," + cuaca + "\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Gagal menyimpan data cuaca.");
            e.printStackTrace();
        }
    }

    private void hapusDariCSV(List<String> kotaList) {
        try {
            File inputFile = new File("data_cuaca.csv");
            File tempFile = new File("data_cuaca_temp.csv");
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

            String currentLine;
            while ((currentLine = reader.readLine()) != null) {
                String[] data = currentLine.split(",");
                boolean isKotaDeleted = false;
                for (String kota : kotaList) {
                    if (data[0].equalsIgnoreCase(kota)) {
                        isKotaDeleted = true;
                        break;
                    }
                }
                if (!isKotaDeleted) {
                    writer.write(currentLine + System.getProperty("line.separator"));
                }
            }
            writer.close();
            reader.close();
            inputFile.delete();
            tempFile.renameTo(inputFile);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Gagal menghapus data cuaca.");
            e.printStackTrace();
        }
    }

    private void simpanKeTabel(String kota, String suhu, String cuaca) {
        tableModel.addRow(new Object[] { kota, suhu, cuaca });
    }

    private void updateComboBox() {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(daftarFavorit.toArray(new String[0]));
        cmbKota.setModel(model);
    }

    private void muatDataCSV() {
        tableModel.setRowCount(0);  // Menghapus data yang sudah ada
        try {
            BufferedReader reader = new BufferedReader(new FileReader("data_cuaca.csv"));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",");
                tableModel.addRow(data);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CekCuacaFrame().setVisible(true));
    }


    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        btnCekCuaca = new javax.swing.JButton();
        cmbKota = new javax.swing.JComboBox<>();
        jLabel2 = new javax.swing.JLabel();
        lblHasilCuaca = new javax.swing.JLabel();
        lblCuacaIcon = new javax.swing.JLabel();
        btnSimpanCSV = new javax.swing.JButton();
        btnHapus = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblCuaca = new javax.swing.JTable();
        txtKota = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel1.setText("Masukkan Kota:");

        btnCekCuaca.setText("Cek Cuaca");
        btnCekCuaca.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCekCuacaActionPerformed(evt);
            }
        });

        cmbKota.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        cmbKota.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cmbKotaItemStateChanged(evt);
            }
        });

        jLabel2.setText("Cuaca:");

        lblHasilCuaca.setText("                   ");

        lblCuacaIcon.setText("                  ");

        btnSimpanCSV.setText("Simpan Ke CSV");
        btnSimpanCSV.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSimpanCSVActionPerformed(evt);
            }
        });

        btnHapus.setText("Hapus Data");
        btnHapus.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnHapusActionPerformed(evt);
            }
        });

        tblCuaca.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null}
            },
            new String [] {
                "Kota", "Suhu", "Cuaca "
            }
        ));
        jScrollPane1.setViewportView(tblCuaca);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(btnSimpanCSV, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btnHapus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtKota))
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addGap(18, 18, 18)
                                .addComponent(lblHasilCuaca)
                                .addGap(61, 61, 61)
                                .addComponent(lblCuacaIcon)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cmbKota, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnCekCuaca)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(5, 5, 5)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btnCekCuaca)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(1, 1, 1)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel1)
                                    .addComponent(txtKota, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addGap(1, 1, 1))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(cmbKota, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(12, 12, 12)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(lblHasilCuaca)
                    .addComponent(lblCuacaIcon))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(btnSimpanCSV)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnHapus)))
                .addGap(156, 156, 156))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 559, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnHapusActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHapusActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnHapusActionPerformed

    private void btnSimpanCSVActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSimpanCSVActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnSimpanCSVActionPerformed

    private void cmbKotaItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cmbKotaItemStateChanged
        // TODO add your handling code here:
    }//GEN-LAST:event_cmbKotaItemStateChanged

    private void btnCekCuacaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCekCuacaActionPerformed

    }//GEN-LAST:event_btnCekCuacaActionPerformed

    /**
     * @param args the command line arguments
     */
  
    

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCekCuaca;
    private javax.swing.JButton btnHapus;
    private javax.swing.JButton btnSimpanCSV;
    private javax.swing.JComboBox<String> cmbKota;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblCuacaIcon;
    private javax.swing.JLabel lblHasilCuaca;
    private javax.swing.JTable tblCuaca;
    private javax.swing.JTextField txtKota;
    // End of variables declaration//GEN-END:variables
}
