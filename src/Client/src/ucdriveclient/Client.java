//Inês Martins Marçal 2019215917
//Noémia Quintano Mora Gonçalves 2019219433
package ucdriveclient;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.Scanner;

public class Client {


        static String hostPrimary = "localhost";
        static int portPrimary = 6000;
        static Socket primary;
        static String hostSecondary = "localhost";
        static int portSecondary = 4000;
        static Socket secondary;
        static String currentDirectory = "home";
        static boolean primaryConnection = false;

    public static void main(String args[]) {

            boolean passwordChanged = false;
            String clientId = null;
            Socket s = null;
            String option = "-1";
            Scanner sc = new Scanner(System.in);

            DataInputStream in = null;
            DataOutputStream out = null;


            //Connect
            while(true) {
                try {
                    while (s == null) {

                        try {
                            primary = new Socket(hostPrimary, portPrimary);
                            System.out.println("Connected to primary server");
                            s = primary;
                            primaryConnection = true;

                        } catch (IOException e) {
                            System.out.println("Failed to connect to primary server. Trying to connect to secondary server...");

                            try {
                                secondary = new Socket(hostSecondary, portSecondary);
                                System.out.println("Connected to secondary server");
                                s = secondary;
                                primaryConnection = false;
                            } catch (IOException ex) {
                                System.out.println("Failed to connect to secondary server. Change connection settings with: config [primary/secondary] [host] [port]\nPress enter to retry or q to exit");
                                System.out.print("$ ");
                                String[] command = sc.nextLine().split(" ");
                                if (command[0].equals("config")) {
                                    Client.configureHostPorts(command, s);
                                }
                                if (command[0].equals("q")){
                                    System.exit(0);
                                }
                            }
                        }
                    }
                    try {
                        in = new DataInputStream(s.getInputStream());
                        out = new DataOutputStream(s.getOutputStream());

                        System.out.println("SOCKET=" + s);
                        while (clientId == null) {
                            clientId = authentication(s, in, out);
                        }
                    } catch (IOException e) {
                        System.out.println("Socket error");
                        s.close();
                        s = null;
                        clientId = null;
                        continue;
                    }
                    while (true) {

                            System.out.print(clientId + ":" + currentDirectory + "$ ");
                            String[] command = sc.nextLine().split(" ");
                            switch (command[0]) {
                                case "cp":
                                    Client.changePassword(command, s, in, out);
                                    break;
                                case "config":
                                    Client.configureHostPorts(command, s);
                                    break;
                                case "ls":
                                    if (command.length != 2) {
                                        System.out.println("Usage: ls [server/local]");
                                    } else {
                                        switch (command[1]) {
                                            case "server" -> Client.listServerDirectory(in, out);
                                            case "local" -> Client.listLocalDirectory();
                                            default -> System.out.println("Invalid configuration");
                                        }
                                    }
                                    break;
                                case "cd":
                                    if (command.length != 3) {
                                        System.out.println("Usage: cd [server/local] [directory]");
                                    } else {
                                        switch (command[1]) {
                                            case "server" -> Client.changeServerDirectory(command[2], in, out);
                                            case "local" -> Client.changeLocalDirectory(command[2]);
                                            default -> System.out.println("Invalid configuration");
                                        }
                                    }
                                    break;

                                case "download":
                                    if (command.length != 2) {
                                        System.out.println("Usage: download [file]");
                                    } else {
                                        new Download(currentDirectory, s, in, out, command[1]);
                                    }
                                    break;

                                case "upload":
                                    if (command.length != 2) {
                                        System.out.println("Usage: upload [file]");
                                    } else {
                                        new Upload(currentDirectory, s, in, out, command[1]);
                                    }

                                    break;
                                case "sh":
                                    if (command.length != 1){
                                        System.out.println("Usage: sh");
                                        break;
                                    }
                                    Client.shConfig(in, out);
                                    break;
                                case "q":
                                    out.writeUTF("9");
                                    System.exit(0);
                                    break;
                                default:
                                    System.out.println("Invalid command");
                                    break;

                            }
                        }

                    } catch (IOException e) {
                    System.out.println("Connection interrupted.");
                    try {
                        s = new Socket(hostPrimary, portPrimary);
                        System.out.println("Reconnected");
                        clientId = null;
                    } catch (IOException ex) {
                        s = null;
                        clientId = null;
                    }
                }
            }
        }




    static String authentication(Socket s, DataInputStream in, DataOutputStream out) throws IOException{

                String studentId;
                String password;
                Scanner sc = new Scanner(System.in);

                System.out.print("Id: ");
                studentId = sc.nextLine();
                System.out.print("Password: ");
                password = sc.nextLine();
                //password = new String(System.console().readPassword("Password: \n"));
                out.writeUTF("0;"+studentId + ";" + password);
                String[] login = in.readUTF().split(";");
                switch (login[0]){
                    case "OK":
                        System.out.println("Logged in");
                        return studentId;
                    case "PENDING":
                        for (int i = 1; i < login.length;i++){
                            if (login[i].charAt(0) == '+'){
                                System.out.println("Upload " + login[i].substring(1)+ " failed");
                            }else{
                                System.out.println("Download " + login[i].substring(1) + " failed");
                            }

                        }
                        return studentId;
                    default:
                        System.out.println("Invalid username/password");
                }
        return null;
        }
        static void shConfig (DataInputStream in, DataOutputStream out) throws IOException {
            System.out.println("\nPrimary server: " + hostPrimary + " " + portPrimary);
            System.out.println("Secondary server: " + hostSecondary + " " + portSecondary);
            out.writeUTF("2");
            System.out.println("Server directory: " + in.readUTF());
            System.out.println("Local current directory: " + currentDirectory + "\n");

        }

        static void changePassword(String[] data, Socket s, DataInputStream in, DataOutputStream out) throws IOException{
                if (data.length != 4){
                    System.out.println("Usage cp [oldpassword] [newpassword] [newpassword]");
                    return;
                }

                if (!data[2].equals(data[3])){
                    System.out.println("Passwords don't match");
                    return;
                }

                //Verify new password: cannot contain ; since it's used to parse the users login file
                if (data[2].contains(";")){
                    System.out.println("Invalid character in new password");
                    return;
                }

                out.writeUTF("1" + ";" + data[1] + ";" + data[2]);

                String result = in.readUTF();
                    if (result.equals("password changed")){
                        System.out.println("Password sucessfully changed");
                        String c = null;
                        while(c == null){
                            c = authentication(s,in,out);
                        }
                    }else{
                        System.out.println("Invalid user/password");
                    }


        }

        static void listServerDirectory(DataInputStream in, DataOutputStream out) throws IOException{

            out.writeUTF("3");
            System.out.println(in.readUTF());

        }

        static void changeServerDirectory(String directory, DataInputStream in, DataOutputStream out) throws IOException {
            out.writeUTF("4");
            out.writeUTF(directory);
            System.out.println(in.readUTF());
        }

         static void listLocalDirectory(){
             File dir = new File(currentDirectory);
             //System.out.println(currentDirectory);
             if (dir.exists() && dir.isDirectory()){
                 File[] fileList = dir.listFiles();
                 if (fileList != null && fileList.length != 0) {
                     System.out.println("Directory " + currentDirectory);
                     for (File file : fileList) {

                         if(file.isDirectory()){
                             System.out.print("\tDirectory: ");
                         }else{
                             System.out.print("\tFile: ");
                         }
                         System.out.println(file.getName());
                     }
                     System.out.println();
                 }else{
                     System.out.println("No file in directory " + currentDirectory+ "\n");
                 }
             }
         }

         static void changeLocalDirectory(String path) {
             Scanner sc = new Scanner(System.in);
             if(verifyDirectory(path)) {
                 System.out.println("Directory changed Succesfully\n");
             }else{
                 System.out.println("Can`t change directory\n");
             }

         }

        private static boolean verifyDirectory(String s){
        //Apanha o caso //ola e /ola
        if(s.charAt(0) == '/'){
            return false;
        }
        String[] aux = s.split("/");
        String atualD = currentDirectory;
        for(int i = 0; i < aux.length; i++){
            if(aux[i].equals("..")){
                int lastOcurrenceBar = atualD.lastIndexOf("/");
                if(lastOcurrenceBar == -1){
                    return false;
                }else{
                    atualD = atualD.substring(0, lastOcurrenceBar);
                }
            }else{
                if(ExistDirectory(atualD, aux[i])){
                    atualD += "/" + aux[i];
                }else{
                    System.out.println("Directory doesn't exist.");
                    return false;
                }
            }
        }
        currentDirectory = atualD;
        return true;
    }

        private static boolean ExistDirectory(String atualDirectory, String newDirectory){

        File dir = new File(atualDirectory);
        if (dir.exists() && dir.isDirectory()){
            File[] fileList = dir.listFiles();
            if (fileList != null && fileList.length != 0) {

                for (File file : fileList) {

                    if(file.isDirectory() && file.getName().equals(newDirectory)){
                        return true;
                    }
                }
            }
        }
        return false;
    }

        private static void configureHostPorts(String[] data, Socket s){
            if (data.length != 4){
                System.out.println("Usage config [primary/secondary] [host] [port]");
                return;
            }
            try {
                Integer.parseInt(data[3]);
            } catch(NumberFormatException e){
                System.out.println("Invalid port");
                return;
            }

            switch (data[1]) {
                case "primary" -> {
                    hostPrimary = data[2];
                    try {
                        portPrimary = Integer.parseInt(data[3]);
                        if (primaryConnection) {
                            if (s != null) {
                                s.close();
                            }
                            s = null;
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid port");
                    } catch (IOException e) {
                        System.out.println("Error: " + e);
                    }


                }
                case "secondary" -> {
                    hostSecondary = data[2];
                    try {
                        portSecondary = Integer.parseInt(data[3]);
                        if(!primaryConnection){
                            s.close();
                            s = null;
                        }

                    } catch (NumberFormatException e) {
                        System.out.println("Invalid port");
                    } catch (IOException e) {
                        System.out.println("Error: " + e);
                    }
                }
                default -> System.out.println("Configuration not available");

            }

        }



    }
class Upload extends Thread {
    Socket uploadSocket;
    File f;

    public Upload(String currentDirectory, Socket s, DataInputStream in, DataOutputStream out,String fileName){
        try {
            File f = new File(currentDirectory + "/" + fileName);
            String[] uploadInfo ;
            if (!f.exists()){
                System.out.println("File does not exist");
                return;
            }
            if (!f.isFile()){
                System.out.println("Only files can be upload");
                return;
            }
            out.writeUTF("8");
            out.writeUTF(f.getName()+";"+f.length());
            String serverResponse = in.readUTF();
            uploadInfo = serverResponse.split(";");
            if (!uploadInfo[0].equals("OK")){
                System.out.println(serverResponse);
                return;
            }

            this.f = f;
            this.uploadSocket = new Socket(s.getInetAddress().getHostAddress(), Integer.parseInt(uploadInfo[1]));

            this.start();
        } catch (IOException e) {
            System.out.println("Error: " + e);
        }
    }

    public void run() {
        try {
            int lenghtSent = 0;
            int nbytes = 4;
            int kbyte = 1024;
            FileInputStream fis = new FileInputStream(f);
            DataOutputStream dos = new DataOutputStream(uploadSocket.getOutputStream());
            byte[] buffer = new byte[nbytes*kbyte];
            while ((lenghtSent=fis.read(buffer))!=-1){
                dos.write(buffer,0,lenghtSent);
                dos.flush();
            }
            fis.close();
            dos.close();
            uploadSocket.close();
            System.out.println("Sucessfully uploaded file "+ f.getName());

        } catch (IOException e) {
            System.out.println("Error uploading file "+ f.getName());
        }
    }
}

class Download extends Thread {

    String currentDirectory;
    Socket downloadSocket;
    String fileName;
    long fileLength;

    public Download(String currentDirectory, Socket s, DataInputStream in, DataOutputStream out,String fileName)  {

        try {
            String serverResponse;
            String[] downloadInfo;

            out.writeUTF(7 + ";" + fileName);
            serverResponse = in.readUTF();

            downloadInfo = serverResponse.split(";");
            if (!downloadInfo[0].equals("OK")){
                System.out.println(serverResponse);
                return;
            }

            this.currentDirectory = currentDirectory;
            this.downloadSocket = new Socket(s.getInetAddress().getHostAddress(), Integer.parseInt(downloadInfo[1]));
            this.fileName = fileName;
            this.fileLength = Long.parseLong(downloadInfo[2]);

            this.start();
        } catch (IOException e) {
            System.out.println("Cannot connect to server aborting download");
        }
    }

    public void run(){

        DataInputStream dis = null;
        FileOutputStream fos = null;
        int bytesReceived;

        byte[] buffer = new byte[1024];
        try {
            dis = new DataInputStream(downloadSocket.getInputStream());
            fos = new FileOutputStream(currentDirectory + "/" + fileName);
            while (fileLength > 0) {
                bytesReceived = dis.read(buffer, 0, (int) Math.min(buffer.length, fileLength));
                fos.write(buffer, 0, bytesReceived);
                fileLength-=bytesReceived;
            }
            dis.close();
            fos.close();
            downloadSocket.close();
            System.out.println("Sucessfully downloaded file "+ fileName);
        } catch (IOException e) {
            try {
                System.out.println("\nServer disconnected, aborting download");
                dis.close();
                fos.close();
                downloadSocket.close();
                File f = new File(currentDirectory + "/" + fileName);
                    if (f.exists() && f.isFile()){
                        Files.delete(f.toPath());
                    }
            } catch (IOException ex) {
                System.out.println("Error: " + e);
            }
        }

    }

}