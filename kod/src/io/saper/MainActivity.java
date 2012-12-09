package io.saper;

import java.awt.font.NumericShaper;
import java.util.Random;

import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
private TextView zegar, minecount;
private ImageButton smiley;
private Block blocks[][];
private TableLayout pole_minowe;
private int number_of_rows = 9;
private int number_of_columns = 9;
private int wielkosc_pola = 15;
private final int odstep = 3;
/** maksymalna liczba min */
private int minesTotal = 10;
private int minesToFind; // pozosta�e do znalezienia miny

private Handler timer = new Handler();
private int czas = 0;

private boolean isTimerstarted = false; // je�li true, to znaczy, �e zegar rozpocz�� odliczanie
private boolean areMinesSet = false; // je�li true, to znaczy, �e miny zosta�y ustawione
private boolean isGameOver; // je�li true, to gra zosta�a zako�czona
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        /*tworzenie napis�w(licznik�w)*/
        zegar = (TextView) findViewById(R.id.timer);
        minecount = (TextView) findViewById(R.id.licznik_min);
        minecount.setText(String.format("%03d", minesTotal));
        
        /*tworzenie referencji do przycisku*/
        smiley = (ImageButton) findViewById(R.id.imageButton1);
        
        /*zmienianie czcionki licznikow*/
        Typeface font = Typeface.createFromAsset(getAssets(), "fonts/LCDM2B__.TTF");
        zegar.setTypeface(font);
        minecount.setTypeface(font);
        
        /*ustawianie pola minowego*/
        pole_minowe = (TableLayout) findViewById(R.id.pole_minowe);
        
        // rozpoczecie gry
        startNewGame();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    /** Funkcja zwracaj�ca status gry
     * @return true je�li gra zosta�a zako�czona, false je�li nie zosta�a zako�czona
     */
    public boolean isGameOver()
    {
    	return isGameOver;
    }
    /** funkcja rozpoczynaj�ca gr�
     * 
     */
    public void startNewGame()
    {
    	// tworzenie pola minowego
        createMineField();
        showMineField();
        
        minesToFind = minesTotal;
        isGameOver = false;
        czas = 0;
    }
    /**tworzy tablelayout i pokazuje pole minowe*/
    private void showMineField()
    {
    	for(int wiersz = 1; wiersz < number_of_rows + 1; wiersz++)
    	{
    		TableRow table = new TableRow(this);
    		table.setLayoutParams(new TableRow.LayoutParams((wielkosc_pola + 2 * odstep) * number_of_columns, wielkosc_pola + 2 * odstep));
    		
    		for(int kolumna = 1; kolumna < number_of_columns + 1; kolumna++)
    		{
    			blocks[wiersz][kolumna].setLayoutParams(new TableRow.LayoutParams(  
    					wielkosc_pola + 2 * odstep,  
    					wielkosc_pola + 2 * odstep));
    			blocks[wiersz][kolumna].setPadding(odstep, odstep, odstep, odstep);
    			table.addView(blocks[wiersz][kolumna]);
    		}
    		pole_minowe.addView(table,new TableLayout.LayoutParams(  
					(wielkosc_pola + 2 * odstep) * number_of_columns, wielkosc_pola + 2 * odstep));;
    	}
    }
    /**tworzy pole minowe*/
    private void createMineField()
{
 // po 2 dodatkowe wiersze i kolumny (potrzebne do obliczania pobliskich min)
	blocks = new Block[number_of_rows + 2][number_of_columns + 2];
	
	//inicjalizowanie p�l z minami
	for(int wiersz = 0; wiersz < number_of_rows + 2; wiersz++)
	{
		for(int kolumna = 0; kolumna < number_of_columns + 2; kolumna++)
		{
			blocks[wiersz][kolumna] = new Block(this);
			
			// ustawiamy na final, �eby przekaza� do klas anonimowych listener�w
			final int currentRow = wiersz;
			final int currentColumn = kolumna;
			
			// tutaj mo�na zaimplementowa� co si� dzieje po kr�tkim klikni�ciu
			blocks[wiersz][kolumna].setOnClickListener(new OnClickListener()
			{
				public void onClick(View v)
				{
					// je�li zegar nie rozpocz�� odliczania, rozpocznij odliczanie
					if(!isTimerstarted)
					{
						startTimer();
						isTimerstarted = true;
					}
					
					if(!areMinesSet)
					{
						setMines(currentRow,currentColumn);
						areMinesSet = true;
					}
					
					// je�li pole nie jest zaznaczone flag�
					if(!blocks[currentRow][currentColumn].isFlagged())
					{
						if(blocks[currentRow][currentColumn].isMined())
						{
							blocks[currentRow][currentColumn].setMineIcon();
							gameLose();
						}
						
						if(blocks[currentRow][currentColumn].isCovered())
						{
							blocks[currentRow][currentColumn].uncover();
						}
					}
					
					// sprawdzenie czy gra zosta�a zako�czona
					if(checkWin())
					{
						gameWin();
					}
				}
			});
			
			
			// tutaj mo�na zaimplementowa� co si� dzieje po d�ugim klikni�ciu
			blocks[wiersz][kolumna].setOnLongClickListener(new OnLongClickListener()
				{
					
					public boolean onLongClick(View v)
					{
						if(!blocks[currentRow][currentColumn].isFlagged() && minesToFind >= 1)
						{
							// zmiejszamy liczb� min do znalezienia
							minesToFind--;
							// ustawiamy ikonk� flagi
							blocks[currentRow][currentColumn].setFlagIcon(true);
							// ustawiamy znacznik
							blocks[currentRow][currentColumn].setFlagged(true);
							// update minecount
							updateMineCount();
							
						}
						else if(blocks[currentRow][currentColumn].isFlagged())
						{
							// zwi�kszamy liczb� min do znalezienia
							minesToFind++;
							// zmieniamy ikonke
							blocks[currentRow][currentColumn].setFlagIcon(false);
							// ustawiamy znacznik
							blocks[currentRow][currentColumn].setFlagged(false);
							// update mineCount
							updateMineCount();
						}
						return true;
					}
				});
		}
	}
}
/** rozpoczyna odliczanie czasu */
public void startTimer()
{
	if(czas == 0)
	{
		timer.removeCallbacks(updateTime);
		timer.postDelayed(updateTime, 500);
	}
}
/** funkcja zatrzymuj�ca czas */
public void stopTimer()
{
	timer.removeCallbacks(updateTime);
}
/** Zadanie stworzone na potrzeby zegara ( klasa anonimowa )*/
private Runnable updateTime = new Runnable()
{
   public void run()
    {
    		long czasRozpoczeciaZadania = System.currentTimeMillis();
    		czas++; // odliczamy jedna sekund�
    		zegar.setText(String.format("%03d", czas)); // update zegara
    		
    		// ustaw zadanie na czas rozpoczecia zadania, a nast�pnie op�nij go o sekund�
    		timer.postAtTime(this, czasRozpoczeciaZadania);
    		timer.postDelayed(updateTime, 1000);
    }
};
/** metoda ustawiaj�ca miny na losowe miejsca */
public void setMines(int blockRow, int BlockColumn)
{
	Random rand = new Random();
	
	int randomRow, randomColumn;
	
	
	// losujemy miejsce dla min tyle razy ile ma by� min
	for(int i = 0; i < minesTotal; i++)
	{
		
		randomRow = rand.nextInt(number_of_rows) + 1;
		randomColumn = rand.nextInt(number_of_columns) + 1;
		
		// sprawdzamy czy to blok klikniety jako pierwszy ( jesli tak, to nie stawiamy tam miny)
		if(randomRow == blockRow && randomColumn == BlockColumn)
		{
			i--;
			continue;
		}
		// sprawdzamy czy na miejscu juz jest jakas mina
		if(blocks[randomRow][randomColumn].isMined())
		{
			i--;
			continue;
		}
		blocks[randomRow][randomColumn].setMine();
	}
	
	// obliczanie ilosci min dookola p�l
	
	int nearbyMines;
	
	for(int row = 0; row < number_of_rows + 2; row++)
	{
		for(int column = 0; column < number_of_columns + 2; column++)
		{
			nearbyMines = 0;
			
			// nie szukamy dla pierwszego i ostatniego rz�du i kolumny (s� to rz�dy i kolumny pomocnicze)
			if((row != 0) && (row != (number_of_rows + 1)) && (column != 0) && (column != (number_of_columns + 1)))
			{
				// szukamy dooko�a pola
				for(int poprzedni_r = -1; poprzedni_r < 2; poprzedni_r++)
				{
					for(int poprzedni_k = -1; poprzedni_k < 2; poprzedni_k++)
					{
						if(blocks[row + poprzedni_r][column + poprzedni_k].isMined())
						{
							nearbyMines++;
						}
					}
				}
				
				// ustaw znalezion� liczb� min do pami�ci pola
				blocks[row][column].setMinesSurrounding(nearbyMines);
			}
		}
	}
}

/** funkcja ustawiaj�ca liczb� min */
public void updateMineCount()
{
	minecount.setText(String.format("%03d", minesToFind));
}


/** funkcja sprawdzaj�ca czy gracz wygra� gr�
 * @version 1.0
 * @return true je�li wszystkie pola, kt�re nie by�y zaminowane zosta�y odkryte,
 * false je�li nie wszystkie zosta�y odkryte
 */
public boolean checkWin()
{
	for(int i = 1; i < number_of_rows + 1; i++)
	{
		for(int j = 1; j < number_of_columns + 1; j++)
		{
			if(blocks[i][j].isFlagged())
			{
				continue;
			}
			else if(!blocks[i][j].isMined() && blocks[i][j].isCovered())
			{
				return false;
			}
		}
	}
	return true;
}
/** funkcja wywo�ywana przy wygraniu gry
 * 
 */
public void gameWin()
{
	stopTimer();
	isGameOver = true;
	
	smiley.setBackgroundResource(R.drawable.smiech);
	
	activateButtons(false);
	
	showDialogBox("Gratulacje, wygra�e�!",czas);
	
}
/** funkcja wywo�ywana przy przegraniu gry
 * 
 */
public void gameLose()
{
	stopTimer();
	isGameOver = true;
	
	smiley.setBackgroundResource(R.drawable.przestraszenie);
	
	activateButtons(false);
	
	
	showDialogBox("Niestety, przegra�e�!", czas);
	
}
/** funkcja w��czaj�ca i wy��czaj�ca przyciski
 * 
 */
public void activateButtons(boolean b)
{
	if(!b)
	{
		// wy��czanie przycisk�w
		for(int i = 0; i < number_of_rows + 1; i++)
		{
			for(int j = 0; j < number_of_columns + 1; j++)
			{
				blocks[i][j].setClickable(false);
				blocks[i][j].setLongClickable(false);
			}
		}
	}
	else
	{
		// w��czenie przycisk�w
		for(int i = 0; i < number_of_rows + 1; i++)
		{
			for(int j = 0; j < number_of_columns + 1; j++)
			{
				blocks[i][j].setClickable(true);
				blocks[i][j].setLongClickable(true);
			}
		}
	}
}
/** funkcja ko�cz�ca obecn� gr�
 * 
 */
public void endGame()
{
	isGameOver = true;
	stopTimer();
	zegar.setText("000");
	minecount.setText("000");
	isTimerstarted = false;
	areMinesSet = false;
	isGameOver = false;
	minesToFind = 0;
	
	pole_minowe.removeAllViews();
}

/** funkcja pokazuj�ca okienko w przypadku wygranej
 * @version 0.0
 */
public void showDialogBox(String message, int seconds)
{
	Context context = getApplicationContext();
	CharSequence text = message + "\nCzas gry: " + String.valueOf(seconds) + " sekund";
	int duration = Toast.LENGTH_LONG;
	
	Toast toast = Toast.makeText(context, text, duration);
	toast.setGravity(Gravity.CENTER, 0, 0);
	
	LinearLayout l = (LinearLayout) toast.getView();
	ImageView i = new ImageView(getApplicationContext());
	i.setBackgroundResource(R.drawable.smiech);
	l.addView(i,0);
	
	
	toast.show();
}

}