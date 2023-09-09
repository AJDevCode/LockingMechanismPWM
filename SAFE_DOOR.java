
#include <hcs12.h>
#include <dbug12.h>

#include "lcd.h"
#include "keypad.h"
#include "util.h"


/**
 * 7 segments LED decoder
 * 0,1,2,3,4,5,6,7,8,9,A,B,C,D,E,F,G,H
 *
 * Example: if you want to show "1" on LED segments
 * you should do the following:
 * DDRB = 0xff; //set all pin on port b to output
 * PORTB = segment_decoder[1]; //which means one decodes to 0x06:
 * G F E D C B A
 * 0 0 0 0 1 1 0
 *
 *		 A
 * 		----
 * 	   |	| B
 * 	 F |  	|
 * 		--G-        ===> if B and C segments are one we get the shape of 1 (number one)
 * 	   |	| C
 * 	 E |	|
 * 		----
 *       D
 */
unsigned int segment_decoder[]={
                                 0x3f,0x06,0x5b,0x4f,0x66,
                                 0x6d,0x7d,0x07,0x7f,0x6f,
                                 0x77,0x7c,0x39,0x5e,0x79,
                                 0x71,0x3d,0x76
                               };


volatile char message_index_on_7segment_LEDs = 0;
volatile unsigned int counter_for_real_time_interrupt;
volatile unsigned int display_counter = 0;
volatile unsigned int counter_for_real_time_interrupt_limit;

void display_hex_number_on_7segment_LEDs(unsigned int number)
{
  static int index_on_7segment_LEDs = 0;

  //DDRB = 0xff; // PortB is set to be output.
  DDRP = 0xff;

  PTP = ~ (1 << (3 - index_on_7segment_LEDs)); //notice it is negative logic
  PORTB = segment_decoder[( number >> (char) (4*(index_on_7segment_LEDs)) ) & 0xf];

  index_on_7segment_LEDs++;
  /**
   * Index should be 1,2,4,8 ... we shift to left each time
   * example: 0001 << 1 will be: 0010 = 2
   * and 2 = 0010 << 1 will be: 0100 = 4
   * and so on ...
   */

  if (index_on_7segment_LEDs > 3) //means we reach the end of 4 segments LEDs we have
    index_on_7segment_LEDs = 0;

  /**
   * simple example of showing "7" on the first LEDs (the most left one)
   DDRB  = 0xff; // PortB is set to be output.
   DDRP  = 0xff;
   PTP   = ~0x1; //negative logic - means "7" will be shown on first LEDs
   PORTB = 0x07;
   */
}

#define PWM_PERIOD 255 // PWM period value

volatile char cal[5] = {'\0'};//varible to hold login attempt
volatile int calCnt = 0;//triverse through both pass and cal
volatile char pass[5] = {'0', '0', '0', '0','\0'};//the password that will open
volatile int attempts = 0; //keeps count of how many attempts made, (let users 3 max)
volatile long timer = 0; //keeps track of how long system is locked out or in idle
volatile char* wrd = "closed"; // holds string being displayed
volatile unsigned int keypad_debounce_timer = 0;
volatile int keypad_enabled = 1;// tells if keypad is on, 1 is true and 0 false
volatile int open = 0;// tells system is open, 1 is true and 0 false
volatile int showPass = 1;// tells if we are displaying the pass, 1 is true and 0 false
volatile int change = 0;// tells if we are changing the password, 1 is true and 0 false
//holds the number being displayed on leds
volatile char* num = "0000\0";
volatile int opening = 0;


void display_pass_on_7segment_LEDs(char* num) // Modified display leds to display pass attempts on all 4 leds
{
	for (int i = 0; i<4; i++){
		if(num[i] == '\0'){break;}
		DDRP = 0xff;
		  PTP = ~ (1 << (3 - i)); //notice it is negative logic
		  PORTB = segment_decoder[(num[i] - '0')];
		  busy_loop_delay(20); //delays leds so all appear to eye
	}
}

void reset(){ // just returns all values to default
	cal[0] = '\0';
	cal[1] = '\0';
	cal[2] = '\0';
	cal[3] = '\0';
	calCnt = 0;
	num = "0000\0";
}

void execute_the_jobs()
{

	if (keypad_enabled){
		unsigned char c = KeypadReadPort();
		if(c != KEYPAD_KEY_NONE) {
			DispInit (2, 16); // Initializes screen
			if(open == 0){//if system is closed
				if (c >= '0' && c<= '9' && cal[3] == '\0'){ // gets input for numbers
					cal[calCnt] = c;
					cal[calCnt+1]= '\0';
					calCnt ++;
					num = cal;
				} else if(c == 'A'){//will displayed password on leds
					showPass = 1;
				} else if(c == 'B'){//will disable the leds
					showPass = 0;
				} else if(c == 'C'){
					reset();
				} else if(c == 'D' && cal[3] != '\0'){//checks if users attempt and memorized pass is same
					num = "0000\0";
					if (cal[0] == pass[0] && cal[1] == pass[1] && cal[2] == pass[2] && cal[3] == pass[3]){
						open = 1;
						opening = 1;
						attempts = 0;
						showPass = 0;
						wrd = "open";
					} else {
						attempts++;
						wrd = "try again!";
						reset();
						if(attempts == 3){
							keypad_enabled = 0;
							DisableKeyboard();
							wrd = "try again later!";
							showPass = 0;
						}
					}
				}
			}
			else{//if system is open
				timer = 0;
				if(change == 1 && c >= '0' && c<= '9'){//will allow user to change pass
					pass[calCnt] = c;
					calCnt ++;
					pass[calCnt] = '\0';
					num = pass;
					if (calCnt == 4) {
						change = 0;
						calCnt = 0;
						showPass = 0;
						wrd = "Password Changed";
					}
				} else if(c == 'C'){//allows user to change passcode
					change = 1;
					calCnt = 0;
					showPass = 1;
					wrd = "Enter new Password";
				} else if(c == 'D' && change == 0){//closes system (as long as not in change state)
					open = 0;
					opening = 2;
					reset();
					wrd = "closed";
					showPass = 1;
					keypad_enabled = 0;
					DisableKeyboard();
				}
			}

			DispClrScr();
			DispStr(1, 1, wrd); // will display whatever action was done at the end
		}

		keypad_debounce_timer ++;
		if (keypad_debounce_timer > 400){
			keypad_debounce_timer = 0;
			keypad_enabled = 0;
			DisableKeyboard();
		}
	}
	else if(attempts == 3){ // Locks the system for a bit if password attempt exceeds 3
		timer++;
		if (timer>10000){
			timer = 0;
			keypad_enabled = 1;
			EnableKeyboardAgain();
			attempts = 0;
			wrd = "closed";
			num = "0000\0";
			showPass = 1;
		}
	}else if (opening == 1 || opening == 2){ // runs motor for a few seconds
		// Configure PWM on channel 4
		    PWME &= ~0x10; // Disable PWM channel 4
		    PWMPOL |= 0x10; // Set polarity for channel 4 to low-to-high
		    if(opening == 1){
		    	 PWMPOL &= ~0x10; // Set polarity for channel 4 to high-to-low (clockwise)
			} else {
				 PWMPOL |= 0x10; // Set polarity for channel 4 to low-to-high (anticlockwise)
			}
		    PWMCLK &= ~0x10; // Select clock A for channel 4
		    PWMPRCLK &= ~0x30; // Set prescaler for clock A to divide by 1
		    PWMSCLA = 0x03; // Set scaler for clock A to divide by 4
		    PWMPER4 = PWM_PERIOD; // Set period for channel 4 to 255
		    PWMDTY4 = 40; // Set initial duty cycle for channel 4 to 0
		    PWME |= 0x10; // Enable PWM channel 4

		    timer++;
		    if(timer > 1000){
		    	opening = 0;
		    	timer = 0;
		    	EnableKeyboardAgain();
		    	keypad_enabled = 1;
		    }
	}
	else{
		keypad_debounce_timer ++;
		if (keypad_debounce_timer > 10){
			keypad_debounce_timer = 0;
			keypad_enabled = 1;
			EnableKeyboardAgain();
		}
	}

	if(showPass == 1) {display_pass_on_7segment_LEDs(num);}
	else{display_hex_number_on_7segment_LEDs(255);}
}

void INTERRUPT rti_isr(void)
{
  //clear the RTI - don't block the other interrupts
  CRGFLG = 0x80;

  //for instance if limit is "10", every 10 interrupts do something ...
  if (counter_for_real_time_interrupt == counter_for_real_time_interrupt_limit)
    {
      //reset the counter
      counter_for_real_time_interrupt = 0;

      //do some work
      execute_the_jobs();
    }
  else
    counter_for_real_time_interrupt ++;

}

/**
 * initialize the rti: rti_ctl_value will set the pre-scaler ...
 */
void rti_init(unsigned char rti_ctl_value, unsigned int counter_limit)
{
  UserRTI = (unsigned int) & rti_isr; //register the ISR unit

  /**
   * set the maximum limit for the counter:
   * if max set to be 10, every 10 interrupts some work will be done
   */
  counter_for_real_time_interrupt_limit = counter_limit;


  /**
   * RTICTL can be calculated like:
   * i.e: RTICTL == 0x63 == set rate to 16.384 ms:
   * The clock divider is set in register RTICTL and is: (N+1)*2^(M+9),
   * where N is the bit field RTR3 through RTR0  (N is lower bits)
   * 	and M is the bit field RTR6 through RTR4. (M is higher bits)
   * 0110 0011 = 0x63 ==> 1 / (8MHz / 4*2^15)
   * 	which means RTI will happen every 16.384 ms
   * Another example:
   * 0111 1111 = 0x7F ==> 1 / (8MHz / 16*2^16)
   * 	which means RTI will happen every 131.072 ms
   * Another example:
   * 0001 0001 = 0x11 ==> 1 / (8MHz / 2*2^10)   = 256us
   */
  RTICTL = rti_ctl_value;

  // How many times we had RTI interrupts
  counter_for_real_time_interrupt = 0;

  // Enable RTI interrupts
  CRGINT |= 0x80;
  // Clear RTI Flag
  CRGFLG = 0x80;
}




int main(void)
{	
	set_clock_24mhz(); //usually done by D-Bug12 anyway
	DDRB = 0xff;
	rti_init(0x11, 10);
	__asm("cli"); //enable interrupts (maskable and I bit in CCR)

	DDRH = 0x00; //for push buttons
	KeypadInitPort();


	DispInit (2, 16);

	while(1);

}
