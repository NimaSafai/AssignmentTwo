# Path Traversal (1 of 1)

_This is the documentation for the Path Traversal safety vulnerability that we have found in the Assignment code._

## Exploit

1. Acquire the URL of one of the flag images in the flag section, e.g. http://localhost:7000/flag?name=sweden.svg, by right-clicking and choosing "Open image in new tab".

2. Change the URL to http://localhost:7000/flag?name=../pom.xml or http://localhost:7000/flag?name=../keys.xml.

3. Access files/folders that you actually shouldn't be able to, e.g. the POM file or the keys.xml file in the above examples.

## Vulnerability

The vulnerability of this particular exploit lies within this code snippet, and specifically in the third line of code:

    private static void singleFlagPage(Context context) throws IOException {
        String flagName = context.queryParam("name");
        Path path = Path.of("flags/" + flagName);
    
The issue with this piece of code is that it creates a relative path. This can easily be manipulated with the usage of `../` which will step out of the `flags` folder and takes us to the parent directory of the given path. Thus, if this isn't accounted for in the code, it's a major safety issue that can be taken advantage of in order to access files and folders that should be secure from unauthorised users.

## Fix

Thankfully, fixing these kinds of Path Traversal vulnerabilities is quite easy and what we need to do is to fully resolve the path.

The addition of `.toRealPath()` does this for us and turns the relative path into an absolute path and invalidates sequences like `../` etc.

Thus, the code now looks like this:

    private static void singleFlagPage(Context context) throws IOException {
        String flagName = context.queryParam("name");
        Path path = Path.of("flags/" + flagName).toRealPath();
        
However, the same needs to be done with the flag folder path, too, so that it also becomes an absolute path and we can compare the two.

        Path flagFolder = Path.of("flags").toRealPath();

The addition of an if statement allows us to return an error message, like such:
        
        if (!path.startsWith(flagFolder)) {
            context.status(403);
            context.result("You cannot access files outside the flag folder.");

            return;
        }
        
##
        
# SQL Injection (1 of 2)

_This is the documentation for one of the SQL Injection safety vulnerabilities that we have found in the Assignment code._

## Exploit

1. By clicking on one of the quizzes, the quiz ID appears in the URL, e.g. http://localhost:7000/play/3 or http://localhost:7000/play/4.

2. Typing in http://localhost:7000/play/5 returns the 404 message _"No quiz with ID 5 or you are not allowed to access this quiz"_, hinting that there is a possibility of a private quiz. 

3. With the SQL Injection method **double dash**, http://localhost:7000/play/5--, we get access to a quiz that is private.

## Vulnerability

Looking at the source code, we can easily find the vulnerability in this snippet of code:
    
    private static void singleQuizData(Context context) throws SQLException {
        try (Connection c = db.getConnection()) {
            Statement quizStatement = c.createStatement();
            String quizSql =
                "SELECT * FROM quiz " +
                // Either the current user owns the quiz and can access it whether it's public or not.
                "WHERE id = " + context.pathParam("quiz_id") + " " +
                "AND user_id = " + context.sessionAttribute("userId") + " " +
                // Or it's public and anybody can access it.
                "OR public = TRUE AND id = " + context.pathParam("quiz_id");
            ResultSet quizRows = quizStatement.executeQuery(quizSql);

What the double dash (or double hyphen) method does is that everything after the double dash is out-commented. So, the apparent safeguard that is there in the code that is supposed to prevent other users from seeing private quizzes is out-commented by simply adding a double dash after the quiz ID.


## Fix

The best way of fixing the code and strengthening it against this classic SQL injection is by changing it to a so-called **Prepared Statement**. Prepared statements are resilient against SQL injection, because parameter values, which are transmitted later using a different protocol, need not be correctly escaped. If the original statement template is not derived from external input, SQL injection cannot occur.
Thus, the fixed code looks like this:
    
    private static void singleQuizData(Context context) throws SQLException {
        try (Connection c = db.getConnection()) {
            PreparedStatement quizStatement = c.prepareStatement("SELECT * FROM quiz WHERE id = ? AND user_id = ? OR public = TRUE AND id = ?");
                quizStatement.setString(1, context.pathParam("quiz_id"));
                quizStatement.setInt(2, context.sessionAttribute("userId"));
                quizStatement.setString(3, context.pathParam("quiz_id"));
            ResultSet quizRows = quizStatement.executeQuery();
            
With the help of this fix, the error message that is returned is: _"No quiz with ID 5--, or you are not allowed to access this quiz."_

[Great success!](https://i.pinimg.com/originals/d3/90/c2/d390c243d97ad215ad83e6dc6f2dda56.jpg)

##

# XSS (1 of 3)

_This is the documentation for the first one of the XSS safety vulnerabilities that we have found in the Assignment code._

## Exploit

1. Go to the Search page.

2. Type a script in the search bar, e.g. `<script>alert("You've been hacked!")</script>`.

3. When pressing ENTER, the code is run and - in this case - an alert pops up with the text `You've been hacked!` which indicates that the code is vulnerable to XSS attacks.

## Vulnerability

The most common types of cross-site scripting comes from taking advantage of user inputs. Thus, any point in our code where there are user inputs can immediately be targeted and if they're not properly secured, they can be taken advantage of.
The vulnerability in our code lies within this snippet:

    if (context.queryParam("search") != null) {
            content +=
                "<p>Search results for: " + context.queryParam("search") + "</p>" +
                "<ul>";

NB: it is specifically `context.queryParam("search")` that contains the vulnerability. It's currently un-encoded which completely leaves our code vulnerable to XSS payloads.


## Fix

Text

    if (context.queryParam("search") != null) {
            // Show what term the user searched for.
            content += 
                    "<p>Search results for: " + Encode.forHtml(context.queryParam("search")) + "</p>" +
                "<ul>";

##

# XSS (2 of 3)

_This is the documentation for the second XSS safety vulnerability that we have found in the Assignment code._

## Exploit

1. Go to the Create page to create a new quiz.

2. As the title of your quiz, type a script, such as `<script>alert("You've been hacked again!")</script>`. Create the quiz.

3. Then, whenever the Play tab is clicked (or http://localhost:7000/play is accessed), an alert pops up saying `You've been hacked again!`

## Vulnerability

Text

    try (Connection c = db.getConnection()) {
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

## Fix

Text

    try (Connection c = db.getConnection()) {
            String title = context.formParam("quiz-title");
            boolean isPublic = context.formParam("quiz-public") != null;
            PreparedStatement s1 = c.prepareStatement(
                "INSERT INTO quiz (user_id, title, datetime, public) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );
            s1.setInt(1, context.sessionAttribute("userId"));
            s1.setString(2,Encode.forHtml(title));
            s1.setString(3, LocalDateTime.now().toString());
            s1.setBoolean(4, isPublic);
            s1.executeUpdate();

##
