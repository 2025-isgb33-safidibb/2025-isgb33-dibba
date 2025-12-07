

import java.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.bson.Document;


import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;



public class FilmApp extends JFrame {
	
	private JTextArea area;
	private JTextField input;
	private JButton knapp;
	

    private static MongoCollection<Document> myCollection;

    public static void main(String[] args) throws Exception {
        initMongo();
        new FilmApp();
    }
    
    public FilmApp() {
    	// Detta är GUI
    	setTitle("Filmförslag");
    	setSize(400,500);
    	setLayout(null);
    	setDefaultCloseOperation(Jframe.EXIT_ON_CLOSE);
    	
    	area = new JTextArea();
    	area.setBounds(10,10,365,400);
    	area.setLineWrap(true);
    	
    	input = new JTextField();
    	input.setBounds(10,415,260,40);
    	
    	knapp = new Jbutton("Hämta filmer");
    	knapp.setBounds(275,415,120,40);
    	
    	add(area);
    	add(input);
    	add(knapp);
    	
    	knapp.addActionListener(new ActionListener() {
    		@Override
    		public void actionPreformed(ActionEvent e) {
    			searchMovies();
    		}
    	});
    	
    	setvisible(true);
    }
    
    private static void initMongo() throws Exception {
        Properties prop = new Properties();

        try (InputStream input = new FileInputStream("connection.properties")) {
            prop.load(input);
        }

        String connString = prop.getProperty("db.connection_string");
        String dbName = prop.getProperty("db.name");

        ConnectionString connectionString = new ConnectionString(connString);
        MongoClientSettings settings = MongoClientSettings.builder()
            .applyConnectionString(new connectionString(connString))
            .build();

        MongoClient mongoClient = MongoClients.create(settings);
       MongoDatabase database = mongoClient.getDatabase(dbName);
       myCollection = database.getCollection("movies");
}

    
  public List<Document>findMoviesByGenre(String genre){
	  
	  AggregateIterable<Document> resultat= collection.aggregate(Arrays.asList(Aggregates.match(Filters.regex("genres","^" + genre + "$", "i" )),
			  Aggregates.project(Projections.include("title", "year")),
			  Aggregates.sort(Sorts.descending("title")),
			  Aggregates.limit(10)
	 ));
			  
			  MongoCursor<Document> cursor = resultat.iterator();
			  List<Document>filer= new java.util.ArrayList<>();
			  while (cursor.hasNext()) {
				  filmer.add(cursornext());
			  }
			  return filmer;
  }
  
   private void searchMovies() {
	   string genre = input.getText().trim();
	   List<Document> filmer=findMoviesByGenre(genre);
	   
	   if(filmer.isEmpty()) {
		   area.setText("Ingen film matchade kategorin");
	   }else {area.setText("");
	   
	   for (Document d : filmer) {
		   String line = d.getString("title") + "," + d.getInteger("year").toString() + "\n";
		   area.append(line);
	   }
	   }
   }
}