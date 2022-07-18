package ucdrive;

import java.time.LocalDate;

import javax.print.event.PrintEvent;
import javax.sound.sampled.SourceDataLine;

public class User{

    private String name;
    private int ccNumber;
    private LocalDate ccExpirDate;
    private String address;
    private String department;
    private String password;
    private int phoneNumber;
    private String directory;
    private int studentId;

    public User(){

    }

    public User(String name, int ccNumber, LocalDate ccExpirDate, String address, String department, String password, int phoneNumber, int studentId, String directory) {
        this.name = name;
        this.ccNumber = ccNumber;
        this.ccExpirDate = ccExpirDate;
        this.address = address;
        this.department = department;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.directory = directory;
        this.studentId = studentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCcNumber() {
        return ccNumber;
    }

    public void setCcNumber(int ccNumber) {
        this.ccNumber = ccNumber;
    }

    public LocalDate getCcExpirDate() {
        return ccExpirDate;
    }

    public void setCcExpirDate(LocalDate ccExpirDate) {
        this.ccExpirDate = ccExpirDate;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public int getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(int phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public String toString(){
        String s = "";

        s += this.studentId + ";" + this.password + ";" + this.ccNumber + ";" + this.name + ";" + this.ccExpirDate + ";" + this.address + ";" + this.department + ";" + this.phoneNumber + ";" + this.directory;
        return s;

    }

}