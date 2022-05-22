# STOP-AND-WAIT-GO-BACK-N-UDP
Implementation of Stop-and-Wait and Go-Back-N over UDP in Java.

This file allows a data file (data.txt) to be read and prints the output.

Usage

In this sender-receiver implementation, 
the receiver is the listener. 
With that in mind, the receiver must always be run before the sender is run. 
The sender will just stall until it finds a listener to connect to.

First get into the directory and compile all of the Java files.

cd GBN_submit

javac *.java

Commands to run the program:

java Receiver <portnumber> <filename>
  
java Sender localhost <portnumber> <filename>

For example, <portnumber> can be 50.

Example of commands:
  
java Receiver 50 data.txt
  
java Sender localhost 50 data.txt
