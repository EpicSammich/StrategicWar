package strategicwar;

import java.util.ArrayList;


public class WarGame {

    private ArrayList<WarGameObserver> observers;

    private static final int MAX_HAND_SIZE = 5;
    private static final int NUM_WAR_PRIZES = 2;

    private WarAI ai;
    private int currentDifficulty;
    private boolean playing;
    private Deck playerDeck,aiDeck;
    private Hand playerHand, aiHand;
    private DiscardPile playerPile, aiPile;

    private Card playerCard;
    private Card aiCard;
    private ArrayList<Card> playerPrizes;
    private ArrayList<Card> aiPrizes;

    private Battle currentBattle;
    private War currentWar;
    private boolean warLoop;

    public enum GamePhase{Starting, Draw, BattleSelection, Battle, PreWar, War, RoundEnd, WinnerPlayer, WinnerAI}
    private GamePhase phase;

    private int lastVictor;

    public WarGame()
    {
        this.observers = new ArrayList<WarGameObserver>();
    }

    /**
     * Start the game
     * @param difficulty
     */
    public void start(int difficulty)
    {
        if(!this.playing)
        {
            this.currentDifficulty = difficulty;
            switch (difficulty)
            {
                case 1:
                    this.ai = new HardAI();
                    break;
                default:
                    this.ai = new EasyAI();
                    break;
            }
            this.playing = true;
            this.initialize();
            this.alertAll();
           this.drawPhase();
        }
    }

    public void setPlaying(boolean playing)
    {
        this.playing = playing;
    }

    public int getCurrentDifficulty()
    {
        return this.currentDifficulty;
    }

    public GamePhase getPhase()
    {
        return this.phase;
    }

    public Card getPlayerCard()
    {
        return this.playerCard;
    }

    public Card getAICard()
    {
        return this.aiCard;
    }

    public int getLastVictor()
    {
        return this.lastVictor;
    }

    public Hand getPlayerHand()
    {
        return this.playerHand;
    }

    public Hand getAIHand()
    {
        return this.aiHand;
    }

    public ArrayList<Card> getPlayerPrizes()
    {
        return this.playerPrizes;
    }

    public ArrayList<Card> getAIPrizes()
    {
        return this.aiPrizes;
    }

    public int getPlayerPileCount()
    {
        return this.playerPile.cardCount();
    }

    public  int getPlayerDeckCount()
    {
        return this.playerDeck.getSize();
    }

    /**
     * Setup the required game objects for a new game
     */
    private void initialize()
    {
        this.warLoop = false;
        this.lastVictor = -1;
        this.playerPrizes = new ArrayList<Card>();
        this.aiPrizes = new ArrayList<Card>();
        this.phase = GamePhase.Starting;
        Deck mainDeck = DeckFactory.getInstance().createDeck();
        System.out.println("Preshuffle: " + mainDeck + "\nCount: " + mainDeck.getSize());
        mainDeck = Deck.shuffleDeck(mainDeck);
        System.out.println("Postshuffle: " + mainDeck + "\nCount: " + mainDeck.getSize());
        Deck[] decks = Deck.splitDeck(mainDeck);
        this.playerDeck = decks[0];
        this.aiDeck = decks[1];
        System.out.println("Player Deck: " + this.playerDeck + " AI deck: " + this.aiDeck);
        System.out.println("Player count: " + this.playerDeck.getSize() + " AI count: " + this.aiDeck.getSize());
        this.playerHand = new Hand();
        this.aiHand = new Hand();
        this.playerPile = new DiscardPile();
        this.aiPile = new DiscardPile();
        System.out.println("New Game Setup");
    }

    /**
     * DRAW PHASE: Refresh player and AI hands before proceeding to SELECTION PHASE
     */
    private void drawPhase()
    {
        System.out.println("Draw phase");
        this.phase = GamePhase.Draw;
        this.drawCards(this.playerDeck, this.playerPile, this.playerHand);
        System.out.println("Player hand: " + this.playerHand.toString());
        this.drawCards(this.aiDeck, this.aiPile, this.aiHand);
        System.out.println("AI hand: " + this.aiHand.toString());
        this.alertHands();
        this.alertDecks();
        this.alertPiles();
        this.selectionPhase();
    }

    /**
     * SELECTION PHASE: AI selects a card and unlocks player selection
     */
    private void selectionPhase()
    {
        System.out.println("Selection phase");
        this.phase = GamePhase.BattleSelection;
        this.alertPhase();
        this.aiCard = this.ai.selectBattleCard(this.aiHand);
        System.out.println("AI selects: " + this.aiCard.toAbbrevString());
    }

    /**
     * Called by the mouseClicked event with the index of the card clicked
     * @param index
     */
    public void playerSelectedCard(int index)
    {
        this.playerCard = this.playerHand.selectCard(index);
        System.out.println("Player selects: " + this.playerCard.toAbbrevString());
        this.alertHands();
        if(this.phase == GamePhase.BattleSelection)
        {
            this.battlePhase();
        }
        else if(this.phase == GamePhase.PreWar)
        {
            this.warPhase();
        }
    }

    /**
     * BATTLE PHASE: Creates a Battle object and determines a winner or war condition
     */
    private void battlePhase()
    {
        System.out.println("Battle phase");
        this.phase = GamePhase.Battle;
        this.alertPhase();
        this.alertPrizes();
        this.alertBattleCards();
        this.currentBattle = new Battle(this.playerCard, this.aiCard);
        this.aiCard = null;
        this.playerCard = null;
        System.out.println("Battle: " + this.currentBattle.toString());
        switch (this.currentBattle.getWinner())
        {
            case 0:
                this.lastVictor = 0;
                this.alertVictor();
                this.roundEndPhase(0);
                break;
            case 1:
                this.lastVictor = 1;
                this.alertVictor();
                this.roundEndPhase(1);
                break;
            case 2:
                this.lastVictor = 2;
                this.alertVictor();
                preWarPhase();
                break;
        }
    }

    /**
     * PRE-WAR PHASE: preps the variables for war
     */
    private void preWarPhase()
    {
        System.out.println("Pre-War phase");
        this.playerPrizes =  new ArrayList<Card>();
        this.aiPrizes = new ArrayList<Card>();
        if(this.phase == GamePhase.Battle)
        {
         this.playerPrizes.add(this.currentBattle.playerCard);
         this.aiPrizes.add(this.currentBattle.aiCard);
        }
        if(this.playerHand.cardCount() > NUM_WAR_PRIZES)
        {
            this.playerPrizes.add(this.playerHand.pickRandom());
            this.playerPrizes.add(this.playerHand.pickRandom());
        }
        else
        {
            this.drawCards(this.playerDeck, this.playerPile, this.playerHand);
            this.alertDecks();
            this.alertPiles();
            if(this.playerHand.cardCount() > NUM_WAR_PRIZES)
            {
                this.playerPrizes.add(this.playerHand.pickRandom());
                this.playerPrizes.add(this.playerHand.pickRandom());
            }
            else if (this.playerHand.cardCount() == NUM_WAR_PRIZES)
            {
                this.playerPrizes.add(this.playerHand.pickRandom());
            }
        }
        this.alertHands();
        if(this.aiHand.cardCount() > NUM_WAR_PRIZES)
        {
            this.aiPrizes.add(this.aiHand.pickRandom());
            this.aiPrizes.add(this.aiHand.pickRandom());
        }
        else
        {
            this.drawCards(this.aiDeck, this.aiPile, this.aiHand);
            if(this.aiHand.cardCount() > NUM_WAR_PRIZES)
            {
                this.aiPrizes.add(this.aiHand.pickRandom());
                this.aiPrizes.add(this.aiHand.pickRandom());
            }
            else if (this.aiHand.cardCount() == NUM_WAR_PRIZES)
            {
                this.aiPrizes.add(this.aiHand.pickRandom());
            }
        }
        this.alertHands();
        this.alertPrizes();
        if (this.playerHand.cardCount() == 0)
        {
            this.gameOverPhase(0);
        }
        else if (this.aiHand.cardCount() == 0)
        {
            this.gameOverPhase(1);
        }
        else
        {
            this.alertHands();
            this.alertPrizes();
            this.phase = GamePhase.PreWar;
            this.alertPhase();
            this.aiCard = this.ai.selectBattleCard(this.aiHand);
            System.out.println("AI selects: " + this.aiCard.toAbbrevString());
            this.alertHands();
        }
    }

    /**
     * WAR PHASE: Initiates a War object and determines a 
     * winner or war condition
     */
    private void warPhase()
    {
    	System.out.println("War phase");
        this.phase = GamePhase.War;
        this.alertPhase();
        this.alertBattleCards();
        //victoryLabel.setText("");
        if(this.warLoop)
        {
            this.currentWar.additionalWar(playerCard, aiCard, playerPrizes, aiPrizes);
        }
        else
        {
            this.currentWar = new War(playerCard, aiCard, playerPrizes, aiPrizes);
        }
        this.aiCard = null;
        this.playerCard = null;
        this.playerPrizes.clear();
        this.aiPrizes.clear();
    	switch (this.currentWar.getWinner())
        {
            case 0:
                this.lastVictor = 0;
                this.alertVictor();
                this.warLoop = false;
                this.roundEndPhase(0);
                break;
            case 1:
                this.lastVictor = 1;
                this.alertVictor();
                this.warLoop = false;
                this.roundEndPhase(1);
                break;
            case 2:
                this.lastVictor = 2;
                this.alertVictor();
            	this.warLoop = true;
                this.preWarPhase();
                break;
        }
    }

    /**
     * ROUND END PHASE: End of a game round, distribute winnings and check win condition.
     * If no victor continue to draw phase.
     */
    private void roundEndPhase(int winner)
    {
        if(this.phase == GamePhase.Battle)
        {
            if(winner == 0)
            {
                this.aiPile.addCards(this.currentBattle.getCards());
            }
            else
            {
                this.playerPile.addCards(this.currentBattle.getCards());
            }
        }
        else
        {
            if(winner == 0)
            {
                this.aiPile.addCards(this.currentWar.getCards());
            }
            else
            {
                this.playerPile.addCards(this.currentWar.getCards());
            }
        }
        this.alertPrizes();
        this.alertPiles();
        this.alertScore();
        this.phase = GamePhase.RoundEnd;
        this.alertPhase();
        if(this.getPlayerScore() == 52)
        {
            System.out.println("The Player Wins the Game!");
            this.gameOverPhase(1);
        }
        else if (this.getComputerScore() == 52)
        {
            System.out.println("The Computer Wins the Game!");
            this.gameOverPhase(0);
        }
        else
        {
            this.drawPhase();
        }
    }

    private void gameOverPhase(int winner)
    {
        this.playing = false;
        if(winner == 0)
        {
            this.phase = GamePhase.WinnerAI;
        }
        else
        {
            this.phase = GamePhase.WinnerPlayer;
        }
        this.alertPhase();
    }

    public int getPlayerScore() {
        return this.playerDeck.getSize() + this.playerHand.cardCount() + this.playerPile.cardCount();
    }

    public int getComputerScore()
    {
        return this.aiDeck.getSize() + this.aiHand.cardCount() + this.aiPile.cardCount();
    }


    /**
     * Determines number of cards to draw to a specified hand
     * @param deck deck to draw from
     * @param pile pile to draw from
     * @param hand hand to draw to
     */
    private void drawCards(Deck deck, DiscardPile pile, Hand hand)
    {
        int toDraw = MAX_HAND_SIZE - hand.cardCount();
        if(deck.getSize() < toDraw)
        {
            deck.addCards((pile.removeCards().toArray(new Card[pile.cardCount()])));
            if(deck.getSize() < toDraw)
            {
                toDraw = deck.getSize();
            }
        }
        hand.addCards(deck.drawCards(toDraw));
    }


    public void addObserver(WarGameObserver observer)
    {
        this.observers.add(observer);
    }

    private void alertHands()
    {
        for(WarGameObserver o : this.observers)
        {
            o.handsUpdated();
        }
    }

    private void alertPiles()
    {
        for(WarGameObserver o : this.observers)
        {
            o.pileUpdated();
        }
    }

    private void alertScore()
    {
        for(WarGameObserver o : this.observers)
        {
            o.scoresUpdated();
        }
    }

    private void alertVictor()
    {
        for(WarGameObserver o : this.observers)
        {
            o.victorUpdated();
        }
    }

    private void alertDecks()
    {
        for(WarGameObserver o : this.observers)
        {
            o.deckUpdated();
        }
    }

    private void alertPhase()
    {
        for(WarGameObserver o : this.observers)
        {
            o.phaseUpdated();
        }
    }

    private void alertPrizes()
    {
        for(WarGameObserver o : this.observers)
        {
            o.prizesUpdated();
        }
    }

    private void alertBattleCards()
    {
        for(WarGameObserver o : this.observers)
        {
            o.battleCardsUpdated();
        }
    }

    private void alertAll()
    {
        for(WarGameObserver o : this.observers)
        {
            o.deckUpdated();
            o.phaseUpdated();
            o.scoresUpdated();
            o.victorUpdated();
            o.handsUpdated();
            o.pileUpdated();
            o.prizesUpdated();
            o.battleCardsUpdated();
        }
    }

}
