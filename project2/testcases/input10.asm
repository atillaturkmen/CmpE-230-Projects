	LOAD 0000
	STORE B
LOOP:
	INC B
	SHL A
	SHL A
	INC A
	CMP 5555
	JNZ LOOP

	LOAD B
	ADD 0030
	PRINT A
	HALT
