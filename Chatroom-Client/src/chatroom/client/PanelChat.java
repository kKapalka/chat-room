/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatroom.client;

/**
 *
 * @author kkapa
 */
public class PanelChat extends javax.swing.JPanel {
    /**
     * Odnosnik do glownej aplikacji klienta
     */
    ChatroomClient client;
    /**
     * Stala okreslajaca delimiter miedzy fragmentami informacji przesylanymi mieszy klientem a serwerem
     */
    static final String DELIMITER=";end;";
    /**
     * Nazwa uzytkownika
     */
    String username="";
    /**
     * Creates new form ChatPanel
     */
    public PanelChat() {
        initComponents();
    }

    /**
     * Funkcja ustawia nazwe uzytkownika w panelu czatu
     * @param name nazwa uzytkownika
     */
    public void setUserName(String name){
        this.username=name;
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButton1 = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        Chat = new javax.swing.JTextPane();
        MessageInput = new javax.swing.JTextField();
        SendButton = new javax.swing.JButton();
        Logout = new javax.swing.JButton();

        jButton1.setText("jButton1");

        Chat.setEditable(false);
        jScrollPane1.setViewportView(Chat);

        SendButton.setText("Wyślij");
        SendButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SendButtonActionPerformed(evt);
            }
        });

        Logout.setText("^");
        Logout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LogoutActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(MessageInput, javax.swing.GroupLayout.PREFERRED_SIZE, 285, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(SendButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(Logout))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 263, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(MessageInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(SendButton)
                    .addComponent(Logout))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void LogoutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LogoutActionPerformed
        client=(ChatroomClient)this.getTopLevelAncestor();
        client.SendData("Logout"+DELIMITER+username+DELIMITER);
    }//GEN-LAST:event_LogoutActionPerformed

    private void SendButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SendButtonActionPerformed
       client=(ChatroomClient)this.getTopLevelAncestor();
       String message=MessageInput.getText().replace("'", ";apos;");
       if("/hide history".equals(message)) client.SendData("Message"+DELIMITER+username+DELIMITER+message+DELIMITER+client.logintime);
       else client.SendData("Message"+DELIMITER+username+DELIMITER+message);
       
       if("/show history".equals(message) || "/hide history".equals(message)) Clear();
       
       MessageInput.setText("");
    }//GEN-LAST:event_SendButtonActionPerformed

    /**
     * Funkcja ma za zadanie wstawić nową linijkę tekstu do czatu oraz przesunięcie karety na sam koniec
     * @param text tekst do podpięcia do okna czatu
     */
    public void TextAppend(String text){
        Chat.setText(Chat.getText()+text+"\n");
       Chat.setCaretPosition(Chat.getDocument().getLength());
    }

    /**
     * Funkcja odpowiada za wyczyszczenie panelu czatu. Korzysta z niej klasa ChatroomClient podczas operacji logowania
     */
    public void Clear(){
        Chat.setText("");
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextPane Chat;
    private javax.swing.JButton Logout;
    private javax.swing.JTextField MessageInput;
    private javax.swing.JButton SendButton;
    private javax.swing.JButton jButton1;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables
}
