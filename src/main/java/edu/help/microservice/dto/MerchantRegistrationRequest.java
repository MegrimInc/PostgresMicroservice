package edu.help.microservice.dto;

public class MerchantRegistrationRequest {
    private String companyName;
    private String companyNickname;
    private String city;
    private String stateOrProvince;
    private String address;
    private String country;
    private String zipCode;
    private String email;
    private String password;
    private String verificationCode;

    // Getters and Setters
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getCompanyNickname() { return companyNickname; }
    public void setCompanyNickname(String companyNickname) { this.companyNickname = companyNickname; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getStateOrProvince() { return stateOrProvince; }
    public void setStateOrProvince(String stateOrProvince) { this.stateOrProvince = stateOrProvince; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }
}
