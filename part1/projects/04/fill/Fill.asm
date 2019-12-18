// This file is part of www.nand2tetris.org
// and the book "The Elements of Computing Systems"
// by Nisan and Schocken, MIT Press.
// File name: projects/04/Fill.asm

// Runs an infinite loop that listens to the keyboard input.
// When a key is pressed (any key), the program blackens the screen,
// i.e. writes "black" in every pixel;
// the screen should remain fully black as long as the key is pressed. 
// When no key is pressed, the program clears the screen, i.e. writes
// "white" in every pixel;
// the screen should remain fully clear as long as no key is pressed.

(LOOP)
    @KBD
    D=M

    @KEY_PRESSED
    D;JGT

(KEY_NOT_PRESSED)
    // Init wordCounter to 8191
    @8191 // 32 words per row * 256 rows - 1 (zero based indexing)
    D=A
    @wordCounter
    M=D

(KEY_NOT_PRESSED_FILL_SCREEN_LOOP)
    @LOOP_END
    D;JLT

    // Calculate position and fill
    @SCREEN
    D=D+A
    A=D
    M=0

    // Decrement wordCounter
    @wordCounter
    D=M
    M=D-1

    // Loop again
    @wordCounter
    D=M 

    @KEY_NOT_PRESSED_FILL_SCREEN_LOOP
    0;JMP

(KEY_PRESSED)
// Init wordCounter to 8191
    @8191 // 32 words per row * 256 rows - 1 (zero based indexing)
    D=A
    @wordCounter
    M=D

(KEY_PRESSED_FILL_SCREEN_LOOP)
    @LOOP_END
    D;JLT

    // Calculate position and fill
    @SCREEN
    D=D+A
    A=D
    M=-1

    // Decrement wordCounter
    @wordCounter
    D=M
    M=D-1

    // Loop again
    @wordCounter
    D=M 

    @KEY_PRESSED_FILL_SCREEN_LOOP
    0;JMP

(LOOP_END)
    @LOOP
    0;JMP