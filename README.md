# SD_Project-UcDrive
- [x] Finished

## Description
This project was developed for Distributed Systems subject @University of Coimbra, Informatics Engineering <br>
Consists in develop a platform(server) to store files from users(clients), using some protocols (TCP and UDP) and other elements like sockets, packets, etc.

#### Main Languages:
![](https://img.shields.io/badge/Java-333333?style=flat&logo=java&logoColor=FFFFFF) 

## Technologies used:
1. Java
    - [Version 17](https://www.oracle.com/java/technologies/downloads/) 

## To run this project:
[WARNING] Java must be installed<br>
You have two ways to run this project:
For both download the folder "src"
1. Run the Executables
    * Run each .jar using terminal in their directory
      ```shellscript
      [your-disk]:[name-path]> java -jar [file-name].jar
      ```

2. Running the code:
    * Compile the code in the directory of the server, this will create a folder ucdrive with files *.class 
      ```shellscript
      [your-disk]:[name-path]> javac -d . *.java
      ```
    * Put the files config.txt, log.txt and users.txt near this new folder.
    * Run the code
      ```shellscript
      [your-disk]:[name-path]> java ucdrive/Server
      ```
    * Run the client code
      ```shellscript
      [your-disk]:[name-path]> java Client.java
      ```
      

## Notes important to read
- For more information about the project, commands and struct of resource files read the Report

## Authors:
- [Inês Marçal](https://github.com/inesmarcal)
- [Noémia Gonçalves](https://github.com/nowaymia)
