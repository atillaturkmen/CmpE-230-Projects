all: Main.class Parser.class
	jar cfm mylang2ir Manifest.txt Main.class Parser.class
	
Main.class: Main.java
	javac -cp . Main.java
	
Parser.class: Parser.java
	javac -cp . Parser.java

# clean up output files
clean:
	rm -f *.class
	rm -f mylang2ir
