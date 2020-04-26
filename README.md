# TextTwist

TextTwist è un gioco in rete multiplayer client/server. Il gioco consiste nel riuscire a trovare più parole
possibili a partire da un gruppo di lettere. Un giocatore crea una partita ed invita altri a partecipare. Alla fine della
partita tutti i giocatori riceveranno i risultati e la classifica della partita. Il server mantiene in modo persistente la classifica generale di tutti i giocatori.

---------------------------
**STRUTTURA DEL PROGETTO**

Il progetto è costituito da 3 packages in cui vengono sviluppate le due componenti del sistema Client e
Server:

- **clientPackage** contiene le seguenti classi:
  - Client
  - ConfirmWaiter
  - WordsReader
  - WordsList
  
  La classe Client contiene il metodo main e si occupa di implementare l’interfaccia remota ClientRmiIF (importata dal commonPackage). All’avvio viene caricato il file config.properties contenente le impostazioni di rete che verranno utilizzate per
  le varie connessioni TCP e UDP e per ottenere l’interfaccia ServerRmiIF (anche essa importata dal commonPackage). Successivamente viene richiesto all’utente di effettuare il login o la registrazione che permetteranno di accedere alle funzionalità principali offerte dal Server.
  Le classi ConfirmWaiter e WordsReader si occuperanno di gestire il meccanismo dei timers durante la partita e di creare la lista di parole attraverso la classe WordsList.

- **serverPackage** contiene le seguenti classi:
  - Server
  - ClientHandler
  - Game
  - Rankings

  La classe Server contiene il metodo main e si occupa di implementare l’interfaccia remota ServerRmiIF
  (importata dal commonPackage).
  All’avvio vengono caricati i seguenti file:
  - config.properties: per ottenere le impostazioni di rete
  - dictionary.txt: per ottenere il dizionario
  - usersListFile.txt: per ottenere la lista degli utenti registrati
  - rankings.txt: per ottenere la classifica generale
  Gli ultimi due vengono creati al primo avvio del Server.
  Successivamente viene registrata l’ interfaccia ServerRmiIF e creata una ServerSocket TCP.
  A questo punto il Server resta in attesa di connessioni dai Client che verranno affidate ognuna ad un thread
  della classe ClientHandler che si occuperà di gestire le richieste di tutte le principali funzionalità offerte.
  La classe Game viene utilizzata per la gestione delle partite e la classe Rankings per la classifica generale.

- **commonPackage** contiene le seguenti strutture in comune:
  - ServerRmiIF
  - ClientRmiIF
  - Result
  L’ interfaccia remota ServerRmiIF permette di effettuare login,registrazione e logout.
  L’ interfaccia remota ClientRmiIF permette di effettuare l’invito ed inoltre è stato scelto di aggiungere un
  metodo, di cui viene gestita solamente l’eccezione, che permette di verificare se l’utente è ancora online.
  La classe Result viene utilizzata per la classifica generale e l’ invio dei risultati della partita.
--------------------------------------------------------
**ISTRUZIONI COMPILAZIONE ED ESECUZIONE**

Il progetto viene esportato in 2 file Runnable .jar: Client.jar e Server.jar
con cui viene esportata anche la libreria esterna Json non presente di default.
Viene fornito il file config.properties (identico sia per Client che per Server)
contenente i parametri da settare secondo la propria configurazione di rete.
E’ necessario che questo sia nella stessa directory dell’ eseguibile.
I parametri sono settati di default per l’esecuzione sullo stesso terminale nel
seguente modo:
- SERVER_ADDRESS=localhost
- RMI_NAME= TextTwistRmi
- TCP_PORT=2001
- RMI_PORT=2000
- MULTICAST_PORT=2002
E’ necessario modificare SERVER_ADDRESS con l’ indirizzo IP del terminale che
avvia il Server nel caso si voglia eseguirlo in LAN.

Per l’esecuzione basta semplicemente inserire da riga di comando:

java -jar Server.jar (LATO SERVER)

java -jar Client.jar (LATO CLIENT)
