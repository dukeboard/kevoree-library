package org.kevoree.library.xmpp;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.packet.Message;
import org.kevoree.library.xmpp.mngr.ConnectionManager;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Created with IntelliJ IDEA.
 * User: gregory.nain
 * Date: 08/05/12
 * Time: 13:47
 * To change this template use File | Settings | File Templates.
 */
public class TestFrame {

    private JFrame frame;
    private ConnectionManager connection;

    public TestFrame() {
        frame = new JFrame();
        connection = new ConnectionManager();

        frame.getContentPane().setLayout(new GridLayout(0,1));
        buildFrame();

        frame.pack();
        frame.setVisible(true);
    }


    private void buildFrame() {
        buildLoginLine();
        buildRemoveContactLine();
        buildAddContactLine();
        builgGetContactList();
        buildChatLine();
        buildSendFileLine();

    }

    private void buildLoginLine() {
        JPanel line1 = new JPanel();
        line1.setLayout(new FlowLayout(FlowLayout.LEFT));
        final JTextField login_txt = new JTextField("entimid@gmail.com");
        final JTextField passwd_txt = new JTextField("entimidpass");
        JButton connect = new JButton("Connect");
        connect.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                if(((JButton)actionEvent.getSource()).getText().equals("Connect")){
                    connection.login(login_txt.getText(), passwd_txt.getText());
                    ((JButton)actionEvent.getSource()).setText("Disconnect");
                } else {
                    connection.disconnect();
                    ((JButton)actionEvent.getSource()).setText("Connect");
                }
            }
        });
        line1.add(login_txt);
        line1.add(passwd_txt);
        line1.add(connect);

        frame.getContentPane().add(line1);
    }

    private void buildAddContactLine() {
        JPanel line1 = new JPanel();
        line1.setLayout(new FlowLayout(FlowLayout.LEFT));
        final JTextField contactAddress = new JTextField("gregory.nain@gmail.com");
        JButton connect = new JButton("Add");
        connect.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                connection.addContact(contactAddress.getText());
            }
        });
        line1.add(contactAddress);
        line1.add(connect);

        frame.getContentPane().add(line1);
    }

    private void buildRemoveContactLine() {
        JPanel line1 = new JPanel();
        line1.setLayout(new FlowLayout(FlowLayout.LEFT));
        final JTextField contactAddress = new JTextField("gregory.nain@gmail.com");
        JButton connect = new JButton("Remove");
        connect.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                connection.removeContact(contactAddress.getText());
            }
        });
        line1.add(contactAddress);
        line1.add(connect);

        frame.getContentPane().add(line1);
    }

    private void builgGetContactList() {
        JPanel line1 = new JPanel();
        line1.setLayout(new FlowLayout(FlowLayout.LEFT));
        JButton connect = new JButton("GetContacts");
        connect.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                connection.printContactsStats();
            }
        });
        line1.add(connect);

        frame.getContentPane().add(line1);
    }

    public static void main(String[] args) {
        new TestFrame();
    }

    private void buildChatLine() {
        JPanel line1 = new JPanel();
        line1.setLayout(new FlowLayout(FlowLayout.LEFT));
        final JTextField contactAddress = new JTextField("gregory.nain@gmail.com");
        final JButton connect = new JButton("StartChat");
        connect.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
               new MyChatFrame(connection, contactAddress.getText());
            }
        });
        line1.add(contactAddress);
        line1.add(connect);

        frame.getContentPane().add(line1);
    }

    private void buildSendFileLine() {
        JPanel line1 = new JPanel();
        line1.setLayout(new FlowLayout(FlowLayout.LEFT));
        final JTextField contactAddress = new JTextField("gregory.nain@gmail.com");
        final JButton connect = new JButton("SendFile");
        connect.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
               JFileChooser fileChooser = new JFileChooser();
                if(fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    System.out.println("Sending file:" + fileChooser.getSelectedFile().getAbsolutePath());
                    connection.sendFile(fileChooser.getSelectedFile(),fileChooser.getSelectedFile().getName(), contactAddress.getText());
                }
            }
        });
        line1.add(contactAddress);
        line1.add(connect);

        frame.getContentPane().add(line1);
    }





    private class MyChatFrame extends JFrame {

        private JTextPane screen;
        private JTextArea inputTextField;
        private JButton send;
        private MessageListener listener;
        private ConnectionManager connection;
        private String buddyAddress;

        public MyChatFrame(ConnectionManager connection, String buddyAddress) {
            this.connection = connection;
            this.buddyAddress = buddyAddress;

            listener = new MessageListener() {
                public void processMessage(Chat chat, Message message) {
                    appendIncomming(message.getBody());
                }
            };

            setPreferredSize(new Dimension(300, 300));
            setLayout(new BorderLayout());
            send = new JButton("Send");
            send.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    if (inputTextField.getText().length() > 1) {
                        MyChatFrame.this.appendOutgoing(inputTextField.getText());
                        MyChatFrame.this.connection.sendMessage(inputTextField.getText(),MyChatFrame.this.buddyAddress,listener);
                    }

                }
            });

            screen = new JTextPane();
            screen.setFocusable(false);
            screen.setEditable(false);
            StyledDocument doc = screen.getStyledDocument();
            Style def = StyleContext.getDefaultStyleContext().
                    getStyle(StyleContext.DEFAULT_STYLE);
            Style system = doc.addStyle("system", def);
            StyleConstants.setForeground(system, Color.GRAY);

            Style incoming = doc.addStyle("incoming", def);
            StyleConstants.setForeground(incoming, Color.BLUE);

            Style outgoing = doc.addStyle("outgoing", def);
            StyleConstants.setForeground(outgoing, Color.GREEN);


            final String INITIAL_MESSAGE = "Type your text here";
            inputTextField = new JTextArea();
            inputTextField.setText(INITIAL_MESSAGE);
            inputTextField.setFocusable(true);
            inputTextField.setRequestFocusEnabled(true);
            inputTextField.requestFocus();
            inputTextField.setCaretPosition(0);
            inputTextField.setSelectionStart(0);
            inputTextField.setSelectionEnd(INITIAL_MESSAGE.length());

            inputTextField.addKeyListener(new KeyAdapter() {

                @Override
                public void keyReleased(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        if (e.isControlDown()) {
                            inputTextField.append("\n");
                        } else {
                            if (inputTextField.getText().length() > 1) {
                                MyChatFrame.this.appendOutgoing(inputTextField.getText());
                            }
                            inputTextField.setText("");
                        }
                    }
                }
            });

            JPanel bottomPanel = new JPanel();
            bottomPanel.setLayout(new BorderLayout());
            bottomPanel.add(inputTextField, BorderLayout.CENTER);
            bottomPanel.add(send, BorderLayout.EAST);

            add(new JScrollPane(screen), BorderLayout.CENTER);
            add(bottomPanel, BorderLayout.SOUTH);
            this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            pack();
            setVisible(true);
        }

        public MessageListener getListener() {
            return listener;
        }

        public void appendSystem(String text) {
            try {
                StyledDocument doc = screen.getStyledDocument();
                doc.insertString(doc.getLength(), formatForPrint(text), doc.getStyle("system"));
            } catch (BadLocationException ex) {
//                ex.printStackTrace();
               // logger.error("Error while trying to append system message in the " + this.getName(), ex);
            }
        }

        public void appendIncomming(String text) {
            try {
                StyledDocument doc = screen.getStyledDocument();
                doc.insertString(doc.getLength(), formatForPrint(text), doc.getStyle("incoming"));
                screen.setCaretPosition(doc.getLength());
            } catch (BadLocationException ex) {
//                ex.printStackTrace();
                //logger.error("Error while trying to append incoming message in the " + this.getName(), ex);
                //getLoggerLocal().error(ex.getClass().getSimpleName() + " occured while trying to append text in the terminal.", ex);
            }
        }

        public void appendOutgoing(String text) {
            try {
                StyledDocument doc = screen.getStyledDocument();
                doc.insertString(doc.getLength(), ">" + formatForPrint(text), doc.getStyle("outgoing"));
            } catch (BadLocationException ex) {
//                ex.printStackTrace();
               // logger.error("Error while trying to append local message in the " + this.getName(), ex);
                //getLoggerLocal().error(ex.getClass().getSimpleName() + " occured while trying to append text in the terminal.", ex);
            }
        }

        private String formatForPrint(String text) {
            return (text.endsWith("\n") ? text : text + "\n");
        }
    }


}
