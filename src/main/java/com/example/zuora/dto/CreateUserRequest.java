package com.example.zuora.dto;

import jakarta.validation.constraints.*;

public class CreateUserRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email")
    private String email;

    @NotBlank(message = "Phone is required")
    private String phone;

    @NotBlank(message = "Address is required")
    private String address1;

    private String address2;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "ZIP code is required")
    private String zipCode;

    @NotBlank(message = "Country is required")
    private String country;

    // Bill To Address
    private String billToName;

    // Sold To Address (separate from Bill To)
    private String soldToAddress1;
    private String soldToAddress2;
    private String soldToCity;
    private String soldToState;
    private String soldToZipCode;
    private String soldToCountry;

    // Flag to indicate if Sold To is same as Bill To
    private boolean sameAsBillTo = true;

    @NotNull(message = "Please select a membership plan")
    private Long ratePlanId;

    // Getters and Setters
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress1() { return address1; }
    public void setAddress1(String address1) { this.address1 = address1; }

    public String getAddress2() { return address2; }
    public void setAddress2(String address2) { this.address2 = address2; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getBillToName() { return billToName; }
    public void setBillToName(String billToName) { this.billToName = billToName; }

    public String getSoldToAddress1() { return soldToAddress1; }
    public void setSoldToAddress1(String soldToAddress1) { this.soldToAddress1 = soldToAddress1; }

    public String getSoldToAddress2() { return soldToAddress2; }
    public void setSoldToAddress2(String soldToAddress2) { this.soldToAddress2 = soldToAddress2; }

    public String getSoldToCity() { return soldToCity; }
    public void setSoldToCity(String soldToCity) { this.soldToCity = soldToCity; }

    public String getSoldToState() { return soldToState; }
    public void setSoldToState(String soldToState) { this.soldToState = soldToState; }

    public String getSoldToZipCode() { return soldToZipCode; }
    public void setSoldToZipCode(String soldToZipCode) { this.soldToZipCode = soldToZipCode; }

    public String getSoldToCountry() { return soldToCountry; }
    public void setSoldToCountry(String soldToCountry) { this.soldToCountry = soldToCountry; }

    public boolean isSameAsBillTo() { return sameAsBillTo; }
    public void setSameAsBillTo(boolean sameAsBillTo) { this.sameAsBillTo = sameAsBillTo; }

    public Long getRatePlanId() { return ratePlanId; }
    public void setRatePlanId(Long ratePlanId) { this.ratePlanId = ratePlanId; }
}
