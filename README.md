Steps to Execute:

1. Compile the three class files Router.java, Controller.java and Host.java as shown below:

javac Router.java
javac Controller.java
javac Host.java

2. Now, command line parameters can be passed to each of them as shown below:

eg:  java Router 0 0 1
     java Host 0 0 sender 50 20
     java Host 1 1 receiver

3. It can be run from a script file "test.sh", a sample of each is shown below:


rm -f lan?  hout?  hin?
rm -f rout?
echo `java Host 0 0 sender 50 20`&
echo `java Host 1 1 receiver `&
echo `java Router 0 0 1 `&
echo `java Router 1 1 2 `&
echo `java Router 2 2 3 `&
echo `java Router 3 3 0 `&
echo `java Controller host 0 1 router 0 1 2 3 lan 0 1 2 3` & 

4. Please note that the letters here are "case sensitive". The class names start with Upper Case letters, whereas the arguments to them start with a lower case letters.

5. Certain temp *_copy files are generated while running the program. These are used for checking difference between the files using unix command 'diff'. Also, These copy files are deleted at the end of the program.  

6. The required output files like lan1.txt, host1.txt, rout1.txt etc are generated in the same directory where it is run
# DistanceVectorMulticastRouting