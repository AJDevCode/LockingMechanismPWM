# LockingMechanismPWM

## Description Of Program
Created a simulated Locking Mechanism of a safe using the PWM function. The locking mechanism will be simulated with a motor connected to the HCS12 board. There will be a password to open and engage the lock of the safe. The password can be entered using the keypad connected to the board. There will be an option to reset the password for this lock. Also if an incorrect password is entered three times, there will be a period in which a password cannot be entered for a specified set time(1 minute). 00FF will be displayed if the password being entered is chosen as hidden by the user. Instead of the actual values being entered. 

## Features
 - **Unlock Safe Door Using Password**
 - **Lock Safe Door Using Password**
 - **Changing the Password**
 - **Lock Out Password failed in 3 attempts for 5 mins. Will output try again later**
 - **Hidden Password, where user can decide whether the password values will be shown to the screen when inputted via keyboard**


## Explanation Of Code
 - **Pulse-Width Modulation (PWM). This is a function that governs a motor that controls the opening and closing of a lock. Our code provided continuously reads input from the keypad and verifies it with the correct code using the check_code() function. When the code is accepted, the motor rotates clockwise to unlock the lock. In contrast, the motor moves counterclockwise if the code is not accepted.**
 
 - **The program initializes the PWM channel 4 by setting various registers, including PWMPOL, PWMCLK, PWMPRCLK, PWMSCLA, PWMPER4, and PWMDTY4, which control the polarity, clock source, prescaler, scaler, period, and duty cycle of the PWM output, respectively.**
 
 - **The program uses an infinite loop to continuously read input from the keypad. When a key is touched, the program uses IF statements to determine whether a correct code has been entered. This occurs when it has been entered and the “D” key is pressed. The default code is “0000” When this occurs, it uses the check code() function to compare the entered code to the proper code. If the code entered is valid and is 1, it will open the lock rotating clockwise, if the code is 2 it will close the lock. which closes the lock by rotating the motor in an anticlockwise direction. Any other input, (3 & 4) will not use the motor at all. The program also sets the polarity for channel 4 accordingly using the PWMPOL register.**
 
 - **Overall, this program provides a basic example of how to control a motorized lock using a keypad input and PWM output on an HCS12 microcontroller. This is done using an HCS12 microcontroller with an LCD screen and a keypad. The code comprises a segment_decoder array that maps integer values to 7-segment LED display patterns for hexadecimal digits and some letters. There is a function called display_hex_number_on_7segment_LEDs, which displays a hexadecimal number on a 4-digit 7-segment LED display. This function uses the segment_decoder array, PORTB, and DDRP registers to set the LED display pins and select the active digit using negative logic on the PTP register. Additionally, the code defines another function called display_pass_on_7segment_LEDs that displays a passcode on the 7-segment LED display. It also uses the segment_decoder array, PORTB, DDRP, and PTP registers, similar to display_hex_number_on_7segment_LEDs, but with a busy loop delay between digits to make them more visible to the user.**

 ## Technologies Used
  -  **Java**
  -  **HCS12 Dragon Board**
  -  **External Keypad**
  -  **7 Segment LED**
  -  **Motor used for Simulating Safe functionality**
