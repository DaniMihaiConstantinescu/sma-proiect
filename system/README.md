[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/FOb7RFS6)
[![Open in Codespaces](https://classroom.github.com/assets/launch-codespace-2972f46106e565e64193e422d61a12cf1da4916b45550586e14ef0a7c637dd04.svg)](https://classroom.github.com/open-in-codespaces?assignment_repo_id=18977093)
# Sisteme Multi-agent

## Curs 6/Laborator 3.

### Obiectivele laboratorului
-	Comunicarea în SMA
-	Interacțiunea agenților/Comunicare inter-agent
-	Mesaje


### Comunicarea inter-agent
Comunicarea și interacțiunea dintre agenți sunt caracteristici fundamentale ale sistemelor de agenți. Acestea se realizează prin schimburi de mesaje. Prin urmare, este foarte important ca agenții să folosească un format și o semantică compatibile pentru aceste mesaje. Deoarece JADE utilizează standarde FIPA, agenții JADE ar trebui, în mod implicit, să poată comunica cu alți agenți care rulează pe alte platforme.
În afară de conținut, un mesaj trebuie să aibă o listă de receptori, un expeditor, un format, un tip de mesaj etc. În JADE mesajele folosesc standardul ACL (Agent Communication Language), care permite mai multe tipuri de formate pentru conținutul mesajul (prezentate în Tabelul 2.2). De asemenea, permite conținutului mesajului să fie un obiect serializat. 
Comunicarea între agenți se bazează pe trimiterea de mesaje asincrone. Fiecare agent are o „căsuță poștală” (o coadă de mesaje), unde JADE stochează mesajele trimise de alți agenți și notifică agentul care primește. Trimiterea unui mesaj presupune crearea unui obiect de tip **ACLMessage** și apelarea metodei *send()* a agentului. Un agent poate selecta mesaje din coada sa de mesaje apelând metoda *receive()*, care returnează primul mesaj din coadă sau *null* dacă nu există mesaje.


```java
//trimiterea unui mesaj: creare obiect de tipul ACLMessage //si setarea destinatarului si a continutului mesajului
  ACLMessage message = new ACLMessage();
  AID receiverAID = new AID("Agent1", AID.ISLOCALNAME); // in acelasi container
  message.addReceiver(receiverAID);
  message.setContent("Salut");
  myAgent.send(message);

  //receiving a message: retrieve a message from the queue
  ACLMessage message = myAgent.receive();
  if (message != null)
  {
    String s = message.getContent() + " de la " +
      message.getSender().getLocalName();
    System.out.println(s);

    ACLMessage answer = new ACLMessage();
    answer.addReceiver(message.getSender());
    answer.setContent("Salut "+message.getSender());
    answer.setConversationId("ID1");
    myAgent.send(answer);

  } //end if
  else 
  {
    /* foarte important: fara acest else comportamentul se executa la infinit, nepermitand executarea altor comportamente 
     */ 
    block(); 
  }
```

![alt text](FIPACommunicativeActs.png "FIPA Communicative Acts")


### Message Transport Service (MTS)
MTS este un serviciu furnizat de o platformă de agenți pentru a transporta mesaje FIPA-ACL între agenți pe orice platformă dată și între agenți pe diferite platforme. Mesajele furnizează un plic de transport care cuprinde setul de parametri care detaliază, de exemplu, cui urmează să fie trimis mesajul. Structura generală a unui mesaj conform FIPA este prezentată în Figura 2.6.

![alt text](FIPAMessageStructure.png "FIPA Message Structure")


### Structura mesajului FIPA-ACL
Un mesaj FIPA-ACL conține un set de unul sau mai mulți parametri pentru mesaj. Exact ce parametri sunt necesari pentru o comunicare eficientă cu un agent va varia în funcție de situație; singurul parametru care este obligatoriu în toate mesajele ACL este *performative*, deși este de așteptat ca majoritatea mesajelor ACL să conțină și parametrii de expeditor, receptor și conținut. Parametrii mesajului FIPA-ACL sunt prezentați în Tabelul 2.1 fără a ține cont de codificările specifice. FIPA definește trei codificări specifice: String (notație EBNF), XML și Bit-Efficient.

![alt text](ACLMessageParameters.png "ACL Message Parameters")


### Sniffer Agent
În timp ce toate celelalte instrumente sunt în cea mai mare parte utilizate pentru depanarea unui singur agent, acest instrument este utilizat pe scară largă pentru depanare, sau pur și simplu pentru documentarea, conversațiilor dintre agenți. Este implementat de clasa jade.tools.sniffer.Sniffer. „Snifferul” se abonează la AMS-ul platformei pentru a fi notificat despre toate evenimentele platformei și despre toate schimburile de mesaje între un set de agenți specificați. 
Figura de mai jos arată interfața grafică a agentului Sniffer. Panoul din stânga este același ca al RMA, și este folosit pentru a naviga pe platforma de agenți și pentru a selecta agenții care urmează să fie urmăriți. Panoul din dreapta oferă o reprezentare grafică a mesajelor schimbate între agenții urmăriți, unde fiecare săgeată reprezintă un mesaj și fiecare culoare identifică o conversație. Atunci când utilizatorul decide să urmărească un agent sau un grup de agenți, fiecare mesaj direcționat către, sau care provine de la acel agent/grup este urmărit și afișat în interfața grafică sniffer. Utilizatorul poate selecta și vizualiza detaliile fiecărui mesaj individual, poate salva mesajul pe disc ca fișier text sau poate serializa o întreagă conversație ca fișier binar (de exemplu, util pentru documentare).

![alt text](SnifferAgent.png "Sniffer Agent")

Dacă există un fișier cu numele „sniffer.inf” sau „sniffer.properties” în directorul de lucru curent, sau într-un director părinte al directorului de lucru, acesta este citit de Agentul Sniffer la momentul lansării și tratat ca o listă de agenți de urmărit și, opțional, ca un filtru asupra performativelor mesajelor ACL. Formatul acestui fișier este o simplă secvență de linii, în care fiecare linie conține un nume de agent și, opțional, o listă de performative. Simbolurile *wildcard* „*” și „?” pot fi, de asemenea, utilizate în conformitate cu semnificația lor obișnuită a expresiei regulate. De exemplu, un fișier „*sniffer.properties*” cu următorul conținut:
```preload = ams;df;rma;Ion;Ana;Maria```
îi spune agentului Sniffer să urmărească agenții: AMS, DF, RMA, Ion, Ana și Maria. 
Pentru a urmării toți agenții, conținutul ar trebui să fie următorul:   ```preload = *```


## Exerciții de laborator

**E1.** Creați 2 tipuri de agenți. Un tip de agent care pune diferite întrebări, de exemplu, *cât este ora?* sau *unde locuiești?*. Și unul care răspunde la întrebările agenților. Creați agenții în containere diferite.



```java
public class Lab3 {
    public static void main(String[] args) {
        jade.core.Runtime rt = jade.core.Runtime.instance();
        Profile pMain = new ProfileImpl();
        AgentContainer mc = rt.createMainContainer(pMain);
        Profile pC1 = new ProfileImpl();
        pC1.setParameter(Profile.CONTAINER_NAME,"Container-1");
        pC1.setParameter(Profile.MAIN, "false");
        pC1.setParameter(Profile.MAIN_HOST, "localhost");
        pC1.setParameter(Profile.MAIN_PORT, "1099");
        pC1.setParameter(Profile.LOCAL_HOST, "localhost");
        pC1.setParameter(Profile.LOCAL_PORT, "1099");
        AgentContainer c1 = rt.createAgentContainer(pC1);
        
        try {
            AgentController rma = mc.createNewAgent("rma", "jade.tools.rma.rma", null);
            rma.start();
            AgentController snif = mc.createNewAgent("snif", "jade.tools.sniffer.Sniffer", null);
            snif.start();
            
            AgentController ac = mc.createNewAgent("Ion","ex1.Agent1", null);
            ac.start();
            mc.createNewAgent("Ana","ex1.Agent2",   new Object[] {"Cat este ora?"}).start();
            c1.createNewAgent("Maria","ex1.Agent2", new Object[] {"Unde locuiesti?"}).start();
            
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }
}
```

**E2.** Creați un chat automat la care să participe 5 agenți: Ion, Ana, ..., Maria. Fiecare agent va trimite periodic un mesaj, către un agent ales aleatoriu, de genul: 

```“[Agentul numeAgent intreaba] Cat este ora?”```

Agentul întrebat va răspunde printr-un mesaj de tipul:

```“[Agentul numeAgent raspunde]: Ora este 08:24”```

**E3.** Creați 3 tipuri de agenți:
1. agenți Ping-Pong care știu:
-	să răspundă la mesaje PING cu PONG
-	să se închidă/distrugă la primirea unui mesaj STOP
      Mesajul primit va avea următorul format:

      ```[AgentX->PING] sau [AgentX->STOP]```

2. agenți STOP, care știu să oprească agenții PING-PONG, aleși aleatoriu la intervale regulate.
3. un agent Manager care raportează periodic care dintre agenții PING-PONG sunt în viață trimițându-le mesaje PING.

Dezvoltați o aplicație care va conține un agent manager, mai mulți agenți PING-PONG și mai mulți agenți capabili să oprească un agent PING-PONG.

