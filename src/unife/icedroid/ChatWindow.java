package unife.icedroid;

import unife.icedroid.core.ICeDROID;
import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.DefaultListModel;

public class ChatWindow {
	
	public static void open(final Subscription subscription) {
		EventQueue.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				start(subscription);
			}
			
		});
	}
	
	private static void start(final Subscription subscription) {
		final JFrame window = new JFrame(subscription.toString());
		window.setSize(400, 500);
		window.setResizable(false);
		
		final DefaultListModel<String> listData = loadMessages(subscription.toString());
		JList<String> list = new JList<String>(listData);
		JScrollPane listContainer = new JScrollPane(list);
		listContainer.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		listContainer.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		window.add(listContainer, BorderLayout.CENTER);
	
		JPanel panel = new JPanel();
		BoxLayout layout = new BoxLayout(panel, BoxLayout.X_AXIS);
		panel.setLayout(layout);
		final JTextField text = new JTextField(25);
		text.setMaximumSize(new Dimension(1000, 1000));
		JButton send = new JButton("Invia");
		send.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				String msg = text.getText();
		        if (!msg.equals("")) {
		            text.setText(null);
		            		            
		            TxtMessage message = new TxtMessage(subscription, msg);
		            ChatsManager.getInstance().saveMessageInConversation(message);

		            ICeDROID.getInstance().send(message);
		        }
			}
			
		});
		panel.add(text);
		panel.add(send);
		window.add(panel, BorderLayout.SOUTH);
		
		window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		window.setVisible(true);
		
		final SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
			
			@Override
			protected Void doInBackground() {
				Path conversationsDir = Paths.get("resources/conversations/");
				
				try {
					WatchService watcher = conversationsDir.getFileSystem().newWatchService();
					conversationsDir.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
					
					while (true) {
						WatchKey wk = watcher.take();
						for (WatchEvent<?> event : wk.pollEvents()) {
							Path file = (Path) event.context();
							if (file.endsWith(subscription.toString())) {
					            try {
					            	String conversationFileName = "resources/conversations/" + subscription.toString();
					                BufferedReader br = new BufferedReader(
					                		new FileReader(conversationFileName));
					                ArrayList<String> previousMessages = new ArrayList<>(0);
					                String msg;
					                while ((msg = br.readLine()) != null) {
					                    previousMessages.add(msg);
					                }
					                if (previousMessages.size() > 0) {
					                    publish(previousMessages.get(previousMessages.size()-1));
					                }
					                br.close();
					            } catch (Exception ex) {
					                System.out.println(ex);
					            }
							}
						}
						wk.reset();
					}
					
				} catch (Exception ex) {
					System.out.println(ex); 
				}
				
				return null;
			}
			
			@Override
			protected void process(List<String> list) {
				for (String s : list) {
					listData.addElement(s);
				}
			}
			
		};
		worker.execute();
		
		window.addWindowListener(new WindowAdapter() {
			
			@Override
			public void windowClosing(WindowEvent e) {
				worker.cancel(true);
				window.dispose();
			}
			
		});
	}
	
	private static DefaultListModel<String> loadMessages(String fileName) {
		DefaultListModel<String> listData = new DefaultListModel<String>();

        try {
        	fileName = "resources/conversations/" + fileName;
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            String line;
            while ((line = br.readLine()) != null) {
                listData.addElement(line);
            }
            br.close();
        } catch (Exception ex) {}
		
		return listData;
	}
}