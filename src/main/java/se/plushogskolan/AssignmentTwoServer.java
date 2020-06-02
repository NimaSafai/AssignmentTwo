package se.plushogskolan;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;
import org.owasp.encoder.Encode;
import org.sqlite.SQLiteDataSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssignmentTwoServer {
    private static final Path DB_PATH = Path.of("app.db");
    private static final Path DB_SCRIPT_PATH = Path.of("create.sql");
    private static SQLiteDataSource db;

    public static void main(String[] args) throws IOException, SQLException {
        db = new SQLiteDataSource();
        db.setUrl("jdbc:sqlite:" + DB_PATH);
        if (!Files.exists(DB_PATH)) {
            createDatabase();
        }

        Javalin app = Javalin.create(config -> {
            config.enableDevLogging();
            config.addStaticFiles("static", Location.EXTERNAL);
        }).start("localhost", 8000);

        app.before(context -> {
            context.contentType("text/html; charset=UTF-8");
        });

        // Main HTML handlers.
        app.get("/", context -> mainPage(context));
        app.get("/create", context -> createQuizPage(context));
        app.post("/create", context -> createQuiz(context));
        app.get("/play", context -> quizListPage(context));
        app.get("/play/:quiz_id", context -> singleQuizPage(context));
        app.get("/search", context -> searchPage(context));
        app.get("/flags", context -> flagListPage(context));
        app.get("/flag", context -> singleFlagPage(context));

        // JSON handlers.
        app.get("/quiz/:quiz_id", context -> singleQuizData(context));

        // Authentication handlers.
        app.before(context -> {
            if (context.path().endsWith(".css") || context.path().endsWith(".js") || context.path().endsWith("jpg")) {
                return;
            }

            boolean authPath = context.path().equals("/login") || context.path().equals("/register");
            // If already logged in and trying to login or register, just redirect to main page.
            if (userIsLoggedIn(context) && authPath) {
                context.redirect("/");
            }
            // If not logged in and trying to access content, redirect to login page.
            else if (!userIsLoggedIn(context) && !authPath) {
                context.redirect("/login");
            }
        });

        app.get("/login", context -> loginPage(context));
        app.post("/login", context -> login(context));
        app.get("/register", context -> registerPage(context));
        app.post("/register", context -> register(context));
        app.post("/logout", context -> {
            context.req.getSession().invalidate();
            context.redirect("/");
        });
    }

    // The main page just contains links to the other pages.
    private static void mainPage(Context context) {
        String content =
            "<div class='index'>" +
                "<h1>\uD83C\uDF0E Welcome to GloboQuiz!</h1>" +
                "<ul>" +
                    "<li><a href='/create'>✏️ Create</a></li>" +
                    "<li><a href='/play'>\uD83C\uDFB2 Play</a></li>" +
                    "<li><a href='/search'>\uD83D\uDD0D Search</a></li>" +
                    "<li><a href='/flags'>\uD83C\uDF8C Flags</a></li>" +
                "</ul>" +
            "</div>";
        String html = template(context, "GloboQuiz", content);
        context.result(html);
    }

    // Show the page for creating a new quiz.
    private static void createQuizPage(Context context) {
        if (!userIsLoggedIn(context)) {
            String content = "<div class='result'>You must be logged in to create a quiz.</div>";
            context.result(template(context, "Please log in", content));
            context.status(403);
            return;
        }

        // Create one <option> tag for each image in the "flags" folder.
        String flagHtml = "";
        for (File f : new File("flags").listFiles()) {
            flagHtml += "<option>" + f.getName() + "</option>";
        }

        String optionsHtml = "";
        for (int i = 1; i <= 4; i++) {
            optionsHtml +=
                "<li>" +
                    "<input type='text' required name='question-1-option-" + i + "' placeholder='Option #" + i + "'>" +
                    "<label>" +
                        "<input type='radio' " + (i == 1 ? "checked " : "") +
                        "name='question-1-answer' value='" + i + "'>" +
                        "<span> Correct</span>" +
                    "</label>" +
                "</li>";
        }

        String content =
            "<div class='create'>" +
                "<h1>✏️ Create</h1>" +
                // We create a single question form here, then the JavaScript on the page will allow the user to add
                // more.
                "<form method='post' action='/create'>" +
                    "<input type='text' required name='quiz-title' class='quiz-title' placeholder='Title of Quiz'>" +
                    "<label class='quiz-public'>" +
                        "<input type='checkbox' checked name='quiz-public' value='true'>" +
                        "<span> This quiz should be public</span>" +
                    "</label>" +
                    // Each question will be contained in a fieldset, that will then be cloned by the JavaScript when
                    // adding more questions.
                    "<fieldset>" +
                        "<input type='text' required name='question-1-prompt' class='question-prompt' " +
                            "placeholder='Question #1'>" +
                        "<label class='question-flag'>" +
                            "<span>Flag: </span>" +
                            "<select name='question-1-flag'>" +
                                flagHtml +
                            "</select>" +
                        "</label>" +
                        "<ul class='create-options'>" +
                            optionsHtml +
                        "</ul>" +
                    "</fieldset>" +
                    "<button type='button' class='secondary' id='add-question'>Add Question</button>" +
                    "<button type='submit'>Create Quiz</button>" +
                "</form>" +
            "</div>";
        String html = template(context, "Create", content);
        context.result(html);
    }

    // Handle the form submission from the "Create Quiz" page.
    private static void createQuiz(Context context) throws SQLException {
        if (!userIsLoggedIn(context)) {
            String content = "<div class='result'>You must be logged in to create a quiz.</div>";
            context.result(template(context, "Please log in", content));
            context.status(403);
            return;
        }

        try (Connection c = db.getConnection()) {
            // Save the quiz itself.
            String title = context.formParam("quiz-title");
            boolean isPublic = context.formParam("quiz-public") != null;
            PreparedStatement s1 = c.prepareStatement(
                "INSERT INTO quiz (user_id, title, datetime, public) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );
            s1.setInt(1, context.sessionAttribute("userId"));
            s1.setString(2, title);
            s1.setString(3, LocalDateTime.now().toString());
            s1.setBoolean(4, isPublic);
            s1.executeUpdate();

            ResultSet keys = s1.getGeneratedKeys();
            keys.next();
            int quizId = keys.getInt(1);

            // Loop from 1 and up until there are no more questions in the form, and save each question. We check if
            // there are more questions by looking for the "question-x-prompt" field, where x is the current number.
            int questionNumber = 1;
            while (context.formParam("question-" + questionNumber + "-prompt") != null) {
                // All the params from here will start with the prefix "question-x-", where x is the number, so
                // create it here.
                String prefix = "question-" + questionNumber + "-";

                PreparedStatement s2 = c.prepareStatement(
                    "INSERT INTO question " +
                    "(quiz_id, number, prompt, option_1, option_2, option_3, option_4, correct_option, image_path) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
                );
                s2.setInt(1, quizId);
                s2.setInt(2, questionNumber);
                s2.setString(3, context.formParam(prefix + "prompt"));
                s2.setString(4, context.formParam(prefix + "option-1"));
                s2.setString(5, context.formParam(prefix + "option-2"));
                s2.setString(6, context.formParam(prefix + "option-3"));
                s2.setString(7, context.formParam(prefix + "option-4"));
                s2.setInt(8, Integer.parseInt(context.formParam(prefix + "answer")));
                s2.setString(9, context.formParam(prefix + "flag"));
                s2.executeUpdate();

                // Increase the question number so that the next iteration continues with the next question.
                questionNumber += 1;

                // Finally, show a link to to the new quiz.
                String quizUrl = "/play/" + quizId;
                String content =
                    "<div class='result'>" +
                        "<p>Your quiz has been created.</p>" +
                        "<a href='" + quizUrl + "'>Play it now</a>" +
                    "</div>";
                String html = template(context, "Quiz created", content);
                context.result(html);
            }
        }
    }

    // Show a list of all the quizzes.
    private static void quizListPage(Context context) throws SQLException {
        String  content =
            "<div class='quiz-index'>" +
                "<h1>\uD83C\uDFB2 Play</h1>" +
                "<form method='get' action='/play'>" +
                    "<label>With " +
                        "<select name='operator'>" +
                            "<option value='&gt;='>at least</option>" +
                            "<option value='&lt;='>at most</option>" +
                            "<option value='='>exactly</option>" +
                        "</select>" +
                    "</label>" +
                    "<label>" +
                        "<input type='number' name='questions' value='0'> questions" +
                    "</label>" +
                    "<button type='submit' class='secondary'>Filter</button>" +
                "</form>";

        // If the user has entered a filter, write a message to highlight this.
        if (context.queryParam("questions") != null) {
            content += "<p>Quizzes matching your filter:</p>";
        }

        content += "<ul>";

        try (Connection c = db.getConnection()) {
            // Select all the quizzes that are public, as well as the private quizzes belonging to the current user.
            String sql =
                "SELECT quiz.id AS quiz_id, title, username, public, COUNT(*) AS question_count " +
                "FROM quiz " +
                "JOIN user ON quiz.user_id = user.id " +
                "JOIN question ON quiz.id = question.quiz_id " +
                "WHERE public = TRUE OR user.id = " + context.sessionAttribute("userId") + " " +
                "GROUP BY quiz.id ";

            // If the user has entered a min/max/exact number of questions, add an extra condition.
            if (context.queryParam("questions") != null) {
                String operator = context.queryParam("operator");
                int questions = Integer.parseInt(context.queryParam("questions"));
                sql += "HAVING COUNT(*) " + operator + " " + questions + " ";
            }

            sql += "ORDER BY quiz.title, user.username";

            Statement s = c.createStatement();
            ResultSet rows = s.executeQuery(sql);
            while (rows.next()) {
                String quizUrl = "/play/" + rows.getInt("quiz_id");
                content +=
                    "<li>" +
                        "<a href='" + quizUrl + "'>" +
                            rows.getString("title") +
                            " by " +
                            Encode.forHtml(rows.getString("username")) +
                            (rows.getBoolean("public") ? "" : " [private]") +
                            " (" + rows.getInt("question_count") + " questions)" +
                        "</a>" +
                    "</li>";
            }
        }
        content +=
                "</ul>" +
            "</div>";

        String html = template(context, "Play", content);
        context.result(html);
    }

    // Show a single quiz and let the user play it. We will implement the quiz with JavaScript, so the server doesn't
    // actually need to create anything except the template here. The JavaScript will get the quiz ID from the URL.
    private static void singleQuizPage(Context context) {
        String html = template(context, "Quiz", "");
        context.result(html);
    }

    // The JavaScript gets the quiz data for a single quiz through this JSON endpoint.
    private static void singleQuizData(Context context) throws SQLException {
        try (Connection c = db.getConnection()) {
            // Get the quiz info and put it in a map.
            PreparedStatement quizStatement = c.prepareStatement("SELECT * FROM quiz WHERE id = ? AND user_id = ? OR public = TRUE AND id = ?");
                quizStatement.setString(1, context.pathParam("quiz_id"));
                quizStatement.setInt(2, context.sessionAttribute("userId"));
                quizStatement.setString(3, context.pathParam("quiz_id"));
            ResultSet quizRows = quizStatement.executeQuery();

            // If there is no quiz, show a 404. This also happens when the quiz exists but belongs to another user
            // and is not private.
            boolean exists = quizRows.next();
            if (!exists) {
                context.status(404);
                context.json(
                    "No quiz with ID " + context.pathParam("quiz_id") +
                    ", or you are not allowed to access this quiz."
                );
            }
            // If there is a quiz, return it.
            else {
                Map<String, Object> quiz = new HashMap<>();
                quiz.put("title", quizRows.getString("title"));
                quiz.put("public", quizRows.getBoolean("public"));

                // Get the questions in the quiz and add them to a list in the map.
                List<Map<String, Object>> questions = new ArrayList<>();
                Statement questionStatement = c.createStatement();
                String questionSql =
                    "SELECT * " +
                    "FROM question " +
                    "WHERE quiz_id = " + quizRows.getInt("id") + " " +
                    "ORDER BY number";
                ResultSet questionRows = questionStatement.executeQuery(questionSql);

                while (questionRows.next()) {
                    Map<String, Object> question = new HashMap<>();
                    question.put("prompt", questionRows.getString("prompt"));
                    question.put("option_1", questionRows.getString("option_1"));
                    question.put("option_2", questionRows.getString("option_2"));
                    question.put("option_3", questionRows.getString("option_3"));
                    question.put("option_4", questionRows.getString("option_4"));
                    question.put("correct_option", questionRows.getInt("correct_option"));
                    question.put("image_path", questionRows.getString("image_path"));
                    questions.add(question);
                }
                quiz.put("questions", questions);

                // Send the map as JSON.
                context.json(quiz);
            }
        }
    }

    // Show a search form allowing the user to search for quizzes.
    private static void searchPage(Context context) throws SQLException {
        String content =
            "<div class='search'>" +
                "<h1>\uD83D\uDD0D Search</h1>" +
                "<form method='get' action='/search'>" +
                    "<input type='text' name='search' required>" +
                    "<button type='submit' class='secondary'>Search</button>" +
                "</form>";

        // If the "search" parameter has been entered, that means there is a search.
        if (context.queryParam("search") != null) {
            // Show what term the user searched for.
            content +=
                    "<p>Search results for: " + Encode.forHtml(context.queryParam("search")) + "</p>" +
                "<ul>";

            try (Connection c = db.getConnection()) {
                // Make sure to only get the quizzes that are public or belong to the current user.
                PreparedStatement s = c.prepareStatement(
                    "SELECT quiz.id AS quiz_id, title, username, public " +
                    "FROM quiz " +
                    "JOIN user ON quiz.user_id = user.id " +
                    "WHERE instr(title, ?) " +
                    "AND (public = TRUE OR user.id = ?)"
                );
                s.setString(1, context.queryParam("search"));
                s.setInt(2, context.sessionAttribute("userId"));

                ResultSet rows = s.executeQuery();
                while(rows.next()) {
                    String quizUrl = "/play/" + rows.getInt("quiz_id");
                    content +=
                        "<li>" +
                            "<a href='" + quizUrl + "'>" +
                                Encode.forHtml(rows.getString("title")) +
                                " by " +
                                Encode.forHtml(rows.getString("username")) +
                                (rows.getBoolean("public") ? "" : " (private)") +
                            "</a>" +
                        "</li>";
                }
            }
        }

        content += "</div>";

        String html = template(context, "Search", content);
        context.result(html);
    }

    // Show a single flag from the "flags" folder. SVG is a text-based format so we can just serve the flag images
    // directly here. In the future this will allow us to do interesting stuff like dynamically manipulate the XML
    // before serving it, to add effects or styling etc.
    private static void singleFlagPage(Context context) throws IOException {
        // Create a Path object from the URL parameter.
        String flagName = context.queryParam("name");

        // Call `toRealPath` to fully resolve the path provided by the user.
        // This turns it into an absolute (rather than relative) path and removes sequences like ".." etc.
        Path path = Path.of("flags/" + flagName).toRealPath();

        // Do the same with the flag folder path, so that it also becomes an absolute path and we can compare the two.
        Path flagFolder = Path.of("flags").toRealPath();

        String svg = Files.readString(path);
        context.contentType("image/svg+xml; charset=UTF-8");
        context.result(svg);

        // If the user-provided path does not "start with" (i.e. is contained inside) the story folder path, abort.
        if (!path.startsWith(flagFolder)) {
            // Give a "403 Forbidden" status and show an error message.
            context.status(403);
            context.result("You cannot access files outside the flag folder.");

            return;
        }
    }

    // Show a gallery of all flags in the "flags" folder.
    private static void flagListPage(Context context) {
        String content =
            "<div class='flags'>" +
                "<h1>\uD83C\uDF8C Flags</h1>" +
                "<p>If you just want to look at flags, this is the place for you!</p>";

        content += "<section class='flag-gallery'>";
        File[] flagFiles = new File("flags").listFiles();
        for (File file : flagFiles) {
            String name = file.getName();
            String withoutExtension = name.split("\\.")[0];
            content +=
                "<div>" +
                    "<h2 class='flag-title'>" + withoutExtension + "</h2>" +
                    "<img src='/flag?name=" + name + "'>" +
                "</div>";
        }
        content +=
                "</section>" +
            "</div>";

        String html = template(context, "Flags", content);
        context.result(html);
    }

    private static boolean userIsLoggedIn(Context context) {
        return context.sessionAttribute("userId") != null;
    }

    private static void loginPage(Context context, String message) {
        if (message != null) {
            context.status(403);
        }

        String content =
            "<div class='login'>" +
                "<h1>Login</h1>" +
                (message == null ? "" : "<p><strong>" + message + "</strong></p>") +
                "<form method='post' action='/login'>" +
                    "<label><span>Username:</span><input type='text' name='username'></label>" +
                    "<label><span>Password:</span><input type='password' name='password'></label>" +
                    "<button type='submit'>Log In</button>" +
                "</form>" +
            "</div>";
        String html = template(context, "Login", content);
        context.result(html);
    }

    private static void loginPage(Context context) {
        loginPage(context, null);
    }

    private static void login(Context context) throws SQLException {
        String username = context.formParam("username");
        String password = context.formParam("password");

        try (Connection c = db.getConnection()) {
            PreparedStatement s = c.prepareStatement("SELECT id FROM user WHERE username = ? AND password = ?");
            s.setString(1, username);
            s.setString(2, password);
            ResultSet rows = s.executeQuery();

            if (rows.next()) {
                context.sessionAttribute("userId", rows.getInt("id"));
                context.sessionAttribute("username", username);
                context.redirect("/");
            }
            else {
                loginPage(context, "The username and/or password are incorrect. Please try again.");
            }
        }
    }

    private static void registerPage(Context context, String message) {
        if (message != null) {
            context.status(403);
        }

        String content =
            "<div class='register'>" +
                "<h1>Register</h1>" +
                (message == null ? "" : "<p><strong>" + message + "</strong></p>") +
                "<form method='post' action='/register'>" +
                    "<label><span>Username:</span><input type='text' name='username'></label>" +
                    "<label><span>Password:</span><input type='password' name='password'></label>" +
                    "<label><span>Repeat password:</span><input type='password' name='password-again'></label>" +
                    "<button type='submit'>Register</button>" +
                "</form>" +
            "</div>";
        String html = template(context, "Register", content);
        context.result(html);
    }

    private static void registerPage(Context context) {
        registerPage(context, null);
    }

    private static void register(Context context) throws SQLException {
        String username = context.formParam("username");
        String password = context.formParam("password");
        String passwordAgain = context.formParam("password-again");

        try (Connection c = db.getConnection()) {
            PreparedStatement s1 = c.prepareStatement("SELECT * FROM user WHERE username = ?");
            s1.setString(1, username);
            ResultSet rows = s1.executeQuery();

            if (rows.next()) {
                registerPage(context, "That username has already been registered.");
            }
            else if (!password.equals(passwordAgain)) {
                registerPage(context, "The passwords did not match.");
            }
            else {
                PreparedStatement s2 = c.prepareStatement(
                    "INSERT INTO user (username, password) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS
                );
                s2.setString(1, username);
                s2.setString(2, password);
                s2.execute();

                ResultSet keys = s2.getGeneratedKeys();
                keys.next();
                context.sessionAttribute("userId", keys.getInt(1));
                context.sessionAttribute("username", username);
                context.redirect("/");
            }
        }
    }

    private static String template(Context context, String title, String content) {
        String authHtml;
        if (userIsLoggedIn(context)) {
            authHtml =
                "<form method='post' action='/logout'>" +
                    "<button type='submit'>Log Out</button>" +
                "</form>";
        }
        else {
            authHtml =
                "<p>" +
                    "<a href='/login' class='secondary'>Log In</a>" +
                    "<a href='/register' class='secondary'>Register</a>" +
                "</p>";
        }

        return
            "<!DOCTYPE html>" +
            "<html lang='en'>" +
                "<head>" +
                    "<meta charset='UTF-8'>" +
                    "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                    "<title>" + title + "</title>" +
                    "<script src='/main.js' defer></script>" +
                    "<link rel='stylesheet' href='/main.css'>" +
                    "<link rel='stylesheet' href='https://fonts.googleapis.com/css2?family=Open+Sans:wght@800&display=swap'>" +
                "</head>" +
                "<body>" +
                    "<header>" +
                        "<nav>" +
                            "<ul>" +
                                "<li><a href='/create'>✏️ Create</a></li>" +
                                "<li><a href='/play'>\uD83C\uDFB2 Play</a></li>" +
                                "<li><a href='/search'>\uD83D\uDD0D Search</a></li>" +
                                "<li><a href='/flags'>\uD83C\uDF8C Flags</a></li>" +
                            "</ul>" +
                        "</nav>" +
                    "</header>" +
                    "<main>" +
                        content +
                    "</main>" +
                    "<footer>"+
                        authHtml +
                    "</footer>" +
                "</body>" +
            "</html>";
    }

    private static void createDatabase() throws SQLException, IOException {
        try {
            String sql = Files.readString(DB_SCRIPT_PATH);
            String[] commands = sql.split(System.lineSeparator() + System.lineSeparator());
            try (Connection c = db.getConnection()) {
                for (String command : commands) {
                    Statement s = c.createStatement();
                    s.executeUpdate(command);
                }
            }
        }
        catch(SQLException e) {
            if (Files.exists(DB_PATH)) {
                Files.delete(DB_PATH);
            }
            throw e;
        }
    }
}
