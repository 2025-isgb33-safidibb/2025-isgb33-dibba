
import javax.swing.*;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

import org.bson.Document;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import com.mongodb.client.model.*;

public class FilmApp extends JFrame {

    private JTextArea area;
    private JTextField input;
    private JButton knapp;

    private static MongoCollection<Document> myCollection;
    
    public FilmApp() {
        setTitle("Filmförslag");
        setSize(400, 500);
        setLayout(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        area = new JTextArea();
        area.setBounds(10, 10, 365, 400);
        area.setLineWrap(true);

        input = new JTextField();
        input.setBounds(10, 415, 260, 40);

        knapp = new JButton("Hämta filmer");
        knapp.setBounds(275, 415, 120, 40);

        add(area);
        add(input);
        add(knapp);

        knapp.addActionListener(e -> searchMovies());

        setVisible(true);
    }

    private static void initMongo() throws Exception {
        Properties prop = new Properties();
        try (InputStream input = new FileInputStream("connection.properties")) {
            prop.load(input);
        }

        String connString = prop.getProperty("db.connection_string");
        String dbName = prop.getProperty("db.name");

        MongoClient client = MongoClients.create(
                MongoClientSettings.builder()
                        .applyConnectionString(new ConnectionString(connString))
                        .build()
        );

        myCollection = client.getDatabase(dbName).getCollection("movies");
    }

    private List<Document> findMoviesByGenre(String genre) {
        AggregateIterable<Document> res = myCollection.aggregate(Arrays.asList(
                Aggregates.match(Filters.regex("genres", genre)),
                Aggregates.project(Projections.include("title", "year")),
                Aggregates.sort(Sorts.descending("title")),
                Aggregates.limit(10)
        ));

        
        MongoCursor<Document> cursor = res.iterator();
        List<Document> filmer = new ArrayList<>();
        
        while (cursor.hasNext()) {
            filmer.add(cursor.next());
        }
        return filmer;
    }

    private void searchMovies() {
        String genre = input.getText().trim();
        List<Document> filmer = findMoviesByGenre(genre);

        area.setText("");
        if (filmer.isEmpty()) {
            area.setText("Ingen film hittades");
        } else {
            for (Document d : filmer) {
                area.append(d.getString("title") + " (" + d.getInteger("year") + ")\n");
            }
        }
    }
    
    public static void main(String[] args) throws Exception {
        initMongo();
        new FilmApp();
    }
    
}
