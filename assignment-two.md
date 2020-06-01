## Uppgift

### 1. Säkerhetshål

I detta program ([`AssignmentTwoServer`](src/main/java/se/plushogskolan/AssignmentTwoServer.java)) finns följande säkerhetshål:

- Åtminstone en *path traversal*.
- Åtminstone två *SQL injection*.
- Åtminstone tre *cross-site scripting* (XSS).

[Video som beskriver programmet](https://web.microsoftstream.com/video/62e92774-9d8f-4ffa-82bd-0da4449958dd)

Din uppgift är att hitta och åtgärda dessa säkerhetshål och dokumentera dem enligt samma mall som vi har följt tidigare:

> 1. **Exploit:** An example of how to perform an exploit and what the effects are.
> 2. **Vulnerability:** Where the vulnerable lines of code are and why they are vulnerable.
> 3. **Fix:** How to fix the vulnerable lines of code and why the fix works.

Betygsättningen styrs bland annat av hur många säkerhetshål du hittar:

- För **godkänt (G)** bör du hitta **tre** av säkerhetshålen.
- För **väl godkänt (VG)** bör du hitta **samtliga** säkerhetshål: en *path traversal*, två *SQL injection*, tre *XSS*.

Dokumentationens målgrupp ska vara någon som kan Java och känner till kodbasen men inte har någon förståelse av denna typ av säkerhetshål. Lägg med de **relevanta** delarna av koden där det är lämpligt för att förklara tydligare. Om en förklaring återkommer flera gånger, skriv denna på bara en plats och referera tillbaka till den där det behövs.

Dokumentationen kan skrivas på antingen svenska eller engelska (men håll dig till ett av språken).

- [Exempel på dokumentation](https://github.com/security-teknikhogskolan-vt-2020/security-teknikhogskolan-vt-2020/blob/master/example-documentation.md)
- Fler exempel finns i lösningsförslagen till många av kursens lektioner

För avsnittet *exploit* till *XSS* räcker det att visa att du kan få upp en `alert`-ruta hos användaren. För *path traversal* och *SQL injection* behöver du visa specifika *exploits* för just de säkerhetshål som du hittar.

Om du hittar fler *path traversal*, *SQL injection* och/eller *XSS* än vad som har nämnts ovan (1+2+3) så måste du själv välja vilka du vill dokumentera, genom att prioritera de som enligt din bedömning är allvarligast. Du kan fortfarande ta med de ytterligare säkerhetshålen i din dokumentation, men då med **enbart ett stycke text per extra säkerhetshål**.

Det finns troligtvis säkerhetshål av andra typer, exempelvis CSRF. **Till denna uppgift ska du enbart leta efter *path traversal*, *SQL injection* och *XSS*.** Om du hittar andra säkerhetshål av andra typer ska du inte ta med dem i din dokumentation.

### 2. Säkerhetsprinciper

Oavsett betygsnivå ska du i din dokumentation också besvara följande fråga:

**Vilka är de viktigaste lärdomarna och principerna om säkerhet som du kommer att ta med dig från denna kurs?**

*Denna fråga är en mindre del av den totala bedömningen och jag väntar mig ett svar på högst 300 ord.*

## Betygsättning

Betygsättningen är en helhetsbedömning som innefattar bland annat:

- Hur många säkerhetshål du har hittat (enligt anvisningarna ovan).
- Hur tydligt du har dokumenterat varje säkerhetshål.
- Hur effektiv din åtgärd (*fix*) för varje säkerhetshål är.
- Dina resonemang kring säkerhetsprinciper.

Betygsättning görs individuellt utifrån din individuella dokumentation. Det är tillåtet att använda sig av samma kod som dina gruppkamrater, men all annan text i dokumentationen måste vara din egen.

## Inlämning

Inlämning kommer att ske via PingPong och enligt följande instruktioner:

1. Inlämningen ska göras individuellt, även om du arbetar i grupp.

2. Gå till [inlämningsformuläret i PingPong](https://yh.pingpong.se/courseId/9771/content.do?id=4367158).

3. Om du arbetar i grupp, ange namnen på dina gruppkamrater i kommentarsfältet. **Detta är viktigt för att delad kod inte ska riskera att bedömas som plagiat.**

4. Lämna in din dokumentation som en enda fil.
    
    - Du ska inte lämna in din kod i någon separat fil. Din dokumentation ska innehålla de **relevanta** delarna av koden tillsammans med dina beskrivningar.
    
    - De mest lämpliga filformaten är HTML, Markdown (`.md`) och EPUB, varav vissa kan exporteras av flera vanliga verktyg, inklusive Google Docs. **Detta är för att jag enkelt ska kunna läsa kodstycken utan att de bryts upp över flera sidor och utan att långa rader bryts upp.**
    
    - Om du av någon anledning inte kan exportera till ovanstående format så kan du använda PDF, men **enbart om du ser till att kodstycken inte formateras och bryts upp på ett svårläsligt sätt**. Om dokumentationen är svårläslig av denna anledning så kan jag kräva retur utan ytterligare kommentarer.
    
    - Format som DOCX och ODT (avsedda för ordbehandlingsprogram) kommer inte att godtas.
    
    - Om du vill använda dig av ett annat format som du tror är lämpligt, kontakta mig först för godkännande.

## Tips

Några ytterligare tips:

- Skicka in din lösning även om du inte har hittat alla säkerhetshål för den betygsnivå som du siktar på. Om din dokumentation i övrigt håller hög kvalitet så kan detta fortfarande vara nog.

- Dokumentera samtliga säkerhetshål som du hittar även om du inte nödvändigtvis kan dokumentera varje del av dem. Om du exempelvis hittar en *exploit* men inte motsvarande *fix*, eller vice versa, så kan detta fortfarande vara nog.

- Vid bedömningen läggs större vikt vid hur du beskriver och åtgärdar säkerhetshålen (*vulnerability* och *fix*) än hur du utnyttjar dem (*exploit*).

- Utgå gärna från [How to find path traversal vulnerabilities](https://github.com/security-teknikhogskolan-vt-2020/security-teknikhogskolan-vt-2020/blob/master/PathTraversal/howto.md) som första och viktigaste steg för att hitta säkerhetshålen. Processen är samma för alla tre typer av säkerhetshål; det som skiljer sig är vilka metodanrop man behöver leta efter: manipulering av filer för *path traversal*, anrop mot databasen för *SQL injection*, skapande av HTML för *XSS*.