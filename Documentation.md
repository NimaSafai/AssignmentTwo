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


This protects our code against a directory traversal. [Fantastic!](https://i.imgur.com/p4pWnIk.gif)

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
                "WHERE id = " + context.pathParam("quiz_id") + " " +
                "AND user_id = " + context.sessionAttribute("userId") + " " +
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

[Problem solved.](https://media.giphy.com/media/fm5JqspHFgIXm/200.gif)

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

As mentioned above, the part of the code which takes the user input for the Search function isn't encoded, which leaves it vulnerable. These types of vulnerabilities have a very easy fix and it is by simply adding `Encode.forHtml()` to the code.
In our case, we want to encode the user input which comes from `context.queryParam("search)`, so all of that gets encapsulated in our method parentheses like so:

    if (context.queryParam("search") != null) {
            // Show what term the user searched for.
            content += 
                    "<p>Search results for: " + Encode.forHtml(context.queryParam("search")) + "</p>" +
                "<ul>";

So now, with this fix, no alert pops up when we type in `<script>alert("You've been hacked!")</script>` in the Search field. Instead, we're faced with the single line `Search results for: <script>alert("You've been hacked!")</script>` and nothing else.

##

# XSS (2 of 3)

_This is the documentation for the second XSS safety vulnerability that we have found in the Assignment code._

## Exploit

1. Go to the Create page to create a new quiz.

2. As the title of your quiz, type a script, such as `<script>alert("You've been hacked again!")</script>`. Create the quiz.

3. Then, whenever the Play tab is clicked (_or http://localhost:7000/play is accessed in any way, such as through the Filter function_), an alert pops up saying `You've been hacked again!`

## Vulnerability

This vulnerability is basically the same as the other XSS attack that I documented here above. It has to do with the user input not being encoded, as we type the script in the field where the Quiz title is supposed to go. With the proper fix, the script shouldn't be read as anything but pure text, but in our case it's immediately parsed as the script that it is. 
The vulnerability lies in this snippet of code and specifically in the fourth from last line in the code, `s1.setString(2, title);`. That's the user input that goes into our code and as we can see, it's not encoded.

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

The fix of this vulnerability is the same as the one before, so I won't go into too much detail about it. It is simply by adding `Encode.forHtml()` and sending our `title` through it that makes sure that our user input is properly encoded and thus not vulnerable to any XSS payloads.
Here's how the code looks when fixed:

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

[Perfect.](https://25.media.tumblr.com/c9954bbae4c124ae9bbeefededd39fda/tumblr_miok00iHvu1rlf0uqo3_250.gif)

##


## SÄKERHETSPRINCIPER

Det här stycket ämnar att besvara frågan "Vilka är de viktigaste lärdomarna och principerna om säkerhet som du kommer att ta med dig från denna kurs?"

Egentligen skulle jag vilja säga att jag tar med mig alla olika typer av säkerhetshål som vi har lärt oss om; men mer specifikt handlar det om den nästan morbida fascinationen kring det hela. Inte nog med att man kan göra ofantligt stor skada med olika typer av attacker, men också faktumet att det finns så många olika typer av attacker och olika sätt man kan orsaka skada genom utnyttjandet av säkerhetshål. Det har gjort att mina ögon verkligen har öppnats upp för ett helt nytt segment inom programmering. 
Självklart visste jag att säkerhet inom programmering existerade som koncept, men att det skulle vara så här? Aldrig. Nu vet jag vad det faktiskt innebär i reella termer och jag tycker det är hur häftigt och fascinerande som helst. Det lägger till ytterligare en dimension i ens arbete som programmerare och det skapar nya utmaningar och trösklar att ta sig över. "Kan någon hacka det här?" är en fråga som jag kommer ställa mig själv varje gång jag skriver kod fr.o.m. nu!
Faktumet att vi har fått arbeta så konkret med allt det här har hjälpt otroligt mycket. Det var samma sak med Frontend-kursen; att vi varje lektion hade nya saker att testa och lära oss hjälpte verkligen i att elda på fascinationen som jag kände för ämnet och det har gjort att jag har fortsatt att utforska utanför lektionstiderna. Faktum är att jag redan har tagit med mig min lärdom ut i det verkliga livet! Det är nämligen så att jag samtidigt som plugget jobbar lite extra på möbelföretaget Mio och häromdagen upptäckte jag [ett XSS-hål på deras intranät](https://i.imgur.com/sgdHOYo.jpg), något som jag direkt rapporterade och fick en stor eloge för. Det kändes så kul att få faktiska bevis på att jag har lärt mig saker från den här kursen och det har genuint varit en upplysande kurs som helt och hållet har ändrat min syn på programmering.