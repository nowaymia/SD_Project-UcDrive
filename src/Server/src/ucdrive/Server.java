//Inês Martins Marçal 2019215917
//Noémia Quintano Mora Gonçalves 2019219433

package ucdrive;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.nio.charset.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;



public class Server {
    private static int serverPort;
    private static int secondaryPort;
    private static int heartbeatPort;
    private static final int maxfailedrounds = 3;
    private static final int timeout = 1000;
    private static final int bufsize = 4096;
    private static final int period = 10000;


    public static void definePorts(){

        try {
            File f = new File("config.txt");
            FileReader fr = new FileReader(f);
            BufferedReader br = new BufferedReader(fr);
            String line;
            String[] port;
            while((line = br.readLine()) != null){
                port = line.split(";");
                serverPort = Integer.parseInt(port[0]);
                secondaryPort = Integer.parseInt(port[1]);
                heartbeatPort = Integer.parseInt(port[2]);

            }
        }catch (IOException e){
            System.out.println("Error: File not found.");
        }
    }

    public static void main(String args[])  {
        int numero=0;
        definePorts();
        while(true) {
            try (ServerSocket listenSocket = new ServerSocket(serverPort)) {
                System.out.println("A escuta no porto " + serverPort);
                File log = new File("log.txt");
                System.out.println("LISTEN SOCKET=" + listenSocket);
                new Heartbeat(heartbeatPort);
                new Replicate(log,secondaryPort);
                while (true) {
                    Socket clientSocket = listenSocket.accept(); // BLOQUEANTE
                    System.out.println("CLIENT_SOCKET (created at accept())=" + clientSocket);
                    numero++;
                    new Connection(clientSocket, numero, log);
                }
            } catch (IOException e) {
                Backup  b = null;
                DatagramSocket ds = null;
                try {
                    b = new Backup(secondaryPort);
                    ds = new DatagramSocket();
                    ds.setSoTimeout(timeout);
                    int failedheartbeats = 0;
                    while (failedheartbeats < maxfailedrounds) {
                        try {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            DataOutputStream dos = new DataOutputStream(baos);
                            dos.writeInt(0);
                            byte[] buf = baos.toByteArray();

                            DatagramPacket dp = new DatagramPacket(buf, buf.length, InetAddress.getByName("localhost"), heartbeatPort);
                            ds.send(dp);

                            byte[] rbuf = new byte[bufsize];
                            DatagramPacket dr = new DatagramPacket(rbuf, rbuf.length);

                            ds.receive(dr);
                            failedheartbeats = 0;
                            ByteArrayInputStream bais = new ByteArrayInputStream(rbuf, 0, dr.getLength());

                        } catch (SocketTimeoutException ste) {
                            failedheartbeats++;
                            System.out.println("Failed heartbeats: " + failedheartbeats);
                        }
                        Thread.sleep(period);
                    }
                    b.kill();
                    ds.close();

                } catch (IOException | InterruptedException ex) {
                    System.out.println("Cannot connect another server");
                    if (ds != null){
                        ds.close();
                    }
                    b.kill();
                    return;

                }
            }
        }
    }
}
class Backup extends Thread {
    private final int secondaryPort;
    private boolean shutdown = false;
    private DatagramSocket ds;
    public Backup(int secondaryPort){
        this.secondaryPort = secondaryPort;
        try {
            this.ds = new DatagramSocket(secondaryPort);
        } catch (SocketException e) {
            System.out.println("Cannot open socket for backup");
            return;
        }
        this.start();
    }

    public void run() {

        byte[] buffer = new byte[1024];


        while(!shutdown) {

            try {
                //Create file
                DatagramPacket fileRequest = new DatagramPacket(buffer,buffer.length);
                ds.receive(fileRequest);
                String[] Data = (new String(fileRequest.getData(),0,fileRequest.getLength())).split(";");
                long lenghtReceive = 0;
                long fileLenght = Long.parseLong(Data[1]);
                File f = new File(Data[0]);
                FileOutputStream fos = new FileOutputStream(f);

                while(lenghtReceive < fileLenght) {
                    DatagramPacket dataFile = new DatagramPacket(buffer,buffer.length);
                    ds.receive(dataFile);
                    byte[] byteContent = new byte[dataFile.getLength()];
                    System.arraycopy(buffer,0,byteContent,0,dataFile.getLength());
                    fos.write(byteContent);

                    lenghtReceive += dataFile.getLength();
                    DatagramPacket ackPack = new DatagramPacket(("ACK").getBytes(),("ACK").getBytes().length,fileRequest.getAddress(),fileRequest.getPort());
                    ds.send(ackPack);
                }
                fos.close();
        } catch (IOException e) {
                kill();
            }

        }
    }
    public void kill(){
        ds.close();
        shutdown = true;
        currentThread().interrupt();
    }
}
class Replicate extends Thread{
    private final File log;
    private final int period = 60000;
    private final int timeout = 10000;
    private  final int secondaryPort;
    private final String  hostName = "localhost";

    public Replicate(File log, int secondaryPort){
        this.secondaryPort = secondaryPort;
        this.log = log;
        this.start();
    }

    @Override
    public void run() {
        ArrayList<String> lines = new ArrayList<>();
        String line;
        DatagramSocket ds = null;
        while(true) {
            try {
                try {
                    ds = new DatagramSocket();
                } catch (SocketException e) {
                    System.out.println("Cannot connect to secondary server");
                    Thread.sleep(period);
                    continue;
                }
                synchronized (log) {
                    try (FileReader fr = new FileReader(log)) {
                        BufferedReader br = new BufferedReader(fr);
                        while ((line = br.readLine()) != null) {
                            lines.add(line);
                        }
                        fr.close();
                        br.close();
                        FileWriter fw = new FileWriter(log);
                        BufferedWriter bw = new BufferedWriter(fw);
                        bw.write("");
                        bw.flush();
                        fw.close();
                        bw.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                byte[] buffer = new byte[1024];
                File f;

                for (int i = 0; i < lines.size();i++) {
                    f = new File(lines.get(i));
                    byte[] fileName = (lines.get(i) + ";" + f.length()).getBytes();
                    DatagramPacket request = new DatagramPacket(fileName, fileName.length, InetAddress.getByName(hostName), secondaryPort);
                    ds.send(request);

                    byte[] fBytes = new byte[(int) f.length()];
                    FileInputStream fis = new FileInputStream(f);
                    fis.read(fBytes);
                    fis.close();
                    int j = 0;
                    while (j < fBytes.length) {
                        try {
                            int lastPos = Math.min(buffer.length, fBytes.length - j);
                            buffer = new byte[lastPos];
                            System.arraycopy(fBytes, j, buffer, 0, lastPos);
                            j += buffer.length;
                            DatagramPacket sendPack = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(hostName), secondaryPort);
                            ds.send(sendPack);


                            try {
                                ds.setSoTimeout(timeout);

                                DatagramPacket ackPack = new DatagramPacket(buffer, buffer.length);
                                ds.receive(ackPack);

                            } catch (IOException e) {
                                j -= buffer.length;
                            }
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }

                }
                lines.clear();
                Thread.sleep(period);
            } catch (IOException e) {
                e.printStackTrace();

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
class Heartbeat extends Thread {
    private int port;
    private static final int bufsize = 4096;

    public Heartbeat(int port){
        this.port = port;
        this.start();
    }

    @Override
    public void run() {
        try (DatagramSocket ds = new DatagramSocket(port)) {
            while (true) {
                byte buf[] = new byte[bufsize];
                DatagramPacket dp = new DatagramPacket(buf, buf.length);
                ds.receive(dp);
                ByteArrayInputStream bais = new ByteArrayInputStream(buf, 0, dp.getLength());
                DataInputStream dis = new DataInputStream(bais);
                int count = dis.readInt();
                System.out.println("RECEIVED " + count);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);
                dos.writeInt(count);
                byte resp[] = baos.toByteArray();
                DatagramPacket dpresp = new DatagramPacket(resp, resp.length, dp.getAddress(), dp.getPort());
                ds.send(dpresp);
            }
        } catch (IOException e) {
            System.out.println("Error " + e);
        }
    }
}
class Connection extends Thread {
    DataInputStream in;
    DataOutputStream out;
    Socket clientSocket;
    int thread_number;
    Boolean isAuthenticated  = false;
    int lineFile;
    User user;
    File log;
    String []data = {""};
    String ServerDirectory;

    public Connection(Socket aClientSocket, int numero, File log) {
        thread_number = numero;
        try{
            clientSocket = aClientSocket;
            in = new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());
            this.log = log;
            this.start();
        }catch(IOException e){System.out.println("Connection:" + e.getMessage());}
    }
    //=============================
    public void run(){
        while(!isAuthenticated){
            try {
                String data[] = in.readUTF().split(";");
                if (data.length == 3){
                    isAuthenticated = authentication(data[1], data[2]);
                }

            } catch (IOException e) {
                System.out.println("Connection closed");
                break;
            }
        }
        try{
            while(!data[0].equals("9")){

                data = in.readUTF().split(";");
                String option = data[0];
                switch (option) {
                    case "0":
                        authentication(data[1],data[2]);
                        break;

                    case "1":
                        if (changePassword(data)) {
                            out.writeUTF("password changed");
                        } else {
                            out.writeUTF("Wrong old password");
                        }
                        break;
                    case "2":
                        out.writeUTF(ServerDirectory);
                        break;
                    case "3":
                        String aux = listCurrentServerDirectory();
                        out.writeUTF(aux);
                        break;
                    case "4":
                        String d = in.readUTF();
                        if(verifyDirectory(d)) {

                            out.writeUTF("Directory changed Succesfully\n");
                        }else{
                            out.writeUTF("Invalid directory\n");
                        }
                        break;

                    case "7":
                        downloadFile(data[1]);
                        break;
                    case "8":
                        uploadFile();
                        break;
                    default:
                        break;
                }
            }
            System.out.println("Connection closed");
            clientSocket.close();

        }catch (IOException e) {
            System.out.println("Client disconnected");

        }
    }


    private String listCurrentServerDirectory(){
        String aux = "";
        File dir = new File("Data/" + ServerDirectory);
        if (dir.exists() && dir.isDirectory()){
            File[] fileList = dir.listFiles();
            if (fileList != null && fileList.length != 0) {
                aux += "Directory " + ServerDirectory + ":\n";
                for (File file : fileList) {

                    if(file.isDirectory()){
                        aux += "\tDirectory: ";
                    }else{
                        aux += "\tFile: ";
                    }
                    aux += file.getName() + "\n";
                }
            }else{
                aux += "No file in directory " + ServerDirectory + "\n";
            }
        }
        return aux;
    }

    private boolean authentication(String username, String passsword){
        try {
            File userFiles = new File("users.txt");
            FileReader fr = new FileReader(userFiles);
            BufferedReader br = new BufferedReader(fr);
            String line;
            int contador = 0;
            while ((line = br.readLine()) != null) {
                String[] d = line.split(";");
                if (d[0].equals(username) && d[1].equals(passsword)){
                    lineFile = contador;
                    user = new User(d[3], Integer.parseInt(d[2]), LocalDate.parse(d[4]),
                            d[5], d[6], d[1], Integer.parseInt(d[7]), Integer.parseInt(d[0]), d[8]);
                    if (d.length > 8){
                        String aux = "PENDING";
                        for (int i = 9; i < d.length; i++){
                            aux += ";" + d[i];
                        }
                        Path file = Paths.get("users.txt");
                        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                        lines.set(this.lineFile, user.toString());
                        Files.write(file, lines, StandardCharsets.UTF_8);
                        out.writeUTF(aux);
                    }else{
                        out.writeUTF("OK");
                    }
                    ServerDirectory = user.getDirectory();
                    fr.close();
                    br.close();
                    return true;
                }
                contador++;
            }
            fr.close();
            br.close();
            out.writeUTF("Invalid username/password");
        } catch (IOException e) {
            System.out.println("Error: " + e);
        }
        return false;

    }

    private boolean verifyDirectory(String s){
        //Apanha o caso //ola e /ola
        if(s.charAt(0) == '/'){
            return false;
        }
        String[] aux = s.split("/");
        String atualD = ServerDirectory;
        for(int i = 0; i < aux.length; i++){

            if(aux[i].equals("..")){
                int lastOcurrenceBar = atualD.lastIndexOf("/");
                if(lastOcurrenceBar == -1){
                    return false;
                }else{
                    atualD = atualD.substring(0, lastOcurrenceBar);
                }
            }else{

                if(ExistDirectory("Data/" +atualD, aux[i])){
                    atualD += "/" + aux[i];
                }else{

                    return false;
                }
            }
        }
        ServerDirectory = atualD;

        try {
            Path file = Paths.get("users.txt");
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            user.setDirectory(ServerDirectory);
            lines.set(this.lineFile, user.toString());
            Files.write(file, lines, StandardCharsets.UTF_8);

        }catch (IOException e) {
            System.out.println("Error: " + e);
        }
        return true;

    }

    private boolean ExistDirectory(String atualDirectory, String newDirectory){

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
    private boolean changePassword(String[] data) throws IOException {
        if (data[1].equals(user.getPassword())) {
            String newPassword = data[2];
            Path file = Paths.get("users.txt");
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            this.user.setPassword(newPassword);
            lines.set(this.lineFile, user.toString());
            Files.write(file, lines, StandardCharsets.UTF_8);
            return true;
        }
        return false;
    }
    private void downloadFile(String filename){
        File currentDir =  new File("Data/"+ServerDirectory);
        File[] fileList = currentDir.listFiles();
        File file = null;
        try {
            for (File f : fileList) {
                if (filename.equals(f.getName())) {
                    file = f;
                    break;
                }
            }
            if (file == null) {
                out.writeUTF("File doesnt exist in current directory");
            } else {
                ServerSocket downloadSocket = new ServerSocket(0);
                out.writeUTF("OK;" + downloadSocket.getLocalPort() + ";" + file.length());
                new Download(downloadSocket, file, lineFile);
            }
        } catch (IOException e) {
            System.out.println("Error: " + e);
        }
    }

    private void uploadFile() {
        try {
            String[] uploadInfo = in.readUTF().split(";");
            ServerSocket uploadSocket = new ServerSocket(0);
            out.writeUTF("OK;" + uploadSocket.getLocalPort());
            new Upload(uploadSocket,uploadInfo[0],uploadInfo[1],ServerDirectory,log,lineFile);

        } catch (IOException e) {
            System.out.println("Error: " + e);
        }
    }

}
class Upload extends Thread{
    private final ServerSocket s;
    private final String fileName;
    private final long fileLenght;
    private final String currentDirectory;
    private final File log;
    private final int lineFile;

    public Upload(ServerSocket s, String fileName, String fileLenght, String currentDirectory, File log, int lineFile){
        this.s = s;
        this.fileName = fileName;
        this.fileLenght = Long.parseLong(fileLenght);
        this.currentDirectory = currentDirectory;
        this.log = log;
        this.lineFile = lineFile;
        this.start();
    }


    public void run() {
        File f = new File("Data/" + currentDirectory +"/" +fileName);
        FileOutputStream fos = null;
        DataInputStream dis = null;
        try{
            Socket c = s.accept();
            fos = new FileOutputStream(f);
            dis = new DataInputStream(c.getInputStream());
            int nbytes = 4;
            int kbyte = 1024;
            int bytesReceived;
            byte[] buffer = new byte[nbytes*kbyte];
            long remainLenght = fileLenght;
            while (remainLenght > 0 ){
                bytesReceived = dis.read(buffer, 0, (int)Math.min(buffer.length, remainLenght));
                fos.write(buffer,0,bytesReceived);
                remainLenght -= bytesReceived;
            }
            dis.close();
            fos.close();
            c.close();
            s.close();
            synchronized (log) {
                FileWriter fw = new FileWriter(log, true);
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write("Data/"+ currentDirectory+ "/" +this.fileName + "\n");
                bw.close();
                fw.close();
            }
        } catch (IOException e) {
            try {
                Path file = Paths.get("users.txt");
                List<String> lines = null;
                lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                String userline = lines.get(lineFile);
                userline += ";+"+currentDirectory+"/"+ f.getName();
                lines.set(this.lineFile,userline);
                Files.write(file, lines, StandardCharsets.UTF_8);
                dis.close();
                fos.close();
                if (f.exists() && f.isFile()){
                    Files.delete(f.toPath());
                }

            } catch (IOException ex) {
                System.out.println("Error deleting the file");
            }
        }
    }
}
class Download extends Thread {
    private final ServerSocket s;
    private final File f;
    private FileInputStream fis;
    private final int lineFile;

    public Download(ServerSocket s, File f, int lineFile){
        this.s = s;
        this.f = f;
        this.lineFile = lineFile;
        try {
            this.fis = new FileInputStream(f);
        } catch (FileNotFoundException e) {
            try {
                Path file = Paths.get("users.txt");
                List<String> lines = null;
                lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                String userline = lines.get(lineFile);
                userline += ";-"+f.getName();
                lines.set(this.lineFile,userline);
                Files.write(file, lines, StandardCharsets.UTF_8);
            } catch (IOException ex) {
                System.out.println("Error: " + e);
            }
        }
        this.start();
    }

    public void run(){
        int lenghtSent = 0;
        byte[] buffer = new byte[1024];
        try {
            Socket client = s.accept();
            DataOutputStream dos = new DataOutputStream(client.getOutputStream());
            this.fis = new FileInputStream(f);

            while ((lenghtSent = fis.read(buffer))!=-1){
                dos.write(buffer,0,lenghtSent);
                dos.flush();
            }
            fis.close();
            dos.close();
            client.close();
            s.close();

        } catch (IOException e) {
            try {
                Path file = Paths.get("users.txt");
                List<String> lines = null;
                lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                String userline = lines.get(lineFile);
                userline += ";-"+f.getName();
                lines.set(this.lineFile,userline);
                Files.write(file, lines, StandardCharsets.UTF_8);
            } catch (IOException ex) {
                System.out.println("Error: " + e);
            }
        }
    }
}
