package movies;

	import io.javalin.Javalin;
	import io.javalin.http.Context;

	import com.mongodb.ConnectionString;
	import com.mongodb.MongoClientSettings;
	import com.mongodb.client.FindIterable;
	import com.mongodb.client.MongoClient;
	import com.mongodb.client.MongoClients;
	import com.mongodb.client.MongoCollection;
	import com.mongodb.client.MongoDatabase;

	import org.bson.Document;

	import org.slf4j.Logger;
	import org.slf4j.LoggerFactory;

	import com.google.gson.JsonArray;
	import com.google.gson.JsonObject;
	import com.google.gson.JsonParser;

	import java.io.FileInputStream;
	import java.io.InputStream;
	import java.util.Locale;
	import java.util.Properties;

	import static com.mongodb.client.model.Filters.eq;

	public class Lab4Server {

	    private static final Logger logg = LoggerFactory.getLogger(Lab4Server.class);
	    private static MongoCollection<Document> filmSamling;

	    public static void main(String[] args) throws Exception {
	        initieraMongo();
	        startaServer();
	    }

	    private static void initieraMongo() throws Exception {

	        Properties egenskaper = new Properties();

	        try (InputStream input = new FileInputStream("connection.properties")) {
	            egenskaper.load(input);
	        }

	        String anslutning = egenskaper.getProperty("db.connection_string");
	        String databasNamn = egenskaper.getProperty("db.name");

	        ConnectionString connString = new ConnectionString(anslutning);
	        MongoClientSettings inställningar =
	                MongoClientSettings.builder()
	                        .applyConnectionString(connString)
	                        .build();

	        MongoClient mongoKlient = MongoClients.create(inställningar);
	        MongoDatabase databas = mongoKlient.getDatabase(databasNamn);

	        filmSamling = databas.getCollection("movies");

	        logg.info("Ansluten till MongoDB, använder databas {}", databasNamn);
	    }

	    private static void startaServer() {

	        Javalin app = Javalin.create().start(4567);

	        app.get("/title/{title}", Lab4Server::hämtaFilmViaTitel);
	        app.get("/cast/{title}", Lab4Server::hämtaRollistaViaTitel);
	        app.get("/fullplot/{title}", Lab4Server::hämtaHandlingViaTitel);
	        app.get("/genre/{genre}", Lab4Server::hämtaFilmerViaGenre);
	        app.get("/actor/{actor}", Lab4Server::hämtaFilmerViaSkådespelare);
	        app.post("/title", Lab4Server::skapaNyFilm);
	    }

	    private static void hämtaFilmViaTitel(Context ctx) {

	        String titel = ctx.pathParam("title").trim();
	        Document film = filmSamling.find(eq("title", titel)).first();

	        if (film != null) {
	            film.remove("_id");
	            film.remove("poster");
	            film.remove("cast");
	            film.remove("fullplot");

	            ctx.status(200)
	               .contentType("application/json")
	               .result(film.toJson());
	        } else {
	            ctx.status(404)
	               .contentType("application/json")
	               .result(skapaFelJson("Movie not found").toString());
	        }
	    }

	    private static void hämtaRollistaViaTitel(Context ctx) {

	        String titel = ctx.pathParam("title").trim();
	        Document film = filmSamling.find(eq("title", titel)).first();

	        if (film != null) {
	            film.remove("_id");
	            film.remove("poster");
	            film.remove("fullplot");

	            ctx.status(200)
	               .contentType("application/json")
	               .result(film.toJson());
	        } else {
	            ctx.status(404)
	               .contentType("application/json")
	               .result(skapaFelJson("Movie not found").toString());
	        }
	    }

	    private static void hämtaHandlingViaTitel(Context ctx) {

	        String titel = ctx.pathParam("title").trim();
	        Document film = filmSamling.find(eq("title", titel)).first();

	        if (film != null) {
	            Document svar = new Document();
	            svar.append("title", film.getString("title"));
	            svar.append("fullplot", film.getString("fullplot"));

	            ctx.status(200)
	               .contentType("application/json")
	               .result(svar.toJson());
	        } else {
	            ctx.status(404)
	               .contentType("application/json")
	               .result(skapaFelJson("Movie not found").toString());
	        }
	    }

	    private static void hämtaFilmerViaGenre(Context ctx) {

	        String genre = ctx.pathParam("genre").trim();
	        FindIterable<Document> resultat =
	                filmSamling.find(eq("genres", genre)).limit(10);

	        JsonArray filmer = new JsonArray();

	        for (Document film : resultat) {
	            film.remove("_id");
	            film.remove("poster");
	            film.remove("cast");
	            film.remove("fullplot");

	            filmer.add(JsonParser.parseString(film.toJson()));
	        }

	        if (filmer.size() == 0) {
	            ctx.status(404)
	               .contentType("application/json")
	               .result(skapaFelJson("No movies found for genre").toString());
	        } else {
	            ctx.status(200)
	               .contentType("application/json")
	               .result(filmer.toString());
	        }
	    }

	    private static void hämtaFilmerViaSkådespelare(Context ctx) {

	        String input = ctx.pathParam("actor").trim();
	        String skådespelare =
	                görVersalerKorrekt(input.toLowerCase(Locale.ROOT));

	        FindIterable<Document> resultat =
	                filmSamling.find(eq("cast", skådespelare)).limit(10);

	        JsonArray filmer = new JsonArray();

	        for (Document film : resultat) {
	            if (film.containsKey("title")) {
	                filmer.add(film.getString("title"));
	            }
	        }

	        if (filmer.size() == 0) {
	            ctx.status(404)
	               .contentType("application/json")
	               .result(skapaFelJson("Actor not found in any movie").toString());
	        } else {
	            ctx.status(200)
	               .contentType("application/json")
	               .result(filmer.toString());
	        }
	    }

	    private static void skapaNyFilm(Context ctx) {

	        try {
	            String body = ctx.body();
	            Document nyFilm = Document.parse(body);

	            if (!nyFilm.containsKey("title") || !nyFilm.containsKey("cast")) {
	                ctx.status(400)
	                   .contentType("application/json")
	                   .result(skapaFelJson("Missing 'title' or 'cast' field").toString());
	                return;
	            }

	            filmSamling.insertOne(nyFilm);
	            ctx.status(202);

	        } catch (Exception e) {
	            logg.error("Misslyckades att spara film: {}", e.getMessage());
	            ctx.status(500)
	               .contentType("application/json")
	               .result(skapaFelJson("Failed to insert movie").toString());
	        }
	    }

	    private static String görVersalerKorrekt(String text) {

	        String[] ord = text.trim().split("\\s+");
	        StringBuilder resultat = new StringBuilder();

	        for (String o : ord) {
	            if (!o.isEmpty()) {
	                resultat.append(
	                        Character.toUpperCase(o.charAt(0))
	                        + o.substring(1).toLowerCase()
	                        + " ");
	            }
	        }
	        return resultat.toString().trim();
	    }

	    private static JsonObject skapaFelJson(String meddelande) {

	        JsonObject obj = new JsonObject();
	        obj.addProperty("error", meddelande);
	        return obj;
	    }
	}


